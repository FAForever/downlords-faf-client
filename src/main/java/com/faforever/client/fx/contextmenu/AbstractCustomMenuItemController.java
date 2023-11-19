package com.faforever.client.fx.contextmenu;

import com.faforever.client.fx.MenuItemController;
import javafx.scene.control.CustomMenuItem;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public abstract class AbstractCustomMenuItemController<T> extends MenuItemController<CustomMenuItem> {

  public CustomMenuItem root;

  protected T object;

  public final void setObject(T object) {
    this.object = object;
    finalizeProperties();
    afterSetObject();
  }

  private void finalizeProperties() {
    if (!getRoot().visibleProperty().isBound()) {
      getRoot().setVisible(isItemVisible());
    }
  }

  public abstract void afterSetObject();

  protected boolean isItemVisible() {
    return true; // by-default;
  }

  @Override
  public CustomMenuItem getRoot() {
    return root;
  }
}
