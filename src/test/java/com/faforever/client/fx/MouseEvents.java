package com.faforever.client.fx;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public final class MouseEvents {

  private MouseEvents() {
  }

  public static MouseEvent generateClick(MouseButton button, int clicks) {
    return new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, button, clicks, false, false, false, false, false, false, false, false, false, false, null);
  }

  /**
   * @param x the x with respect to the node.
   * @param y the y with respect to the node.
   */
  public static MouseEvent generateMouseMoved(int x, int y) {
    return new MouseEvent(MouseEvent.MOUSE_MOVED, x, y, x, y, null, 0, false, false, false, false, false, false, false, false, false, false, null);
  }
}
