package com.faforever.client.main.hub;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ConcurrentUsersController {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private class DummyData {

    public Long time;
    public Integer value;

    public DummyData(Long time, Integer value) {
      this.time = time;
      this.value = value;
    }
  }

  @FXML
  Pane concurrentUsersRoot;

  @FXML
  XYChart<Long, Integer> chart;

  @FXML
  NumberAxis yAxis;

  @FXML
  NumberAxis xAxis;

  @FXML
  void initialize() {
    xAxis.setForceZeroInRange(false);
    xAxis.setTickLabelFormatter(new StringConverter<Number>() {
      @Override
      public String toString(Number object) {
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(object.longValue() / 1000), ZoneId.systemDefault());
        return DATE_FORMATTER.format(zonedDateTime);
      }

      @Override
      public Number fromString(String string) {
        return null;
      }
    });

    plotStatistics();
  }

  @SuppressWarnings("unchecked")
  private void plotStatistics() {
    // FIXME so far, dummy data
    Duration duration = Duration.ofDays(2);
    long now = System.currentTimeMillis();
    long startTime = now - duration.toMillis();
    List<DummyData> dummyData = new ArrayList<>();
    for (int minute = 0; minute < duration.toMinutes(); minute++) {
      dummyData.add(new DummyData(startTime + minute * 60 * 1000, (int) (Math.sin((float) minute / duration.toMinutes() * 12) * 300 + 800)));
    }

    xAxis.setLowerBound(startTime);
    xAxis.setUpperBound(now);

    XYChart.Series<Long, Integer> series = new XYChart.Series<>();
    List<XYChart.Data<Long, Integer>> values = new ArrayList<>();

    for (DummyData data : dummyData) {
      values.add(new XYChart.Data<>(data.time, data.value));
    }

    series.getData().setAll(FXCollections.observableList(values));
    chart.getData().setAll(series);
  }

  public Node getRoot() {
    return concurrentUsersRoot;
  }
}
