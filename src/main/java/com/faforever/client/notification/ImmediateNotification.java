package com.faforever.client.notification;

import java.util.List;

/**
 * A notification that requires the user's immediate attention. It is displayed until the user performs a suggested
 * action or dismisses it. The notification consists of a title, a text, an optional image and zero or more actions.
 */
public class ImmediateNotification {

  private final String title;
  private final String text;
  private final List<Action> actions;
  private final Severity severity;

  public ImmediateNotification(String title, String text, Severity severity) {
    this(title, text, severity, null);
  }

  public ImmediateNotification(String title, String text, Severity severity, List<Action> actions) {
    this.title = title;
    this.text = text;
    this.severity = severity;
    this.actions = actions;
  }

  public String getTitle() {
    return title;
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
}
