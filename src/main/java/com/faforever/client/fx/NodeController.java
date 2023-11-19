package com.faforever.client.fx;

import com.faforever.client.main.event.NavigateEvent;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;


public abstract non-sealed class NodeController<ROOT extends Node> implements Controller<ROOT> {

  protected BooleanExpression showing;

  @Override
  public final void initialize() {
    ROOT root = getRoot();
    ObservableValue<Boolean> attached = root.sceneProperty()
                                            .flatMap(Scene::windowProperty)
                                            .flatMap(Window::showingProperty)
                                            .orElse(false);

    showing = root.visibleProperty().and(BooleanExpression.booleanExpression(attached));
    showing.subscribe(isShowing -> {
      if (isShowing) {
        onShow();
      } else {
        onHide();
      }
    });

    onInitialize();
  }

  /**
   * Subclasses may override in order to perform actions when the controller is being initialized.
   */
  protected void onInitialize() {
    // To be overridden by subclass
  }

  public final void display(NavigateEvent navigateEvent) {
    getRoot().setVisible(true);
    onNavigate(navigateEvent);
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

  /**
   * Subclasses may override in order to perform actions when the controller is no longer being displayed.
   */
  protected void onHide() {
    // To be overridden by subclass
  }

  /**
   * Subclasses may override in order to perform actions when the controller is being displayed.
   */
  protected void onShow() {
    // To be overridden by subclass
  }
}
