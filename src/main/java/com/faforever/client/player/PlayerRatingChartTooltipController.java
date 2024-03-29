package com.faforever.client.player;

import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
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
public class PlayerRatingChartTooltipController extends NodeController<Node> {

  private final TimeService timeService;
  private final I18n i18n;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public Pane root;
  public Label dateLabel;
  public Label ratingLabel;

  @Override
  public Node getRoot() {
    return root;
  }

  public void setDateAndRating(long dateValueInSec, int rating) {
    fxApplicationThreadExecutor.execute(() -> {
      dateLabel.setText(timeService.asDate(Instant.ofEpochSecond(dateValueInSec)));
      ratingLabel.setText(i18n.number(rating));
    });
  }

  public void clear() {
    fxApplicationThreadExecutor.execute(() -> {
      dateLabel.setText("");
      ratingLabel.setText("");
    });
  }
}
