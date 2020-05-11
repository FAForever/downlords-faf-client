package com.faforever.client.fx;

import com.faforever.client.main.event.NavigateEvent;
import javafx.scene.Node;


public abstract class AbstractViewController<ROOT extends Node> implements Controller<ROOT> {

  public final void display(NavigateEvent navigateEvent) {
    getRoot().setVisible(true);
    onDisplay(navigateEvent);
  }

  /**
   * Subclasses may override in order to perform actions when the view is being displayed.
   */
  protected void onDisplay(NavigateEvent navigateEvent) {
    // To be overridden by subclass
  }

  public final void hide() {
    getRoot().setVisible(false);
    onHide();
  }

  /**
   * Subclasses may override in order to perform actions when the view is no longer being displayed.
   */
  public void onHide() {
    // To be overridden by subclass
  }
}
