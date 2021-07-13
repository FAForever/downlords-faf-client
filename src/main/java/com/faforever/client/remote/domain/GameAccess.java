package com.faforever.client.remote.domain;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum GameAccess {
  @JsonProperty("public") @JsonEnumDefaultValue PUBLIC,
  @JsonProperty("password") PASSWORD
}
