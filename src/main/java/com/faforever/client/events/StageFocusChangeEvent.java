package com.faforever.client.events;

public class StageFocusChangeEvent {
  private final boolean oldValue;
  private final boolean newValue;

  public StageFocusChangeEvent(boolean oldValue, boolean newValue) {
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  public boolean isOldValue() {
    return oldValue;
  }

  public boolean isNewValue() {
    return newValue;
  }
}
