package com.faforever.client.ui.tray.event;

/**
 * An application badge is a small icon that indicates new activity in the application, like unread messages. This event
 * increments (or decrements, if negative) the badge number by a specified delta.
 */
public class IncrementApplicationBadgeEvent {
  private final int delta;

  public IncrementApplicationBadgeEvent(int delta) {
    this.delta = delta;
  }

  public int getDelta() {
    return delta;
  }
}
