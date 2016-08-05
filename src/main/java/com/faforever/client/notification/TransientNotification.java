package com.faforever.client.notification;

import com.faforever.client.notification.Action.ActionCallback;
import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A notification that is displayed for a short amount of time, or until the user the user performs a suggested actionCallback
 * or dismisses it. The notification consist of a text, an optional image, an optional actionCallback and is always
 * rendered with a close button. The actionCallback is executed when the user clicks on the notification.
 */
public class TransientNotification implements Comparable<TransientNotification> {

  private final String title;
  private final String text;
  private final Image image;
  private final ActionCallback actionCallback;

  public TransientNotification(String title, String text) {
    this(title, text, null, null);
  }

  public TransientNotification(String title, String text, Image image, ActionCallback actionCallback) {
    this.title = title;
    this.text = text;
    this.image = image;
    this.actionCallback = actionCallback;
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

  public ActionCallback getActionCallback() {
    return actionCallback;
  }

  @Override
  public int compareTo(@NotNull TransientNotification o) {
    return text.compareTo(o.text);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, text, image, actionCallback);
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
        Objects.equals(actionCallback, that.actionCallback);
  }
}
