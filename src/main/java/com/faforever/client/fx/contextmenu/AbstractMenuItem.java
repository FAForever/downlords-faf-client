package com.faforever.client.fx.contextmenu;

import javafx.scene.control.MenuItem;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public abstract class AbstractMenuItem<T> extends MenuItem {

  private T object;

  public final void setObject(T object) {
    this.object = object;
    startItemInitialization();
  }

  private void startItemInitialization() {
    setOnAction(event -> onClicked(object));
    setText(getItemText());
    setVisible(isItemVisible(object));
  }

  protected abstract void onClicked(T object);

  protected abstract String getItemText();

  protected boolean isItemVisible(T object) {
    return true; // by-default;
  }
}
