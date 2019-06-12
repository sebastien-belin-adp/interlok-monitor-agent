package com.adaptris.monitor.agent;

import org.junit.Test;

import com.adaptris.profiler.client.ClientPlugin;

import junit.framework.TestCase;

public class InterlokMonitorPluginFactoryTest extends TestCase {
  
  private InterlokMonitorPluginFactory factory;
  
  public void setUp() throws Exception {
    factory = new InterlokMonitorPluginFactory();
  }
  
  @Test
  public void testCreate() throws Exception {
    assertTrue(factory.getPlugin() instanceof ClientPlugin);
  }

}