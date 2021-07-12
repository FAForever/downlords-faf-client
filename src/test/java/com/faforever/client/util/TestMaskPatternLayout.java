package com.faforever.client.util;


import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class TestMaskPatternLayout extends ServiceTest {

  private MaskPatternLayout instance;

  @BeforeEach
  public void set() {
    instance = new MaskPatternLayout();
  }

  @Test
  public void testStringMasking() {
    String userProfile = System.getProperty("user.home");
    String userName = System.getProperty("user.name");
    String machineName;
    try {
      machineName = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      machineName = "";
    }

    String logMessage = String.format("%ssdfe%segfd%seew", userProfile, userName, machineName);
    String cleanLogMessage = instance.maskMessage(logMessage);

    assertThat(cleanLogMessage, not(containsString(userProfile)));
    assertThat(cleanLogMessage, not(containsString(userName)));
    assertThat(cleanLogMessage, not(containsString(machineName)));

  }
}
