package com.faforever.client.filter;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ui.RemovableListCellController;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.list.NoFocusModelListView;
import com.faforever.client.ui.list.NoSelectionModelListView;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
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

  public MenuButton root;
  public ListView<String> listView;
  public TextField addItemTextField;

  private final ListProperty<String> property = new SimpleListProperty<>(FXCollections.observableArrayList());
  private boolean bound;

  @Override
  public void initialize() {
    JavaFxUtil.bindManagedToVisible(listView);
    listView.setSelectionModel(new NoSelectionModelListView<>());
    listView.setFocusModel(new NoFocusModelListView<>());

    JavaFxUtil.addAndTriggerListener(property, (InvalidationListener) observable -> {
      listView.setItems(property.getValue());
      JavaFxUtil.bind(listView.visibleProperty(), property.emptyProperty().not());
    });
    listView.setCellFactory(param -> uiService.<RemovableListCellController<String>>loadFxml("theme/settings/removable_cell.fxml"));
  }

  public void onAddItem() {
    if (!addItemTextField.getText().isEmpty()) {
      property.getValue().add(addItemTextField.getText());
      addItemTextField.clear();
    }
  }

  @Override
  public boolean hasDefaultValue() {
    return bound || property.getValue().isEmpty();
  }

  @Override
  public void resetFilter() {
    if (!bound) {
      property.getValue().clear();
    }
  }

  public void setText(String text) {
    JavaFxUtil.bind(root.textProperty(), Bindings.createStringBinding(() -> i18n.get("filter.category", text,
        String.join(", ", property.getValue())), property, property.getValue()));
  }

  public void setPromptText(String promptText) {
    addItemTextField.setPromptText(promptText);
  }

  public void bindBidirectional(ListProperty<String> property) {
    bound = true;
    JavaFxUtil.bindBidirectional(this.property, property);
  }

  @Override
  public ListProperty<String> getObservable() {
    return property;
  }

  @Override
  protected List<String> getValue() {
    return property.getValue();
  }

  @Override
  public MenuButton getRoot() {
    return root;
  }
}
