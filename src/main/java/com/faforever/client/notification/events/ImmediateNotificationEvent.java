package com.faforever.client.notification.events;

import com.faforever.client.notification.Action;
import com.faforever.client.notification.Severity;
import javafx.scene.Parent;
import lombok.Data;

import java.util.List;

@Data
public class ImmediateNotificationEvent {
  private final String title;
  private final String text;
  private final Severity severity;
  private final Throwable throwable;
  private final List<Action> actions;
  private final Parent customUI;

  public ImmediateNotificationEvent(String title, String text, Severity severity) {
    this(title, text, severity, null);
  }

  public ImmediateNotificationEvent(String title, String text, Severity severity, List<Action> actions) {
    this(title, text, severity, null, actions, null);
  }

  public ImmediateNotificationEvent(String title, String text, Severity severity, Throwable throwable, List<Action> actions) {
    this(title, text, severity, throwable, actions, null);
  }

  public ImmediateNotificationEvent(String title, String text, Severity severity, List<Action> actions, Parent customUI) {
    this(title, text, severity, null, actions, customUI);
  }

  public ImmediateNotificationEvent(String title, String text, Severity severity, Throwable throwable, List<Action> actions, Parent customUI) {
    this.title = title;
    this.text = text;
    this.severity = severity;
    this.throwable = throwable;
    this.actions = actions;
    this.customUI = customUI;
  }
}
