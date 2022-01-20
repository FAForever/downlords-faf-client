package com.faforever.client.vault.replay;

import javafx.scene.control.SplitMenuButton;

import java.util.function.Supplier;

public class WatchLiveReplaySplitMenuButton extends SplitMenuButton {

  private Supplier<Boolean> isCannotWatchSupplier;

  @Override
  public void show() {
    if (isCannotWatchSupplier.get()) {
      super.show();
    }
  }

  public void setIsCannotWatchSupplier(Supplier<Boolean> supplier) {
    isCannotWatchSupplier = supplier;
  }
}
