package com.faforever.client.fx;

import javafx.beans.binding.BooleanExpression;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;


public abstract non-sealed class MenuItemController<ITEM extends MenuItem> implements Controller<ITEM> {

  protected BooleanExpression attached;
  protected BooleanExpression showing;

  @Override
  public final void initialize() {
    ITEM root = getRoot();
    attached = BooleanExpression.booleanExpression(
        root.parentMenuProperty().flatMap(Menu::showingProperty).orElse(false));
    attached.subscribe(isAttached -> {
      if (isAttached) {
        onAttached();
      } else {
        onDetached();
      }
    });

    showing = root.visibleProperty().and(attached);
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

  /**
   * Subclasses may override in order to perform actions when the controller is being removed from a menu.
   */
  protected void onDetached() {
    // To be overridden by subclass
  }

  /**
   * Subclasses may override in order to perform actions when the controller is being added to a menu.
   */
  protected void onAttached() {
    // To be overridden by subclass
  }
}
