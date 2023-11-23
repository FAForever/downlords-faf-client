package com.faforever.client.fx;

import javafx.beans.binding.BooleanExpression;
import javafx.scene.control.MenuItem;


public abstract non-sealed class MenuItemController<ITEM extends MenuItem> extends Controller<ITEM> {

  @Override
  protected BooleanExpression createAttachedExpression() {
    return AttachedUtil.attachedProperty(getRoot());
  }

  @Override
  protected BooleanExpression createVisibleExpression() {
    return getRoot().visibleProperty();
  }
}
