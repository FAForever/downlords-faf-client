package com.faforever.client.fx;

import com.faforever.client.chat.PlayerRatingChartTooltipController;
import com.faforever.client.theme.UiService;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.NamedArg;
import javafx.collections.ListChangeListener.Change;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Player rating line chart with support for displaying more accurate values via the tooltip on mouse hover
 */
@Slf4j
public class PlayerRatingChart extends LineChart<Double, Double> {

  private static final int OFFSET_TOOLTIP_FROM_CURSOR_BY_X = 20; // in px
  private static final int OFFSET_TOOLTIP_FROM_CURSOR_BY_Y = 15; // in px

  private final Region chartBackground;
  private final Line verticalLine = new Line(0, 0, 0, 0);
  private PlayerRatingChartTooltipController tooltipController;
  private Tooltip hoverTooltip;
  private boolean available = true;

  private boolean valid = false;
  private final Map<Integer, Double> ratingMap = new HashMap<>(); // key - pixel in chart background

  public PlayerRatingChart(@NamedArg("xAxis") Axis<Double> xAxis, @NamedArg("yAxis") Axis<Double> yAxis) {
    super(xAxis, yAxis);
    chartBackground = (Region) lookup(".chart-plot-background");
    if (chartBackground == null) {
      log.warn("'Chart background' object was no found. The hover tooltip will not be displayed.");
      available = false;
    } else {
      chartBackground.getParent().getChildrenUnmodifiable().stream()
          .filter((node) -> node != chartBackground)
          .forEach((node) -> node.setMouseTransparent(true));
      chartBackground.setOnMouseMoved((event) -> {
        setValues(event);
        moveTooltipAndLineToCursor(event);
      });
      addVerticalLine();
    }
  }

  private void addVerticalLine() {
    getPlotChildren().add(verticalLine);
    JavaFxUtil.bind(verticalLine.endYProperty(), chartBackground.heightProperty());
  }

  public void initializeTooltip(UiService uiService) {
    if (available) {
      this.tooltipController = uiService.loadFxml("theme/chat/player_rating_chart_tooltip.fxml");
      prepareHoverTooltip();
    }
  }

  private void prepareHoverTooltip() {
    hoverTooltip = JavaFxUtil.createCustomTooltip(tooltipController.getRoot());
    hoverTooltip.setAutoFix(false);
    hoverTooltip.setHideDelay(Duration.ZERO);
    hoverTooltip.setShowDelay(Duration.ZERO);
    hoverTooltip.setShowDuration(Duration.INDEFINITE);
    Tooltip.install(chartBackground, hoverTooltip);
  }

  private void setValues(MouseEvent event) {
    if (valid) {
      double x = event.getX();
      long dateValueInSec = getDisplayedDateValue(x);
      Double rating = ratingMap.get((int) x);
      if (rating != null && dateValueInSec != Long.MIN_VALUE) {
        tooltipController.setDateAndRating(dateValueInSec, rating.intValue());
      } else {
        tooltipController.clear();
      }
    }
  }

  private void moveTooltipAndLineToCursor(MouseEvent event) {
    hoverTooltip.setX(event.getScreenX() + OFFSET_TOOLTIP_FROM_CURSOR_BY_X);
    hoverTooltip.setY(event.getScreenY() + OFFSET_TOOLTIP_FROM_CURSOR_BY_Y);
    verticalLine.setLayoutX(event.getX());
  }

  private void recalculateData() {
    ratingMap.clear();
    for (int i = 0; i < getData().size(); i++) {
      Series<Double, Double> series = getData().get(i);
      if (series.getNode() instanceof Path) {
        buildData(((Path) series.getNode()).getElements());
        break;
      }
    }
  }

  private void buildData(List<PathElement> elements) {
    List<LineTo> lines = elements.stream()
        .filter((element) -> element instanceof LineTo)
        .map((element) -> (LineTo) element)
        .collect(Collectors.toList());
    if (lines.size() < 2) {
      return;
    }

    for (int i = 0; i < lines.size() - 1; i++) {
      LineTo line1 = lines.get(i);
      LineTo line2 = lines.get(i + 1);
      int leftXCoordinate = (int) Math.floor(line1.getX());
      int rightXCoordinate = (int) Math.floor(line2.getX());
      int rating1 = getDisplayedRatingValue(line1.getY());
      int rating2 = getDisplayedRatingValue(line2.getY());
      if (rating1 == Integer.MIN_VALUE || rating2 == Integer.MIN_VALUE) {
        continue;
      }
      int distance = rightXCoordinate - leftXCoordinate;
      if (distance > 1) {
        double augmentation = (double) (rating2 - rating1) / distance; // augmentation may be positive or negative
        double value = rating1;
        while (leftXCoordinate < rightXCoordinate) {
          putMapRatingValue(++leftXCoordinate, value += augmentation);
        }
      } else {
        putMapRatingValue(leftXCoordinate, rating1);
      }
    }
  }

  private void putMapRatingValue(int xCoordinate, double rating) {
    ratingMap.merge(xCoordinate, rating, (oldRating, newRating) -> (oldRating + newRating) / 2);
  }

  private long getDisplayedDateValue(double displayPosition) {
    Double value = getXAxis().getValueForDisplay(displayPosition);
    return value != null ? value.longValue() : Long.MIN_VALUE;
  }

  private int getDisplayedRatingValue(double displayPosition) {
    Double value = getYAxis().getValueForDisplay(displayPosition);
    return value != null ? value.intValue() : Integer.MIN_VALUE;
  }

  @Override
  protected void seriesChanged(Change<? extends Series> change) {
    super.seriesChanged(change);
    valid = false;
  }

  @Override
  protected void seriesAdded(Series<Double, Double> series, int seriesIndex) {
    super.seriesAdded(series, seriesIndex);
    valid = false;
  }

  @Override
  protected void updateAxisRange() {
    super.updateAxisRange();
    valid = false;
  }

  @Override
  protected void layoutPlotChildren() {
    super.layoutPlotChildren();
    if (!valid && available) {
      recalculateData();
      valid = true;
    }
  }

  @VisibleForTesting
  Region getChartBackground() {
    return chartBackground;
  }
}
