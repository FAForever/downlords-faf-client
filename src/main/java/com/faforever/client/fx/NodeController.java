package com.faforever.client.fx;

import com.faforever.client.main.event.NavigateEvent;
import javafx.beans.binding.BooleanExpression;
import javafx.scene.Node;


public abstract non-sealed class NodeController<ROOT extends Node> extends Controller<ROOT> {

  @Override
  protected BooleanExpression createAttachedExpression() {
    return AttachedUtil.attachedProperty(getRoot());
  }

  @Override
  protected BooleanExpression createVisibleExpression() {
    return getRoot().visibleProperty();
  }

  public final void display(NavigateEvent navigateEvent) {
    onNavigate(navigateEvent);
    getRoot().setVisible(true);
  }

  /**
   * Subclasses may override in order to perform actions when the controller is being displayed.
   */
  protected void onNavigate(NavigateEvent navigateEvent) {
    // To be overridden by subclass
  }

  public final void hide() {
    getRoot().setVisible(false);
  }
}
