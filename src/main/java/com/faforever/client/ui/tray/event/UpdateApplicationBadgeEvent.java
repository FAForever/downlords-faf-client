package com.faforever.client.ui.tray.event;

import java.util.Optional;

/**
 * An application badge is a small icon that indicates new activity in the application, like unread messages. This event
 * either increments (or decrements, if negative) the badge number by a specified delta or specifies the new value.
 */
public final class UpdateApplicationBadgeEvent {
  private final Integer delta;
  private final Integer newValue;

  private UpdateApplicationBadgeEvent(Integer delta, Integer newValue) {
    this.delta = delta;
    this.newValue = newValue;
  }

  public static UpdateApplicationBadgeEvent ofDelta(int delta) {
    return new UpdateApplicationBadgeEvent(delta, null);
  }

  public static UpdateApplicationBadgeEvent ofNewValue(int newValue) {
    return new UpdateApplicationBadgeEvent(null, newValue);
  }

  public Optional<Integer> getDelta() {
    return Optional.ofNullable(delta);
  }

  public Optional<Integer> getNewValue() {
    return Optional.ofNullable(newValue);
  }
}
