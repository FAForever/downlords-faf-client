package com.faforever.client.serialization;

import com.fasterxml.jackson.annotation.JsonValue;

public abstract class FactionMixin {

  @JsonValue
  private String string;

  @JsonValue(value = false)
  abstract int toFaValue();
}
