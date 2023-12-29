package com.faforever.client.notification;

import javafx.scene.Parent;

import java.util.List;

/**
 * A notification that requires the user's immediate attention. It is displayed until the user performs a suggested
 * onAction or dismisses it. The notification consists of a title, a text, an optional image and zero or more actions.
 */
public record ImmediateNotification(
    String title, String text, Severity severity, Throwable throwable, List<Action> actions, Parent customUI
) implements Notification {

  public ImmediateNotification(String title, String text, Severity severity) {
    this(title, text, severity, null);
  }

  public ImmediateNotification(String title, String text, Severity severity, List<Action> actions) {
    this(title, text, severity, null, actions, null);
  }

  public ImmediateNotification(String title, String text, Severity severity, Throwable throwable, List<Action> actions) {
    this(title, text, severity, throwable, actions, null);
  }

  public ImmediateNotification(String title, String text, Severity severity, List<Action> actions, Parent customUI) {
    this(title, text, severity, null, actions, customUI);
  }
}
