package com.faforever.client.fx;

import javafx.beans.binding.BooleanExpression;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.stage.Window;


public abstract non-sealed class TabController implements Controller<Tab> {

  protected BooleanExpression selected;
  protected BooleanExpression attached;

  @Override
  public final void initialize() {
    attached = BooleanExpression.booleanExpression(getRoot().tabPaneProperty()
                                                            .flatMap(Node::sceneProperty)
                                                            .flatMap(Scene::windowProperty)
                                                            .flatMap(Window::showingProperty)
                                                            .orElse(false));
    attached.subscribe(isAttached -> {
      if (isAttached) {
        onAttached();
      } else {
        onDetached();
      }
    });

    selected = getRoot().selectedProperty().and(attached);
    selected.subscribe(isSelected -> {
      if (isSelected) {
        onSelected();
      } else {
        onUnselected();
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

  /**
   * Subclasses may override in order to perform actions when the controller is being removed from a tabPane.
   */
  protected void onDetached() {
    // To be overridden by subclass
  }

  /**
   * Subclasses may override in order to perform actions when the controller is being added to a tabPane.
   */
  protected void onAttached() {
    // To be overridden by subclass
  }

  /**
   * Subclasses may override in order to perform actions when the controller is no longer being displayed.
   */
  protected void onUnselected() {
    // To be overridden by subclass
  }

  /**
   * Subclasses may override in order to perform actions when the controller is being displayed.
   */
  protected void onSelected() {
    // To be overridden by subclass
  }
}
