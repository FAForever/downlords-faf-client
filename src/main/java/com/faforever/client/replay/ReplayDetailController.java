package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GamePlayerStatsBean;
import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.domain.ReplayBean.ChatMessage;
import com.faforever.client.domain.ReplayBean.GameOption;
import com.faforever.client.domain.ReplayReviewBean;
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
import java.util.Map;
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
  private final ObjectProperty<ReplayBean> replay = new SimpleObjectProperty<>();
  private final ObservableList<ReplayReviewBean> replayReviews = FXCollections.observableArrayList();
  private final ObjectProperty<Map<String, List<GamePlayerStatsBean>>> teams = new SimpleObjectProperty<>();
  private final SimpleChangeListener<Map<String, List<GamePlayerStatsBean>>> teamsListener = this::populateTeamsContainer;

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
  public ReviewsController<ReplayReviewBean> reviewsController;
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

    teams.orElse(Map.of()).addListener(teamsListener);
    replay.addListener((SimpleChangeListener<ReplayBean>) this::onReplayChanged);
  }

  private void bindProperties() {
    ObservableValue<Validity> validityObservable = replay.flatMap(ReplayBean::validityProperty);
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
        replay.flatMap(ReplayBean::replayFileProperty).map(Objects::nonNull).orElse(false));
    BooleanExpression replayAvailableOnline = BooleanExpression.booleanExpression(
        replay.flatMap(ReplayBean::replayAvailableProperty));
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

    replayIdField.textProperty()
                 .bind(replay.flatMap(ReplayBean::idProperty).map(id -> i18n.get("game.idFormat", id)).when(showing));
    titleLabel.textProperty().bind(replay.flatMap(ReplayBean::titleProperty).when(showing));
    dateLabel.textProperty().bind(replay.flatMap(ReplayBean::startTimeProperty).map(timeService::asDate).when(showing));
    timeLabel.textProperty()
             .bind(replay.flatMap(ReplayBean::startTimeProperty).map(timeService::asShortTime).when(showing));
    ObservableValue<MapVersionBean> mapVersionObservable = replay.flatMap(ReplayBean::mapVersionProperty);
    mapThumbnailImageView.imageProperty()
                         .bind(mapVersionObservable.flatMap(mapVersion -> Bindings.createObjectBinding(
                                                       () -> mapService.loadPreview(mapVersion, PreviewSize.SMALL),
                                                       mapService.isInstalledBinding(mapVersion)))
                                                   .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable)
                                                   .when(showing));
    onMapLabel.textProperty()
              .bind(mapVersionObservable.flatMap(MapVersionBean::mapProperty)
                                        .flatMap(MapBean::displayNameProperty)
                                        .orElse(i18n.get("game.onUnknownMap"))
                                        .when(showing));
    durationLabel.visibleProperty()
                 .bind(replay.flatMap(ReplayBean::endTimeProperty).map(Objects::nonNull).orElse(false).when(showing));
    durationLabel.textProperty().bind(replay.flatMap(replayValue -> Bindings.createObjectBinding(() -> {
      OffsetDateTime startTime = replayValue.getStartTime();
      OffsetDateTime endTime = replayValue.getEndTime();
      return startTime == null || endTime == null ? null : Duration.between(startTime, endTime);
    }, replayValue.startTimeProperty(), replayValue.endTimeProperty()).map(timeService::shortDuration)).when(showing));

    replayDurationLabel.visibleProperty()
                       .bind(replay.flatMap(ReplayBean::replayTicksProperty)
                                   .map(Objects::nonNull)
                                   .orElse(false)
                                   .when(showing));

    replayDurationLabel.textProperty()
                       .bind(replay.flatMap(ReplayBean::replayTicksProperty)
                                   .map(ticks -> ticks * 100)
                                   .map(Duration::ofMillis)
                                   .map(timeService::shortDuration)
                                   .when(showing));

    modLabel.textProperty()
            .bind(replay.flatMap(ReplayBean::featuredModProperty).map(FeaturedModBean::displayName)
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

    playerCountLabel.textProperty()
                    .bind(replay.flatMap(ReplayBean::teamsProperty)
                                .map(teams -> teams.values().stream().mapToInt(Collection::size).sum())
                                .map(i18n::number)
                                .when(showing));

    ratingLabel.textProperty()
               .bind(replay.flatMap(ReplayBean::teamPlayerStatsProperty)
                           .map(teamStats -> teamStats.values()
                                                      .stream()
                                                      .flatMapToInt(playerStats -> playerStats.stream()
                                                                                              .map(
                                                                                                  GamePlayerStatsBean::leaderboardRatingJournals)
                                                                                              .mapToInt(
                                                                                                  journals -> journals.stream()
                                                                                                                      .map(
                                                                                                                          RatingUtil::getRating)
                                                                                                                      .findFirst()
                                                                                                                      .orElse(
                                                                                                                          0)))
                                                      .average()
                                                      .orElse(Double.NaN))
                           .map(average -> average.isNaN() ? "-" : i18n.number(average))
                           .when(showing));

    BooleanExpression hasChatMessages = BooleanExpression.booleanExpression(
        replay.flatMap(ReplayBean::chatMessagesProperty).map(Collection::isEmpty)).not();
    BooleanExpression hasGameOptions = BooleanExpression.booleanExpression(
        replay.flatMap(ReplayBean::gameOptionsProperty).map(Collection::isEmpty)).not();
    moreInformationPane.visibleProperty().bind(Bindings.or(hasChatMessages, hasGameOptions).when(showing));

    ratingSeparator.visibleProperty().bind(reviewsContainer.visibleProperty().when(showing));
    reviewSeparator.visibleProperty().bind(reviewsContainer.visibleProperty().when(showing));

    BooleanExpression localObservable = BooleanExpression.booleanExpression(replay.flatMap(ReplayBean::localProperty));
    reviewsContainer.visibleProperty().bind(localObservable.not().when(showing));
    deleteButton.visibleProperty().bind(localObservable.when(showing));
    teams.bind(replay.flatMap(ReplayBean::teamPlayerStatsProperty).when(showing));

    optionsTable.itemsProperty()
                .bind(replay.flatMap(ReplayBean::gameOptionsProperty).map(FXCollections::observableList).when(showing));
    chatTable.itemsProperty()
             .bind(replay.flatMap(ReplayBean::chatMessagesProperty).map(FXCollections::observableList).when(showing));
  }

  private void onReplayChanged(ReplayBean newValue) {
    if (newValue == null) {
      reviewsController.setCanWriteReview(false);
      replayReviews.clear();
      return;
    }

    if (newValue.getReplayFile() != null) {
      enrichReplayLater(newValue.getReplayFile(), newValue);
    }

    reviewsController.setCanWriteReview(true);

    reviewService.getReplayReviews(newValue)
                 .collectList()
                 .publishOn(fxApplicationThreadExecutor.asScheduler())
                 .subscribe(replayReviews::setAll, throwable -> log.error("Unable to populate reviews", throwable));
  }

  public void setReplay(ReplayBean replay) {
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
      ReplayReviewBean review = new ReplayReviewBean();
      review.setPlayer(playerService.getCurrentPlayer());
      review.setReplay(replay.get());
      return review;
    });
    reviewsController.bindReviews(replayReviews);
  }

  @VisibleForTesting
  void onDeleteReview(ReplayReviewBean review) {
    reviewService.deleteGameReview(review)
                 .publishOn(fxApplicationThreadExecutor.asScheduler())
                 .subscribe(null, throwable -> {
                   log.error("Review could not be saved", throwable);
                   notificationService.addImmediateErrorNotification(throwable, "review.delete.error");
                 }, () -> replayReviews.remove(review));
  }

  @VisibleForTesting
  void onSendReview(ReplayReviewBean review) {
    reviewService.saveReplayReview(review)
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
    ReplayBean replayValue = replay.get();
    replayService.downloadReplay(replayValue.getId()).thenCompose(path -> enrichReplayLater(path, replayValue));
  }

  private CompletableFuture<Void> enrichReplayLater(Path path, ReplayBean replay) {
    replay.setReplayFile(path);
    CompletableFuture<ReplayDetails> replayDetailsFuture = CompletableFuture.supplyAsync(() -> {
      try {
        return replayService.loadReplayDetails(path);
      } catch (CompressorException | IOException e) {
        throw new RuntimeException(e);
      }
    });

    replayDetailsFuture.thenAccept(replayDetails -> {
      MapVersionBean mapVersion = replayDetails.mapVersion();
      if (mapGeneratorService.isGeneratedMap(mapVersion.getFolderName())) {
        mapService.generateIfNotInstalled(mapVersion.getFolderName());
      }
    });

    return replayDetailsFuture.thenAcceptAsync(replayDetails -> {
      if (replay.getMapVersion() == null) {
        replay.setMapVersion(replayDetails.mapVersion());
      }

      replay.setChatMessages(replayDetails.chatMessages());
      replay.setGameOptions(replayDetails.gameOptions());
    }, fxApplicationThreadExecutor).exceptionally(throwable -> {
      if (throwable.getCause() instanceof FileNotFoundException) {
        log.warn("Replay file not available", throwable);
        notificationService.addImmediateWarnNotification("replayNotAvailable", replay.getId());
      } else {
        log.error("Replay could not be enriched", throwable);
        notificationService.addImmediateErrorNotification(throwable, "replay.enrich.error");
      }
      return null;
    });
  }

  private void populateTeamsContainer(Map<String, List<GamePlayerStatsBean>> newValue) {
    CompletableFuture.supplyAsync(() -> createTeamCardControllers(newValue)).thenAcceptAsync(controllers -> {
      teamCardControllers.clear();
      teamCardControllers.addAll(controllers);
      teamsContainer.getChildren().setAll(teamCardControllers.stream().map(TeamCardController::getRoot).toList());
    }, fxApplicationThreadExecutor);
  }

  private List<TeamCardController> createTeamCardControllers(Map<String, List<GamePlayerStatsBean>> teamsValue) {
    return teamsValue.entrySet().stream().map(entry -> {
      String team = entry.getKey();
      List<GamePlayerStatsBean> playerStats = entry.getValue();

      Map<PlayerBean, GamePlayerStatsBean> statsByPlayer = playerStats.stream()
                                                                      .collect(Collectors.toMap(
                                                                          GamePlayerStatsBean::player,
                                                                          Function.identity()));

      TeamCardController controller = uiService.loadFxml("theme/team_card.fxml");

      controller.setRatingPrecision(RatingPrecision.EXACT);
      controller.setRatingProvider(player -> getPlayerRating(player, statsByPlayer));
      controller.setFactionProvider(player -> getPlayerFaction(player, statsByPlayer));
      controller.setTeamId(Integer.parseInt(team));
      controller.setPlayers(statsByPlayer.keySet());

      return controller;
    }).toList();
  }

  private Faction getPlayerFaction(PlayerBean player, Map<PlayerBean, GamePlayerStatsBean> statsByPlayerId) {
    GamePlayerStatsBean playerStats = statsByPlayerId.get(player);
    return playerStats == null ? null : playerStats.faction();
  }

  private Integer getPlayerRating(PlayerBean player, Map<PlayerBean, GamePlayerStatsBean> statsByPlayerId) {
    GamePlayerStatsBean playerStats = statsByPlayerId.get(player);
    return playerStats == null ? null : playerStats.leaderboardRatingJournals()
                                                   .stream()
                                                   .findFirst()
                                                   .filter(ratingJournal -> ratingJournal.meanBefore() != null)
                                                   .filter(ratingJournal -> ratingJournal.deviationBefore() != null)
                                                   .map(RatingUtil::getRating)
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
        new ImmediateNotification(i18n.get("replay.deleteNotification.heading", replay.get().getTitle()),
                                  i18n.get("replay.deleteNotification.info"), Severity.INFO,
                                  Arrays.asList(new Action(i18n.get("cancel")),
                                                new Action(i18n.get("delete"), this::deleteReplay))));
  }

  public void setOnDeleteListener(Runnable onDeleteListener) {
    this.onDeleteListener = onDeleteListener;
  }

  private void deleteReplay() {
    if (replayService.deleteReplayFile(replay.get().getReplayFile()) && onDeleteListener != null) {
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
    String replayUrl = ReplayBean.getReplayUrl(replay.get().getId(),
                                               clientProperties.getVault().getReplayDownloadUrlFormat());
    ClipboardUtil.copyToClipboard(replayUrl);
  }

  public void showRatingChange() {
    Map<String, List<GamePlayerStatsBean>> teamsValue = teams.get();

    teamCardControllers.forEach(teamCardController -> teamCardController.setStats(
        teamsValue.get(String.valueOf(teamCardController.getTeamId()))));
  }

  public void onMapPreviewImageClicked() {
    ReplayBean replayValue = replay.get();
    if (replayValue != null && replayValue.getMapVersion() != null) {
      PopupUtil.showImagePopup(mapService.loadPreview(replayValue.getMapVersion(), PreviewSize.LARGE));
    }
  }
}
