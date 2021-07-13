package com.faforever.client.game;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum GameVisibility {
  @JsonProperty("public") @JsonEnumDefaultValue PUBLIC,
  @JsonProperty("friends") PRIVATE
}
