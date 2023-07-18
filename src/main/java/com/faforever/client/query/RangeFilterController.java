package com.faforever.client.query;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.properties.concrete.DoubleProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import lombok.Data;
import lombok.SneakyThrows;
import org.controlsfx.control.RangeSlider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Data
public class RangeFilterController implements FilterNodeController {

  private final I18n i18n;

  public RangeSlider rangeSlider;
  public MenuButton menu;
  public TextField lowValue;
  public TextField highValue;

  private String propertyName;
  private Function<Double, ? extends Number> valueTransform;
  private int numberOfFractionDigits;
  private DecimalFormat numberFormat;

  public void initialize() {
    JavaFxUtil.bindManagedToVisible(menu);
    rangeSlider.setShowTickMarks(true);
    rangeSlider.setShowTickLabels(true);
    rangeSlider.setMinorTickCount(0);
    valueTransform = Function.identity();
  }

  public Optional<List<Condition>> getCondition() {
    List<Condition> conditions = new ArrayList<>();
    double lowValueValue = normalizeToFormat(rangeSlider.getLowValue());
    if (!lowValue.getText().isBlank() && lowValueValue > rangeSlider.getMin()) {
      QBuilder qBuilderLow = new QBuilder<>();
      DoubleProperty propertyLow = qBuilderLow.doubleNum(propertyName);
      conditions.add(propertyLow.gte(valueTransform.apply(lowValueValue)));
    }
    double highValueValue = normalizeToFormat(rangeSlider.getHighValue());
    if (!highValue.getText().isBlank() && highValueValue < rangeSlider.getMax()) {
      QBuilder qBuilderHigh = new QBuilder<>();
      DoubleProperty propertyHigh = qBuilderHigh.doubleNum(propertyName);
      conditions.add(propertyHigh.lte(valueTransform.apply(highValueValue)));
    }
    if (!conditions.isEmpty()) {
      if (!menu.getStyleClass().contains("query-filter-selected")) {
        menu.getStyleClass().add("query-filter-selected");
      }
      return Optional.of(conditions);
    } else {
      menu.getStyleClass().removeIf(styleClass -> styleClass.equals("query-filter-selected"));
      return Optional.empty();
    }
  }

  public void addQueryListener(InvalidationListener queryListener) {
    rangeSlider.lowValueProperty().addListener(queryListener);
    rangeSlider.highValueProperty().addListener(queryListener);
    lowValue.textProperty().addListener(queryListener);
    highValue.textProperty().addListener(queryListener);
  }

  public void clear() {
    rangeSlider.setLowValue(rangeSlider.getMin());
    rangeSlider.setHighValue(rangeSlider.getMax());
  }

  public void setTitle(String title) {
    menu.textProperty().unbind();
    menu.textProperty().bind(Bindings.createStringBinding(() -> i18n.get("query.rangeFilter", title, lowValue.getText(), highValue.getText()), lowValue.textProperty(), highValue.textProperty()));
  }

  public void setMinMax(double min, double max) {
    rangeSlider.setMin(min);
    rangeSlider.setLowValue(min);
    lowValue.setText("");

    rangeSlider.setMax(max);
    rangeSlider.setHighValue(max);
    highValue.setText("");
  }

  public void setIncrement(double increment) {
    rangeSlider.setBlockIncrement(increment);
  }

  public void setTickUnit(double increment) {
    rangeSlider.setMajorTickUnit(increment);
  }

  public void setSnapToTicks(boolean value) {
    rangeSlider.setSnapToTicks(value);
  }

  public void setValueTransform(Function<Double, ? extends Number> valueTransform) {
    this.valueTransform = valueTransform;
  }

  public void setNumberOfFractionDigits(int numberOfFractionDigits) {
    this.numberOfFractionDigits = numberOfFractionDigits;
  }

  public void bind() {
    numberFormat = (DecimalFormat) DecimalFormat.getInstance();
    numberFormat.setRoundingMode(RoundingMode.HALF_UP);
    numberFormat.setMinimumFractionDigits(0);
    numberFormat.setMaximumFractionDigits(numberOfFractionDigits);
    JavaFxUtil.bindTextFieldAndRangeSlider(lowValue, highValue, rangeSlider, numberFormat);
  }

  @Override
  public Node getRoot() {
    return menu;
  }

  @SneakyThrows
  private double normalizeToFormat(double value) {
    return numberFormat.parse(numberFormat.format(value)).doubleValue();
  }
}
