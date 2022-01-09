package com.faforever.client.test.contextmenu;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;
import org.springframework.context.ApplicationContext;

public class ContextMenuBuilder {

  public static Builder newBuilder(ApplicationContext context) {
    return new Builder(context);
  }

  public static class Builder {

    private final ApplicationContext context;

    private final ContextMenu contextMenu = new ContextMenu();
    private SeparatorMenuItem separator;
    private BooleanProperty separatorVisibleProperty;
    private BooleanBinding booleanBinding;

    private Builder(ApplicationContext context) {
      this.context = context;
    }

    public <T> Builder addItem(AbstractMenuItem<T> item) {
      return addItem(item, null);
    }

    public <T> Builder addItem(AbstractMenuItem<T> item, T object) {
      item.setObject(object);
      item.setContext(context);
      contextMenu.getItems().add(item);
      bindVisiblePropertyToSeparator(item.visibleProperty());
      return this;
    }

    public <T> Builder addCustomItem(AbstractCustomMenuController<T> item) {
      return addCustomItem(item, null);
    }

    public <T> Builder addCustomItem(AbstractCustomMenuController<T> item, T object) {
      item.setObject(object);
      contextMenu.getItems().add(item.getRoot());
      bindVisiblePropertyToSeparator(item.getRoot().visibleProperty());
      return this;
    }

    private <T> void bindVisiblePropertyToSeparator(BooleanProperty visibleProperty) {
      if (separator != null) {
        if (separatorVisibleProperty == null) {
          separator.visibleProperty().bind(visibleProperty);
          separatorVisibleProperty = visibleProperty;
        } else {
          if (booleanBinding == null) {
            booleanBinding = separatorVisibleProperty.or(visibleProperty);
          } else {
            booleanBinding = booleanBinding.or(visibleProperty);
          }
          separator.visibleProperty().bind(booleanBinding);
        }
      }
    }

    public Builder addSeparator() {
      separator = new SeparatorMenuItem();
      separatorVisibleProperty = null;
      booleanBinding = null;
      contextMenu.getItems().add(separator);
      return this;
    }

    public ContextMenu build() {
      return contextMenu;
    }
  }
}
