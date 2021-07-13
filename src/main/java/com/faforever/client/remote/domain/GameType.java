package com.faforever.client.remote.domain;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum GameType {
  @JsonProperty("unknown") @JsonEnumDefaultValue UNKNOWN,
  @JsonProperty("custom") CUSTOM,
  @JsonProperty("matchmaker") MATCHMAKER,
  @JsonProperty("coop") COOP,
  @JsonProperty("tutorial") TUTORIAL;
}
