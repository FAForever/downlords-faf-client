/*
 * Copyright (c) 2014, Andrea Vacondio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.faforever.client.ui.progress;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.SizeConverter;
import javafx.scene.control.Control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Base class for the progress indicator controls represented by circular shapes
 * *
 * @author Andrea Vacondio
 */
abstract class ProgressCircleIndicator extends Control {
  private static final int INDETERMINATE_PROGRESS = -1;

  private final ReadOnlyIntegerWrapper progress = new ReadOnlyIntegerWrapper(0);
  private final ReadOnlyBooleanWrapper indeterminate = new ReadOnlyBooleanWrapper(false);

  public ProgressCircleIndicator() {
  }

  public int getProgress() {
    return progress.get();
  }

  /**
   * Set the value for the progress, it cannot be more then 100 (meaning 100%). A negative value means indeterminate
   * progress.
   *
   * @param progressValue
   * @see ProgressCircleIndicator#makeIndeterminate()
   */
  public void setProgress(int progressValue) {
    progress.set(defaultToHundred(progressValue));
    indeterminate.set(progressValue < 0);
  }

  public ReadOnlyIntegerProperty progressProperty() {
    return progress.getReadOnlyProperty();
  }

  public boolean isIndeterminate() {
    return indeterminate.get();
  }

  public void makeIndeterminate() {
    setProgress(INDETERMINATE_PROGRESS);
  }

  public ReadOnlyBooleanProperty indeterminateProperty() {
    return indeterminate.getReadOnlyProperty();
  }

  private int defaultToHundred(int value) {
    if (value > 100) {
      return 100;
    }
    return value;
  }

  public final void setInnerCircleRadius(int value) {
    innerCircleRadiusProperty().set(value);
  }

  public final DoubleProperty innerCircleRadiusProperty() {
    return innerCircleRadius;
  }

  public final double getInnerCircleRadius() {
    return innerCircleRadiusProperty().get();
  }

  /**
   * radius of the inner circle
   */
  private final DoubleProperty innerCircleRadius = new StyleableDoubleProperty(60) {
    @Override
    public Object getBean() {
      return ProgressCircleIndicator.this;
    }

    @Override
    public String getName() {
      return "innerCircleRadius";
    }

    @Override
    public CssMetaData<ProgressCircleIndicator, Number> getCssMetaData() {
      return StyleableProperties.INNER_CIRCLE_RADIUS;
    }
  };

  private static class StyleableProperties {
    private static final CssMetaData<ProgressCircleIndicator, Number> INNER_CIRCLE_RADIUS = new CssMetaData<ProgressCircleIndicator, Number>(
        "-fx-inner-radius", SizeConverter.getInstance(), 60) {

      @Override
      public boolean isSettable(ProgressCircleIndicator n) {
        return n.innerCircleRadiusProperty() == null || !n.innerCircleRadiusProperty().isBound();
      }

      @Override
      public StyleableProperty<Number> getStyleableProperty(ProgressCircleIndicator n) {
        return (StyleableProperty<Number>) n.innerCircleRadiusProperty();
      }
    };

    public static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

    static {
      final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Control.getClassCssMetaData());
      styleables.add(INNER_CIRCLE_RADIUS);
      STYLEABLES = Collections.unmodifiableList(styleables);
    }
  }

  /**
   * @return The CssMetaData associated with this class, which may include the CssMetaData of its super classes.
   */
  public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
    return StyleableProperties.STYLEABLES;
  }

  @Override
  public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
    return StyleableProperties.STYLEABLES;
  }
}