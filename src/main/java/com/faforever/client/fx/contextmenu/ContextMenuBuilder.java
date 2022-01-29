package com.faforever.client.fx.contextmenu;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.SeparatorMenuItem;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
@RequiredArgsConstructor
public class ContextMenuBuilder {

  private final ApplicationContext applicationContext;

  public MenuItemBuilder newBuilder() {
    return new MenuItemBuilder(applicationContext);
  }

  public void addCopyLabelContextMenu(Label... labels) {
    for (Label label : labels) {
      label.setOnContextMenuRequested(event -> newBuilder()
          .addItem(CopyLabelMenuItem.class, label)
          .build()
          .show(label.getScene().getWindow(), event.getScreenX(), event.getScreenY()));
    }
  }

  public static class MenuItemBuilder {

    private final ApplicationContext applicationContext;

    private final ContextMenu contextMenu = new ContextMenu();
    private SeparatorMenuItem separator;
    private BooleanBinding totalVisibleBinding;

    private MenuItemBuilder(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
    }

    public <T, B extends AbstractMenuItem<T>> MenuItemBuilder addItem(Class<B> clazz) {
      return addItem(clazz, null);
    }

    public <T, B extends AbstractMenuItem<T>> MenuItemBuilder addItem(Class<B> clazz, T object) {
      B item = applicationContext.getBean(clazz);
      item.setObject(object);
      contextMenu.getItems().add(item);
      bindVisiblePropertyToSeparator(item.visibleProperty());
      return this;
    }

    public <T> MenuItemBuilder addCustomItem(AbstractCustomMenuItemController<T> item) {
      return addCustomItem(item, null);
    }

    public <T> MenuItemBuilder addCustomItem(AbstractCustomMenuItemController<T> item, T object) {
      item.setObject(object);
      contextMenu.getItems().add(item.getRoot());
      bindVisiblePropertyToSeparator(item.getRoot().visibleProperty());
      return this;
    }

    private void bindVisiblePropertyToSeparator(BooleanProperty visibleProperty) {
      if (separator != null) {
        if (totalVisibleBinding == null) {
          totalVisibleBinding = Bindings.createBooleanBinding(visibleProperty::get, visibleProperty);
        } else {
          totalVisibleBinding = totalVisibleBinding.or(visibleProperty);
        }
        separator.visibleProperty().unbind();
        separator.visibleProperty().bind(totalVisibleBinding);
      }
    }

    public MenuItemBuilder addSeparator() {
      separator = new SeparatorMenuItem();
      totalVisibleBinding = null;
      contextMenu.getItems().add(separator);
      return this;
    }

    public ContextMenu build() {
      return contextMenu;
    }
  }
}
