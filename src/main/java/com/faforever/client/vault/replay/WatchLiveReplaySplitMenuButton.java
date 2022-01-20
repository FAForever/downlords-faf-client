package com.faforever.client.vault.replay;

import javafx.scene.Node;
import javafx.scene.control.SplitMenuButton;

import java.util.function.Supplier;

public class WatchLiveReplaySplitMenuButton extends SplitMenuButton {

  private Supplier<Boolean> isUnavailableSupplier;

  @Override
  public void show() {
    if (isUnavailableSupplier.get()) {
      super.show();
    }
  }

  public void setIsUnavailableSupplier(Supplier<Boolean> supplier) {
    isUnavailableSupplier = supplier;
  }

  @Override
  public Node getStyleableNode() {
    return super.getStyleableNode();
  }
}
