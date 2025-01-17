package com.adaptris.monitor.agent.multicast;

import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adaptris.monitor.agent.EventReceiverListener;
import com.adaptris.monitor.agent.activity.ActivityMap;
import com.adaptris.monitor.agent.activity.AdapterActivity;

import junit.framework.TestCase;

public class MulticastEventReceiverTest extends TestCase {
  
  private static final String DEFAULT_MULTICAST_GROUP = "224.0.0.4";
  private static final int DEFAULT_MULTICAST_PORT = 5577;
  private static final int STANDARD_PACKET_SIZE = 120400;
  
  private MulticastEventReceiver receiver;
  
  @Mock MulticastSocketReceiver mockReceiver;
  
  private final Object monitor = new Object();
    
  @Override
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    
    receiver = new MulticastEventReceiver();
    receiver.setMulticastSocketReceiver(mockReceiver);
  }
  
  public void testReceive() throws Exception {    
    when(mockReceiver.receive(STANDARD_PACKET_SIZE))
        .thenReturn(buildPacket());
    
    receiver.addEventReceiverListener(new EventReceiverListener() {
      @Override
      public void eventReceived(ActivityMap activityMap) {
          monitor.notifyAll();
      }
    });
    receiver.start();
        
    synchronized(monitor) {
      // Wait at most 10 seconds... since multicast doesn't always work.
      monitor.wait(TimeUnit.SECONDS.toMillis(10L));
    }
    receiver.stop();
  }
  
  private DatagramPacket buildPacket() throws Exception {
    ActivityMap activityMap = new ActivityMap();
    activityMap.getAdapters().put("adapter", new AdapterActivity());
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(activityMap);
    oos.flush();
    byte[] data= baos.toByteArray();

    return new DatagramPacket(data, data.length, InetAddress.getByName(DEFAULT_MULTICAST_GROUP), DEFAULT_MULTICAST_PORT);
  }
}
