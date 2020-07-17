package com.faforever.client.vault.search;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.query.LogicalNodeController;
import com.faforever.client.query.SearchablePropertyMappings.Property;
import com.faforever.client.query.SpecificationController;
import com.faforever.client.theme.UiService;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SearchController implements Controller<Pane> {
  private final UiService uiService;
  private final I18n i18n;
  private final PreferencesService preferencesService;
  /**
   * The first query element.
   */
  public LogicalNodeController initialLogicalNodeController;
  public Pane criteriaPane;
  public TextField queryTextField;
  public CheckBox displayQueryCheckBox;
  public Button searchButton;
  public Pane searchRoot;
  public ComboBox<Property> sortPropertyComboBox;
  public ComboBox<SortOrder> sortOrderChoiceBox;
  public HBox sortBox;
  public CheckBox onlyShowLastYearCheckBox;

  private List<LogicalNodeController> queryNodes;
  private InvalidationListener queryInvalidationListener;
  /**
   * Called with the query string when the user hits "search".
   */
  private Consumer<SearchConfig> searchListener;
  private Map<String, Property> searchableProperties;
  /**
   * Type of the searchable entity.
   */
  private Class<?> rootType;

  public SearchController(UiService uiService, I18n i18n, PreferencesService preferencesService) {
    this.uiService = uiService;
    this.i18n = i18n;
    this.preferencesService = preferencesService;
    queryNodes = new ArrayList<>();
  }

  @Override
  public void initialize() {
    queryTextField.managedProperty().bind(queryTextField.visibleProperty());
    queryTextField.visibleProperty().bind(displayQueryCheckBox.selectedProperty());

    onlyShowLastYearCheckBox.managedProperty().bind(onlyShowLastYearCheckBox.visibleProperty());
    onlyShowLastYearCheckBox.setVisible(false);

    initialLogicalNodeController.logicalOperatorField.managedProperty()
        .bind(initialLogicalNodeController.logicalOperatorField.visibleProperty());
    initialLogicalNodeController.removeCriteriaButton.managedProperty()
        .bind(initialLogicalNodeController.removeCriteriaButton.visibleProperty());

    initialLogicalNodeController.logicalOperatorField.setValue(null);
    initialLogicalNodeController.logicalOperatorField.setDisable(true);
    initialLogicalNodeController.logicalOperatorField.setVisible(false);
    initialLogicalNodeController.removeCriteriaButton.setVisible(false);

    queryInvalidationListener = observable -> queryTextField.setText(buildQuery(initialLogicalNodeController.specificationController, queryNodes));
    onlyShowLastYearCheckBox.selectedProperty().addListener(queryInvalidationListener);
    addInvalidationListener(initialLogicalNodeController);
    initSorting();
  }

  private void initSorting() {
    sortPropertyComboBox.setConverter(new StringConverter<>() {
      @Override
      public String toString(Property property) {
        return i18n.get(property.getI18nKey());
      }

      @Override
      public Property fromString(String string) {
        throw new UnsupportedOperationException("Not supported");
      }
    });
    sortOrderChoiceBox.setConverter(new StringConverter<>() {
      @Override
      public String toString(SortOrder order) {
        return i18n.get(order.getI18nKey());
      }

      @Override
      public SortOrder fromString(String string) {
        throw new UnsupportedOperationException("Not supported");
      }
    });
    sortOrderChoiceBox.getItems().addAll(SortOrder.values());
  }

  public void setSearchableProperties(Map<String, Property> searchableProperties) {
    this.searchableProperties = searchableProperties;
    initialLogicalNodeController.specificationController.setProperties(searchableProperties);
  }

  public void setSortConfig(ObjectProperty<SortConfig> sortConfigObjectProperty) {
    List<Property> sortableProperties = searchableProperties.values().stream()
        .filter(Property::isSortable)
        .collect(Collectors.toList());
    sortPropertyComboBox.getItems().addAll(sortableProperties);
    sortOrderChoiceBox.getSelectionModel().select(sortConfigObjectProperty.get().getSortOrder());

    Property savedSortProperty = searchableProperties.get(sortConfigObjectProperty.get().getSortProperty());

    if (savedSortProperty == null || !savedSortProperty.isSortable()) {
      savedSortProperty = sortableProperties.iterator().next();
    }

    sortPropertyComboBox.getSelectionModel().select(savedSortProperty);

    sortPropertyComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
      sortConfigObjectProperty.set(new SortConfig(getCurrentEntityKey(), sortOrderChoiceBox.getValue()));
      preferencesService.storeInBackground();
    });
    sortOrderChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> {
      sortConfigObjectProperty.set(new SortConfig(getCurrentEntityKey(), newValue));
      preferencesService.storeInBackground();
    });
  }

  private void addInvalidationListener(LogicalNodeController logicalNodeController) {
    logicalNodeController.specificationController.propertyField.valueProperty().addListener(queryInvalidationListener);
    logicalNodeController.specificationController.operationField.valueProperty().addListener(queryInvalidationListener);
    logicalNodeController.specificationController.valueField.valueProperty().addListener(queryInvalidationListener);
    logicalNodeController.specificationController.valueField.getEditor().textProperty().addListener(observable -> {
      if (!logicalNodeController.specificationController.valueField.valueProperty().isBound()) {
        Platform.runLater(() -> logicalNodeController.specificationController.valueField.setValue(logicalNodeController.specificationController.valueField.getEditor().getText()));
      }
    });
    logicalNodeController.specificationController.valueField.setOnKeyReleased(event -> {
      if (event.getCode() == KeyCode.ENTER) {
        searchButton.fire();
      }
    });
  }

  public void onSearchButtonClicked() {
    String sortPropertyKey = getCurrentEntityKey();
    searchListener.accept(new SearchConfig(new SortConfig(sortPropertyKey, sortOrderChoiceBox.getValue()), queryTextField.getText()));
  }

  private String getCurrentEntityKey() {
    return searchableProperties.entrySet().stream()
        .filter(stringStringEntry -> stringStringEntry.getValue().equals(sortPropertyComboBox.getValue()))
        .findFirst()
        .get()
        .getKey();
  }

  public void onAddCriteriaButtonClicked() {
    LogicalNodeController controller = uiService.loadFxml("theme/vault/search/logical_node.fxml");
    controller.logicalOperatorField.valueProperty().addListener(queryInvalidationListener);
    controller.specificationController.setRootType(rootType);
    controller.specificationController.setProperties(searchableProperties);
    controller.setRemoveCriteriaButtonListener(() -> {
      criteriaPane.getChildren().remove(controller.getRoot());
      queryNodes.remove(controller);
      if (queryNodes.isEmpty()) {
        initialLogicalNodeController.logicalOperatorField.setVisible(false);
      }
      queryInvalidationListener.invalidated(queryTextField.textProperty());
    });
    addInvalidationListener(controller);

    criteriaPane.getChildren().add(controller.getRoot());
    queryNodes.add(controller);
    initialLogicalNodeController.logicalOperatorField.setVisible(true);
  }

  public void onResetButtonClicked() {
    new ArrayList<>(queryNodes).forEach(logicalNodeController -> logicalNodeController.removeCriteriaButton.fire());
    initialLogicalNodeController.specificationController.propertyField.getSelectionModel().select(0);
    initialLogicalNodeController.specificationController.operationField.getSelectionModel().select(0);
    initialLogicalNodeController.specificationController.valueField.setValue(null);
  }

  /**
   * Builds the query string if possible, returns empty string if not. A query string can not be built if the user
   * selected no or invalid values.
   */
  private String buildQuery(SpecificationController initialSpecification, List<LogicalNodeController> queryNodes) {
    QBuilder qBuilder = new QBuilder<>();
    boolean isLastYearChecked = onlyShowLastYearCheckBox.isVisible() && onlyShowLastYearCheckBox.isSelected();
    Optional<Condition> condition = initialSpecification.appendTo(qBuilder);

    if (!condition.isPresent()) {
      return isLastYearChecked ?
          (String) qBuilder.instant("endTime").after(OffsetDateTime.now().minusYears(1).toInstant(), false).query(new RSQLVisitor())
          : "";
    }

    for (LogicalNodeController queryNode : queryNodes) {
      Optional<Condition> currentCondition = queryNode.appendTo(condition.get());
      if (!currentCondition.isPresent()) {
        break;
      }
      condition = currentCondition;
    }

    Condition toQuery = isLastYearChecked ? condition.get().and().instant("endTime").after(OffsetDateTime.now().minusYears(1).toInstant(), false)
        : condition.get();
    return (String) toQuery.query(new RSQLVisitor());
  }


  @Override
  public Pane getRoot() {
    return searchRoot;
  }

  public void setSearchListener(Consumer<SearchConfig> searchListener) {
    this.searchListener = searchListener;
  }

  public void setRootType(Class<?> rootType) {
    this.rootType = rootType;
    initialLogicalNodeController.specificationController.setRootType(rootType);
  }

  public void setSearchButtonDisabledCondition(BooleanBinding inSearchableState) {
    searchButton.disableProperty().bind(queryTextField.textProperty().isEmpty().or(inSearchableState.not()));
  }

  public void setOnlyShowLastYearCheckBoxVisible(boolean visible, boolean selectedBaseValue) {
    onlyShowLastYearCheckBox.setVisible(visible);
    onlyShowLastYearCheckBox.setSelected(selectedBaseValue);
  }

  @Getter
  public enum SortOrder {
    DESC("-", "search.sort.descending"),
    ASC("", "search.sort.ascending");

    private final String query;
    private final String i18nKey;

    SortOrder(String query, String i18nKey) {
      this.query = query;
      this.i18nKey = i18nKey;
    }
  }

  @Data
  @AllArgsConstructor
  public static class SortConfig {
    private String sortProperty;
    private SortOrder sortOrder;

    public String toQuery() {
      return sortOrder.getQuery() + sortProperty;
    }
  }

  @Data
  @AllArgsConstructor
  public static class SearchConfig {
    private SortConfig sortConfig;
    private String searchQuery;

    public boolean hasQuery() {
      return searchQuery != null && !searchQuery.isEmpty();
    }
  }
}
