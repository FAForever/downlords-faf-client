package com.faforever.client.game;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.SimpleChangeListener;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.WatchButtonController;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.PopupUtil;
import com.faforever.client.util.RatingUtil;
import com.faforever.client.util.TimeService;
import com.faforever.commons.lobby.GameStatus;
import com.faforever.commons.lobby.GameType;
import com.google.common.annotations.VisibleForTesting;
import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Component
@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class GameDetailController extends NodeController<Pane> {

  private final I18n i18n;
  private final MapService mapService;
  private final ModService modService;
  private final TimeService timeService;
  private final UiService uiService;
  private final JoinGameHelper joinGameHelper;
  private final ContextMenuBuilder contextMenuBuilder;
  private final MapGeneratorService mapGeneratorService;
  private final NotificationService notificationService;
  private final ImageViewHelper imageViewHelper;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final ObjectProperty<GameBean> game = new SimpleObjectProperty<>();
  private final BooleanProperty playtimeVisible = new SimpleBooleanProperty();
  private final ObservableValue<Map<Integer, List<Integer>>> teams = game.flatMap(GameBean::teamsProperty)
                                                                         .orElse(Map.of());
  private final ObservableValue<String> leaderboard = game.flatMap(GameBean::leaderboardProperty);
  private final Timeline playTimeTimeline = new Timeline(new KeyFrame(Duration.ZERO, event -> updatePlaytimeValue()),
                                                         new KeyFrame(Duration.seconds(1)));

  public Pane root;
  public Label gameTypeLabel;
  public Label mapLabel;
  public Label numberOfPlayersLabel;
  public Label hostLabel;
  public VBox teamListPane;
  public StackPane mapPreviewContainer;
  public ImageView mapImageView;
  public Label gameTitleLabel;
  public Label playtimeLabel;
  public Node joinButton;
  public WatchButtonController watchButtonController;
  public Node watchButton;
  public Button generateMapButton;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(root, joinButton, watchButton, gameTitleLabel, hostLabel, mapLabel,
                                    numberOfPlayersLabel, mapPreviewContainer, gameTypeLabel, playtimeLabel,
                                    generateMapButton);
    JavaFxUtil.bind(mapPreviewContainer.visibleProperty(), mapImageView.imageProperty().isNotNull());

    contextMenuBuilder.addCopyLabelContextMenu(gameTitleLabel, mapLabel, gameTypeLabel);

    playTimeTimeline.setCycleCount(Timeline.INDEFINITE);

    root.maxWidthProperty()
        .bind(root.parentProperty().flatMap(parent -> parent instanceof Pane pane ? pane.widthProperty() : null));

    root.visibleProperty().bind(game.isNotNull());
    gameTitleLabel.textProperty()
                  .bind(game.flatMap(GameBean::titleProperty).map(StringUtils::normalizeSpace).when(showing));
    hostLabel.textProperty().bind(game.flatMap(GameBean::hostProperty).when(showing));

    ObservableValue<String> mapFolderNameObservable = game.flatMap(GameBean::mapFolderNameProperty);
    mapLabel.textProperty().bind(mapFolderNameObservable.when(showing));
    mapImageView.imageProperty()
                .bind(mapFolderNameObservable.flatMap(folderName -> Bindings.createObjectBinding(
                                                 () -> mapService.loadPreview(folderName, PreviewSize.SMALL),
                                                 mapService.isInstalledBinding(folderName)))
                                             .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable)
                                             .when(showing));

    game.flatMap(GameBean::featuredModProperty)
        .when(showing)
        .addListener((SimpleChangeListener<String>) this::onFeaturedModChanged);

    watchButtonController.gameProperty().bind(game);

    watchButton.visibleProperty()
               .bind(game.flatMap(gameBean -> gameBean.statusProperty()
                                                      .isEqualTo(GameStatus.PLAYING)
                                                      .and(gameBean.startTimeProperty().isNotNull()))
                         .orElse(false)
                         .when(showing));
    joinButton.visibleProperty()
              .bind(game.flatMap(gameBean -> gameBean.statusProperty()
                                                     .isEqualTo(GameStatus.OPEN)
                                                     .and(
                                                         gameBean.gameTypeProperty().isNotEqualTo(GameType.MATCHMAKER)))
                        .orElse(false)
                        .when(showing));

    generateMapButton.visibleProperty()
                     .bind(mapFolderNameObservable.flatMap(mapName -> Bindings.createBooleanBinding(
                         () -> mapGeneratorService.isGeneratedMap(mapName) && !mapService.isInstalled(mapName),
                         mapService.isInstalledBinding(mapName))).orElse(false).when(showing));

    playtimeLabel.visibleProperty()
                 .bind(playtimeVisible.and(BooleanExpression.booleanExpression(game.flatMap(
                                          gameBean -> gameBean.startTimeProperty()
                                                              .isNotNull()
                                                              .and(gameBean.statusProperty().isEqualTo(GameStatus.PLAYING)))))
                                      .and(playTimeTimeline.statusProperty().isEqualTo(Status.RUNNING))
                                      .when(showing));

    numberOfPlayersLabel.textProperty()
                        .bind(game.flatMap(gameBean -> Bindings.createStringBinding(
                            () -> i18n.get("game.detail.players.format", gameBean.getNumActivePlayers(),
                                           gameBean.getMaxPlayers()), gameBean.numActivePlayersProperty(),
                            gameBean.maxPlayersProperty())).when(showing));

    game.flatMap(GameBean::statusProperty).when(showing).subscribe(this::onGameStatusChanged);

    teams.when(showing).subscribe(this::populateTeamsContainer);
  }

  private void populateTeamsContainer(Map<Integer, List<Integer>> newValue) {
    CompletableFuture.supplyAsync(() -> createTeamCardControllers(newValue))
                     .thenAcceptAsync(controllers -> teamListPane.getChildren()
                                                                 .setAll(controllers.stream()
                                                                                    .map(TeamCardController::getRoot)
                                                                                    .toList()),
                                      fxApplicationThreadExecutor);
  }

  private List<TeamCardController> createTeamCardControllers(Map<Integer, List<Integer>> teamsValue) {
    return teamsValue.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry -> {
      Integer team = entry.getKey();
      List<Integer> playerIds = entry.getValue();

      TeamCardController controller = uiService.loadFxml("theme/team_card.fxml");
      controller.setRatingPrecision(RatingPrecision.ROUNDED);
      controller.ratingProviderProperty()
                .bind(leaderboard.map(
                                     name -> (Function<PlayerBean, Integer>) player -> RatingUtil.getLeaderboardRating(player, name))
                                 .when(showing));
      controller.setTeamId(team);
      controller.setPlayerIds(playerIds);
      controller.bindPlayersToPlayerIds();

      return controller;
    }).toList();
  }

  private void onFeaturedModChanged(String featuredModTechnicalName) {
    Mono.justOrEmpty(featuredModTechnicalName)
        .flatMap(modService::getFeaturedMod)
        .map(FeaturedModBean::getDisplayName)
        .switchIfEmpty(Mono.just(i18n.get("unknown")))
        .publishOn(fxApplicationThreadExecutor.asScheduler())
        .subscribe(gameTypeLabel::setText);
  }

  private void onGameStatusChanged(GameStatus gameStatus) {
    if (Objects.equals(gameStatus, GameStatus.PLAYING)) {
      startPlayTime();
    }
  }

  private void startPlayTime() {
    if (playtimeVisible.get()) {
      playTimeTimeline.play();
    }
  }

  private void updatePlaytimeValue() {
    String durationText;

    GameBean gameBean = getGame();
    if (gameBean == null || gameBean.getStartTime() == null || gameBean.getStatus() != GameStatus.PLAYING) {
      durationText = null;
      playTimeTimeline.stop();
    } else {
      durationText = timeService.shortDuration(
          java.time.Duration.between(gameBean.getStartTime(), OffsetDateTime.now()));
    }

    fxApplicationThreadExecutor.execute(() -> playtimeLabel.setText(durationText));
  }

  @Override
  protected void onHide() {
    playTimeTimeline.stop();
  }

  public void setGame(GameBean game) {
    this.game.set(game);
  }

  public GameBean getGame() {
    return game.get();
  }

  public ObjectProperty<GameBean> gameProperty() {
    return game;
  }

  public void setPlaytimeVisible(boolean visible) {
    playtimeVisible.set(visible);
  }

  @Override
  public Pane getRoot() {
    return root;
  }

  public void onJoinButtonClicked() {
    joinGameHelper.join(getGame());
  }

  public void onMapPreviewImageClicked() {
    GameBean gameBean = getGame();
    if (gameBean != null) {
      PopupUtil.showImagePopup(mapService.loadPreview(gameBean.getMapFolderName(), PreviewSize.LARGE));
    }
  }

  public void onGenerateMapClicked() {
    setGeneratingMapInProgress(true);
    mapService.generateIfNotInstalled(getGame().getMapFolderName()).exceptionally(throwable -> {
      notificationService.addImmediateErrorNotification(throwable, "game.mapGeneration.failed.title");
      return null;
    }).whenComplete((unused, throwable) -> setGeneratingMapInProgress(false));
  }

  private void setGeneratingMapInProgress(boolean inProgress) {
    fxApplicationThreadExecutor.execute(() -> {
      generateMapButton.setDisable(inProgress);
      generateMapButton.setText(
          i18n.get(inProgress ? "game.mapGeneration.notification.title" : "game.create.generatedMap"));
    });
  }

  @VisibleForTesting
  protected Timeline getPlayTimeTimeline() {
    return playTimeTimeline;
  }
}
