package com.faforever.client.main;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigationItem;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class StartTabChooseController extends AbstractViewController<Parent> {
  private final I18n i18n;
  public ComboBox<NavigationItem> tabItemChoiceBox;
  public HBox root;

  @Override
  public void initialize() {
    tabItemChoiceBox.setConverter(new StringConverter<>() {
      @Override
      public String toString(NavigationItem navigationItem) {
        return i18n.get(navigationItem.getI18nKey());
      }

      @Override
      public NavigationItem fromString(String s) {
        throw new UnsupportedOperationException("Not needed");
      }
    });
    tabItemChoiceBox.setItems(FXCollections.observableArrayList(NavigationItem.values()));
    tabItemChoiceBox.getSelectionModel().select(0);
  }

  public NavigationItem getSelected() {
    return tabItemChoiceBox.getSelectionModel().getSelectedItem();
  }

  @Override
  public Parent getRoot() {
    return root;
  }
}
