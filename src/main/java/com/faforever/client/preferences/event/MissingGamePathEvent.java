package com.faforever.client.preferences.event;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class MissingGamePathEvent {
  boolean immediateUserActionRequired;

  public MissingGamePathEvent() {
    this(false);
  }
}
