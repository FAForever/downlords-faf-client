package com.faforever.client.notification;

import javafx.scene.image.Image;

import java.util.List;

/**
 * A notification that keeps displaying until the user performs a suggested action or dismisses it. The notification
 * consist of a text, an optional image and zero or actions and is always rendered with a close button.
 */
public class PersistentNotification {

  private String message;
  private Image image;
  private List<Action> actions;

  public PersistentNotification(String message) {
    this.message = message;
  }

  public PersistentNotification(String message, Image image) {
    this.message = message;
    this.image = image;
  }

  public PersistentNotification(String message, List<Action> actions) {
    this.message = message;
    this.actions = actions;
  }

  public PersistentNotification(String message, Image image, List<Action> actions) {
    this.message = message;
    this.image = image;
    this.actions = actions;
  }
}
