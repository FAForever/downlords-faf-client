package com.faforever.client.chat;

import com.faforever.client.achievements.AchievementService;
import com.faforever.client.api.dto.AchievementState;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameDetailController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.client.util.RatingUtil;
import com.neovisionaries.i18n.CountryCode;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class PrivateUserInfoController {
  private final CountryFlagService countryFlagService;
  private final I18n i18n;
  private final AchievementService achievementService;

  public ImageView userImageView;
  public Label usernameLabel;
  public ImageView countryImageView;
  public Label countryLabel;
  public Label globalRatingLevel;
  public Label leaderboardRatingLevel;
  public Label gamesPlayedLabel;
  public GameDetailController gameDetailController;
  public Pane gameDetailWrapper;
  public Label unlockedAchievementsLabel;
  private Player player;

  public PrivateUserInfoController(CountryFlagService countryFlagService, I18n i18n, AchievementService achievementService) {
    this.countryFlagService = countryFlagService;
    this.i18n = i18n;
    this.achievementService = achievementService;
  }

  public void initialize() {
    onPlayerGameChanged(null);

    gameDetailWrapper.managedProperty().bind(gameDetailWrapper.visibleProperty());
  }

  public void setPlayer(Player player) {
    this.player = player;
    CountryCode countryCode = CountryCode.getByCode(player.getCountry());

    usernameLabel.setText(player.getUsername());
    userImageView.setImage(IdenticonUtil.createIdenticon(player.getId()));
    countryFlagService.loadCountryFlag(player.getCountry()).ifPresent(image -> countryImageView.setImage(image));
    countryLabel.setText(countryCode == null ? player.getCountry() : countryCode.getName());

    JavaFxUtil.addListener(player.globalRatingMeanProperty(), (observable) -> loadReceiverGlobalRatingInformation(player));
    JavaFxUtil.addListener(player.globalRatingDeviationProperty(), (observable) -> loadReceiverGlobalRatingInformation(player));
    loadReceiverGlobalRatingInformation(player);

    JavaFxUtil.addListener(player.leaderboardRatingMeanProperty(), (observable) -> loadReceiverLadderRatingInformation(player));
    JavaFxUtil.addListener(player.leaderboardRatingDeviationProperty(), (observable) -> loadReceiverLadderRatingInformation(player));
    loadReceiverLadderRatingInformation(player);

    JavaFxUtil.addListener(player.gameProperty(), observable -> onPlayerGameChanged(player.getGame()));
    onPlayerGameChanged(player.getGame());

    gamesPlayedLabel.textProperty().bind(player.numberOfGamesProperty().asString());

    populateUnlockedAchievementsLabel(player);
  }

  private CompletableFuture<CompletableFuture<Void>> populateUnlockedAchievementsLabel(Player player) {
    return achievementService.getAchievementDefinitions()
        .thenApply(achievementDefinitions -> {
          int totalAchievements = achievementDefinitions.size();
          return achievementService.getPlayerAchievements(player.getId())
              .thenAccept(playerAchievements -> {
                long unlockedAchievements = playerAchievements.stream()
                    .filter(playerAchievement -> playerAchievement.getState() == AchievementState.UNLOCKED)
                    .count();

                Platform.runLater(() -> unlockedAchievementsLabel.setText(
                    i18n.get("chat.privateMessage.achievements.unlockedFormat", unlockedAchievements, totalAchievements))
                );
              })
              .exceptionally(throwable -> {
                log.warn("Could not load achievements for player '" + player.getId(), throwable);
                return null;
              });
        });
  }

  private void onPlayerGameChanged(Game newGame) {
    gameDetailController.setGame(newGame);
    gameDetailWrapper.setVisible(newGame != null);
  }

  private void loadReceiverGlobalRatingInformation(Player player) {
    Platform.runLater(() -> globalRatingLevel.setText(i18n.get("chat.privateMessage.ratingFormat",
        RatingUtil.getRating(player.getGlobalRatingMean(), player.getGlobalRatingDeviation()))));
  }

  private void loadReceiverLadderRatingInformation(Player player) {
    Platform.runLater(() -> leaderboardRatingLevel.setText(i18n.get("chat.privateMessage.ratingFormat",
        RatingUtil.getRating(player.getLeaderboardRatingMean(), player.getLeaderboardRatingDeviation()))));
  }
}
