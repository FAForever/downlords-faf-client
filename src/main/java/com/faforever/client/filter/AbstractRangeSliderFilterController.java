package com.faforever.client.filter;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import javafx.beans.binding.ObjectBinding;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.Range;
import org.controlsfx.control.RangeSlider;

public abstract class AbstractRangeSliderFilterController<U, T> extends AbstractFilterNodeController<U, ObjectBinding<U>, T> {

  public static Range<Integer> NO_CHANGE = Range.between(0, 0);

  protected final I18n i18n;

  public MenuButton root;
  public VBox contentVBox;
  public RangeSlider rangeSlider;
  public TextField lowValueTextField;
  public TextField highValueTextField;

  protected ObjectBinding<U> rangeProperty;

  protected double minValue;
  protected double maxValue;

  protected AbstractRangeSliderFilterController(I18n i18n) {
    this.i18n = i18n;
  }

  @Override
  protected void onInitialize() {
    rangeSlider.setShowTickLabels(true);
    rangeSlider.setShowTickMarks(true);
  }

  public void setMinMaxValue(double minValue, double maxValue) {
    this.minValue = minValue;
    rangeSlider.setMin(minValue);
    rangeSlider.setLowValue(minValue);

    this.maxValue = maxValue;
    rangeSlider.setMax(maxValue);
    rangeSlider.setHighValue(maxValue);

    JavaFxUtil.bindTextFieldAndRangeSlider(lowValueTextField, highValueTextField, rangeSlider);
  }

  @Override
  public boolean hasDefaultValue() {
    return hasDefaultLowValue() && hasDefaultHighValue();
  }

  protected boolean hasDefaultLowValue() {
    return rangeSlider.getLowValue() == minValue;
  }

  protected boolean hasDefaultHighValue() {
    return rangeSlider.getHighValue() == maxValue;
  }

  @Override
  public void resetFilter() {
    rangeSlider.setLowValue(minValue);
    rangeSlider.setHighValue(maxValue);
  }

  @Override
  protected U getValue() {
    return rangeProperty.getValue();
  }

  @Override
  public Node getRoot() {
    return root;
  }
}
