package com.faforever.client.remote;

import com.google.common.annotations.VisibleForTesting;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ReconnectTimerService {
  /**
   * The maximum time somebody will ever wait to reconnect. Though don't worry in the case you are in a game it will
   * always be RECONNECT_DELAY_FACTOR .
   */
  @VisibleForTesting
  static final long RECONNECT_DELAY_MAX = 80000;
  /**
   * The time we start with waiting till we reconnect. After failures the time will increase exponentially.
   */
  private static final long RECONNECT_DELAY_FACTOR = 3000;
  private int connectionFailures = 0;
  @Setter
  private boolean gameRunning = false;
  private CountDownLatch waitForReconnectLatch;

  void resetConnectionFailures() {
    connectionFailures = 0;
  }

  void incrementConnectionFailures() {
    connectionFailures++;
  }

  @VisibleForTesting
  long getReconnectTimeOut() {
    if (gameRunning) {
      return RECONNECT_DELAY_FACTOR;
    }
    double timeToWait = RECONNECT_DELAY_FACTOR * Math.pow(2, connectionFailures);
    timeToWait = Math.min(timeToWait, RECONNECT_DELAY_MAX);
    return (long) (Math.random() * timeToWait);
  }

  void waitForReconnect() {
    long reconnectTimeOut = getReconnectTimeOut();
    log.warn("Lost connection to FAF server, trying to reconnect in " + reconnectTimeOut / 1000 + "s");
    waitForReconnectLatch = new CountDownLatch(1);
    try {
      waitForReconnectLatch.await(reconnectTimeOut, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      log.debug("Waited and then reconnected");
    }
  }

  void skipWait() {
    waitForReconnectLatch.countDown();
  }
}
