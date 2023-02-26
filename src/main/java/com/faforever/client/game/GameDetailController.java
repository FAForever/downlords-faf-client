package com.faforever.client.game;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxService;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.SimpleChangeListener;
import com.faforever.client.fx.SimpleInvalidationListener;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratedEvent;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.PopupUtil;
import com.faforever.client.util.RatingUtil;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.replay.WatchButtonController;
import com.faforever.commons.lobby.GameStatus;
import com.faforever.commons.lobby.GameType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.SortedList;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class GameDetailController implements Controller<Pane> {

  private final I18n i18n;
  private final MapService mapService;
  private final ModService modService;
  private final TimeService timeService;
  private final UiService uiService;
  private final JoinGameHelper joinGameHelper;
  private final ContextMenuBuilder contextMenuBuilder;
  private final MapGeneratorService mapGeneratorService;
  private final NotificationService notificationService;
  private final JavaFxService javaFxService;
  private final EventBus eventBus;

  private final ObjectProperty<GameBean> game = new SimpleObjectProperty<>();
  private final BooleanProperty playtimeVisible = new SimpleBooleanProperty();
  private final MapProperty<Integer, List<Integer>> teams = new SimpleMapProperty<>(FXCollections.emptyObservableMap());
  private final ObservableList<Integer> teamIds = new SortedList<>(JavaFxUtil.attachListToMapKeys(FXCollections.observableArrayList(), teams), Comparator.naturalOrder());
  private final ListProperty<TeamCardController> teamCardControllers = new SimpleListProperty<>(FXCollections.observableArrayList());
  private final ObservableValue<String> leaderboard = game.flatMap(GameBean::leaderboardProperty);
  private final Timeline playTimeTimeline = new Timeline(new KeyFrame(Duration.ZERO, event -> updatePlaytimeValue()), new KeyFrame(Duration.seconds(1)));

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

  public void initialize() {
    playTimeTimeline.setCycleCount(Timeline.INDEFINITE);

    contextMenuBuilder.addCopyLabelContextMenu(gameTitleLabel, mapLabel, gameTypeLabel);
    JavaFxUtil.bindManagedToVisible(root, joinButton, watchButton, gameTitleLabel, hostLabel, mapLabel, numberOfPlayersLabel, mapPreviewContainer, gameTypeLabel, playtimeLabel, generateMapButton);
    JavaFxUtil.bind(mapPreviewContainer.visibleProperty(), mapImageView.imageProperty().isNotNull());
    root.parentProperty().addListener(observable -> {
      if (!(root.getParent() instanceof Pane)) {
        return;
      }
      root.maxWidthProperty().bind(((Pane) root.getParent()).widthProperty());
    });

    root.visibleProperty().bind(game.isNotNull());
    gameTitleLabel.textProperty().bind(game.flatMap(GameBean::titleProperty).map(StringUtils::normalizeSpace));
    hostLabel.textProperty().bind(game.flatMap(GameBean::hostProperty));
    mapLabel.textProperty().bind(game.flatMap(GameBean::mapFolderNameProperty));
    mapImageView.imageProperty()
        .bind(game.flatMap(GameBean::mapFolderNameProperty)
            .map(folderName -> mapService.loadPreview(folderName, PreviewSize.LARGE))
            .flatMap(image -> image.errorProperty()
                .map(error -> error ? uiService.getThemeImage(UiService.NO_IMAGE_AVAILABLE) : image)));

    game.flatMap(GameBean::featuredModProperty).addListener((SimpleChangeListener<String>) this::onFeaturedModChanged);

    watchButtonController.gameProperty().bind(game);

    watchButton.visibleProperty()
        .bind(game.flatMap(gameBean -> gameBean.statusProperty()
            .isEqualTo(GameStatus.PLAYING)
            .and(gameBean.startTimeProperty().isNotNull())).orElse(false));
    joinButton.visibleProperty()
        .bind(game.flatMap(gameBean -> gameBean.statusProperty()
            .isEqualTo(GameStatus.OPEN)
            .and(gameBean.gameTypeProperty().isNotEqualTo(GameType.MATCHMAKER))).orElse(false));

    generateMapButton.visibleProperty()
        .bind(game.flatMap(GameBean::mapFolderNameProperty)
            .map(mapName -> mapGeneratorService.isGeneratedMap(mapName) && !mapService.isInstalled(mapName))
            .orElse(false));

    playtimeLabel.visibleProperty()
        .bind(playtimeVisible.and(BooleanExpression.booleanExpression(game.flatMap(gameBean -> gameBean.startTimeProperty()
            .isNotNull().and(gameBean.statusProperty().isEqualTo(GameStatus.PLAYING))))).and(playTimeTimeline.statusProperty().isEqualTo(Status.RUNNING)));

    numberOfPlayersLabel.textProperty()
        .bind(game.flatMap(gameBean -> gameBean.numActivePlayersProperty()
            .flatMap(numActive -> gameBean.maxPlayersProperty()
                .map(numMax -> i18n.get("game.detail.players.format", numActive, numMax)))));

    game.flatMap(GameBean::statusProperty).addListener((SimpleChangeListener<GameStatus>) this::onGameStatusChanged);

    teams.bind(game.flatMap(GameBean::teamsProperty));
    teams.addListener((SimpleInvalidationListener) this::onTeamsInvalidated);

    eventBus.register(this);
  }

  private void onFeaturedModChanged(String featuredModTechnicalName) {
    Mono.justOrEmpty(featuredModTechnicalName)
        .flatMap(modService::getFeaturedMod)
        .map(FeaturedModBean::getDisplayName)
        .switchIfEmpty(Mono.just(i18n.get("unknown")))
        .publishOn(javaFxService.getFxApplicationScheduler())
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

  private void stopPlaytime() {
    JavaFxUtil.runLater(playTimeTimeline::stop);
  }

  private void updatePlaytimeValue() {
    String durationText;

    GameBean gameBean = getGame();
    if (gameBean == null || gameBean.getStartTime() == null || gameBean.getStatus() != GameStatus.PLAYING) {
      durationText = null;
      stopPlaytime();
    } else {
      durationText = timeService.shortDuration(java.time.Duration.between(gameBean.getStartTime(), OffsetDateTime.now()));
    }

    JavaFxUtil.runLater(() -> playtimeLabel.setText(durationText));
  }

  public void dispose() {
    stopPlaytime();
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

  public ObservableMap<Integer, List<Integer>> getTeams() {
    return teams.get();
  }

  public MapProperty<Integer, List<Integer>> teamsProperty() {
    return teams;
  }

  public void setTeams(ObservableMap<Integer, List<Integer>> teams) {
    this.teams.set(teams);
  }

  private void onTeamsInvalidated() {
    int numTeams = teams.size();
    int numControllers = teamCardControllers.size();
    int difference = numTeams - numControllers;
    if (difference > 0) {
      TeamCardController teamCardController = uiService.loadFxml("theme/team_card.fxml");
      teamCardController.bindPlayersToPlayerIds();
      teamCardController.setRatingPrecision(RatingPrecision.ROUNDED);
      teamCardController.ratingProviderProperty().bind(leaderboard.map(name -> player -> RatingUtil.getLeaderboardRating(player, name)));
      teamCardController.playerIdsProperty().bind(Bindings.valueAt(teams, teamCardController.teamIdProperty().asObject()).map(FXCollections::observableList));
      IntegerBinding indexBinding = Bindings.createIntegerBinding(() -> teamCardControllers.indexOf(teamCardController), teamCardControllers);
      teamCardController.teamIdProperty().bind(Bindings.valueAt(teamIds, indexBinding));
      teamCardControllers.add(teamCardController);
      teamListPane.getChildren().add(teamCardController.getRoot());
    } else if (difference < 0) {
      int from = numControllers + difference;
      teamCardControllers.remove(from, numControllers);
      teamListPane.getChildren().remove(from, numControllers);
    }
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
      PopupUtil.showImagePopup(mapImageView.getImage());
    }
  }

  public void onGenerateMapClicked() {
    setGeneratingMapInProgress(true);
    mapService.generateIfNotInstalled(getGame().getMapFolderName()).exceptionally(throwable -> {
      notificationService.addImmediateErrorNotification(throwable, "game.mapGeneration.failed.title");
      return null;
    }).whenComplete((unused, throwable) -> setGeneratingMapInProgress(false));
  }

  @Subscribe
  public void onMapGeneratedEvent(MapGeneratedEvent event) {
    rebindGeneratedProperties();
  }

  private void rebindGeneratedProperties() {
    JavaFxUtil.runLater(() -> {
      mapImageView.imageProperty()
          .bind(game.flatMap(GameBean::mapFolderNameProperty)
              .map(folderName -> mapService.loadPreview(folderName, PreviewSize.LARGE))
              .flatMap(image -> image.errorProperty()
                  .map(error -> error ? uiService.getThemeImage(UiService.NO_IMAGE_AVAILABLE) : image)));
      generateMapButton.visibleProperty()
          .bind(game.flatMap(GameBean::mapFolderNameProperty)
              .map(mapName -> mapGeneratorService.isGeneratedMap(mapName) && !mapService.isInstalled(mapName))
              .orElse(false));
    });
  }

  private void setGeneratingMapInProgress(boolean inProgress) {
    JavaFxUtil.runLater(() -> {
      generateMapButton.setDisable(inProgress);
      generateMapButton.setText(i18n.get(inProgress ? "game.mapGeneration.notification.title" : "game.create.generatedMap"));
    });
  }

  @VisibleForTesting
  protected Timeline getPlayTimeTimeline() {
    return playTimeTimeline;
  }
}
