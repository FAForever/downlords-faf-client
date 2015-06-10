package com.faforever.client.notification;

import javafx.scene.image.Image;

/**
 * Toasts are small notification boxes that are usually displayed in the top right or bottom right corner of a screen.
 * Toasts consist of a title, a message and an optional image. Toasts disappear automatically after some seconds and are
 * displayed with a close button.
 */
public class ToastNotification {

  private String title;
  private String message;
  private Image image;

  public ToastNotification(String title, String message) {
    this.title = title;
    this.message = message;
  }

  public ToastNotification(String title, String message, Image image) {
    this.title = title;
    this.message = message;
    this.image = image;
  }

}
