package com.faforever.client.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.replay.Replay.PlayerStats;
import com.faforever.client.util.RatingUtil;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RatingChangeLabelController implements Controller<Node> {
  private static final PseudoClass POSITIVE = PseudoClass.getPseudoClass("positive");
  private static final PseudoClass NEGATIVE = PseudoClass.getPseudoClass("negative");
  public Label ratingChangLabelRoot;
  private final I18n i18n;


  public RatingChangeLabelController(I18n i18n) {
    this.i18n = i18n;
  }

  @Override
  public Node getRoot() {
    return ratingChangLabelRoot;
  }

  @Override
  public void initialize() {
    ratingChangLabelRoot.setVisible(false);
  }

  public void setRatingChange(PlayerStats playerStats) {
    if (playerStats.getAfterMean() == null || playerStats.getAfterDeviation() == null) {
      return;
    }
    int newRating = RatingUtil.getRating(playerStats.getAfterMean(), playerStats.getAfterDeviation());
    int oldRating = RatingUtil.getRating(playerStats.getBeforeMean(), playerStats.getBeforeDeviation());

    int ratingChange = newRating - oldRating;
    ratingChangLabelRoot.setText(i18n.numberWithSign(ratingChange));
    ratingChangLabelRoot.pseudoClassStateChanged(ratingChange < 0 ? NEGATIVE : POSITIVE, true);

    ratingChangLabelRoot.setVisible(true);
  }
}
