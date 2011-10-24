package com.cp.test;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.atmosphere.annotation.Suspend;
import org.atmosphere.annotation.Suspend.SCOPE;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListener;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.jersey.Broadcastable;

@Path("resource")
//@Consumes({MediaType.APPLICATION_JSON})
//@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.TEXT_PLAIN})
@Produces({MediaType.TEXT_PLAIN})
public class SomeResource {
	
	public static final String DATA = "data-.... " +
	"the long this is the faster the OOM occurs .... " +
	"asdflkjsdaflkjasdlfkj aasdlkj sdfa" +
	"asdflkjlokjolkajsdf lkoasjdfo  aosdfljlosadfjolk jsadlokjfasdf  oasdfjlkoja sdf  " +
	"asflkjjifjoiureoiqrzhvfbdkfjdbfk afkjdshv fdjskbvn vdkndfjv nfdjkvnv fvfdknvf dnkjfd" +
	"aycxvkjibadsvkjhsdvah akjshv jkadsh vkjh dsvkh kjdsavh dshkavj kdsjavh kja vhsd" +
	"vdaskjhsdkvj  vkdsjh khasdvjksdha vkdsajh vdsavdsakjh skjdavh" +
	"dvaslh dskjvdslkvjds vdslkvjds vljdsalkv jdasvlkjdv lsdakvjdslavkjds vlkjsadvlk" +
	"vdsalkvj dsalvj vds kdsjvlkdsaj vlkdsjvjsdav dsalvskjavsd vlkj vdsa" +
	"asdvlkjdsv v ladsjkvsd avlksdj vnv";
	
	/**
	 * a comet-read-request. 
	 * @param jaxbe
	 * @param bc
	 * @return
	 */
	@Path("/read")
	@POST
	@Suspend(scope = SCOPE.REQUEST, resumeOnBroadcast = true, outputComments = true, listeners = Listener.class)
	//@Resume(value=1)
	public Broadcastable readEntries(
			String reqBody, 
			@HeaderParam("X-RequestId") final Broadcaster bc) {

		final CountDownLatch latch = new CountDownLatch(1);
		latches.put(bc, latch);
		Broadcastable broadcastable = new Broadcastable(null, bc);
		new Thread() {
			public void run() {
				try {
					latch.await();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				bc.broadcast(DATA);
			}
		}.start();
		return broadcastable;
	}
	
	private static Map<Broadcaster, CountDownLatch> latches = new IdentityHashMap<Broadcaster, CountDownLatch>();
	
	public static class Listener implements AtmosphereResourceEventListener {

		@Override
		public void onSuspend(
				AtmosphereResourceEvent<HttpServletRequest, HttpServletResponse> event) {
//			log.info("on suspend: " + event);
			Broadcaster bc = event.getResource().getBroadcaster();
			CountDownLatch l = latches.get(bc);
			//log.info("suspend broadcaster " + bc.getID());
			if(l == null) {
				System.err.println("the latch is null for id: " + bc.getID());
			} else {
				l.countDown();
				CountDownLatch rem = latches.remove(bc);
				if(rem == null)
					throw new IllegalStateException("some broadcaster got suspended that wasn't used in the resource-method");
			}
		}

		@Override
		public void onResume(
				AtmosphereResourceEvent<HttpServletRequest, HttpServletResponse> event) {
			//log.info("on resumed: " + event);
		}

		@Override
		public void onDisconnect(
				AtmosphereResourceEvent<HttpServletRequest, HttpServletResponse> event) {
			System.out.println("on disconnect: " + event);
		}

		@Override
		public void onBroadcast(
				AtmosphereResourceEvent<HttpServletRequest, HttpServletResponse> event) {
			//System.out.println("on broadcast: " + event);
		}

		@Override
		public void onThrowable(
				AtmosphereResourceEvent<HttpServletRequest, HttpServletResponse> event) {
			System.out.println("on throwable: " + event);
		}

		
	}
}
