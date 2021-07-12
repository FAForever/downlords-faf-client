package com.faforever.client.util;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

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

  public String maskMessage(String message) {
    return message
        .replaceAll("(?i)" + Pattern.quote(userProfile), "%USER_PROFILE%")
        .replaceAll("(?i)" + Pattern.quote(machineName), "%CPU_NAME%")
        .replaceAll("(?i)" + Pattern.quote(user), "%USER%");
  }
}
