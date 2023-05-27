package com.faforever.client.ui.tray.event;

/**
 * An application badge is a small icon that indicates new activity in the application, like unread messages. This event
 * either increments (or decrements, if negative) the badge number by a specified delta or specifies the new value.
 */
public sealed interface UpdateApplicationBadgeEvent {

  static UpdateApplicationBadgeEvent ofDelta(int delta) {
    return new UpdateApplicationBadgeEvent.Delta(delta);
  }

  static UpdateApplicationBadgeEvent ofNewValue(int newValue) {
    return new UpdateApplicationBadgeEvent.NewValue(newValue);
  }

  record Delta(Integer value) implements UpdateApplicationBadgeEvent {}

  record NewValue(Integer value) implements UpdateApplicationBadgeEvent {}
}
