package com.faforever.client.remote.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum MatchmakingState {
  @JsonProperty("start") START,
  @JsonProperty("stop") STOP
}