package com.faforever.client.filter;

import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.theme.UiService;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

@RequiredArgsConstructor
public abstract class AbstractFilterController<T> extends NodeController<SplitPane> {


  /**
   * Variables are located at {@code theme/filter/filter.fxml} file
   */
  public SplitPane root;
  public VBox filtersContent;
  public Button resetAllButton;

  protected final UiService uiService;
  protected final I18n i18n;
  protected final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final ObservableMap<ObservableValue<?>, Predicate<T>> externalFilters = FXCollections.observableHashMap();
  private final List<AbstractFilterNodeController<?, ? extends ObservableValue<?>, T>> filters = new ArrayList<>();
  private final ObservableList<ObservableValue<Predicate<T>>> filterPredicates = FXCollections.observableList(
      new ArrayList<>(), observable -> new Observable[]{observable});
  private Predicate<T> defaultPredicate = t -> true;
  private boolean resetInProgress = false;

  private final BooleanProperty filterActive = new SimpleBooleanProperty(false);
  private final ObjectProperty<Predicate<T>> predicate = new SimpleObjectProperty<>(defaultPredicate);
  private final ObjectProperty<Predicate<T>> filtersPredicate = new SimpleObjectProperty<>(t -> true);

  @Override
  protected void onInitialize() {
    build(new FilterBuilder<>(uiService, fxApplicationThreadExecutor, filters::add));
    afterBuilt();
  }

  protected abstract void build(FilterBuilder<T> filterBuilder);

  protected void afterBuilt() {
    // To be overridden by subclass
  }

  public ObjectProperty<Predicate<T>> predicateProperty() {
    return predicate;
  }

  public Predicate<T> getPredicate() {
    return predicate.get();
  }

  public BooleanProperty filterActiveProperty() {
    return filterActive;
  }

  public boolean getFilterActive() {
    return filterActive.get();
  }

  public void setDefaultPredicate(Predicate<T> defaultPredicate) {
    this.defaultPredicate = defaultPredicate;
    predicate.setValue(defaultPredicate);
  }

  private void setFilterContent() {
    filtersContent.getChildren().setAll(filters.stream().map(NodeController::getRoot).toList());
  }

  public void completeSetting() {
    setFilterContent();
    filters.stream().map(filter -> filter.predicateProperty().when(showing)).forEach(filterPredicates::add);
    externalFilters.subscribe(this::invalidate);
    filterPredicates.subscribe(this::invalidate);
    invalidate();
  }

  private synchronized void invalidate() {
    if (!resetInProgress) {
      Predicate<T> finalPredicate = Stream.concat(filterPredicates.stream().map(ObservableValue::getValue),
                                                  externalFilters.values().stream())
                                          .reduce(Predicate::and)
                                          .orElseThrow();
      predicate.setValue(defaultPredicate.and(finalPredicate));
      updateFilterState();
    }
  }

  private void updateFilterState() {
    boolean hasDefaultValues = filters.stream().allMatch(AbstractFilterNodeController::hasDefaultValue);
    filterActive.setValue(!hasDefaultValues);
    resetAllButton.setDisable(hasDefaultValues);
  }

  public <U> void addExternalFilter(ObservableValue<U> property, BiFunction<U, T, Boolean> filter) {
    property.subscribe(value -> externalFilters.put(property, item -> filter.apply(value, item)));
  }

  public void onResetAllButtonClicked() {
    resetInProgress = true;
    filters.stream().filter(filter -> !filter.hasDefaultValue()).forEach(AbstractFilterNodeController::resetFilter);
    resetAllButton.setDisable(true);
    resetInProgress = false;
    invalidate();
  }

  @Override
  public SplitPane getRoot() {
    return root;
  }
}
