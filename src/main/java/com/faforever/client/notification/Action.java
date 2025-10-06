package com.faforever.client.notification;

import lombok.Getter;

/**
 * Notifications have actions associated with it. This class represents such an onAction, which is usually displayed to
 * the user as a button.
 */
public class Action {

  @Getter
  private final String title;
  private final Runnable onAction;
  @Getter
  private final Type type;

  /**
   * Creates an onAction that calls the specified callback when executed. Also, a type is specified that
   */
  public Action(String title, Type type, Runnable onAction) {
    this.title = title;
    this.type = type;
    this.onAction = onAction;
  }

  /**
   * Creates an onAction that does nothing.
   */
  public Action(String title) {
    this(title, Type.OK_DONE, null);
  }

  /**
   * Creates an onAction that calls the specified callback when executed. The onAction will have the default onAction type
   * {@link com.faforever.client.notification.Action.Type#OK_DONE}.
   */
  public Action(String title, Runnable onAction) {
    this(title, Type.OK_DONE, onAction);
  }

  /**
   * Calls the specified callback, if any. Subclasses may override.
   */
  public void run() {
    if (onAction != null) {
      onAction.run();
    }
  }

  public enum Type {
    OK_DONE,
    OK_STAY
  }
}
