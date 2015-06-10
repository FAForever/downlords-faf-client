package com.faforever.client.notification;

import javafx.scene.control.Button;
import javafx.scene.image.Image;

import java.util.List;

/**
 * A notification that is displayed at the top of the window and takes its full width. It consist of a text, one or more
 * action buttons, an optional image and is always rendered with a close button. Notification bars are displayed until
 * the user dismisses them.
 */
public class BarNotification {

  private String message;
  private Image image;
  private List<Button> buttons;

  public BarNotification(String message) {
    this.message = message;
  }

  public BarNotification(String message, Image image) {
    this.message = message;
    this.image = image;
  }

  public BarNotification(String message, Image image, List<Button> buttons) {
    this.message = message;
    this.image = image;
    this.buttons = buttons;
  }
}
