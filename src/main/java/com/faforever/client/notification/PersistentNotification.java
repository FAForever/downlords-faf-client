package com.faforever.client.notification;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A notification that keeps displaying until the user performs a suggested action or dismisses it. The notification
 * consist of a severity, a text and zero or more actions and is always rendered with a close button.
 */
public class PersistentNotification implements Comparable<PersistentNotification> {

  private final String text;
  private final Severity severity;
  private final List<Action> actions;

  public PersistentNotification(String text, Severity severity) {
    this(text, severity, null);
  }

  public PersistentNotification(String text, Severity severity, List<Action> actions) {
    this.text = text;
    this.severity = severity;
    this.actions = actions;
  }

  public String getText() {
    return text;
  }

  public List<Action> getActions() {
    return actions;
  }

  public Severity getSeverity() {
    return severity;
  }

  @Override
  public int compareTo(@NotNull PersistentNotification o) {
    return text.compareTo(o.text);
  }
}
