package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.api.FeaturedMod;
import com.faforever.client.domain.api.GamePlayerStats;
import com.faforever.client.domain.GameOutcome;
import com.faforever.client.domain.LeagueScoreJournalBean;
import com.faforever.client.domain.api.Map;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.domain.api.Replay;
import com.faforever.client.domain.api.Replay.ChatMessage;
import com.faforever.client.domain.api.Replay.GameOption;
import com.faforever.client.domain.api.ReplayReview;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.SimpleChangeListener;
import com.faforever.client.fx.StringCell;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.game.RatingPrecision;
import com.faforever.client.game.TeamCardController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.main.event.DeleteLocalReplayEvent;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.PlayerService;
import com.faforever.client.rating.RatingService;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.ClipboardUtil;
import com.faforever.client.util.PopupUtil;
import com.faforever.client.util.RatingUtil;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.commons.api.dto.Faction;
import com.faforever.commons.api.dto.Validity;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.CompressorException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class ReplayDetailController extends NodeController<Node> {

  private final TimeService timeService;
  private final I18n i18n;
  private final UiService uiService;
  private final ReplayService replayService;
  private final RatingService ratingService;
  private final MapService mapService;
  private final MapGeneratorService mapGeneratorService;
  private final PlayerService playerService;
  private final ClientProperties clientProperties;
  private final NotificationService notificationService;
  private final ReviewService reviewService;
  private final ContextMenuBuilder contextMenuBuilder;
  private final ImageViewHelper imageViewHelper;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final ArrayList<TeamCardController> teamCardControllers = new ArrayList<>();
  private final ObjectProperty<Replay> replay = new SimpleObjectProperty<>();
  private final ObservableList<ReplayReview> replayReviews = FXCollections.observableArrayList();
  private final ObjectProperty<java.util.Map<String, List<GamePlayerStats>>> teams = new SimpleObjectProperty<>();
  private final SimpleChangeListener<java.util.Map<String, List<GamePlayerStats>>> teamsListener = this::populateTeamsContainer;

  public Pane replayDetailRoot;
  public Label titleLabel;
  public Button copyButton;
  public Label dateLabel;
  public Label timeLabel;
  public Label modLabel;
  public Label durationLabel;
  public Label replayDurationLabel;
  public Label playerCountLabel;
  public Label ratingLabel;
  public Label qualityLabel;
  public Label onMapLabel;
  public Pane teamsInfoBox;
  public Pane teamsContainer;
  public Separator ratingSeparator;
  public Pane reviewsContainer;
  public ReviewsController<ReplayReview> reviewsController;
  public Separator reviewSeparator;
  public TableView<ChatMessage> chatTable;
  public TableColumn<ChatMessage, Duration> chatGameTimeColumn;
  public TableColumn<ChatMessage, String> chatSenderColumn;
  public TableColumn<ChatMessage, String> chatMessageColumn;
  public TableView<GameOption> optionsTable;
  public TableColumn<GameOption, String> optionKeyColumn;
  public TableColumn<GameOption, String> optionValueColumn;
  public Button downloadMoreInfoButton;
  public Pane moreInformationPane;
  public ImageView mapThumbnailImageView;
  public Button watchButton;
  public TextField replayIdField;
  public ScrollPane scrollPane;
  public Button showRatingChangeButton;
  public Button reportButton;
  public Button deleteButton;
  public Label notRatedReasonLabel;

  private Runnable onDeleteListener;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(downloadMoreInfoButton, moreInformationPane, teamsInfoBox, reviewsContainer,
                                    ratingSeparator, reviewSeparator, deleteButton, getRoot());

    JavaFxUtil.bindManagedToVisible(notRatedReasonLabel, showRatingChangeButton);
    contextMenuBuilder.addCopyLabelContextMenu(onMapLabel, titleLabel);
    JavaFxUtil.fixScrollSpeed(scrollPane);

    initializeReviewsController();
    initializeTableColumns();
    initializeTooltips();
    bindProperties();

    replayDetailRoot.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode() == KeyCode.ESCAPE) {
        onCloseButtonClicked();
      }
    });

    copyButton.setText(i18n.get("replay.copyUrl"));

    teams.orElse(java.util.Map.of()).addListener(teamsListener);
    replay.addListener((SimpleChangeListener<Replay>) this::onReplayChanged);
  }

  private void bindProperties() {
    ObservableValue<Validity> validityObservable = replay.map(Replay::validity);
    BooleanExpression isValidObservable = BooleanExpression.booleanExpression(
        validityObservable.map(Validity.VALID::equals));
    BooleanExpression changedRatingObservable = BooleanExpression.booleanExpression(
        replay.map(replayService::replayChangedRating));

    showRatingChangeButton.visibleProperty()
                          .bind(Bindings.and(isValidObservable, changedRatingObservable).when(showing));
    notRatedReasonLabel.visibleProperty().bind(showRatingChangeButton.visibleProperty().not());
    notRatedReasonLabel.textProperty()
                       .bind(validityObservable.map(
                                                   validity -> i18n.getOrDefault(validity.toString(), "game.reasonNotValid",
                                                                                 i18n.get(validity.getI18nKey())))
                                               .orElse(i18n.get("game.notRatedYet")));

    BooleanExpression hasReplayFileObservable = BooleanExpression.booleanExpression(
        replay.map(Replay::replayFile).map(Objects::nonNull).orElse(false));
    BooleanExpression replayAvailableOnline = BooleanExpression.booleanExpression(replay.map(Replay::replayAvailable));
    BooleanExpression replayAvailable = Bindings.or(hasReplayFileObservable, replayAvailableOnline);

    watchButton.disableProperty().bind(replayAvailable.not().when(showing));
    watchButton.textProperty()
               .bind(replayAvailable.map(
                   available -> available ? i18n.get("game.watch") : i18n.get("game.replayFileMissing")).when(showing));
    downloadMoreInfoButton.visibleProperty()
                          .bind(Bindings.and(replayAvailableOnline, hasReplayFileObservable.not()).when(showing));
    downloadMoreInfoButton.textProperty()
                          .bind(replayAvailable.map(
                              available -> available ? i18n.get("game.downloadMoreInfoNoSize") : i18n.get(
                                  "game.replayFileMissing")).when(showing));

    replayIdField.textProperty().bind(replay.map(Replay::id).map(id -> i18n.get("game.idFormat", id)).when(showing));
    titleLabel.textProperty().bind(replay.map(Replay::title).when(showing));
    dateLabel.textProperty().bind(replay.map(Replay::startTime).map(timeService::asDate).when(showing));
    timeLabel.textProperty().bind(replay.map(Replay::startTime).map(timeService::asShortTime).when(showing));
    ObservableValue<MapVersion> mapVersionObservable = replay.map(Replay::mapVersion);
    mapThumbnailImageView.imageProperty()
                         .bind(mapVersionObservable.flatMap(mapVersion -> Bindings.createObjectBinding(
                                                       () -> mapService.loadPreview(mapVersion, PreviewSize.SMALL),
                                                       mapService.isInstalledBinding(mapVersion)))
                                                   .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable)
                                                   .when(showing));
    onMapLabel.textProperty()
              .bind(mapVersionObservable.map(MapVersion::map)
                                        .map(Map::displayName)
                                        .orElse(i18n.get("game.onUnknownMap"))
                                        .when(showing));
    durationLabel.visibleProperty().bind(replay.map(Replay::endTime).map(Objects::nonNull).orElse(false).when(showing));
    durationLabel.textProperty().bind(replay.map(replayValue -> {
      OffsetDateTime startTime = replayValue.startTime();
      OffsetDateTime endTime = replayValue.endTime();
      return startTime == null || endTime == null ? null : Duration.between(startTime, endTime);
    }).map(timeService::shortDuration).when(showing));

    replayDurationLabel.visibleProperty()
                       .bind(replay.map(Replay::replayTicks).map(Objects::nonNull).orElse(false).when(showing));

    replayDurationLabel.textProperty().bind(replay.map(Replay::replayTicks)
                                   .map(ticks -> ticks * 100)
                                   .map(Duration::ofMillis)
                                   .map(timeService::shortDuration)
                                   .when(showing));

    modLabel.textProperty()
            .bind(replay.map(Replay::featuredMod)
                        .map(FeaturedMod::displayName)
                        .orElse(i18n.get("unknown"))
                        .when(showing));

    ObservableValue<Double> qualityObservable = replay.map(ratingService::calculateQuality);
    BooleanExpression qualityNotDefined = BooleanExpression.booleanExpression(
        qualityObservable.map(quality -> quality.isNaN()));
    qualityLabel.textProperty()
                .bind(Bindings.when(qualityNotDefined)
                              .then(i18n.get("gameQuality.undefined"))
                              .otherwise(StringExpression.stringExpression(
                                  qualityObservable.map(quality -> quality * 100)
                                                   .map(Math::round)
                                                   .map(quality -> i18n.get("percentage", quality))))
                              .when(showing));

    playerCountLabel.textProperty().bind(replay.map(Replay::numPlayers).map(i18n::number).when(showing));

    ratingLabel.textProperty().bind(replay.map(Replay::averageRating)
                           .map(average -> average.isNaN() ? "-" : i18n.number(average))
                           .when(showing));

    BooleanExpression hasChatMessages = BooleanExpression.booleanExpression(
        replay.map(Replay::chatMessages).map(Collection::isEmpty)).not();
    BooleanExpression hasGameOptions = BooleanExpression.booleanExpression(
        replay.map(Replay::gameOptions).map(Collection::isEmpty)).not();
    moreInformationPane.visibleProperty().bind(Bindings.or(hasChatMessages, hasGameOptions).when(showing));

    ratingSeparator.visibleProperty().bind(reviewsContainer.visibleProperty().when(showing));
    reviewSeparator.visibleProperty().bind(reviewsContainer.visibleProperty().when(showing));

    BooleanExpression localObservable = BooleanExpression.booleanExpression(replay.map(Replay::local));
    reviewsContainer.visibleProperty().bind(localObservable.not().when(showing));
    deleteButton.visibleProperty().bind(localObservable.when(showing));
    teams.bind(replay.map(Replay::teamPlayerStats).when(showing));

    optionsTable.itemsProperty().bind(replay.map(Replay::gameOptions).map(FXCollections::observableList).when(showing));
    chatTable.itemsProperty().bind(replay.map(Replay::chatMessages).map(FXCollections::observableList).when(showing));
  }

  private void onReplayChanged(Replay newValue) {
    if (newValue == null) {
      reviewsController.setCanWriteReview(false);
      replayReviews.clear();
      return;
    }

    if (newValue.replayFile() != null) {
      enrichReplayLater(newValue.replayFile(), newValue);
    }

    newValue.setLeagueScores(null);
    replayService.getLeagueScoreJournalForReplay(newValue)
        .thenAcceptAsync(scores -> {
          newValue.setLeagueScores(scores);
          // This looks a bit ugly. Ideally we should wait with drawing the window until we have the league scores,
          // then we don't need to trigger a redraw here
          populateTeamsContainer(teams.getValue());
        }, fxApplicationThreadExecutor);

    reviewsController.setCanWriteReview(true);

    reviewService.getReplayReviews(newValue)
                 .collectList()
                 .publishOn(fxApplicationThreadExecutor.asScheduler())
                 .subscribe(replayReviews::setAll, throwable -> log.error("Unable to populate reviews", throwable));
  }

  public void setReplay(Replay replay) {
    this.replay.set(replay);
  }

  private void initializeTooltips() {
    dateLabel.setTooltip(new Tooltip(i18n.get("replay.dateTooltip")));
    timeLabel.setTooltip(new Tooltip(i18n.get("replay.timeTooltip")));
    modLabel.setTooltip(new Tooltip(i18n.get("replay.modTooltip")));
    durationLabel.setTooltip(new Tooltip(i18n.get("replay.durationTooltip")));
    replayDurationLabel.setTooltip(new Tooltip(i18n.get("replay.replayDurationTooltip")));
    playerCountLabel.setTooltip(new Tooltip(i18n.get("replay.playerCountTooltip")));
    ratingLabel.setTooltip(new Tooltip(i18n.get("replay.ratingTooltip")));
    qualityLabel.setTooltip(new Tooltip(i18n.get("replay.qualityTooltip")));
    deleteButton.setTooltip(new Tooltip(i18n.get("replay.deleteButton.tooltip")));
  }

  private void initializeTableColumns() {
    chatGameTimeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().time()));
    chatGameTimeColumn.setCellFactory(param -> new StringCell<>(timeService::asHms));

    chatSenderColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().sender()));
    chatSenderColumn.setCellFactory(param -> new StringCell<>(String::toString));

    chatMessageColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().message()));
    chatMessageColumn.setCellFactory(param -> new StringCell<>(String::toString));

    optionKeyColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().key()));
    optionKeyColumn.setCellFactory(param -> new StringCell<>(String::toString));

    optionValueColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().value()));
    optionValueColumn.setCellFactory(param -> new StringCell<>(String::toString));
  }

  private void initializeReviewsController() {
    reviewsController.setCanWriteReview(true);
    reviewsController.setOnSendReviewListener(this::onSendReview);
    reviewsController.setOnDeleteReviewListener(this::onDeleteReview);
    reviewsController.setReviewSupplier(() -> {
      return new ReplayReview(null, null, playerService.getCurrentPlayer(), null, replay.get());
    });
    reviewsController.bindReviews(replayReviews);
  }

  @VisibleForTesting
  void onDeleteReview(ReplayReview review) {
    reviewService.deleteReview(review)
                 .publishOn(fxApplicationThreadExecutor.asScheduler())
                 .subscribe(null, throwable -> {
                   log.error("Review could not be saved", throwable);
                   notificationService.addImmediateErrorNotification(throwable, "review.delete.error");
                 }, () -> replayReviews.remove(review));
  }

  @VisibleForTesting
  void onSendReview(ReplayReview review) {
    reviewService.saveReview(review)
                 .filter(savedReview -> !replayReviews.contains(savedReview))
                 .publishOn(fxApplicationThreadExecutor.asScheduler())
                 .subscribe(savedReview -> {
                   replayReviews.remove(review);
                   replayReviews.add(savedReview);
                 }, throwable -> {
                   log.error("Review could not be saved", throwable);
                   notificationService.addImmediateErrorNotification(throwable, "review.save.error");
                 });
  }

  public void onDownloadMoreInfoClicked() {
    // TODO display loading indicator
    Replay replayValue = replay.get();
    replayService.downloadReplay(replayValue.id()).thenCompose(path -> enrichReplayLater(path, replayValue));
  }

  private CompletableFuture<Void> enrichReplayLater(Path path, Replay replay) {
    CompletableFuture<ReplayDetails> replayDetailsFuture = CompletableFuture.supplyAsync(() -> {
      try {
        return replayService.loadReplayDetails(path);
      } catch (CompressorException | IOException e) {
        throw new RuntimeException(e);
      }
    });

    replayDetailsFuture.thenAccept(replayDetails -> {
      MapVersion mapVersion = replayDetails.mapVersion();
      if (mapGeneratorService.isGeneratedMap(mapVersion.folderName())) {
        mapService.generateIfNotInstalled(mapVersion.folderName()).subscribe();
      }
    });

    return replayDetailsFuture.thenAcceptAsync(
                                  replayDetails -> this.replay.set(replay.withReplayDetails(replayDetails, path)), fxApplicationThreadExecutor)
                              .exceptionally(throwable -> {
                                if (throwable.getCause() instanceof FileNotFoundException) {
                                  log.warn("Replay file not available", throwable);
                                  notificationService.addImmediateWarnNotification("replayNotAvailable", replay.id());
                                } else {
                                  log.error("Replay could not be enriched", throwable);
                                  notificationService.addImmediateErrorNotification(throwable, "replay.enrich.error");
                                }
                                return null;
                              });
  }

  private void populateTeamsContainer(java.util.Map<String, List<GamePlayerStats>> newValue) {
    CompletableFuture.supplyAsync(() -> createTeamCardControllers(newValue)).thenAcceptAsync(controllers -> {
      teamCardControllers.clear();
      if (controllers.stream()
          .map(TeamCardController::getTeamOutcome).noneMatch(gameOutcome -> gameOutcome != GameOutcome.DEFEAT)) {
        controllers.forEach(teamCardController -> teamCardController.setTeamOutcome(GameOutcome.DRAW));
      }
      teamCardControllers.addAll(controllers);
      teamsContainer.getChildren().setAll(teamCardControllers.stream().map(TeamCardController::getRoot).toList());
    }, fxApplicationThreadExecutor);
  }

  private List<TeamCardController> createTeamCardControllers(java.util.Map<String, List<GamePlayerStats>> teamsValue) {
    return teamsValue.entrySet().stream().map(entry -> {
      String team = entry.getKey();
      List<GamePlayerStats> playerStats = entry.getValue();

      java.util.Map<PlayerInfo, GamePlayerStats> statsByPlayer = playerStats.stream()
                                                                            .collect(Collectors.toMap(
                                                                                GamePlayerStats::player,
                                                                                           Function.identity()));

      TeamCardController controller = uiService.loadFxml("theme/team_card.fxml");

      controller.setTeamOutcome(calculateTeamOutcome(statsByPlayer.values()));
      controller.setRatingPrecision(RatingPrecision.EXACT);
      controller.setRatingProvider(player -> getPlayerRating(player, statsByPlayer));
      controller.setDivisionProvider(this::getPlayerDivision);
      controller.setFactionProvider(player -> getPlayerFaction(player, statsByPlayer));
      controller.setTeamId(Integer.parseInt(team));
      controller.setPlayers(statsByPlayer.keySet());

      return controller;
    }).toList();
  }

  private GameOutcome calculateTeamOutcome(Collection<GamePlayerStatsBean> statsByPlayer) {
    // Game outcomes are saved since 2020, so this should suffice for the
    // vast majority of replays that people will realistically look up.
    Map<GameOutcome, Long> outcomeCounts = statsByPlayer.stream()
        .map(GamePlayerStatsBean::getOutcome)
        .filter(Objects::nonNull)
        .map(gameOutcome -> (gameOutcome == GameOutcome.CONFLICTING) ? GameOutcome.UNKNOWN : gameOutcome)
        .map(gameOutcome -> (gameOutcome == GameOutcome.MUTUAL_DRAW) ? GameOutcome.DRAW : gameOutcome)
        .collect(Collectors.groupingBy(gameOutcome -> gameOutcome, Collectors.counting()));

    if (outcomeCounts.containsKey(GameOutcome.VICTORY)) {
      return GameOutcome.VICTORY;
    }

    return outcomeCounts.entrySet()
        .stream()
        .max(Entry.comparingByValue()).map(Entry::getKey).orElse(GameOutcome.UNKNOWN);
  }

  private Faction getPlayerFaction(PlayerInfo player, java.util.Map<PlayerInfo, GamePlayerStats> statsByPlayerId) {
    GamePlayerStats playerStats = statsByPlayerId.get(player);
    return playerStats == null ? null : playerStats.faction();
  }

  private Integer getPlayerRating(PlayerInfo player, java.util.Map<PlayerInfo, GamePlayerStats> statsByPlayerId) {
    GamePlayerStats playerStats = statsByPlayerId.get(player);
    if ((!replay.get().getLeagueScores().isEmpty()) || playerStats == null) {
      return null;
    }
    return playerStats.leaderboardRatingJournals()
                      .stream()
                      .findFirst()
                      .filter(ratingJournal -> ratingJournal.meanBefore() != null)
                      .filter(ratingJournal -> ratingJournal.deviationBefore() != null)
                      .map(RatingUtil::getRating)
                      .orElse(null);
  }

  private SubdivisionBean getPlayerDivision(PlayerBean player) {
    return replay.get().getLeagueScores()
        .stream()
        .filter(leagueScoreJournalBean -> leagueScoreJournalBean.getLoginId() == player.getId())
        .findFirst()
        .map(LeagueScoreJournalBean::getDivisionBefore)
        .orElse(null);
  }

  public void onReport() {
    ReportDialogController reportDialogController = uiService.loadFxml("theme/reporting/report_dialog.fxml");
    reportDialogController.setReplay(replay.get());
    Scene scene = getRoot().getScene();
    if (scene != null) {
      reportDialogController.setOwnerWindow(scene.getWindow());
    }
    reportDialogController.show();
  }

  public void onDeleteButtonClicked() {
    notificationService.addNotification(
        new ImmediateNotification(i18n.get("replay.deleteNotification.heading", replay.get().title()),
                                  i18n.get("replay.deleteNotification.info"), Severity.INFO,
                                  Arrays.asList(new Action(i18n.get("cancel")),
                                                new Action(i18n.get("delete"), this::deleteReplay))));
  }

  public void setOnDeleteListener(Runnable onDeleteListener) {
    this.onDeleteListener = onDeleteListener;
  }

  private void deleteReplay() {
    if (replayService.deleteReplayFile(replay.get().replayFile()) && onDeleteListener != null) {
      onDeleteListener.run();
    }
    onCloseButtonClicked();
  }

  @Override
  public Node getRoot() {
    return replayDetailRoot;
  }

  public void onCloseButtonClicked() {
    getRoot().setVisible(false);
  }

  public void onDimmerClicked() {
    onCloseButtonClicked();
  }

  public void onContentPaneClicked(MouseEvent event) {
    event.consume();
  }

  public void onWatchButtonClicked() {
    replayService.runReplay(replay.get());
  }


  public void copyLink() {
    String replayUrl = clientProperties.getVault().getReplayDownloadUrlFormat().formatted(replay.get().id());
    ClipboardUtil.copyToClipboard(replayUrl);
  }

  public void showRatingChange() {
    teamCardControllers.forEach(TeamCardController::showGameResult);
    if (replay.get().getLeagueScores().isEmpty()) {
      java.util.Map<String, List<GamePlayerStats>> teamsValue = teams.get();
      teamCardControllers.forEach(teamCardController -> teamCardController.setStats(
        teamsValue.get(String.valueOf(teamCardController.getTeamId()))));
    }
  }

  public void onMapPreviewImageClicked() {
    Replay replayValue = replay.get();
    if (replayValue != null && replayValue.mapVersion() != null) {
      PopupUtil.showImagePopup(mapService.loadPreview(replayValue.mapVersion(), PreviewSize.LARGE));
    }
  }
}
