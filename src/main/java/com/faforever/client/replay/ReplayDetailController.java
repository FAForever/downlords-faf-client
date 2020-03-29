package com.faforever.client.replay;

import com.faforever.client.api.dto.Validity;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringCell;
import com.faforever.client.game.Faction;
import com.faforever.client.game.RatingType;
import com.faforever.client.game.TeamCardController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.rating.RatingService;
import com.faforever.client.replay.Replay.ChatMessage;
import com.faforever.client.replay.Replay.GameOption;
import com.faforever.client.replay.Replay.PlayerStats;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.Rating;
import com.faforever.client.util.RatingUtil;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.Review;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.commons.io.Bytes;
import javafx.application.Platform;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.temporal.Temporal;
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
public class ReplayDetailController implements Controller<Node> {

  private final TimeService timeService;
  private final I18n i18n;
  private final UiService uiService;
  private final ReplayService replayService;
  private final RatingService ratingService;
  private final MapService mapService;
  private final PlayerService playerService;
  private final ClientProperties clientProperties;
  private final ReviewService reviewService;
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
  public Pane teamsContainer;
  public ReviewsController reviewsController;
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
  @Setter
  private Runnable onClosure;
  private Replay replay;
  private ArrayList<TeamCardController> teamCardControllers = new ArrayList<>();
  private ObservableMap<String, List<PlayerStats>> teams;

  public ReplayDetailController(TimeService timeService, I18n i18n, UiService uiService, ReplayService replayService,
                                RatingService ratingService, MapService mapService, PlayerService playerService,
                                ReviewService reviewService, ClientProperties clientProperties) {
    this.timeService = timeService;
    this.i18n = i18n;
    this.uiService = uiService;
    this.replayService = replayService;
    this.ratingService = ratingService;
    this.mapService = mapService;
    this.playerService = playerService;
    this.reviewService = reviewService;
    this.clientProperties = clientProperties;
  }

  public void initialize() {
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

    downloadMoreInfoButton.managedProperty().bind(downloadMoreInfoButton.visibleProperty());
    moreInformationPane.managedProperty().bind(moreInformationPane.visibleProperty());
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

    onClosure = () -> ((Pane) replayDetailRoot.getParent()).getChildren().remove(replayDetailRoot);
  }

  public void setReplay(Replay replay) {
    this.replay = replay;

    replayIdField.setText(i18n.get("game.idFormat", replay.getId()));
    titleLabel.setText(replay.getTitle());
    dateLabel.setText(timeService.asDate(replay.getStartTime()));
    timeLabel.setText(timeService.asShortTime(replay.getStartTime()));

    Optional<MapBean> optionalMap = Optional.ofNullable(replay.getMap());
    if (optionalMap.isPresent()) {
      MapBean map = optionalMap.get();
      Image image = mapService.loadPreview(map, PreviewSize.LARGE);
      mapThumbnailImageView.setImage(image);
      onMapLabel.setText(i18n.get("game.onMapFormat", map.getDisplayName()));
    } else {
      onMapLabel.setText(i18n.get("game.onUnknownMap"));
    }

    Temporal endTime = replay.getEndTime();
    if (endTime != null) {
      durationLabel.setText(timeService.shortDuration(Duration.between(replay.getStartTime(), endTime)));
    } else {
      durationLabel.setVisible(false);
    }

    Integer replayTicks = replay.getReplayTicks();
    if (replayTicks != null) {
      replayDurationLabel.setText(timeService.shortDuration(Duration.ofMillis(replayTicks * 100)));
    } else {
      replayDurationLabel.setVisible(false);
    }

    modLabel.setText(
      Optional.ofNullable(replay.getFeaturedMod())
        .map(mod -> mod.getDisplayName())
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
        .flatMapToInt(playerStats -> playerStats.stream()
            .mapToInt(stats -> RatingUtil.getRating(stats.getBeforeMean(), stats.getBeforeDeviation())))
        .average()
        .ifPresent(averageRating -> ratingLabel.setText(i18n.number((int) averageRating)));

    replayService.getSize(replay.getId())
        .thenAccept(replaySize -> Platform.runLater(() -> {
          if (replaySize > -1) {
            String humanReadableSize = Bytes.formatSize(replaySize, i18n.getUserSpecificLocale());
            downloadMoreInfoButton.setText(i18n.get("game.downloadMoreInfo", humanReadableSize));
            watchButton.setText(i18n.get("game.watchButtonFormat", humanReadableSize));
          } else {
            downloadMoreInfoButton.setText(i18n.get("game.replayFileMissing"));
            downloadMoreInfoButton.setDisable(true);
            watchButton.setText(i18n.get("game.replayFileMissing"));
            watchButton.setDisable(true);
          }
        }));

    Optional<Player> currentPlayer = playerService.getCurrentPlayer();
    Assert.state(currentPlayer.isPresent(), "No user is logged in");

    reviewsController.setOnSendReviewListener(this::onSendReview);
    reviewsController.setOnDeleteReviewListener(this::onDeleteReview);
    reviewsController.setReviews(replay.getReviews());
    reviewsController.setOwnReview(replay.getReviews().stream()
        .filter(review -> review.getPlayer().equals(currentPlayer.get()))
        .findFirst());

    // These items are initially empty but will be populated in #onDownloadMoreInfoClicked()
    optionsTable.setItems(replay.getGameOptions());
    chatTable.setItems(replay.getChatMessages());
    teams = replay.getTeamPlayerStats();
    populateTeamsContainer();
  }

  private void onDeleteReview(Review review) {
    reviewService.deleteGameReview(review)
        .thenRun(() -> Platform.runLater(() -> {
          replay.getReviews().remove(review);
          reviewsController.setOwnReview(Optional.empty());
        }))
        // TODO display error to user
        .exceptionally(throwable -> {
          log.warn("Review could not be saved", throwable);
          return null;
        });
  }

  private void onSendReview(Review review) {
    boolean isNew = review.getId() == null;
    Player player = playerService.getCurrentPlayer()
        .orElseThrow(() -> new IllegalStateException("No current player is available"));
    review.setPlayer(player);
    reviewService.saveGameReview(review, replay.getId())
        .thenRun(() -> {
          if (isNew) {
            replay.getReviews().add(review);
          }
          reviewsController.setOwnReview(Optional.of(review));
        })
        // TODO display error to user
        .exceptionally(throwable -> {
          log.warn("Review could not be saved", throwable);
          return null;
        });
  }

  public void onDownloadMoreInfoClicked() {
    // TODO display loading indicator
    downloadMoreInfoButton.setVisible(false);
    replayService.downloadReplay(replay.getId())
        .thenAccept(path -> {
          replayService.enrich(replay, path);
          chatTable.setItems(replay.getChatMessages());
          optionsTable.setItems(replay.getGameOptions());
          moreInformationPane.setVisible(true);
        })
        .exceptionally(throwable -> {
          log.error("Replay could not be enriched", throwable);
          return null;
        });
  }

  private void populateTeamsContainer() {
    if (!replay.getValidity().equals(Validity.VALID)) {
      showRatingChangeButton.setDisable(true);
      showRatingChangeButton.setText(i18n.get("game.notValid"));
    } else if (!replayService.replayChangedRating(replay)) {
      showRatingChangeButton.setDisable(true);
      showRatingChangeButton.setText(i18n.get("game.notRatedYet"));
    }
    Map<Integer, PlayerStats> statsByPlayerId = teams.values().stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toMap(PlayerStats::getPlayerId, Function.identity()));

    Platform.runLater(() -> teams.forEach((team, value) -> {
      List<Integer> playerIds = value.stream()
          .map(PlayerStats::getPlayerId)
          .collect(Collectors.toList());


      TeamCardController controller = uiService.loadFxml("theme/team_card.fxml");
      teamCardControllers.add(controller);

      Function<Player, Rating> playerRatingFunction = player -> {
        PlayerStats playerStats = statsByPlayerId.get(player.getId());
        return new Rating(playerStats.getBeforeMean(), playerStats.getBeforeDeviation());
      };

      Function<Player, Faction> playerFactionFunction = player -> statsByPlayerId.get(player.getId()).getFaction();

      playerService.getPlayersByIds(playerIds)
          .thenAccept(players ->
              controller.setPlayersInTeam(team, players, playerRatingFunction, playerFactionFunction, RatingType.EXACT)
          );

      teamsContainer.getChildren().add(controller.getRoot());
    }));
  }

  @Override
  public Node getRoot() {
    return replayDetailRoot;
  }

  public void onCloseButtonClicked() {
    onClosure.run();
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
    final Clipboard clipboard = Clipboard.getSystemClipboard();
    final ClipboardContent content = new ClipboardContent();
    content.putString(Replay.getReplayUrl(replay.getId(), clientProperties.getVault().getReplayDownloadUrlFormat()));
    clipboard.setContent(content);
  }

  public void showRatingChange() {
    teamCardControllers.forEach(teamCardController -> teamCardController.showRatingChange(teams));
    showRatingChangeButton.setVisible(false);
  }
}
