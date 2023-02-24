package com.faforever.client.game;

import com.faforever.client.domain.GamePlayerStatsBean;
import com.faforever.client.domain.LeaderboardRatingJournalBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.util.RatingUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
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

  private final I18n i18n;

  private final ObjectProperty<GamePlayerStatsBean> playerStats = new SimpleObjectProperty<>();

  public Label ratingChangeLabelRoot;

  @Override
  public Node getRoot() {
    return ratingChangeLabelRoot;
  }

  @Override
  public void initialize() {
    ratingChangeLabelRoot.visibleProperty().bind(playerStats.isNotNull());
    ObservableValue<Integer> ratingChangeObservable = playerStats.flatMap(stats -> Bindings.valueAt(stats.getLeaderboardRatingJournals(), 0))
        .map(RatingChangeLabelController::getRatingChange);
    ratingChangeLabelRoot.textProperty().bind(ratingChangeObservable.map(i18n::numberWithSign));
    ratingChangeObservable.addListener((observable, oldValue, newValue) -> onRatingChanged(oldValue, newValue));
  }

  private void onRatingChanged(Integer oldValue, Integer newValue) {
    if (oldValue != null) {
      ratingChangeLabelRoot.pseudoClassStateChanged(oldValue < 0 ? NEGATIVE : POSITIVE, false);
    }

    if (newValue != null) {
      ratingChangeLabelRoot.pseudoClassStateChanged(newValue < 0 ? NEGATIVE : POSITIVE, true);
    }
  }

  private static Integer getRatingChange(LeaderboardRatingJournalBean ratingJournal) {
    if (ratingJournal.getMeanAfter() != null && ratingJournal.getDeviationAfter() != null) {
      int newRating = RatingUtil.getRating(ratingJournal.getMeanAfter(), ratingJournal.getDeviationAfter());
      int oldRating = RatingUtil.getRating(ratingJournal.getMeanBefore(), ratingJournal.getDeviationBefore());

      return newRating - oldRating;
    }

    return null;
  }

  public void setRatingChange(GamePlayerStatsBean playerStats) {
    this.playerStats.set(playerStats);
  }
}
