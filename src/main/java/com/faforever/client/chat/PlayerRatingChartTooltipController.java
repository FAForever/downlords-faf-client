package com.faforever.client.chat;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlayerRatingChartTooltip;
import com.faforever.client.util.TimeService;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@RequiredArgsConstructor
public class PlayerRatingChartTooltipController implements PlayerRatingChartTooltip {

  private final TimeService timeService;

  public Pane root;
  public Label dateLabel;
  public Label ratingLabel;

  @Override
  public void initialize() {  }

  @Override
  public Node getRoot() {
    return root;
  }

  @Override
  public void setXY(long dateValueInSec, int rating) {
    JavaFxUtil.runLater(() -> {
      dateLabel.setText(timeService.asDate(Instant.ofEpochSecond(dateValueInSec)));
      ratingLabel.setText(Long.toString(Math.round(rating)));
    });
  }

  @Override
  public void clear() {
    JavaFxUtil.runLater(() -> {
      dateLabel.setText("");
      ratingLabel.setText("");
    });
  }
}
