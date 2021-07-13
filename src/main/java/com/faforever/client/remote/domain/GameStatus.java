package com.faforever.client.remote.domain;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum GameStatus {
  @JsonProperty("unknown") @JsonEnumDefaultValue UNKNOWN,
  @JsonProperty("playing") PLAYING,
  @JsonProperty("open") OPEN,
  @JsonProperty("closed") CLOSED;
}
