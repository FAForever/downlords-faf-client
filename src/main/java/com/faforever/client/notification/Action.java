package com.faforever.client.notification;

import javafx.event.ActionEvent;

/**
 * Notifications have actions associated with it. This class represents such an action, which is usually displayed to
 * the user as a button.
 */
public class Action {

  public interface ActionCallback {

    void call(ActionEvent event);
  }

  public enum Type {
    OK_DONE
  }

  private final String title;
  private final ActionCallback callback;
  private final Type type;

  /**
   * Creates an action that does nothing.
   */
  public Action(String title) {
    this(title, Type.OK_DONE, null);
  }

  /**
   * Creates an action that calls the specified callback when executed. The action will have the default action type
   * {@link com.faforever.client.notification.Action.Type#OK_DONE}.
   */
  public Action(String title, ActionCallback callback) {
    this(title, Type.OK_DONE, callback);
  }

  /**
   * Creates an action that calls the specified callback when executed. Also, a type is specified that
   */
  public Action(String title, Type type, ActionCallback callback) {
    this.title = title;
    this.type = type;
    this.callback = callback;
  }

  /**
   * Calls the specified callback, if any. Subclasses may override.
   */
  public void call(ActionEvent event) {
    if (callback != null) {
      callback.call(event);
    }
  }

  public String getTitle() {
    return title;
  }

  public Type getType() {
    return type;
  }
}
