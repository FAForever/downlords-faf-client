package com.faforever.client.filter;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.ui.list.NoFocusModelListView;
import com.faforever.client.ui.list.NoSelectionModelListView;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.textfield.TextFields;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class FilterMultiCheckboxController<U, T> extends AbstractFilterNodeController<List<U>, ListProperty<U>, T> {

  private final I18n i18n;

  public MenuButton root;
  public VBox contentVBox;
  public TextField searchTextField;
  public CheckListView<String> listView;

  private final ObservableList<U> selectedItems = FXCollections.observableArrayList();
  private final ListProperty<U> selectedItemListProperty = new SimpleListProperty<>(selectedItems);
  private final ObservableList<String> selectedStringItems = FXCollections.observableArrayList();

  private StringConverter<U> converter;
  private List<U> sourceList;
  private List<String> convertedSourceList;
  private FilteredList<String> filteredList;
  private String text;

  @Override
  public void initialize() {
    listView.setSelectionModel(new NoSelectionModelListView<>());
    listView.setFocusModel(new NoFocusModelListView<>());
    addSearchBar();
  }

  @Override
  public boolean hasDefaultValue() {
    return selectedItems.isEmpty();
  }

  @Override
  public void resetFilter() {
    selectedStringItems.clear();
    searchTextField.setText("");
    selectedItems.clear();
    listView.getCheckModel().clearChecks();
  }

  @Override
  public ListProperty<U> getObservable() {
    return selectedItemListProperty;
  }

  @Override
  protected List<U> getValue() {
    return selectedItemListProperty.getValue();
  }

  @Override
  public MenuButton getRoot() {
    return root;
  }

  public void setText(String text) {
    this.text = text;
    root.setText(text);
  }

  public void setItems(List<U> items) {
    this.sourceList = items;
    convertedSourceList = convertToStringItems(items);
    filteredList = new FilteredList<>(FXCollections.observableList(convertedSourceList));
    listView.setItems(filteredList);
    addListeners();
  }

  private void addListeners() {
    JavaFxUtil.addListener(searchTextField.textProperty(), observable ->
        filteredList.setPredicate(item -> StringUtils.containsIgnoreCase(item, searchTextField.getText())));
    JavaFxUtil.addListener(filteredList.predicateProperty(), observable -> restoreSelectedItems());
    JavaFxUtil.addListener(listView.getCheckModel().getCheckedItems(), (ListChangeListener<String>) this::invalidate);
    JavaFxUtil.bind(root.textProperty(), Bindings.createStringBinding(() -> i18n.get("filter.category", text,
        String.join(", ", selectedStringItems)), selectedStringItems));
  }

  private void restoreSelectedItems() {
    selectedStringItems.forEach(item -> listView.getCheckModel().check(item));
  }

  private void addSearchBar() {
    searchTextField = TextFields.createClearableTextField();
    searchTextField.setPromptText(i18n.get("chat.searchPrompt"));
    contentVBox.getChildren().add(0, searchTextField);
  }

  private void invalidate(Change<? extends String> change) {
    if (change.next()) {
      if (change.wasAdded()) {
        String checkedItem = change.getAddedSubList().get(0);
        if (!selectedStringItems.contains(checkedItem)) {
          selectedStringItems.add(checkedItem);
          selectedItems.add(sourceList.get(convertedSourceList.indexOf(checkedItem)));
        }
      } else if (change.wasRemoved()) {
        String uncheckedItem = change.getRemoved().get(0);
        if (selectedStringItems.contains(uncheckedItem)) {
          selectedStringItems.remove(uncheckedItem);
          selectedItems.remove(sourceList.get(convertedSourceList.indexOf(uncheckedItem)));
        }
      }
    }
  }

  public void setConverter(StringConverter<U> converter) {
    this.converter = converter;
  }

  private String convertToString(U item) {
    return converter != null ? converter.toString(item) : item.toString();
  }

  private List<String> convertToStringItems(List<U> items) {
    return items.stream().map(this::convertToString).toList();
  }

  @VisibleForTesting
  List<U> getSelectedItems() {
    return selectedItems;
  }
}
