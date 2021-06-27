package com.faforever.client.serialization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import javafx.scene.paint.Color;

public abstract class ColorMixin {

  @JsonCreator
  static Color web(String value) {
    return null;
  }

  @JsonValue
  public abstract String toString();
}
