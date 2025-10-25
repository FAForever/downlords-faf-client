package com.faforever.client.player;

import com.faforever.client.achievements.AchievementService;
import com.faforever.client.domain.api.Leaderboard;
import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.game.GameDetailController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.client.util.RatingUtil;
import com.faforever.commons.api.dto.AchievementState;
import com.faforever.commons.api.dto.PlayerAchievement;
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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class PrivatePlayerInfoController extends NodeController<Node> {

  private final I18n i18n;
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

  @Getter
  private final ObjectProperty<PlayerInfo> playerProperty = new SimpleObjectProperty<>();

  private final ChangeListener<PlayerInfo> playerChangeListener = (observable, oldValue, newValue) -> {
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
    JavaFxUtil.bindManagedToVisible(gameDetailWrapper, country, gamesPlayed, unlockedAchievements, ratingsLabels,
                                    ratingsValues, gamesPlayedLabel, unlockedAchievementsLabel, separator);
    JavaFxUtil.bind(separator.visibleProperty(), gameDetailWrapper.visibleProperty());
    gameDetailController.setPlaytimeVisible(true);
    gameDetailWrapper.setVisible(false);

    ObservableValue<Boolean> playerExistsProperty = playerProperty.isNotNull().when(showing);
    userImageView.visibleProperty().bind(playerExistsProperty);
    country.visibleProperty().bind(playerExistsProperty);
    ratingsLabels.visibleProperty().bind(playerExistsProperty);
    ratingsValues.visibleProperty().bind(playerExistsProperty);
    gamesPlayed.visibleProperty().bind(playerExistsProperty);
    gamesPlayedLabel.visibleProperty().bind(playerExistsProperty);
    unlockedAchievements.visibleProperty().bind(playerExistsProperty);
    unlockedAchievementsLabel.visibleProperty().bind(playerExistsProperty);

    gamesPlayed.textProperty()
               .bind(playerProperty.flatMap(PlayerInfo::numberOfGamesProperty).map(i18n::number).when(showing));

    username.textProperty().bind(playerProperty.map(PlayerInfo::getUsername).when(showing));
    country.textProperty()
           .bind(playerProperty.flatMap(PlayerInfo::countryProperty).map(i18n::getCountryNameLocalized).when(showing));
    userImageView.imageProperty()
                 .bind(playerProperty.map(PlayerInfo::getId).map(IdenticonUtil::createIdenticon).when(showing));
    ObservableValue<GameInfo> gameObservable = playerProperty.flatMap(PlayerInfo::gameProperty);
    gameDetailController.gameProperty().bind(gameObservable.when(showing));
    gameDetailWrapper.visibleProperty().bind(gameObservable.flatMap(GameInfo::statusProperty)
                                         .map(status -> status == GameStatus.OPEN || status == GameStatus.PLAYING)
                                         .orElse(false)
                                         .when(showing));
    playerProperty.addListener(playerChangeListener);
  }

  private void populateUnlockedAchievementsLabel(PlayerInfo player) {
    Mono<Long> totalAchievementsMono = achievementService.getAchievementDefinitions().count();
    Mono<Long> numAchievementsUnlockedMono = achievementService.getPlayerAchievements(player.getId())
                                                               .map(PlayerAchievement::getState)
                                                               .filter(AchievementState.UNLOCKED::equals)
                                                               .count();
    Mono.zip(totalAchievementsMono, numAchievementsUnlockedMono)
        .publishOn(fxApplicationThreadExecutor.asScheduler())
        .subscribe(TupleUtils.consumer((totalAchievements, numUnlockedAchievements) -> unlockedAchievements.setText(
                       i18n.get("chat.privateMessage.achievements.unlockedFormat", numUnlockedAchievements, totalAchievements))),
                   throwable -> log.error("Could not load achievements for player '" + player.getId(), throwable));
  }

  private void loadReceiverRatingInformation(PlayerInfo player) {
    Flux<Leaderboard> leaderboardFlux = leaderboardService.getLeaderboards()
                                                          .filter(leaderboard -> player.getLeaderboardRatings()
                                                                                           .containsKey(
                                                                                               leaderboard.technicalName()))
                                                          .cache();

    leaderboardFlux.map(leaderboard -> i18n.getOrDefault(leaderboard.technicalName(), leaderboard.nameKey()))
                   .collect(Collectors.joining("\n\n"))
                   .publishOn(fxApplicationThreadExecutor.asScheduler())
                   .subscribe(ratingsLabels::setText);

    leaderboardFlux.map(leaderboard -> RatingUtil.getLeaderboardRating(player, leaderboard))
                   .map(i18n::number)
                   .collect(Collectors.joining("\n\n"))
                   .publishOn(fxApplicationThreadExecutor.asScheduler())
                   .subscribe(ratingsValues::setText);
  }
}

