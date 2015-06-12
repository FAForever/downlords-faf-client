package com.faforever.client.notification;

import javafx.event.ActionEvent;

/**
 * When displaying a notification, there may be associated actions consisting of a title and an action.
 */
public abstract class Action {

  private String title;

  public Action(String title) {
    this.title = title;
  }

  public void onDismissed() {
  }

  public abstract void call(ActionEvent event);
}
