package com.faforever.client.fx.contextmenu;

import javafx.scene.control.MenuItem;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public abstract class AbstractMenuItem<T> extends MenuItem {

  protected T object;

  public final void setObject(T object) {
    this.object = object;
    finalizeProperties();
  }

  private void finalizeProperties() {
    if (isItemVisible()) {
      setOnAction(event -> onClicked());
      setText(getItemText());
    } else {
      setVisible(false);
    }
  }

  protected abstract void onClicked();

  protected abstract String getItemText();

  protected boolean isItemVisible() {
    return true; // by-default;
  }
}
