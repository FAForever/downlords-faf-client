package com.faforever.client.replay;

import java.util.HashMap;

public enum ReplayAction {
  LIST_RECENTS("list_recents");

  private static final HashMap<String, ReplayAction> fromString;

  static {
    fromString = new HashMap<>();
    for (ReplayAction replayAction : values()) {
      fromString.put(replayAction.string, replayAction);
    }
  }

  private final String string;

  ReplayAction(String string) {
    this.string = string;
  }

  public static ReplayAction fromString(String string) {
    return fromString.get(string);
  }
}
