package com.faforever.client.filter;

import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ui.RemovableListCell;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.list.NoFocusModelListView;
import com.faforever.client.ui.list.NoSelectionModelListView;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class MutableListFilterController<T> extends AbstractFilterNodeController<List<String>, ListProperty<String>, T> {

  private final UiService uiService;
  private final I18n i18n;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public MenuButton root;
  public ListView<String> listView;
  public TextField addItemTextField;

  private final ListProperty<String> itemListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(listView);
    listView.setSelectionModel(new NoSelectionModelListView<>());
    listView.setFocusModel(new NoFocusModelListView<>());

    JavaFxUtil.addAndTriggerListener(itemListProperty, observable -> {
      listView.setItems(itemListProperty.getValue());
      JavaFxUtil.bind(listView.visibleProperty(), itemListProperty.emptyProperty().not());
    });
    listView.setCellFactory(param -> new RemovableListCell<>(uiService, fxApplicationThreadExecutor));
  }

  public void onAddItem() {
    if (!addItemTextField.getText().isEmpty()) {
      itemListProperty.getValue().add(addItemTextField.getText());
      addItemTextField.clear();
    }
  }

  @Override
  public boolean hasDefaultValue() {
    return itemListProperty.getValue().isEmpty();
  }

  @Override
  public void resetFilter() {
    itemListProperty.getValue().clear();
  }

  public void setText(String text) {
    JavaFxUtil.bind(root.textProperty(), itemListProperty.map(items -> String.join(", ", items))
        .map(strings -> i18n.get("filter.category", text, strings)));
  }

  public void setPromptText(String promptText) {
    addItemTextField.setPromptText(promptText);
  }

  @Override
  public ListProperty<String> valueProperty() {
    return itemListProperty;
  }

  @Override
  protected List<String> getValue() {
    return itemListProperty.getValue();
  }

  @Override
  public MenuButton getRoot() {
    return root;
  }
}
