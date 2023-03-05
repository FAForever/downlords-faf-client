package com.faforever.client.filter;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import org.apache.commons.lang3.Range;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RangeSliderFilterController<T> extends AbstractRangeSliderFilterController<Range<Integer>, T> {

  protected RangeSliderFilterController(I18n i18n) {
    super(i18n);
  }

  public void setText(String text) {
    JavaFxUtil.bind(root.textProperty(), Bindings.createStringBinding(() -> getFormattedText(text), rangeSlider.lowValueProperty(), rangeSlider.highValueProperty()));
  }

  private String getFormattedText(String text) {
    return i18n.get("filter.range",
        text,
        hasDefaultLowValue() ? "" : ((int) rangeSlider.getLowValue()),
        hasDefaultHighValue() ? "" : ((int) rangeSlider.getHighValue())
    );
  }

  @Override
  public ObjectBinding<Range<Integer>> valueProperty() {
    if (rangeProperty == null) {
      rangeProperty = Bindings.createObjectBinding(() -> {
            int lowValue = (int) rangeSlider.getLowValue();
            int highValue = (int) rangeSlider.getHighValue();
            return lowValue == minValue && highValue == maxValue ? NO_CHANGE : Range.between(lowValue, highValue);
          },
          rangeSlider.lowValueProperty(), rangeSlider.highValueProperty());
    }
    return rangeProperty;
  }
}
