package com.faforever.client.notification;

import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;

/**
 * A notification that is displayed for a short amount of time, or until the user the user performs a suggested
 * onAction or dismisses it. The notification consist of a text, an optional image, an optional onAction and
 * is always rendered with a close button. The onAction is executed when the user clicks on the notification.
 */
public record TransientNotification(
    String title, String text, Image image, Runnable onAction
) implements Comparable<TransientNotification>, Notification {

  public TransientNotification(String title, String text) {
    this(title, text, null, null);
  }

  public TransientNotification(String title, String text, Image image) {
    this(title, text, image, null);
  }

  @Override
  public int compareTo(@NotNull TransientNotification o) {
    return text.compareTo(o.text);
  }
}
