package com.faforever.client.game;

import com.faforever.client.domain.GamePlayerStatsBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
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

  public void setRatingChange(GamePlayerStatsBean playerStats) {
    playerStats.getLeaderboardRatingJournals().stream().findFirst()
        .ifPresent(ratingChange -> {
          if (ratingChange.getMeanAfter() != null && ratingChange.getDeviationAfter() != null) {
            int newRating = RatingUtil.getRating(ratingChange.getMeanAfter(), ratingChange.getDeviationAfter());
            int oldRating = RatingUtil.getRating(ratingChange.getMeanBefore(), ratingChange.getDeviationBefore());

            int ratingChangeValue = newRating - oldRating;
            ratingChangeLabelRoot.setText(i18n.numberWithSign(ratingChangeValue));
            ratingChangeLabelRoot.pseudoClassStateChanged(ratingChangeValue < 0 ? NEGATIVE : POSITIVE, true);

            ratingChangeLabelRoot.setVisible(true);
          }
        });
  }
}
