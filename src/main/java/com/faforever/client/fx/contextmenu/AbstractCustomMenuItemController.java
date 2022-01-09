package com.faforever.client.fx.contextmenu;

import com.faforever.client.fx.Controller;
import javafx.scene.control.CustomMenuItem;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public abstract class AbstractCustomMenuItemController<T> implements Controller<CustomMenuItem> {

  public CustomMenuItem root;

  private T object;

  public final void setObject(T object) {
    this.object = object;
    afterSetObject(object);
  }

  public abstract void afterSetObject(T object);

  @Override
  public CustomMenuItem getRoot() {
    return root;
  }
}
