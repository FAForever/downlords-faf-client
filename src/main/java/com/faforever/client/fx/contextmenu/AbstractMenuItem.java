package com.faforever.client.fx.contextmenu;

import javafx.scene.control.MenuItem;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public abstract class AbstractMenuItem<T> extends MenuItem {

  private T object;

  public final void setObject(T object) {
    this.object = object;
    startItemInitialization();
  }

  private void startItemInitialization() {
    setOnAction(event -> onClicked());
    setText(getItemText());
    setVisible(isItemVisible());
  }

  protected abstract void onClicked();

  protected abstract String getItemText();

  protected boolean isItemVisible() {
    return true; // by-default;
  }

  protected T getUnsafeObject() {
    return object;
  }

  protected T getObject() {
    Assert.notNull(object, "object is null");
    return object;
  }
}
