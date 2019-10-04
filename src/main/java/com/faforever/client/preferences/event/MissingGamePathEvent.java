package com.faforever.client.preferences.event;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
public class MissingGamePathEvent {
  private boolean immediateUserActionRequired;

  public MissingGamePathEvent(boolean immediateUserActionRequired) {
    this.immediateUserActionRequired = immediateUserActionRequired;
  }

  public MissingGamePathEvent() {
    this(false);
  }
}
