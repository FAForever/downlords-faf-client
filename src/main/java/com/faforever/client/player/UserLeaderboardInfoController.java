package com.faforever.client.player;

import com.faforever.client.domain.api.Leaderboard;
import com.faforever.client.domain.api.LeagueEntry;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.util.RatingUtil;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@Slf4j
@RequiredArgsConstructor
public class UserLeaderboardInfoController extends NodeController<Node> {

  private final I18n i18n;
  private final LeaderboardService leaderboardService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public ImageView divisionImage;
  public Label divisionLabel;
  public Label leaderboardNameLabel;
  public Label gamesPlayedLabel;
  public Label ratingLabel;
  public VBox root;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(divisionImage, divisionLabel);

    try {
      divisionImage.setImage(
          new Image(new ClassPathResource("/images/leagueUnlistedDivision.png").getURL().toString(), true));
      divisionImage.setVisible(true);
    } catch (IOException e) {
      log.error("Could not load unlisted division image.", e);
    }
    divisionLabel.setText(i18n.get("teammatchmaking.inPlacement").toUpperCase());
    divisionLabel.setVisible(true);
  }

  @Override
  public VBox getRoot() {
    return root;
  }

  public void setLeaderboardInfo(PlayerInfo player, Leaderboard leaderboard) {
    String leaderboardName = i18n.getOrDefault(leaderboard.technicalName(), leaderboard.nameKey());
    String gameNumber = i18n.get("leaderboard.gameNumber",
                                 player.getNumberOfGamesForLeaderboard(leaderboard.technicalName()));
    String ratingNumber = i18n.get("leaderboard.rating", RatingUtil.getLeaderboardRating(player, leaderboard));

    fxApplicationThreadExecutor.execute(() -> {
      leaderboardNameLabel.setText(leaderboardName);
      gamesPlayedLabel.setText(gameNumber);
      ratingLabel.setText(ratingNumber);
    });
  }

  public void setLeagueInfo(LeagueEntry leagueEntry) {
    Image image = leaderboardService.loadDivisionImage(leagueEntry.subdivision().imageUrl());
    String divisionNameKey = leagueEntry.subdivision().division().nameKey();
    String divisionName = i18n.get("leaderboard.divisionName", i18n.getOrDefault(divisionNameKey,
                                                                                 "leagues.divisionName.%s".formatted(
                                                                                     divisionNameKey)),
                                   leagueEntry.subdivision().nameKey()).toUpperCase();

    fxApplicationThreadExecutor.execute(() -> {
      divisionImage.setImage(image);
      divisionImage.setVisible(true);
      divisionLabel.setText(divisionName);
      divisionLabel.setVisible(true);
    });
  }
}
