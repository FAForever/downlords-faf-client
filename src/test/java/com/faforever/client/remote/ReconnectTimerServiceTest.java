package com.faforever.client.remote;


import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReconnectTimerServiceTest extends ServiceTest {

  @InjectMocks
  private ReconnectTimerService instance;

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