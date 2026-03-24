package com.faforever.client.player;

import com.faforever.client.fx.JavaFxUtil;
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
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Player rating line chart with support for displaying more accurate values via the tooltip on mouse hover
 */
@Slf4j
public class PlayerRatingChart extends LineChart<Number, Number> {

  private static final int OFFSET_TOOLTIP_FROM_CURSOR_BY_X = 20; // in px
  private static final int OFFSET_TOOLTIP_FROM_CURSOR_BY_Y = 15; // in px
  private static final int DRAG_THRESHOLD_PX = 5; // minimum pixels before drag is recognized

  private final Region chartBackground;
  private final Line verticalLine = new Line(0, 0, 0, 0);
  private final Rectangle selectionRect = new Rectangle();
  private PlayerRatingChartTooltipController tooltipController;
  private Tooltip hoverTooltip;
  private boolean available = true;

  private boolean valid = false;
  private boolean isDragging = false;
  private double selectionStartX;
  private long selectionStartTimeSec = Long.MIN_VALUE;
  private long selectionEndTimeSec = Long.MIN_VALUE;
  private Consumer<long[]> selectionListener;

  private final Map<Integer, Integer> ratingMap = new HashMap<>(); // key - X coordinate of chart background

  public PlayerRatingChart(@NamedArg("xAxis") Axis<Number> xAxis, @NamedArg("yAxis") Axis<Number> yAxis) {
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
      chartBackground.setOnMousePressed(this::onMousePressed);
      chartBackground.setOnMouseDragged(this::onMouseDragged);
      chartBackground.setOnMouseReleased(this::onMouseReleased);
      addVerticalLine();
      addSelectionRect();
    }
  }

  private void addVerticalLine() {
    getPlotChildren().add(verticalLine);
    JavaFxUtil.bind(verticalLine.endYProperty(), chartBackground.heightProperty());
  }

  private void addSelectionRect() {
    selectionRect.setVisible(false);
    selectionRect.getStyleClass().add("rating-selection-rect");
    JavaFxUtil.bind(selectionRect.heightProperty(), chartBackground.heightProperty());
    getPlotChildren().add(selectionRect);
  }

  public void setSelectionListener(Consumer<long[]> listener) {
    this.selectionListener = listener;
  }

  public void clearSelection() {
    isDragging = false;
    selectionStartTimeSec = Long.MIN_VALUE;
    selectionEndTimeSec = Long.MIN_VALUE;
    selectionRect.setVisible(false);
    if (selectionListener != null) {
      selectionListener.accept(null);
    }
  }

  private void onMousePressed(MouseEvent event) {
    selectionStartX = event.getX();
    isDragging = false;
  }

  private void onMouseDragged(MouseEvent event) {
    if (!valid) {
      return;
    }
    double currentX = event.getX();
    if (!isDragging && Math.abs(currentX - selectionStartX) < DRAG_THRESHOLD_PX) {
      return;
    }
    isDragging = true;

    double startX = Math.min(selectionStartX, currentX);
    double endX = Math.max(selectionStartX, currentX);
    selectionRect.setX(startX);
    selectionRect.setY(0);
    selectionRect.setWidth(endX - startX);
    selectionRect.setVisible(true);

    // hide hover elements during drag
    if (hoverTooltip != null) {
      hoverTooltip.hide();
    }
    verticalLine.setVisible(false);
  }

  private void onMouseReleased(MouseEvent event) {
    if (isDragging) {
      isDragging = false;
      verticalLine.setVisible(true);
      if (selectionListener != null && valid) {
        double startX = Math.min(selectionStartX, event.getX());
        double endX = Math.max(selectionStartX, event.getX());
        long startTime = getDisplayedDateValue(startX);
        long endTime = getDisplayedDateValue(endX);
        if (startTime != Long.MIN_VALUE && endTime != Long.MIN_VALUE) {
          selectionStartTimeSec = startTime;
          selectionEndTimeSec = endTime;
          selectionListener.accept(new long[]{startTime, endTime});
        }
      }
    } else {
      // plain click — clear the selection
      clearSelection();
    }
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
    if (valid && !isDragging) {
      int x = (int) event.getX();
      long dateValueInSec = getDisplayedDateValue(x);
      Integer rating = ratingMap.get(x);
      if (rating != null && dateValueInSec != Long.MIN_VALUE) {
        tooltipController.setDateAndRating(dateValueInSec, rating);
      } else {
        tooltipController.clear();
      }
    }
  }

  private void moveTooltipAndLineToCursor(MouseEvent event) {
    if (!isDragging) {
      hoverTooltip.setX(event.getScreenX() + OFFSET_TOOLTIP_FROM_CURSOR_BY_X);
      hoverTooltip.setY(event.getScreenY() + OFFSET_TOOLTIP_FROM_CURSOR_BY_Y);
      verticalLine.setLayoutX(event.getX());
    }
  }

  private void recalculateData() {
    ratingMap.clear();
    for (int i = 0; i < getData().size(); i++) {
      Series<Number, Number> series = getData().get(i);
      if (series.getNode() instanceof Path path) {
        buildData(path.getElements());
        break;
      }
    }
  }

  private void buildData(List<PathElement> elements) {
    List<LineTo> lines = elements.stream()
        .filter((element) -> element instanceof LineTo)
        .map((element) -> (LineTo) element)
        .toList();
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
        putMapRatingValue(++leftXCoordinate, rating1);
      }
    }
  }

  private void putMapRatingValue(int xCoordinate, double rating) {
    ratingMap.merge(xCoordinate, (int) rating, (currentRating, newRating) -> (currentRating + newRating) / 2); // average
  }

  private long getDisplayedDateValue(double displayPosition) {
    return Optional.ofNullable(getXAxis().getValueForDisplay(displayPosition))
        .map(Number::longValue)
        .orElse(Long.MIN_VALUE);
  }

  private int getDisplayedRatingValue(double displayPosition) {
    return Optional.ofNullable(getYAxis().getValueForDisplay(displayPosition))
        .map(Number::intValue)
        .orElse(Integer.MIN_VALUE);
  }

  @Override
  protected void seriesChanged(Change<? extends Series> change) {
    super.seriesChanged(change);
    valid = false;
  }

  @Override
  protected void seriesAdded(Series<Number, Number> series, int seriesIndex) {
    super.seriesAdded(series, seriesIndex);
    valid = false;
  }

  @Override
  protected void updateAxisRange() {
    super.updateAxisRange();
    valid = false;
  }

  private void repositionSelectionRect() {
    if (selectionStartTimeSec == Long.MIN_VALUE || selectionEndTimeSec == Long.MIN_VALUE) {
      return;
    }
    double startPx = getXAxis().getDisplayPosition(selectionStartTimeSec);
    double endPx = getXAxis().getDisplayPosition(selectionEndTimeSec);
    if (startPx >= 0 && endPx > startPx) {
      selectionRect.setX(startPx);
      selectionRect.setWidth(endPx - startPx);
      selectionRect.setVisible(true);
    }
  }

  @Override
  protected void layoutPlotChildren() {
    super.layoutPlotChildren();
    if (!valid && available) {
      recalculateData();
      valid = true;
    }
    if (available && !isDragging) {
      repositionSelectionRect();
    }
  }

  @VisibleForTesting
  public Region getChartBackground() {
    return chartBackground;
  }
}
