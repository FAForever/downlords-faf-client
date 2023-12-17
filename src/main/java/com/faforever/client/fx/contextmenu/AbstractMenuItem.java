package com.faforever.client.fx.contextmenu;

import javafx.scene.control.MenuItem;
import javafx.scene.layout.Region;

public abstract class AbstractMenuItem<T> extends MenuItem {

  protected T object;

  public final void setObject(T object) {
    this.object = object;
    finalizeProperties();
  }

  private void finalizeProperties() {
    if (isDisplayed()) {
      setOnAction(event -> onClicked());
      setText(getItemText());
      setIcon();
    } else {
      setVisible(false);
    }
  }

  private void setIcon() {
    String styleIcon = getStyleIcon();
    if (styleIcon != null) {
      setGraphic(initializeIcon(styleIcon));
    }
  }

  private Region initializeIcon(String styleIcon) {
    Region iconView = new Region();
    iconView.getStyleClass().addAll("icon", "icon16x16", styleIcon);
    return iconView;
  }

  protected String getStyleIcon() {
    return null; // by-default
  }

  protected abstract void onClicked();

  protected abstract String getItemText();

  protected boolean isDisplayed() {
    return true; // by-default;
  }
}
