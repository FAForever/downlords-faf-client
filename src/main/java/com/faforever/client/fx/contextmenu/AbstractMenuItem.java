package com.faforever.client.fx.contextmenu;

import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
      setIcon();
    } else {
      setVisible(false);
    }
  }

  private void setIcon() {
    String resourceUrl = getIconResourceUrl();
    if (resourceUrl == null) {
      return;
    }

    int defaultSize = 16;
    Image icon = new Image(resourceUrl, defaultSize, defaultSize, true, true);
    ImageView iconView = new ImageView(icon);
    iconView.setFitWidth(defaultSize);
    iconView.setFitHeight(defaultSize);
    setGraphic(new ImageView(icon));
  }

  protected abstract void onClicked();

  protected abstract String getItemText();

  protected String getIconResourceUrl() {
    return null; // by-default;
  }

  protected boolean isItemVisible() {
    return true; // by-default;
  }
}
