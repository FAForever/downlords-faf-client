package com.faforever.client.fx;

import javafx.beans.binding.BooleanExpression;
import javafx.scene.control.Tab;


public abstract non-sealed class TabController extends Controller<Tab> {

  @Override
  protected BooleanExpression createAttachedExpression() {
    return AttachedUtil.attachedProperty(getRoot());
  }

  @Override
  protected BooleanExpression createVisibleExpression() {
    return getRoot().selectedProperty();
  }
}
