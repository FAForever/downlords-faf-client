package com.faforever.client.filter;

import com.faforever.client.theme.UiService;
import javafx.beans.value.ObservableValue;
import javafx.util.StringConverter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Range;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@RequiredArgsConstructor(access = AccessLevel.MODULE)
public class FilterBuilder<T> {

  private final UiService uiService;
  private final Consumer<AbstractFilterNodeController<?, ? extends ObservableValue<?>, T>> onFilterBuilt;

  public void checkbox(FilterName filterName, String text, BiFunction<Boolean, T, Boolean> filter) {
    FilterCheckboxController<T> controller = uiService.loadFxml("theme/filter/checkbox_filter.fxml");
    controller.setFilterName(filterName);
    controller.setText(text);
    controller.registerListener(filter);
    onFilterBuilt.accept(controller);
  }

  public void textField(FilterName filterName, String promptText, BiFunction<String, T, Boolean> filter) {
    FilterTextFieldController<T> controller = uiService.loadFxml("theme/filter/textfield_filter.fxml");
    controller.setFilterName(filterName);
    controller.setPromptText(promptText);
    controller.registerListener(filter);
    onFilterBuilt.accept(controller);
  }

  public <U> void multiCheckbox(FilterName filterName, String text, List<U> items, StringConverter<U> converter, BiFunction<List<U>, T, Boolean> filter) {
    multiCheckbox(filterName, text, CompletableFuture.completedFuture(items), converter, filter);
  }

  public <U> void multiCheckbox(FilterName filterName, String text, CompletableFuture<List<U>> future, StringConverter<U> converter, BiFunction<List<U>, T, Boolean> filter) {
    FilterMultiCheckboxController<U, T> controller = uiService.loadFxml("theme/filter/multicheckbox_filter.fxml");
    controller.setFilterName(filterName);
    controller.setText(text);
    controller.setConverter(converter);
    future.thenAccept(items -> {
      controller.setItems(items);
      controller.registerListener(filter);
    });
    onFilterBuilt.accept(controller);
  }

  public void rangeSlider(FilterName filterName, String text, double minValue, double maxValue, BiFunction<Range<Integer>, T, Boolean> filter) {
    RangeSliderFilterController<T> controller = uiService.loadFxml("theme/filter/range_slider_filter.fxml", RangeSliderFilterController.class);
    controller.setFilterName(filterName);
    controller.setText(text);
    controller.setMinValue(minValue);
    controller.setMaxValue(maxValue);
    controller.registerListener(filter);
    onFilterBuilt.accept(controller);
  }

  public <I> void rangeSliderWithCombobox(FilterName filterName, String text, CompletableFuture<List<I>> future, StringConverter<I> converter, double minValue, double maxValue, BiFunction<ItemWithRange<I, Integer>, T, Boolean> filter) {
    RangeSliderWithChoiceFilterController<I, T> controller = uiService.loadFxml("theme/filter/range_slider_filter.fxml", RangeSliderWithChoiceFilterController.class);
    controller.setFilterName(filterName);
    controller.setText(text);
    controller.setMinValue(minValue);
    controller.setMaxValue(maxValue);
    controller.setConverter(converter);
    future.thenAccept(items -> {
      controller.setItems(items);
      controller.registerListener(filter);
    });
    onFilterBuilt.accept(controller);
  }

  public void mutableList(FilterName filterName, String text, String promptText, BiFunction<List<String>, T, Boolean> filter) {
    MutableListFilterController<T> controller = uiService.loadFxml("theme/filter/mutable_list_filter.fxml");
    controller.setFilterName(filterName);
    controller.setText(text);
    controller.setPromptText(promptText);
    controller.registerListener(filter);
    onFilterBuilt.accept(controller);
  }
}
