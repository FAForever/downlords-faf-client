package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.AbstractEntityBean;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GamePlayerStatsBean;
import com.faforever.client.domain.LeaderboardRatingJournalBean;
import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.domain.ReplayBean.ChatMessage;
import com.faforever.client.domain.ReplayBean.GameOption;
import com.faforever.client.domain.ReplayReviewBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.JavaFxService;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.SimpleChangeListener;
import com.faforever.client.fx.SimpleInvalidationListener;
import com.faforever.client.fx.StringCell;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.game.RatingPrecision;
import com.faforever.client.game.TeamCardController;
import com.faforever.client.i18n.I18n;
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
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import reactor.core.publisher.Mono;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class ReplayDetailController implements Controller<Node> {

  private final TimeService timeService;
  private final I18n i18n;
  private final EventBus eventBus;
  private final UiService uiService;
  private final ReplayService replayService;
  private final RatingService ratingService;
  private final MapService mapService;
  private final MapGeneratorService mapGeneratorService;
  private final PlayerService playerService;
  private final ClientProperties clientProperties;
  private final NotificationService notificationService;
  private final ReviewService reviewService;
  private final JavaFxService javaFxService;
  private final ContextMenuBuilder contextMenuBuilder;
  private final ImageViewHelper imageViewHelper;

  private final ArrayList<TeamCardController> teamCardControllers = new ArrayList<>();
  private final ObjectProperty<ReplayBean> replay = new SimpleObjectProperty<>();
  private final ObservableList<ReplayReviewBean> reviews = FXCollections.observableArrayList();
  private final ObjectProperty<Map<String, List<GamePlayerStatsBean>>> teams = new SimpleObjectProperty<>();

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

  public void initialize() {
    JavaFxUtil.bindManagedToVisible(downloadMoreInfoButton, moreInformationPane, teamsInfoBox, reviewsContainer, ratingSeparator, reviewSeparator, deleteButton, getRoot());

    imageViewHelper.setDefaultPlaceholderImage(mapThumbnailImageView);
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

    teams.addListener((SimpleInvalidationListener) this::populateTeamsContainer);
    replay.addListener((SimpleChangeListener<ReplayBean>) this::onReplayChanged);
  }

  private void bindProperties() {
    ObservableValue<Boolean> showing = JavaFxUtil.showingProperty(getRoot());

    ObservableValue<Validity> validityObservable = replay.flatMap(ReplayBean::validityProperty);
    BooleanExpression isValidObservable = BooleanExpression.booleanExpression(validityObservable.map(Validity.VALID::equals));
    BooleanExpression changedRatingObservable = BooleanExpression.booleanExpression(replay.map(replayService::replayChangedRating));

    showRatingChangeButton.visibleProperty()
        .bind(Bindings.and(isValidObservable, changedRatingObservable).when(showing));
    notRatedReasonLabel.visibleProperty().bind(showRatingChangeButton.visibleProperty().not());
    notRatedReasonLabel.textProperty()
        .bind(validityObservable.map(validity -> i18n.getOrDefault(validity.toString(), "game.reasonNotValid", i18n.get(validity.getI18nKey())))
            .orElse(i18n.get("game.notRatedYet")));

    BooleanExpression hasReplayFileObservable = BooleanExpression.booleanExpression(replay.flatMap(ReplayBean::replayFileProperty)
        .map(Objects::nonNull)
        .orElse(false));
    BooleanExpression replayAvailableOnline = BooleanExpression.booleanExpression(replay.flatMap(ReplayBean::replayAvailableProperty));
    BooleanExpression replayAvailable = Bindings.or(hasReplayFileObservable, replayAvailableOnline);

    watchButton.disableProperty().bind(replayAvailable.not().when(showing));
    watchButton.textProperty()
        .bind(replayAvailable.map(available -> available ? i18n.get("game.watch") : i18n.get("game.replayFileMissing"))
            .when(showing));
    downloadMoreInfoButton.visibleProperty()
        .bind(Bindings.and(replayAvailableOnline, hasReplayFileObservable.not()).when(showing));
    downloadMoreInfoButton.textProperty()
        .bind(replayAvailable.map(available -> available ? i18n.get("game.downloadMoreInfoNoSize") : i18n.get("game.replayFileMissing"))
            .when(showing));

    replayIdField.textProperty()
        .bind(replay.flatMap(ReplayBean::idProperty).map(id -> i18n.get("game.idFormat", id)).when(showing));
    titleLabel.textProperty().bind(replay.flatMap(ReplayBean::titleProperty).when(showing));
    dateLabel.textProperty().bind(replay.flatMap(ReplayBean::startTimeProperty).map(timeService::asDate).when(showing));
    timeLabel.textProperty()
        .bind(replay.flatMap(ReplayBean::startTimeProperty).map(timeService::asShortTime).when(showing));
    ObservableValue<MapVersionBean> mapVersionObservable = replay.flatMap(ReplayBean::mapVersionProperty);
    mapThumbnailImageView.imageProperty()
        .bind(mapVersionObservable.flatMap(mapVersion -> Bindings.createObjectBinding(() -> mapService.loadPreview(mapVersion, PreviewSize.SMALL), mapService.isInstalledBinding(mapVersion)))
            .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable)
            .orElse(imageViewHelper.getDefaultPlaceholderImage())
            .when(showing));
    onMapLabel.textProperty()
        .bind(mapVersionObservable.flatMap(MapVersionBean::mapProperty)
            .flatMap(MapBean::displayNameProperty)
            .map(displayName -> i18n.get("game.onMapFormat", displayName))
            .orElse(i18n.get("game.onUnknownMap"))
            .when(showing));
    durationLabel.visibleProperty()
        .bind(replay.flatMap(ReplayBean::endTimeProperty).map(Objects::nonNull).orElse(false).when(showing));
    durationLabel.textProperty()
        .bind(replay.flatMap(replayValue -> Bindings.createObjectBinding(() -> Duration.between(replayValue.getStartTime(), replayValue.getEndTime()), replayValue.startTimeProperty(), replayValue.endTimeProperty())
            .map(timeService::shortDuration)));
    replayDurationLabel.visibleProperty()
        .bind(replay.flatMap(ReplayBean::replayTicksProperty).map(Objects::nonNull).orElse(false).when(showing));
    replayDurationLabel.textProperty()
        .bind(replay.flatMap(ReplayBean::replayTicksProperty)
            .map(ticks -> ticks * 100)
            .map(Duration::ofMillis)
            .map(timeService::shortDuration)
            .when(showing));
    modLabel.textProperty()
        .bind(replay.flatMap(ReplayBean::featuredModProperty)
            .flatMap(FeaturedModBean::displayNameProperty)
            .orElse(i18n.get("unknown"))
            .when(showing));
    ObservableValue<Double> qualityObservable = replay.map(ratingService::calculateQuality);
    BooleanExpression qualityNotDefined = BooleanExpression.booleanExpression(qualityObservable.map(quality -> quality.isNaN()));
    qualityLabel.textProperty()
        .bind(Bindings.when(qualityNotDefined)
            .then(i18n.get("gameQuality.undefined"))
            .otherwise(StringExpression.stringExpression(qualityObservable.map(quality -> quality * 100)
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
                    .map(GamePlayerStatsBean::getLeaderboardRatingJournals)
                    .mapToInt(journals -> journals.stream().map(RatingUtil::getRating).findFirst().orElse(0)))
                .average()
                .orElse(Double.NaN))
            .map(average -> average.isNaN() ? "-" : i18n.number(average))
            .when(showing));

    BooleanExpression hasChatMessages = BooleanExpression.booleanExpression(replay.flatMap(ReplayBean::chatMessagesProperty)
        .map(Collection::isEmpty)).not();
    BooleanExpression hasGameOptions = BooleanExpression.booleanExpression(replay.flatMap(ReplayBean::gameOptionsProperty)
        .map(Collection::isEmpty)).not();
    moreInformationPane.visibleProperty().bind(Bindings.or(hasChatMessages, hasGameOptions).when(showing));

    ratingSeparator.visibleProperty().bind(reviewsContainer.visibleProperty());
    reviewSeparator.visibleProperty().bind(reviewsContainer.visibleProperty());
    BooleanExpression localObservable = BooleanExpression.booleanExpression(replay.flatMap(ReplayBean::localProperty));
    reviewsContainer.visibleProperty().bind(localObservable.not().when(showing));
    deleteButton.visibleProperty().bind(localObservable.when(showing));
    teams.bind(replay.flatMap(ReplayBean::teamPlayerStatsProperty));

    optionsTable.itemsProperty()
        .bind(replay.flatMap(ReplayBean::gameOptionsProperty).map(FXCollections::observableList).when(showing));
    chatTable.itemsProperty()
        .bind(replay.flatMap(ReplayBean::chatMessagesProperty).map(FXCollections::observableList).when(showing));
  }

  private void onReplayChanged(ReplayBean newValue) {
    if (newValue == null) {
      reviewsController.setCanWriteReview(false);
      reviews.clear();
      return;
    }

    if (newValue.getReplayFile() != null) {
      enrichReplayLater(newValue.getReplayFile(), newValue);
    }

    reviewsController.setCanWriteReview(true);

    reviewService.getReplayReviews(newValue)
        .collectList()
        .publishOn(javaFxService.getFxApplicationScheduler())
        .doOnNext(reviews::setAll)
        .publishOn(javaFxService.getSingleScheduler())
        .subscribe(null, throwable -> log.error("Unable to populate reviews", throwable));
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
    chatGameTimeColumn.setCellValueFactory(param -> param.getValue().timeProperty());
    chatGameTimeColumn.setCellFactory(param -> new StringCell<>(timeService::asHms));

    chatSenderColumn.setCellValueFactory(param -> param.getValue().senderProperty());
    chatSenderColumn.setCellFactory(param -> new StringCell<>(String::toString));

    chatMessageColumn.setCellValueFactory(param -> param.getValue().messageProperty());
    chatMessageColumn.setCellFactory(param -> new StringCell<>(String::toString));

    optionKeyColumn.setCellValueFactory(param -> param.getValue().keyProperty());
    optionKeyColumn.setCellFactory(param -> new StringCell<>(String::toString));

    optionValueColumn.setCellValueFactory(param -> param.getValue().valueProperty());
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
    reviewsController.bindReviews(reviews);
  }

  @VisibleForTesting
  void onDeleteReview(ReplayReviewBean review) {
    reviewService.deleteGameReview(review)
        .publishOn(javaFxService.getFxApplicationScheduler())
        .then(Mono.fromRunnable(() -> reviews.remove(review)))
        .publishOn(javaFxService.getSingleScheduler())
        .subscribe(null, throwable -> {
          log.error("Review could not be saved", throwable);
          notificationService.addImmediateErrorNotification(throwable, "review.delete.error");
        });
  }

  @VisibleForTesting
  void onSendReview(ReplayReviewBean review) {
    reviewService.saveReplayReview(review)
        .filter(savedReview -> !reviews.contains(savedReview))
        .publishOn(javaFxService.getFxApplicationScheduler())
        .doOnNext(savedReview -> {
          reviews.remove(review);
          reviews.add(savedReview);
        })
        .publishOn(javaFxService.getSingleScheduler())
        .subscribe(null, throwable -> {
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
    }, JavaFxUtil::runLater).exceptionally(throwable -> {
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

  private void populateTeamsContainer() {
    Map<String, List<GamePlayerStatsBean>> teamsValue = teams.get();
    Map<Integer, GamePlayerStatsBean> statsByPlayerId = teamsValue.values()
        .stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toMap(stats -> stats.getPlayer().getId(), Function.identity()));

    playerService.getPlayersByIds(statsByPlayerId.keySet()).thenAccept(players -> {
      List<TeamCardController> newControllers = teamsValue.entrySet().stream().map(entry -> {
        String team = entry.getKey();
        List<GamePlayerStatsBean> playerStats = entry.getValue();

        Set<Integer> playerIds = playerStats.stream()
            .map(GamePlayerStatsBean::getPlayer)
            .map(AbstractEntityBean::getId)
            .collect(Collectors.toSet());

        TeamCardController controller = uiService.loadFxml("theme/team_card.fxml");

        List<PlayerBean> teamPlayers = players.stream()
            .filter(playerBean -> playerIds.contains(playerBean.getId()))
            .toList();

        controller.setRatingPrecision(RatingPrecision.EXACT);
        controller.setRatingProvider(player -> getPlayerRating(player, statsByPlayerId));
        controller.setFactionProvider(player -> getPlayerFaction(player, statsByPlayerId));
        controller.setTeamId(Integer.parseInt(team));
        controller.setPlayers(teamPlayers);

        return controller;
      }).toList();

      JavaFxUtil.runLater(() -> {
        teamCardControllers.clear();
        teamCardControllers.addAll(newControllers);
        teamsContainer.getChildren().setAll(teamCardControllers.stream().map(TeamCardController::getRoot).toList());
      });
    });
  }

  @VisibleForTesting
  Faction getPlayerFaction(PlayerBean player, Map<Integer, GamePlayerStatsBean> statsByPlayerId) {
    return statsByPlayerId.get(player.getId()).getFaction();
  }

  @VisibleForTesting
  Integer getPlayerRating(PlayerBean player, Map<Integer, GamePlayerStatsBean> statsByPlayerId) {
    GamePlayerStatsBean playerStats = statsByPlayerId.get(player.getId());
    if (playerStats == null) {
      return null;
    }

    LeaderboardRatingJournalBean ratingJournal = playerStats.getLeaderboardRatingJournals()
        .stream()
        .findFirst()
        .orElse(null);
    if (ratingJournal == null || ratingJournal.getMeanBefore() == null || ratingJournal.getDeviationBefore() == null) {
      return null;
    }

    return RatingUtil.getRating(ratingJournal.getMeanBefore(), ratingJournal.getDeviationBefore());
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
    notificationService.addNotification(new ImmediateNotification(i18n.get("replay.deleteNotification.heading", replay.get()
        .getTitle()), i18n.get("replay.deleteNotification.info"), Severity.INFO, Arrays.asList(new Action(i18n.get("cancel")), new Action(i18n.get("delete"), event -> deleteReplay()))));
  }

  private void deleteReplay() {
    eventBus.post(new DeleteLocalReplayEvent(replay.get().getReplayFile()));
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
    String replayUrl = ReplayBean.getReplayUrl(replay.get().getId(), clientProperties.getVault()
        .getReplayDownloadUrlFormat());
    ClipboardUtil.copyToClipboard(replayUrl);
  }

  public void showRatingChange() {
    Map<String, List<GamePlayerStatsBean>> teamsValue = teams.get();

    teamCardControllers.forEach(teamCardController -> teamCardController.setStats(teamsValue.get(String.valueOf(teamCardController.getTeamId()))));
  }

  public void onMapPreviewImageClicked() {
    ReplayBean replayValue = replay.get();
    if (replayValue != null && replayValue.getMapVersion() != null) {
      PopupUtil.showImagePopup(mapService.loadPreview(replayValue.getMapVersion(), PreviewSize.LARGE));
    }
  }
}
