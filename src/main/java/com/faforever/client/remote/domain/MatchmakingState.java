package com.faforever.client.remote.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public enum MatchmakingState {
  @JsonProperty("start") START("start"),
  @JsonProperty("stop") STOP("stop");

  @Getter
  private final String string;

  MatchmakingState(String string) {
    this.string = string;
  }
}