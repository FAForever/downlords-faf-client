package com.faforever.client.games;

import javafx.scene.control.Skin;
import javafx.scene.control.ToggleButton;

public class LadderButton extends ToggleButton {

  @Override
  protected Skin<?> createDefaultSkin() {
    return new LadderButtonSkin(this);
  }
}
