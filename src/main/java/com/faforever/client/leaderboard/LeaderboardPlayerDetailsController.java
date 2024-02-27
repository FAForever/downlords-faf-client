package com.faforever.client.leaderboard;

import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.LeagueSeasonBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
import javafx.beans.binding.IntegerExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Arc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Objects;


@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class LeaderboardPlayerDetailsController extends NodeController<HBox> {

  private final I18n i18n;
  private final LeaderboardService leaderboardService;

  public HBox detailsRoot;
  public Label playerDivisionNameLabel;
  public Label playerScoreLabel;
  public Label scoreLabel;
  public Arc scoreArc;
  public ImageView playerDivisionImageView;
  public Label placementLabel;

  private final ObjectProperty<LeagueEntryBean> leagueEntry = new SimpleObjectProperty<>();
  private final ObjectProperty<LeagueSeasonBean> leagueSeason = new SimpleObjectProperty<>();

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindVisibleToTextNotEmpty(playerDivisionNameLabel);
    JavaFxUtil.bindVisibleToImageNotNull(playerDivisionImageView);

    scoreLabel.setText(i18n.get("leaderboard.score").toUpperCase());

    playerDivisionNameLabel.textProperty().bind(leagueEntry.map(LeagueEntryBean::subdivision).map(subdivision -> {
      String divisionName = i18n.get("leagues.divisionName.%s".formatted(subdivision.division().nameKey()));
      return i18n.get("leaderboard.divisionName", divisionName, subdivision.nameKey());
    }).map(String::toUpperCase));

    playerScoreLabel.textProperty().bind(leagueEntry.map(LeagueEntryBean::score).map(i18n::number).orElse("/"));

    placementLabel.visibleProperty()
                  .bind(leagueEntry.map(LeagueEntryBean::subdivision).map(Objects::isNull).orElse(true).when(showing));

    IntegerExpression returningGames = IntegerExpression.integerExpression(
        leagueSeason.map(LeagueSeasonBean::placementGamesReturningPlayer));
    IntegerExpression defaultGames = IntegerExpression.integerExpression(
        leagueSeason.map(LeagueSeasonBean::placementGames));
    placementLabel.textProperty().bind(leagueEntry.flatMap(leagueEntry -> {
      IntegerExpression gamesNeeded = leagueEntry.returningPlayer() ? returningGames : defaultGames;
      return gamesNeeded.map(needed -> i18n.get("leaderboard.placement", leagueEntry.gamesPlayed(), needed))
                        .orElse(i18n.get("leaderboard.noEntry"))
                        .when(showing);
    }));

    playerDivisionImageView.imageProperty()
                           .bind(leagueEntry.map(LeagueEntryBean::subdivision)
                                            .map(SubdivisionBean::imageUrl)
                                            .map(leaderboardService::loadDivisionImage));

    scoreArc.lengthProperty()
            .bind(leagueEntry.map(
                                 leagueEntry -> leagueEntry.score().doubleValue() / leagueEntry.subdivision().highestScore() * -360d)
                             .when(showing));
  }

  public LeagueEntryBean getLeagueEntry() {
    return leagueEntry.get();
  }

  public ObjectProperty<LeagueEntryBean> leagueEntryProperty() {
    return leagueEntry;
  }

  public void setLeagueEntry(LeagueEntryBean leagueEntry) {
    this.leagueEntry.set(leagueEntry);
  }

  public LeagueSeasonBean getLeagueSeason() {
    return leagueSeason.get();
  }

  public ObjectProperty<LeagueSeasonBean> leagueSeasonProperty() {
    return leagueSeason;
  }

  public void setLeagueSeason(LeagueSeasonBean leagueSeason) {
    this.leagueSeason.set(leagueSeason);
  }

  @Override
  public HBox getRoot() {
    return detailsRoot;
  }
}
