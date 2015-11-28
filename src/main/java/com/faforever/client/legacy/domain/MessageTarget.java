package com.faforever.client.legacy.domain;

import java.util.HashMap;
import java.util.Map;

public enum MessageTarget {
  GAME("game"),
  CONNECTIVITY("connectivity"),
  CLIENT(null);

  private static final Map<String, MessageTarget> fromString;

  static {
    fromString = new HashMap<>();
    for (MessageTarget messageTarget : values()) {
      fromString.put(messageTarget.string, messageTarget);
    }
  }

  private String string;

  MessageTarget(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }

  public static MessageTarget fromString(String string) {
    return fromString.get(string);
  }
}
