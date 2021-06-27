package com.faforever.client.remote.domain;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum MessageTarget {
  @JsonProperty("game") GAME,
  @JsonProperty("client") @JsonEnumDefaultValue CLIENT,
  @JsonProperty("lobby") LOBBY
}
