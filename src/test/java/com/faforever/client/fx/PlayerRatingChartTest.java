package com.faforever.client.fx;

import com.faforever.client.chat.PlayerRatingChart;
import com.faforever.client.chat.PlayerRatingChartTooltipController;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.collections.FXCollections;
import javafx.scene.chart.Axis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
public class PlayerRatingChartTest extends AbstractPlainJavaFxTest {

  @Mock
  private PlayerRatingChartTooltipController chartTooltip;
  @Mock
  private UiService uiService;

  private PlayerRatingChart instance;
  private Region chartBackground;

  @Before
  public void setUp() {
    when(uiService.loadFxml("theme/chat/player_rating_chart_tooltip.fxml")).thenReturn(chartTooltip);
    when(chartTooltip.getRoot()).thenReturn(new Pane());
    instance.initializeTooltip(uiService);
    chartBackground = instance.getChartBackground();
  }

  @Override
  protected Pane getRoot() {
    return new Pane(initializeChart());
  }

  @Test
  public void testShowCurrentXYWhenMouseMovingOverLine() {
    moveMouseTo(10, 10);
    verify(chartTooltip).setDateAndRating(any(Long.class), any(Integer.class));
    verify(chartTooltip, never()).clear();
  }

  @Test
  public void testNoCurrentXYWhenMouseMovingOverWithoutLine() {
    moveMouseTo((int) chartBackground.getWidth() - 5, 5);
    verify(chartTooltip).clear();
    verify(chartTooltip, never()).setDateAndRating(any(Long.class), any(Integer.class));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private PlayerRatingChart initializeChart() {
    Axis xAxis = new NumberAxis("X Axis", 0d, 100d, 5d);
    Axis yAxis = new NumberAxis("Y Axis", 0d, 100d, 5d);
    instance = new PlayerRatingChart(xAxis, yAxis);
    instance.getData().add(new XYChart.Series<>(FXCollections.observableArrayList(List.of(
        new Data<>(0d, -5d),
        new Data<>(10d, 20d),
        new Data<>(25d, 10d),
        new Data<>(30d, 40d),
        new Data<>(50d, 15d),
        new Data<>(80d, 10d),
        new Data<>(90d, -10d)
        // leave emtpy without the lines from 90 to 100
    ))));
    return instance;
  }

  private void moveMouseTo(int x, int y) {
    runOnFxThreadAndWait(() -> chartBackground.getOnMouseMoved().handle(MouseEvents.generateMouseMoved(x, y)));
  }
}
