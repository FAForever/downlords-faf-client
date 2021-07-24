package com.faforever.client.chat;

import com.faforever.client.achievements.AchievementService;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameDetailController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardRating;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.player.Player;
import com.faforever.client.util.Assert;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.client.util.RatingUtil;
import com.faforever.commons.api.dto.AchievementState;
import com.google.common.eventbus.EventBus;
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
  private final I18n i18n;
  private final AchievementService achievementService;
  private final LeaderboardService leaderboardService;
  private final EventBus eventBus;
  private final ChatUserService chatUserService;
  public ImageView userImageView;
  public Label usernameLabel;
  public ImageView countryImageView;
  public Label countryLabel;
  public Label ratingsLabels;
  public Label ratingsValues;
  public Label gamesPlayedLabel;
  public GameDetailController gameDetailController;
  public Pane gameDetailWrapper;
  public Label unlockedAchievementsLabel;
  public Node privateUserInfoRoot;
  public Label gamesPlayedLabelLabel;
  public Label unlockedAchievementsLabelLabel;

  private ChatChannelUser chatUser;
  private InvalidationListener gameInvalidationListener;
  private InvalidationListener chatUserPropertiesInvalidationListener;
  private InvalidationListener ratingInvalidationListener;

  public PrivateUserInfoController(I18n i18n, AchievementService achievementService, LeaderboardService leaderboardService,
                                   EventBus eventBus, ChatUserService chatUserService) {
    this.i18n = i18n;
    this.achievementService = achievementService;
    this.leaderboardService = leaderboardService;
    this.eventBus = eventBus;
    this.chatUserService = chatUserService;
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
        ratingsLabels,
        ratingsValues,
        gamesPlayedLabelLabel,
        unlockedAchievementsLabelLabel
    );
    initializeListeners();
    onPlayerGameChanged(null);
  }

  private void initializeListeners() {
    ratingInvalidationListener = observable -> chatUser.getPlayer().ifPresent(this::loadReceiverRatingInformation);
    gameInvalidationListener = observable -> chatUser.getPlayer().ifPresent(chatPlayer -> onPlayerGameChanged(chatPlayer.getGame()));
    chatUserPropertiesInvalidationListener = observable -> JavaFxUtil.runLater(() -> {
      usernameLabel.setText(this.chatUser.getUsername());
      countryImageView.setImage(this.chatUser.getCountryFlag().orElse(null));
      countryLabel.setText(this.chatUser.getCountryName().orElse(""));
    });
  }

  public void setChatUser(@NotNull ChatChannelUser chatUser) {
    Assert.checkNotNullIllegalState(this.chatUser, "Chat user is already set");
    this.chatUser = chatUser;
    this.chatUser.setDisplayed(true);
    this.chatUser.getPlayer().ifPresentOrElse(this::displayPlayerInfo, () -> {
      this.chatUser.playerProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue != null) {
          displayPlayerInfo(newValue);
        }
      });
      displayChatUserInfo();
    });
    JavaFxUtil.addAndTriggerListener(this.chatUser.usernameProperty(), new WeakInvalidationListener(chatUserPropertiesInvalidationListener));
    JavaFxUtil.addListener(this.chatUser.countryFlagProperty(), new WeakInvalidationListener(chatUserPropertiesInvalidationListener));
    JavaFxUtil.addListener(this.chatUser.countryNameProperty(), new WeakInvalidationListener(chatUserPropertiesInvalidationListener));
  }

  private void displayChatUserInfo() {
    onPlayerGameChanged(null);
    setPlayerInfoVisible(false);
  }

  private void setPlayerInfoVisible(boolean visible) {
    userImageView.setVisible(visible);
    countryLabel.setVisible(visible);
    ratingsLabels.setVisible(visible);
    ratingsValues.setVisible(visible);
    gamesPlayedLabel.setVisible(visible);
    gamesPlayedLabelLabel.setVisible(visible);
    unlockedAchievementsLabel.setVisible(visible);
    unlockedAchievementsLabelLabel.setVisible(visible);
  }

  private void displayPlayerInfo(Player player) {
    chatUserService.associatePlayerToChatUser(chatUser, player);
    setPlayerInfoVisible(true);

    userImageView.setImage(IdenticonUtil.createIdenticon(player.getId()));
    userImageView.setVisible(true);

    JavaFxUtil.addAndTriggerListener(player.leaderboardRatingMapProperty(), new WeakInvalidationListener(ratingInvalidationListener));
    JavaFxUtil.addAndTriggerListener(player.gameProperty(), new WeakInvalidationListener(gameInvalidationListener));

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

                JavaFxUtil.runLater(() -> unlockedAchievementsLabel.setText(
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

  private void loadReceiverRatingInformation(Player player) {
    leaderboardService.getLeaderboards().thenAccept(leaderboards -> {
      StringBuilder ratingNames = new StringBuilder();
      StringBuilder ratingNumbers = new StringBuilder();
      leaderboards.forEach(leaderboard -> {
        LeaderboardRating leaderboardRating = player.getLeaderboardRatings().get(leaderboard.getTechnicalName());
        if (leaderboardRating != null) {
          String leaderboardName = i18n.getOrDefault(leaderboard.getTechnicalName(), leaderboard.getNameKey());
          ratingNames.append(i18n.get("leaderboard.rating", leaderboardName)).append("\n\n");
          ratingNumbers.append(i18n.number(RatingUtil.getLeaderboardRating(player, leaderboard))).append("\n\n");
        }
      });
      JavaFxUtil.runLater(() -> {
        ratingsLabels.setText(ratingNames.toString());
        ratingsValues.setText(ratingNumbers.toString());
        gamesPlayedLabel.setText(i18n.number(player.getNumberOfGames()));
      });
    });
  }
}

