package com.faforever.client.notification;

import javafx.event.ActionEvent;

/**
 * Notifications have actions associated with it. This class represents such an action.
 */
public class Action {

  public interface ActionCallback {

    void call(ActionEvent event);
  }

  private String title;
  private ActionCallback callback;

  public Action(String title) {
    this.title = title;
  }

  public Action(String title, ActionCallback callback) {
    this.title = title;
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
}
