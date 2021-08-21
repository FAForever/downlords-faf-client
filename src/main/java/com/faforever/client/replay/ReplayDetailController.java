package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GamePlayerStatsBean;
import com.faforever.client.domain.LeaderboardRatingJournalBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.domain.ReplayBean.ChatMessage;
import com.faforever.client.domain.ReplayBean.GameOption;
import com.faforever.client.domain.ReplayReviewBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringCell;
import com.faforever.client.game.RatingPrecision;
import com.faforever.client.game.TeamCardController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.rating.RatingService;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.ClipboardUtil;
import com.faforever.client.util.RatingUtil;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.commons.api.dto.Faction;
import com.faforever.commons.api.dto.Validity;
import com.faforever.commons.io.Bytes;
import com.google.common.annotations.VisibleForTesting;
import javafx.collections.ObservableMap;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class ReplayDetailController implements Controller<Node> {

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
  private final ArrayList<TeamCardController> teamCardControllers = new ArrayList<>();
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
  public Label notRatedReasonLabel;
  private ReplayBean replay;
  private ObservableMap<String, List<GamePlayerStatsBean>> teams;

  public void initialize() {
    JavaFxUtil.bindManagedToVisible(notRatedReasonLabel, showRatingChangeButton);
    JavaFxUtil.addLabelContextMenus(uiService, onMapLabel, titleLabel);
    JavaFxUtil.fixScrollSpeed(scrollPane);

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

    JavaFxUtil.bindManagedToVisible(downloadMoreInfoButton, moreInformationPane, teamsInfoBox,
        reviewsContainer, ratingSeparator, reviewSeparator, getRoot());

    replayDetailRoot.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode() == KeyCode.ESCAPE) {
        onCloseButtonClicked();
      }
    });

    moreInformationPane.setVisible(false);

    reviewsController.getRoot().setMaxSize(Integer.MAX_VALUE, Integer.MAX_VALUE);

    copyButton.setText(i18n.get("replay.copyUrl"));

    dateLabel.setTooltip(new Tooltip(i18n.get("replay.dateTooltip")));
    timeLabel.setTooltip(new Tooltip(i18n.get("replay.timeTooltip")));
    modLabel.setTooltip(new Tooltip(i18n.get("replay.modTooltip")));
    durationLabel.setTooltip(new Tooltip(i18n.get("replay.durationTooltip")));
    replayDurationLabel.setTooltip(new Tooltip(i18n.get("replay.replayDurationTooltip")));
    playerCountLabel.setTooltip(new Tooltip(i18n.get("replay.playerCountTooltip")));
    ratingLabel.setTooltip(new Tooltip(i18n.get("replay.ratingTooltip")));
    qualityLabel.setTooltip(new Tooltip(i18n.get("replay.qualityTooltip")));
    reviewsController.setReviewSupplier(ReplayReviewBean::new);
  }

  public void setReplay(ReplayBean replay) {
    this.replay = replay;
    watchButton.setDisable(false);
    downloadMoreInfoButton.setDisable(false);
    mapThumbnailImageView.setImage(null);

    replayIdField.setText(i18n.get("game.idFormat", replay.getId()));
    titleLabel.setText(replay.getTitle());
    dateLabel.setText(timeService.asDate(replay.getStartTime()));
    timeLabel.setText(timeService.asShortTime(replay.getStartTime()));

    Optional<MapVersionBean> optionalMapVersion = Optional.ofNullable(replay.getMapVersion());
    if (optionalMapVersion.isPresent()) {
      MapVersionBean mapVersion = optionalMapVersion.get();
      Image image = mapService.loadPreview(mapVersion.getFolderName(), PreviewSize.LARGE);
      mapThumbnailImageView.setImage(image);
      onMapLabel.setText(i18n.get("game.onMapFormat", mapVersion.getMap().getDisplayName()));
    } else {
      onMapLabel.setText(i18n.get("game.onUnknownMap"));
    }

    OffsetDateTime endTime = replay.getEndTime();
    if (endTime != null) {
      durationLabel.setText(timeService.shortDuration(Duration.between(replay.getStartTime(), endTime)));
      durationLabel.setVisible(true);
    } else {
      durationLabel.setVisible(false);
    }

    Integer replayTicks = replay.getReplayTicks();
    if (replayTicks != null) {
      replayDurationLabel.setText(timeService.shortDuration(Duration.ofMillis(replayTicks * 100)));
      replayDurationLabel.setVisible(true);
    } else {
      replayDurationLabel.setVisible(false);
    }

    modLabel.setText(
        Optional.ofNullable(replay.getFeaturedMod())
            .map(FeaturedModBean::getDisplayName)
            .orElseGet(() -> i18n.get("unknown"))
    );
    playerCountLabel.setText(i18n.number(replay.getTeams().values().stream().mapToInt(List::size).sum()));

    double gameQuality = ratingService.calculateQuality(replay);
    if (!Double.isNaN(gameQuality)) {
      qualityLabel.setText(i18n.get("percentage", Math.round(gameQuality * 100)));
    } else {
      qualityLabel.setText(i18n.get("gameQuality.undefined"));
    }


    replay.getTeamPlayerStats().values().stream()
        .flatMapToInt(playerStats -> playerStats.stream().map(stats -> stats.getLeaderboardRatingJournals().stream().findFirst())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .mapToInt(ratingJournal -> RatingUtil.getRating(ratingJournal.getMeanBefore(), ratingJournal.getDeviationBefore())))
        .average()
        .ifPresentOrElse(averageRating -> ratingLabel.setText(i18n.number((int) averageRating)),
            () -> ratingLabel.setText("-"));

    if (replay.getReplayFile() == null) {
      if (replay.getReplayAvailable()) {
        replayService.getSize(replay.getId())
            .thenAccept(replaySize -> JavaFxUtil.runLater(() -> {
              String humanReadableSize = Bytes.formatSize(replaySize, i18n.getUserSpecificLocale());
              downloadMoreInfoButton.setText(i18n.get("game.downloadMoreInfo", humanReadableSize));
              watchButton.setText(i18n.get("game.watchButtonFormat", humanReadableSize));
              downloadMoreInfoButton.setVisible(true);
            }));
      } else {
        if (replay.getStartTime().isBefore(OffsetDateTime.now().minusDays(1))) {
          downloadMoreInfoButton.setText(i18n.get("game.replayFileMissing"));
          watchButton.setText(i18n.get("game.replayFileMissing"));
        } else {
          downloadMoreInfoButton.setText(i18n.get("game.replayNotAvailable"));
          watchButton.setText(i18n.get("game.replayNotAvailable"));
        }
        downloadMoreInfoButton.setDisable(true);
        watchButton.setDisable(true);
      }
      PlayerBean currentPlayer = playerService.getCurrentPlayer();

      reviewsController.setOnSendReviewListener(this::onSendReview);
      reviewsController.setOnDeleteReviewListener(this::onDeleteReview);
      reviewsController.setReviews(replay.getReviews());
      reviewsController.setOwnReview(replay.getReviews().stream()
          .filter(review -> review.getPlayer().equals(currentPlayer))
          .findFirst().orElse(null));

      // These items are initially empty but will be populated in #onDownloadMoreInfoClicked()
      moreInformationPane.setVisible(false);
      optionsTable.setItems(replay.getGameOptions());
      chatTable.setItems(replay.getChatMessages());
      teams = replay.getTeamPlayerStats();
      populateTeamsContainer();
    } else {
      watchButton.setText(i18n.get("game.watch"));
      ratingSeparator.setVisible(false);
      reviewSeparator.setVisible(false);
      reviewsContainer.setVisible(false);
      teamsInfoBox.setVisible(false);
      downloadMoreInfoButton.setVisible(false);
      showRatingChangeButton.setVisible(false);
      optionsTable.setItems(replay.getGameOptions());
      chatTable.setItems(replay.getChatMessages());
      replayService.enrich(replay, replay.getReplayFile());
      chatTable.setItems(replay.getChatMessages());
      optionsTable.setItems(replay.getGameOptions());
      moreInformationPane.setVisible(true);
    }
  }

  @VisibleForTesting
  void onDeleteReview(ReplayReviewBean review) {
    reviewService.deleteGameReview(review)
        .thenRun(() -> JavaFxUtil.runLater(() -> {
          replay.getReviews().remove(review);
          reviewsController.setOwnReview(null);
        }))
        .exceptionally(throwable -> {
          log.warn("Review could not be saved", throwable);
          notificationService.addImmediateErrorNotification(throwable, "review.delete.error");
          return null;
        });
  }

  @VisibleForTesting
  void onSendReview(ReplayReviewBean review) {
    boolean isNew = review.getId() == null;
    PlayerBean player = playerService.getCurrentPlayer();
    review.setPlayer(player);
    review.setReplay(replay);
    reviewService.saveReplayReview(review)
        .thenRun(() -> {
          if (isNew) {
            replay.getReviews().add(review);
          }
          reviewsController.setOwnReview(review);
        })
        .exceptionally(throwable -> {
          log.warn("Review could not be saved", throwable);
          notificationService.addImmediateErrorNotification(throwable, "review.save.error");
          return null;
        });
  }

  public void onDownloadMoreInfoClicked() {
    // TODO display loading indicator
    downloadMoreInfoButton.setVisible(false);
    replayService.downloadReplay(replay.getId())
        .thenAccept(path -> {
          replayService.enrich(replay, path);
          if (onMapLabel.getText().equals(i18n.get("game.onUnknownMap")) && replay.getMapVersion() != null) {
            MapVersionBean map = replay.getMapVersion();
            onMapLabel.setText(i18n.get("game.onMapFormat", map.getFolderName()));
            Image image = mapService.loadPreview(map.getFolderName(), PreviewSize.LARGE);
            mapThumbnailImageView.setImage(image);
            if (mapGeneratorService.isGeneratedMap(map.getFolderName())) {
              mapService.generateIfNotInstalled(map.getFolderName()).thenAccept(mapName -> JavaFxUtil.runLater(() -> {
                Image generatedPreview = mapService.loadPreview(map.getFolderName(), PreviewSize.LARGE);
                mapThumbnailImageView.setImage(generatedPreview);
              }));
            }
          }
          chatTable.setItems(replay.getChatMessages());
          optionsTable.setItems(replay.getGameOptions());
          moreInformationPane.setVisible(true);
        })
        .exceptionally(throwable -> {
          if (throwable.getCause() instanceof FileNotFoundException) {
            log.warn("Replay not available on server yet", throwable);
            notificationService.addImmediateWarnNotification("replayNotAvailable", replay.getId());
          } else {
            log.error("Replay could not be enriched", throwable);
            notificationService.addImmediateErrorNotification(throwable, "replay.enrich.error");
          }
          return null;
        });
  }

  private void populateTeamsContainer() {
    teamsContainer.getChildren().clear();
    teamCardControllers.clear();
    configureRatingControls();
    Map<Integer, GamePlayerStatsBean> statsByPlayerId = teams.values().stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toMap(stats -> stats.getPlayer().getId(), Function.identity()));

    teams.forEach((team, value) -> {
      List<Integer> playerIds = value.stream()
          .map(stats -> stats.getPlayer().getId())
          .collect(Collectors.toList());


      TeamCardController controller = uiService.loadFxml("theme/team_card.fxml");
      teamCardControllers.add(controller);

      Function<PlayerBean, Integer> playerRatingFunction = player -> getPlayerRating(player, statsByPlayerId);

      Function<PlayerBean, Faction> playerFactionFunction = player -> getPlayerFaction(player, statsByPlayerId);

      playerService.getPlayersByIds(playerIds)
          .thenAccept(players ->
              controller.setPlayersInTeam(team, players, playerRatingFunction, playerFactionFunction, RatingPrecision.EXACT)
          );

      JavaFxUtil.runLater(() -> teamsContainer.getChildren().add(controller.getRoot()));
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

    LeaderboardRatingJournalBean ratingJournal = playerStats.getLeaderboardRatingJournals().stream().findFirst().orElse(null);
    if (ratingJournal == null || ratingJournal.getMeanBefore() == null || ratingJournal.getDeviationBefore() == null) {
      return null;
    }

    return RatingUtil.getRating(ratingJournal.getMeanBefore(), ratingJournal.getDeviationBefore());
  }

  private void configureRatingControls() {
    if (!replay.getValidity().equals(Validity.VALID)) {
      showRatingChangeButton.setVisible(false);
      notRatedReasonLabel.setVisible(true);
      String reasonText = i18n.getOrDefault(replay.getValidity().toString(), "game.reasonNotValid", i18n.get(replay.getValidity().getI18nKey()));
      notRatedReasonLabel.setText(reasonText);
    } else if (!replayService.replayChangedRating(replay)) {
      showRatingChangeButton.setVisible(false);
      notRatedReasonLabel.setVisible(true);
      notRatedReasonLabel.setText(i18n.get("game.notRatedYet"));
    } else {
      showRatingChangeButton.setVisible(true);
      showRatingChangeButton.setDisable(false);
      notRatedReasonLabel.setVisible(false);
    }
  }

  public void onReport() {
    ReportDialogController reportDialogController = uiService.loadFxml("theme/reporting/report_dialog.fxml");
    reportDialogController.setReplay(replay);
    Scene scene = getRoot().getScene();
    if (scene != null) {
      reportDialogController.setOwnerWindow(scene.getWindow());
    }
    reportDialogController.show();
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
    replayService.runReplay(replay);
  }


  public void copyLink() {
    String replayUrl = ReplayBean.getReplayUrl(replay.getId(), clientProperties.getVault().getReplayDownloadUrlFormat());
    ClipboardUtil.copyToClipboard(replayUrl);
  }

  public void showRatingChange() {
    teamCardControllers.forEach(teamCardController -> teamCardController.showRatingChange(teams));
    showRatingChangeButton.setDisable(true);
  }
}
