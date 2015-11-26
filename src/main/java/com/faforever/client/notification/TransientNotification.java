package com.faforever.client.notification;

import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A notification that is displayed for a short amount of time, or until the user the user performs a suggested action
 * or dismisses it. The notification consist of a text, an optional image, an optional action and is always
 * rendered with a close button. The action is executed when the user clicks on the notification.
 */
public class TransientNotification implements Comparable<TransientNotification> {

  private final String title;
  private final String text;
  private final Image image;
  private final Action action;

  public TransientNotification(String title, String text) {
    this(title, text, null, null);
  }

  public TransientNotification(String title, String text, Image image, Action action) {
    this.title = title;
    this.text = text;
    this.image = image;
    this.action = action;
  }

  public TransientNotification(String title, String text, Image image) {
    this(title, text, image, null);
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

  public Action getAction() {
    return action;
  }

  @Override
  public int compareTo(@NotNull TransientNotification o) {
    return text.compareTo(o.text);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, text, image, action);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TransientNotification that = (TransientNotification) o;
    return Objects.equals(title, that.title) &&
        Objects.equals(text, that.text) &&
        Objects.equals(image, that.image) &&
        Objects.equals(action, that.action);
  }
}
