package com.faforever.client.test.contextmenu;

import com.faforever.client.fx.Controller;
import javafx.scene.control.CustomMenuItem;
import lombok.Getter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public abstract class AbstractCustomMenuItemController<T> implements Controller<CustomMenuItem> {

  public CustomMenuItem root;

  @Getter
  private T object;

  public final void setObject(T object) {
    this.object = object;
    afterSetObject();
  }

  public abstract void afterSetObject();

  @Override
  public CustomMenuItem getRoot() {
    return root;
  }
}
