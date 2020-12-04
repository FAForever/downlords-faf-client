package com.faforever.client.remote.domain;

import java.util.Arrays;

public enum MatchmakingState {
  START("start"), STOP("stop");

  private final String string;

  MatchmakingState(String string) {
    this.string = string;
  }

  public static MatchmakingState fromString(String string) {
    return Arrays.stream(MatchmakingState.values())
        .filter(s -> s.getString().equals(string))
        .findFirst()
        .orElse(null);
  }

  public String getString() {
    return string;
  }
}