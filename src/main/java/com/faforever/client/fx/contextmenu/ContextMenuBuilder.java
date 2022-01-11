package com.faforever.client.fx.contextmenu;

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

    private final ApplicationContext applicationContext;

    private final ContextMenu contextMenu = new ContextMenu();
    private SeparatorMenuItem separator;
    private BooleanProperty firstItemVisibleProperty;
    private BooleanBinding totalVisibleBinding;

    private Builder(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
    }

    public <T, B extends AbstractMenuItem<T>> Builder addItem(Class<B> clazz) {
      return addItem(clazz, null);
    }

    public <T, B extends AbstractMenuItem<T>> Builder addItem(Class<B> clazz, T object) {
      B item = applicationContext.getBean(clazz);
      item.setObject(object);
      contextMenu.getItems().add(item);
      bindVisiblePropertyToSeparator(item.visibleProperty());
      return this;
    }

    public <T> Builder addCustomItem(AbstractCustomMenuItemController<T> item) {
      return addCustomItem(item, null);
    }

    public <T> Builder addCustomItem(AbstractCustomMenuItemController<T> item, T object) {
      item.setObject(object);
      contextMenu.getItems().add(item.getRoot());
      bindVisiblePropertyToSeparator(item.getRoot().visibleProperty());
      return this;
    }

    private void bindVisiblePropertyToSeparator(BooleanProperty visibleProperty) {
      if (separator != null) {
        if (totalVisibleBinding == null) {
          totalVisibleBinding = (BooleanBinding) BooleanBinding.booleanExpression(visibleProperty);
        } else {
          totalVisibleBinding = totalVisibleBinding.or(visibleProperty);
        }
        separator.visibleProperty().bind(totalVisibleBinding);
      }
    }

    public Builder addSeparator() {
      separator = new SeparatorMenuItem();
      firstItemVisibleProperty = null;
      totalVisibleBinding = null;
      contextMenu.getItems().add(separator);
      return this;
    }

    public ContextMenu build() {
      return contextMenu;
    }
  }
}
