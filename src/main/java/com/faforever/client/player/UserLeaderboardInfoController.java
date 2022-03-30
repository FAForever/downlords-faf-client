package com.faforever.client.player;

import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.util.RatingUtil;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@RequiredArgsConstructor
public class UserLeaderboardInfoController implements Controller<Node> {
  
  private final I18n i18n;
  private final LeaderboardService leaderboardService;
  public ImageView divisionImage;
  public Label divisionLabel;
  public Label leaderboardNameLabel;
  public Label gamesPlayedLabel;
  public Label ratingLabel;
  public VBox root;

  @Override
  public void initialize() {
    JavaFxUtil.bindManagedToVisible(divisionImage, divisionLabel);
  }

  @Override
  public VBox getRoot() {
    return root;
  }

  public void setLeaderboardInfo(PlayerBean player, LeaderboardBean leaderboard) {
    String leaderboardName = i18n.getOrDefault(leaderboard.getTechnicalName(), leaderboard.getNameKey());
    String gameNumber = i18n.get("leaderboard.gameNumber", player.getNumberOfGames(leaderboard.getTechnicalName()));
    String ratingNumber = i18n.get("leaderboard.rating", RatingUtil.getLeaderboardRating(player, leaderboard));

    JavaFxUtil.runLater(() -> {
      leaderboardNameLabel.setText(leaderboardName);
      gamesPlayedLabel.setText(gameNumber);
      ratingLabel.setText(ratingNumber);
    });
  }

  public void setLeagueInfo(LeagueEntryBean leagueEntry) {
    Image image = leaderboardService.loadDivisionImage(leagueEntry.getSubdivision().getImageUrl());
    String divisionName = i18n.get("leaderboard.divisionName",
        i18n.getOrDefault(leagueEntry.getSubdivision().getDivision().getNameKey(), leagueEntry.getSubdivision().getDivisionI18nKey()),
        leagueEntry.getSubdivision().getNameKey()).toUpperCase();

    JavaFxUtil.runLater(() -> {
      divisionImage.setImage(image);
      divisionImage.setVisible(true);
      divisionLabel.setText(divisionName);
      divisionLabel.setVisible(true);
    });
  }

  public void setUnrankedLeague() {
    JavaFxUtil.runLater(() -> {
      try {
        divisionImage.setImage(leaderboardService.loadDivisionImage(new URL("https://content.faforever.com/divisions/icons/unranked.png")));
        divisionImage.setVisible(true);
        divisionLabel.setText(i18n.get("teammatchmaking.inPlacement").toUpperCase());
        divisionLabel.setVisible(true);
      } catch (MalformedURLException ignored) {}
    });
  }
}
