package com.faforever.client.player;

import com.faforever.client.achievements.AchievementService;
import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.LeaderboardRatingBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.game.GameDetailController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.client.util.RatingUtil;
import com.faforever.commons.api.dto.AchievementState;
import com.faforever.commons.lobby.GameStatus;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class PrivatePlayerInfoController extends NodeController<Node> {

  private final I18n i18n;
  private final UiService uiService;
  private final AchievementService achievementService;
  private final LeaderboardService leaderboardService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public ImageView userImageView;
  public Label username;
  public ImageView countryImageView;
  public Label country;
  public Label ratingsLabels;
  public Label ratingsValues;
  public Label gamesPlayed;
  public GameDetailController gameDetailController;
  public Pane gameDetailWrapper;
  public Label unlockedAchievements;
  public Node privateUserInfoRoot;
  public Label gamesPlayedLabel;
  public Label unlockedAchievementsLabel;
  public Separator separator;

  private final ObjectProperty<ChatChannelUser> chatUser = new SimpleObjectProperty<>();

  private final ChangeListener<PlayerBean> playerChangeListener = (observable, oldValue, newValue) -> {
    if (newValue != null && !Objects.equals(oldValue, newValue)) {
      loadReceiverRatingInformation(newValue);
      populateUnlockedAchievementsLabel(newValue);
    }
  };

  @Override
  public Node getRoot() {
    return privateUserInfoRoot;
  }

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(gameDetailWrapper, country, gamesPlayed, unlockedAchievements,
        ratingsLabels, ratingsValues, gamesPlayedLabel, unlockedAchievementsLabel, separator);
    JavaFxUtil.bind(separator.visibleProperty(), gameDetailWrapper.visibleProperty());
    gameDetailController.setPlaytimeVisible(true);
    gameDetailWrapper.setVisible(false);

    ObservableValue<Boolean> playerExistsProperty = chatUser.flatMap(user -> user.playerProperty().isNotNull()).when(showing);
    userImageView.visibleProperty().bind(playerExistsProperty);
    country.visibleProperty().bind(playerExistsProperty);
    ratingsLabels.visibleProperty().bind(playerExistsProperty);
    ratingsValues.visibleProperty().bind(playerExistsProperty);
    gamesPlayed.visibleProperty().bind(playerExistsProperty);
    gamesPlayedLabel.visibleProperty().bind(playerExistsProperty);
    unlockedAchievements.visibleProperty().bind(playerExistsProperty);
    unlockedAchievementsLabel.visibleProperty().bind(playerExistsProperty);

    ObservableValue<PlayerBean> playerObservable = chatUser.flatMap(ChatChannelUser::playerProperty);

    gamesPlayed.textProperty()
        .bind(playerObservable
            .flatMap(PlayerBean::numberOfGamesProperty)
            .map(i18n::number)
            .when(showing));

    username.textProperty().bind(chatUser.map(ChatChannelUser::getUsername).when(showing));
    country.textProperty().bind(playerObservable
        .flatMap(PlayerBean::countryProperty)
        .map(i18n::getCountryNameLocalized).when(showing));
    userImageView.imageProperty()
        .bind(playerObservable
            .map(PlayerBean::getId)
            .map(IdenticonUtil::createIdenticon).when(showing));
    ObservableValue<GameBean> gameObservable = playerObservable
        .flatMap(PlayerBean::gameProperty);
    gameDetailController.gameProperty().bind(gameObservable.when(showing));
    gameDetailWrapper.visibleProperty().bind(gameObservable.flatMap(GameBean::statusProperty)
        .map(status -> status == GameStatus.OPEN || status == GameStatus.PLAYING)
        .orElse(false).when(showing));
    chatUser.flatMap(ChatChannelUser::playerProperty).addListener(playerChangeListener);
  }

  public void setChatUser(ChatChannelUser chatUser) {
    this.chatUser.set(chatUser);
  }

  public ChatChannelUser getChatUser() {
    return chatUser.get();
  }

  public ObjectProperty<ChatChannelUser> chatUserProperty() {
    return chatUser;
  }

  private void populateUnlockedAchievementsLabel(PlayerBean player) {
    achievementService.getAchievementDefinitions()
        .thenApply(achievementDefinitions -> {
          int totalAchievements = achievementDefinitions.size();
          return achievementService.getPlayerAchievements(player.getId())
              .thenAccept(playerAchievements -> {
                long numUnlockedAchievements = playerAchievements.stream()
                    .filter(playerAchievement -> playerAchievement.getState() == AchievementState.UNLOCKED)
                    .count();

                fxApplicationThreadExecutor.execute(() -> unlockedAchievements.setText(
                    i18n.get("chat.privateMessage.achievements.unlockedFormat", numUnlockedAchievements, totalAchievements))
                );
              })
              .exceptionally(throwable -> {
                log.error("Could not load achievements for player '" + player.getId(), throwable);
                return null;
              });
        });
  }

  private void loadReceiverRatingInformation(PlayerBean player) {
    leaderboardService.getLeaderboards().thenAccept(leaderboards -> {
      StringBuilder ratingNames = new StringBuilder();
      StringBuilder ratingNumbers = new StringBuilder();
      leaderboards.forEach(leaderboard -> {
        LeaderboardRatingBean leaderboardRating = player.getLeaderboardRatings().get(leaderboard.getTechnicalName());
        if (leaderboardRating != null) {
          String leaderboardName = i18n.getOrDefault(leaderboard.getTechnicalName(), leaderboard.getNameKey());
          ratingNames.append(i18n.get("leaderboard.rating", leaderboardName)).append("\n\n");
          ratingNumbers.append(i18n.number(RatingUtil.getLeaderboardRating(player, leaderboard))).append("\n\n");
        }
      });
      fxApplicationThreadExecutor.execute(() -> {
        ratingsLabels.setText(ratingNames.toString());
        ratingsValues.setText(ratingNumbers.toString());
      });
    });
  }
}

