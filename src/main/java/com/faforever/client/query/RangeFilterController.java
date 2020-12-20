package com.faforever.client.query;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.properties.concrete.IntegerProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import lombok.Data;
import org.apache.commons.lang3.math.NumberUtils;
import org.controlsfx.control.RangeSlider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

  public void initialize() {
    JavaFxUtil.bindManagedToVisible(menu);
    rangeSlider.setShowTickMarks(true);
    rangeSlider.setShowTickLabels(true);
    rangeSlider.setMinorTickCount(0);
    lowValue.textProperty().bindBidirectional(rangeSlider.lowValueProperty(), new StringConverter<>() {
      @Override
      public String toString(Number number) {
        if (!number.equals(rangeSlider.getMin())) {
          return String.valueOf(number.intValue());
        } else {
          return "";
        }
      }

      @Override
      public Number fromString(String string) {
        if (NumberUtils.isParsable(string)) {
          return Double.parseDouble(string);
        } else {
          if (!string.equals("-") && !string.equals(".")) {
            lowValue.setText("");
          }
          return rangeSlider.getMin();
        }
      }
    });
    highValue.textProperty().bindBidirectional(rangeSlider.highValueProperty(), new StringConverter<>() {
      @Override
      public String toString(Number number) {
        if (!number.equals(rangeSlider.getMax())) {
          return String.valueOf(number.intValue());
        } else {
          return "";
        }
      }

      @Override
      public Number fromString(String string) {
        if (NumberUtils.isParsable(string)) {
          return Double.parseDouble(string);
        } else {
          if (!string.equals("-") && !string.equals(".")) {
            highValue.setText("");
          }
          return rangeSlider.getMax();
        }
      }
    });
  }

  public Optional<List<Condition>> getCondition() {
    List<Condition> conditions = new ArrayList<>();
    if (!lowValue.getText().isBlank() && rangeSlider.getLowValue() > rangeSlider.getMin()) {
      QBuilder qBuilderLow = new QBuilder<>();
      IntegerProperty propertyLow = qBuilderLow.intNum(propertyName);
      conditions.add(propertyLow.gte((int) rangeSlider.getLowValue()));
    }
    if (!highValue.getText().isBlank() && rangeSlider.getHighValue() < rangeSlider.getMax()) {
      QBuilder qBuilderHigh = new QBuilder<>();
      IntegerProperty propertyHigh = qBuilderHigh.intNum(propertyName);
      conditions.add(propertyHigh.lte((int) rangeSlider.getHighValue()));
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

  public void setMin(double min) {
    rangeSlider.setMin(min);
    rangeSlider.setLowValue(min);
    lowValue.setText("");
  }

  public void setMax(double max) {
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

  @Override
  public Node getRoot() {
    return menu;
  }

}
