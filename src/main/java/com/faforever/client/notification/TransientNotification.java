package com.faforever.client.notification;

import javafx.scene.image.Image;

import java.util.List;

/**
 * A notification that is displayed for a short amount of time, or until the user the user performs a suggested action
 * or dismisses it. The notification consist of a text, an optional image and zero or more actions and is always
 * rendered with a close button.
 */
public class TransientNotification {

  private final String title;
  private final String text;
  private Image image;
  private List<Action> actions;

  public TransientNotification(String title, String text) {
    this(title, text, null, null);
  }

  public TransientNotification(String title, String text, Image image, List<Action> actions) {
    this.title = title;
    this.text = text;
    this.image = image;
    this.actions = actions;
  }

  public String getTitle() {
    return title;
  }

  public String getText() {
    return text;
  }

  public Image getImage() {
    return image;
  }

  public List<Action> getActions() {
    return actions;
  }
}
