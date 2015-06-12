package com.faforever.client.notification;

import javafx.scene.image.Image;

import java.util.List;

/**
 * A notification that is displayed for a short amount of time, or until the user the user performs a suggested action
 * or dismisses it. The notification consist of a text, an optional image and zero or more actions and is always
 * rendered with a close button.
 */
public class TransientNotification {

  private String title;
  private String message;
  private Image image;
  private List<Action> actions;

  public TransientNotification(String title, String message) {
    this.title = title;
    this.message = message;
  }

  public TransientNotification(String title, String message, Image image) {
    this.title = title;
    this.message = message;
    this.image = image;
  }

  public TransientNotification(String title, String message, List<Action> actions) {
    this.title = title;
    this.message = message;
    this.actions = actions;
  }

  public TransientNotification(String title, String message, Image image, List<Action> actions) {
    this.title = title;
    this.message = message;
    this.image = image;
    this.actions = actions;
  }
}
