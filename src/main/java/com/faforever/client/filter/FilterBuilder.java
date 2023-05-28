package com.faforever.client.filter;

import com.faforever.client.fx.FxApplicationThreadExecutor;
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

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class FilterBuilder<T> {

  private final UiService uiService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final Consumer<AbstractFilterNodeController<?, ? extends ObservableValue<?>, T>> onFilterBuilt;

  public FilterCheckboxController<T> checkbox(String text, BiFunction<Boolean, T, Boolean> filter) {
    FilterCheckboxController<T> controller = uiService.loadFxml("theme/filter/checkbox_filter.fxml");
    controller.setText(text);
    controller.registerListener(filter);
    onFilterBuilt.accept(controller);
    return controller;
  }

  public FilterTextFieldController<T> textField(String promptText, BiFunction<String, T, Boolean> filter) {
    FilterTextFieldController<T> controller = uiService.loadFxml("theme/filter/textfield_filter.fxml");
    controller.setPromptText(promptText);
    controller.registerListener(filter);
    onFilterBuilt.accept(controller);
    return controller;
  }

  public <U> FilterMultiCheckboxController<U, T> multiCheckbox(String text, List<U> items, StringConverter<U> converter, BiFunction<List<U>, T, Boolean> filter) {
    return multiCheckbox(text, CompletableFuture.completedFuture(items), converter, filter);
  }

  public <U> FilterMultiCheckboxController<U, T> multiCheckbox(String text, CompletableFuture<List<U>> future, StringConverter<U> converter, BiFunction<List<U>, T, Boolean> filter) {
    FilterMultiCheckboxController<U, T> controller = uiService.loadFxml("theme/filter/multicheckbox_filter.fxml");
    controller.setText(text);
    controller.setConverter(converter);
    future.thenAccept(items -> {
      controller.setItems(items);
      controller.registerListener(filter);
    });
    onFilterBuilt.accept(controller);
    return controller;
  }

  public RangeSliderFilterController<T> rangeSlider(String text, double minValue, double maxValue, BiFunction<Range<Integer>, T, Boolean> filter) {
    RangeSliderFilterController<T> controller = uiService.loadFxml("theme/filter/range_slider_filter.fxml", RangeSliderFilterController.class);
    controller.setText(text);
    controller.setMinMaxValue(minValue, maxValue);
    controller.registerListener(filter);
    onFilterBuilt.accept(controller);
    return controller;
  }

  public <I> RangeSliderWithChoiceFilterController<I, T> rangeSliderWithCombobox(String text, CompletableFuture<List<I>> future, StringConverter<I> converter, double minValue, double maxValue, BiFunction<ItemWithRange<I, Integer>, T, Boolean> filter) {
    RangeSliderWithChoiceFilterController<I, T> controller = uiService.loadFxml("theme/filter/range_slider_filter.fxml", RangeSliderWithChoiceFilterController.class);
    controller.setText(text);
    controller.setMinMaxValue(minValue, maxValue);
    controller.setConverter(converter);
    future.thenAcceptAsync(items -> {
      controller.setItems(items);
      controller.registerListener(filter);
    }, fxApplicationThreadExecutor);
    onFilterBuilt.accept(controller);
    return controller;
  }

  public MutableListFilterController<T> mutableList(String text, String promptText, BiFunction<List<String>, T, Boolean> filter) {
    MutableListFilterController<T> controller = uiService.loadFxml("theme/filter/mutable_list_filter.fxml");
    controller.setText(text);
    controller.setPromptText(promptText);
    controller.registerListener(filter);
    onFilterBuilt.accept(controller);
    return controller;
  }
}
