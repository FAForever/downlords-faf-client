package com.faforever.client.notification.events;

import com.faforever.client.notification.Action.ActionCallback;
import javafx.scene.image.Image;
import lombok.Data;

@Data
public class TransientNotificationEvent {
  private final String title;
  private final String text;
  private final Image image;
  private final ActionCallback actionCallback;

  public TransientNotificationEvent(String title, String text, Image image) {
    this(title, text, image, null);
  }

  public TransientNotificationEvent(String title, String text, Image image, ActionCallback actionCallback) {
    this.title = title;
    this.text = text;
    this.image = image;
    this.actionCallback = actionCallback;
  }
}
