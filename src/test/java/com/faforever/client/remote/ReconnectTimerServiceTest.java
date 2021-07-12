package com.faforever.client.remote;


import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReconnectTimerServiceTest extends ServiceTest {

  private ReconnectTimerService instance;

  @BeforeEach
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