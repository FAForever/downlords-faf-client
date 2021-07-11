package com.faforever.client.util;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MaskPatternLayout extends PatternLayout {
  private final String userProfile;
  private final String machineName;
  private final String user;

  public MaskPatternLayout() {
    userProfile = System.getProperty("user.home");
    user = System.getProperty("user.name");
    String machineName;
    try {
      machineName = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      machineName = "";
    }
    this.machineName = machineName;
  }

  @Override
  public String doLayout(ILoggingEvent event) {
    return maskMessage(super.doLayout(event));
  }

  private String maskMessage(String message) {
    return message
        .replace(userProfile, "%USER_PROFILE%")
        .replace(machineName, "%CPU_NAME%")
        .replace(user, "%USER%");
  }
}
