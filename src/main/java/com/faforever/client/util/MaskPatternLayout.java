package com.faforever.client.util;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

public class MaskPatternLayout extends PatternLayout {
  // Tokens shorter than this are too generic to mask safely - e.g. a single-letter
  // hostname like "A" would match every 'a' in every log line under the (?i) regex
  // and shred readability. The masks exist to redact identifying information from
  // shared logs; a 1-2 character token is not identifying anyway.
  private static final int MIN_MASK_LENGTH = 3;

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
    String masked = message;
    if (isMaskable(userProfile)) {
      masked = masked.replaceAll("(?i)" + Pattern.quote(userProfile), "%USER_PROFILE%");
    }
    if (isMaskable(machineName)) {
      masked = masked.replaceAll("(?i)" + Pattern.quote(machineName), "%CPU_NAME%");
    }
    if (isMaskable(user)) {
      masked = masked.replaceAll("(?i)" + Pattern.quote(user), "%USER%");
    }

    return LogMaskingRegistry.maskMessage(masked);
  }

  private static boolean isMaskable(String token) {
    return token != null && token.length() >= MIN_MASK_LENGTH;
  }
}
