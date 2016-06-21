package com.faforever.client.fx;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public final class MouseEvents {

  private MouseEvents() {
  }

  public static MouseEvent generateClick(MouseButton button, int clicks) {
    return new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, button, clicks, false, false, false, false, false, false, false, false, false, false, null);
  }
}
