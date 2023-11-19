package com.faforever.client.filter;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import org.apache.commons.lang3.Range;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RangeSliderWithChoiceFilterController<I, T> extends AbstractRangeSliderFilterController<ItemWithRange<I, Integer>, T> {

  public ComboBox<I> choiceView;
  private StringConverter<I> converter;

  protected RangeSliderWithChoiceFilterController(I18n i18n) {
    super(i18n);
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    choiceView = new ComboBox<>();
    JavaFxUtil.bind(choiceView.prefWidthProperty(), rangeSlider.widthProperty());
  }

  public void setText(String text) {
    JavaFxUtil.bind(root.textProperty(), Bindings.createStringBinding(() -> getFormattedText(text), rangeSlider.lowValueProperty(), rangeSlider.highValueProperty(), choiceView.getSelectionModel()
        .selectedItemProperty()));
  }

  private String getFormattedText(String text) {
    String convertedText = text;
    I selectedItem = choiceView.getSelectionModel().getSelectedItem();
    if (converter != null && selectedItem != null) {
      convertedText = converter.toString(selectedItem);
    }
    return i18n.get("filter.range",
        convertedText,
        hasDefaultLowValue() ? "" : ((int) rangeSlider.getLowValue()),
        hasDefaultHighValue() ? "" : ((int) rangeSlider.getHighValue())
    );
  }

  public void setItems(List<I> items) {
    choiceView.setItems(FXCollections.observableList(items));
    choiceView.getSelectionModel().selectFirst();
    contentVBox.getChildren().add(0, choiceView);
  }

  @Override
  public ObjectBinding<ItemWithRange<I, Integer>> valueProperty() {
    if (rangeProperty == null) {
      rangeProperty = Bindings.createObjectBinding(() -> {
            int lowValue = (int) rangeSlider.getLowValue();
            int highValue = (int) rangeSlider.getHighValue();
            I item = choiceView.getSelectionModel().getSelectedItem();
            return lowValue == minValue && highValue == maxValue
                ? new ItemWithRange<>(item, NO_CHANGE)
                : new ItemWithRange<>(item, Range.between(lowValue, highValue));
          },
          rangeSlider.lowValueProperty(), rangeSlider.highValueProperty(), choiceView.getSelectionModel()
              .selectedItemProperty());
    }
    return rangeProperty;
  }

  public void setConverter(StringConverter<I> converter) {
    this.converter = converter;
    choiceView.setConverter(converter);
  }
}
