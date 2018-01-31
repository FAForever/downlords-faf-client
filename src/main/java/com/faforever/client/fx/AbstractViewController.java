package com.faforever.client.fx;

import javafx.application.Platform;
import javafx.scene.Node;

public abstract class AbstractViewController<ROOT extends Node> implements Controller<ROOT> {

  private Runnable onInitializationCallback;
  private boolean initialized = false;

  public AbstractViewController() {
  }

  public void initialize() {
    if (onInitializationCallback != null) {
      onInitializationCallback.run();
    }
    initialized = true;
    ROOT root = getRoot();
    root.sceneProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != null && root.isVisible()) {
        onDisplay();
      } else {
        onHide();
      }
    });
    root.visibleProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue && root.getScene() != null) {
        onDisplay();
      } else {
        onHide();
      }
    });
  }

  /**
   * Subclasses may override in order to perform actions when the view is being displayed.
   */
  protected void onDisplay() {

  }

  /**
   * Subclasses may override in order to perform actions when the view is no longer being displayed.
   */
  protected void onHide() {

  }

  public void setOnInitialized(Runnable onInitilizationCallback) {
    if (initialized) {
      Platform.runLater(onInitilizationCallback);
    }
    this.onInitializationCallback = onInitilizationCallback;
  }
}
