package com.faforever.client.notification;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A notification that keeps displaying until the user performs a suggested onAction or dismisses it. The notification
 * consist of a severity, a text and zero or more actions and is always rendered with a close button.
 */
public record PersistentNotification(
    String text, Severity severity, List<Action> actions
) implements Comparable<PersistentNotification>, Notification {

  public PersistentNotification(String text, Severity severity) {
    this(text, severity, null);
  }

  @Override
  public int compareTo(@NotNull PersistentNotification o) {
    return text.compareTo(o.text);
  }
}
