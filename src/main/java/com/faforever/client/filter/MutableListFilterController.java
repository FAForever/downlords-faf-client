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
import javafx.collections.ObservableList;
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

  private final ObservableList<String> items = FXCollections.observableArrayList();
  private final ListProperty<String> property = new SimpleListProperty<>(items);

  @Override
  public void initialize() {
    listView.setSelectionModel(new NoSelectionModelListView<>());
    listView.setFocusModel(new NoFocusModelListView<>());

    listView.setItems(items);
    listView.setCellFactory(param -> uiService.<RemovableListCellController<String>>loadFxml("theme/settings/removable_cell.fxml"));

    JavaFxUtil.bindManagedToVisible(listView);
    JavaFxUtil.addAndTriggerListener(items, (InvalidationListener)  observable -> listView.setVisible(!items.isEmpty()));

    addItemTextField.setOnAction(event -> {
      if (!addItemTextField.getText().isEmpty()) {
        items.add(addItemTextField.getText());
        addItemTextField.clear();
      }
    });
  }

  @Override
  public boolean hasDefaultValue() {
    return property.isBound() || items.isEmpty();
  }

  @Override
  public void resetFilter() {
    if (!property.isBound()) {
      items.clear();
    }
  }

  public void setText(String text) {
    JavaFxUtil.bind(root.textProperty(), Bindings.createStringBinding(() -> i18n.get("filter.category", text,
        String.join(", ", items)), items));
  }

  public void setPromptText(String promptText) {
    addItemTextField.setPromptText(promptText);
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
