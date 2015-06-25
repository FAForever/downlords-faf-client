package com.faforever.client.fx;

import javafx.scene.chart.Axis;

import java.time.LocalDateTime;
import java.util.List;

public class LocalDateTimeAxis extends Axis<LocalDateTime> {

  @Override
  protected Object autoRange(double length) {
    return null;
  }

  @Override
  protected void setRange(Object range, boolean animate) {

  }

  @Override
  protected Object getRange() {
    return null;
  }

  @Override
  public double getZeroPosition() {
    return 0;
  }

  @Override
  public double getDisplayPosition(LocalDateTime value) {
    return 0;
  }

  @Override
  public LocalDateTime getValueForDisplay(double displayPosition) {
    return null;
  }

  @Override
  public boolean isValueOnAxis(LocalDateTime value) {
    return false;
  }

  @Override
  public double toNumericValue(LocalDateTime value) {
    return 0;
  }

  @Override
  public LocalDateTime toRealValue(double value) {
    return null;
  }

  @Override
  protected List<LocalDateTime> calculateTickValues(double length, Object range) {
    return null;
  }

  @Override
  protected String getTickMarkLabel(LocalDateTime value) {
    return null;
  }
}
