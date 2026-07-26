package com.faforever.client.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DeviceCodeResponseTest {

  private DeviceCodeResponse withInterval(Integer interval) {
    return new DeviceCodeResponse("device", "USER-CODE", "https://verify", "https://verify?user_code=USER-CODE", 600,
                                  interval);
  }

  @Test
  public void testNullIntervalDefaultsToFive() {
    assertEquals(5, withInterval(null).interval());
  }

  @Test
  public void testPositiveIntervalIsKept() {
    assertEquals(3, withInterval(3).interval());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  public void testNonPositiveIntervalIsRejected(int interval) {
    assertThrows(IllegalArgumentException.class, () -> withInterval(interval));
  }
}
