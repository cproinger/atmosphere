package com.cp.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.atmosphere.container.Jetty7CometSupport;
import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.cpr.BroadcasterLifeCyclePolicy;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.ListenableFuture;

public class SomeResourceTest {

	private final class MyAsyncHandler implements AsyncHandler<String> {
		private CountDownLatch la = new CountDownLatch(1);
		
		@Override
		public com.ning.http.client.AsyncHandler.STATE onBodyPartReceived(
				HttpResponseBodyPart arg0) throws Exception {
			return STATE.CONTINUE;
		}

		@Override
		public String onCompleted() throws Exception {
			
			return "asdf";
		}

		@Override
		public com.ning.http.client.AsyncHandler.STATE onHeadersReceived(
				HttpResponseHeaders arg0) throws Exception {
			return STATE.CONTINUE;
		}

		@Override
		public com.ning.http.client.AsyncHandler.STATE onStatusReceived(
				HttpResponseStatus arg0) throws Exception {
			return STATE.CONTINUE;
		}

		@Override
		public void onThrowable(Throwable arg0) {
			System.err.println("onThrowable");
			arg0.printStackTrace();
		}
	}

	private Server server;
	private AsyncHttpClient asyncClient;

	@Before
	public void setup() throws Exception {
		server = new Server(9876);

		ServletContextHandler context = new ServletContextHandler(
				ServletContextHandler.SESSIONS);
		// ServletContextHandler context = new
		// ServletContextHandler(ServletContextHandler.NO_SESSIONS);

		// context.setAttribute("mzsCore", core);
		// context.setAttribute("mzsMessageDistributor", messageDistributor);
		// context.setAttribute("serializers", serializers);
		context.setContextPath("/");
		server.setHandler(context);
		AtmosphereServlet atmoServlet = new AtmosphereServlet();

		// atmoServlet.addInitParameter("org.atmosphere.cpr.broadcasterClass",
		// RecyclableBroadcaster.class.getName());

		atmoServlet.addInitParameter("com.sun.jersey.config.property.packages",
				"com.cp.test");
		atmoServlet
				.addInitParameter(
						"org.atmosphere.cpr.broadcasterLifeCyclePolicy",
						// atmoServlet.addInitParameter(ApplicationConfig.BROADCASTER_LIFECYCLE_POLICY,
						BroadcasterLifeCyclePolicy.ATMOSPHERE_RESOURCE_POLICY.EMPTY_DESTROY
								.name());
		// atmoServlet.addInitParameter("org.atmosphere.cpr.broadcasterClass",
		// RecyclableBroadcaster.class.getName());

		// atmoServlet.addInitParameter(JSONConfiguration.FEATURE_POJO_MAPPING,
		// "true");
		atmoServlet.addInitParameter(
				"com.sun.jersey.spi.container.ContainerResponseFilters",
				"com.sun.jersey.server.linking.LinkFilter");
		// atmoServlet.addInitParameter("org.atmosphere.cpr.recoverFromDestroyedBroadcaster",
		// "true");
		// atmoServlet.setCometSupport(new
		// JettyCometSupport(atmoServlet.getAtmosphereConfig()));
		atmoServlet.setCometSupport(new Jetty7CometSupport(atmoServlet
				.getAtmosphereConfig()));
		// atmoServlet.setCometSupport(new
		// JettyCometSupportWithWebSocket(atmoServlet.getAtmosphereConfig()));
		// atmoServlet.setCometSupport(new
		// Jetty8WebSocketSupport(atmoServlet.getAtmosphereConfig()));

		ServletHolder sh = new ServletHolder(atmoServlet);
		context.addServlet(sh, "/*");
		server.start();

		asyncClient = new AsyncHttpClient(new AsyncHttpClientConfig.Builder()
				.setConnectionTimeoutInMs(0).setRequestTimeoutInMs(-1).build());
	}

	@After
	public void tearDown() throws Exception {
		server.stop();
		server.join();
		asyncClient.close();
	}

	/**
	 * i run this with -Xmx12m (0.7.2) and it takes about 10 minutes until the GC
	 * runs constantly without beeing able to free memory. but requests are processed
	 * anyhow just slower than before. 
	 * 
	 * i'm not sure why my application takes so much more memory in comparison to this
	 * test-case, but maybe it's my XStream Serializer is to blame for that.  
	 * 
	 */
	@Test
	public void testReadEntries() throws IllegalArgumentException, IOException, InterruptedException, ExecutionException {
		int rqId = 1;
		for (int i = 0; i < 10000000; i++) {
			rqId++;
			BoundRequestBuilder rb = asyncClient.preparePost(
					"http://localhost:9876/resource/read").addHeader(
					"X-RequestId", "requestssssssssssssssssssssssssssssssssssssss/" + rqId);
			MyAsyncHandler handler = new MyAsyncHandler();
			ListenableFuture<String> future = rb.setBody(SomeResource.DATA.getBytes()).execute(handler);
			future.get();
			System.out.println("request " + rqId + " done");
		}
		// asyncClient.close();
	}

}
