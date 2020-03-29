package com.faforever.client.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.replay.Replay.PlayerStats;
import com.faforever.client.util.RatingUtil;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class RatingChangeLabelController implements Controller<Node> {
  private static final PseudoClass POSITIVE = PseudoClass.getPseudoClass("positive");
  private static final PseudoClass NEGATIVE = PseudoClass.getPseudoClass("negative");
  public Label ratingChangeLabelRoot;
  private final I18n i18n;

  @Override
  public Node getRoot() {
    return ratingChangeLabelRoot;
  }

  @Override
  public void initialize() {
    ratingChangeLabelRoot.setVisible(false);
  }

  public void setRatingChange(PlayerStats playerStats) {
    if (playerStats.getAfterMean() == null || playerStats.getAfterDeviation() == null) {
      return;
    }
    int newRating = RatingUtil.getRating(playerStats.getAfterMean(), playerStats.getAfterDeviation());
    int oldRating = RatingUtil.getRating(playerStats.getBeforeMean(), playerStats.getBeforeDeviation());

    int ratingChange = newRating - oldRating;
    ratingChangeLabelRoot.setText(i18n.numberWithSign(ratingChange));
    ratingChangeLabelRoot.pseudoClassStateChanged(ratingChange < 0 ? NEGATIVE : POSITIVE, true);

    ratingChangeLabelRoot.setVisible(true);
  }
}
