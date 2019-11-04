package com.faforever.client.remote;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ReconnectTimerServiceTest {

  private ReconnectTimerService instance;

  @Before
  public void setUp() throws Exception {
    instance = new ReconnectTimerService();
  }

  @Test
  public void testReconnectStaysBelowMax() {
    instance.incrementConnectionFailures();
    instance.incrementConnectionFailures();
    instance.incrementConnectionFailures();
    instance.incrementConnectionFailures();
    instance.incrementConnectionFailures();
    instance.incrementConnectionFailures();
    assertTrue(instance.getReconnectTimeOut() <= ReconnectTimerService.RECONNECT_DELAY_MAX);
  }
}