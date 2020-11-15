package com.faforever.client.chat;

import com.faforever.client.achievements.AchievementService;
import com.faforever.client.api.dto.AchievementState;
import com.faforever.client.chat.event.ChatUserPopulateEvent;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameDetailController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.client.util.RatingUtil;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class PrivateUserInfoController implements Controller<Node> {
  private final CountryFlagService countryFlagService;
  private final I18n i18n;
  private final AchievementService achievementService;
  private final EventBus eventBus;
  private ChatChannelUser chatUser;

  public ImageView userImageView;
  public Label usernameLabel;
  public ImageView countryImageView;
  public Label countryLabel;
  public Label globalRatingLabel;
  public Label leaderboardRatingLabel;
  public Label gamesPlayedLabel;
  public GameDetailController gameDetailController;
  public Pane gameDetailWrapper;
  public Label unlockedAchievementsLabel;
  public Node privateUserInfoRoot;
  public Label globalRatingLabelLabel;
  public Label leaderboardRatingLabelLabel;
  public Label gamesPlayedLabelLabel;
  public Label unlockedAchievementsLabelLabel;

  @SuppressWarnings("FieldCanBeLocal")
  private InvalidationListener globalRatingInvalidationListener;
  @SuppressWarnings("FieldCanBeLocal")
  private InvalidationListener leaderboardRatingInvalidationListener;
  @SuppressWarnings("FieldCanBeLocal")
  private InvalidationListener gameInvalidationListener;

  public PrivateUserInfoController(CountryFlagService countryFlagService, I18n i18n, AchievementService achievementService, EventBus eventBus) {
    this.countryFlagService = countryFlagService;
    this.i18n = i18n;
    this.achievementService = achievementService;
    this.eventBus = eventBus;
  }

  @Override
  public Node getRoot() {
    return privateUserInfoRoot;
  }

  public void initialize() {
    JavaFxUtil.bindManagedToVisible(
        gameDetailWrapper,
        countryLabel,
        gamesPlayedLabel,
        unlockedAchievementsLabel,
        globalRatingLabel,
        leaderboardRatingLabel,
        globalRatingLabelLabel,
        leaderboardRatingLabelLabel,
        gamesPlayedLabelLabel,
        unlockedAchievementsLabelLabel
    );
    onPlayerGameChanged(null);
  }

  public void setChatUser(@NotNull ChatChannelUser chatUser) {
    this.chatUser = chatUser;
    this.chatUser.setDisplayed(true);
    this.chatUser.getPlayer().ifPresentOrElse(this::displayPlayerInfo, () -> {
      this.chatUser.playerProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue != null) {
          displayPlayerInfo(newValue);
        } else {
          displayChatUserInfo();
        }
      });
      displayChatUserInfo();
    });
    JavaFxUtil.bind(usernameLabel.textProperty(), this.chatUser.usernameProperty());
    JavaFxUtil.bind(countryImageView.imageProperty(), this.chatUser.countryFlagProperty());
    JavaFxUtil.bind(countryLabel.textProperty(), this.chatUser.countryNameProperty());
    eventBus.post(new ChatUserPopulateEvent(this.chatUser));
  }

  private void displayChatUserInfo() {
    onPlayerGameChanged(null);
    setPlayerInfoVisible(false);
  }

  private void setPlayerInfoVisible(boolean visible) {
    userImageView.setVisible(visible);
    countryLabel.setVisible(visible);
    globalRatingLabel.setVisible(visible);
    globalRatingLabelLabel.setVisible(visible);
    leaderboardRatingLabel.setVisible(visible);
    leaderboardRatingLabelLabel.setVisible(visible);
    gamesPlayedLabel.setVisible(visible);
    gamesPlayedLabelLabel.setVisible(visible);
    unlockedAchievementsLabel.setVisible(visible);
    unlockedAchievementsLabelLabel.setVisible(visible);
  }

  private void displayPlayerInfo(Player player) {
    setPlayerInfoVisible(true);

    userImageView.setImage(IdenticonUtil.createIdenticon(player.getId()));
    userImageView.setVisible(true);

    globalRatingInvalidationListener = (observable) -> loadReceiverGlobalRatingInformation(player);
    JavaFxUtil.addListener(player.globalRatingMeanProperty(), new WeakInvalidationListener(globalRatingInvalidationListener));
    JavaFxUtil.addListener(player.globalRatingDeviationProperty(), new WeakInvalidationListener(globalRatingInvalidationListener));
    loadReceiverGlobalRatingInformation(player);

    leaderboardRatingInvalidationListener = (observable) -> loadReceiverLadderRatingInformation(player);
    JavaFxUtil.addListener(player.leaderboardRatingMeanProperty(), new WeakInvalidationListener(leaderboardRatingInvalidationListener));
    JavaFxUtil.addListener(player.leaderboardRatingDeviationProperty(), new WeakInvalidationListener(leaderboardRatingInvalidationListener));
    loadReceiverLadderRatingInformation(player);

    gameInvalidationListener = observable -> onPlayerGameChanged(player.getGame());
    JavaFxUtil.addListener(player.gameProperty(), new WeakInvalidationListener(gameInvalidationListener));
    onPlayerGameChanged(player.getGame());

    JavaFxUtil.bind(gamesPlayedLabel.textProperty(), player.numberOfGamesProperty().asString());

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
    Platform.runLater(() -> globalRatingLabel.setText(i18n.get("chat.privateMessage.ratingFormat",
        RatingUtil.getRating(player.getGlobalRatingMean(), player.getGlobalRatingDeviation()))));
  }

  private void loadReceiverLadderRatingInformation(Player player) {
    Platform.runLater(() -> leaderboardRatingLabel.setText(i18n.get("chat.privateMessage.ratingFormat",
        RatingUtil.getRating(player.getLeaderboardRatingMean(), player.getLeaderboardRatingDeviation()))));
  }
}
