package com.faforever.client.replay;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringCell;
import com.faforever.client.game.TeamCardController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.temporal.Temporal;
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
  private final ReviewService reviewService;
  public Pane replayDetailRoot;
  public Label titleLabel;
  public Label dateLabel;
  public Label timeLabel;
  public Label modLabel;
  public Label durationLabel;
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
  private Replay replay;

  public ReplayDetailController(TimeService timeService, I18n i18n, UiService uiService, ReplayService replayService,
                                RatingService ratingService, MapService mapService, PlayerService playerService,
                                ReviewService reviewService) {
    this.timeService = timeService;
    this.i18n = i18n;
    this.uiService = uiService;
    this.replayService = replayService;
    this.ratingService = ratingService;
    this.mapService = mapService;
    this.playerService = playerService;
    this.reviewService = reviewService;
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

    modLabel.setText(replay.getFeaturedMod().getDisplayName());
    playerCountLabel.setText(i18n.number(replay.getTeams().values().stream().mapToInt(List::size).sum()));
    qualityLabel.setText(i18n.number((int) ((ratingService.calculateQuality(replay) * 10) / 10)));
    replay.getTeamPlayerStats().values().stream()
        .flatMapToInt(playerStats -> playerStats.stream()
            .mapToInt(stats -> RatingUtil.getRating(stats.getMean(), stats.getDeviation())))
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

    populateTeamsContainer(replay.getTeamPlayerStats());
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

  private void populateTeamsContainer(ObservableMap<String, List<PlayerStats>> teams) {
    Map<Integer, PlayerStats> statsByPlayerId = teams.values().stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toMap(PlayerStats::getPlayerId, Function.identity()));

    Platform.runLater(() -> teams.forEach((key, value) -> {
      List<Integer> playerIds = value.stream()
          .map(PlayerStats::getPlayerId)
          .collect(Collectors.toList());

      TeamCardController controller = uiService.loadFxml("theme/team_card.fxml");
      playerService.getPlayersByIds(playerIds)
          .thenAccept(players -> controller.setPlayersInTeam(key, players, player -> {
            PlayerStats playerStats = statsByPlayerId.get(player.getId());
            return new Rating(playerStats.getMean(), playerStats.getDeviation());
          }));

      teamsContainer.getChildren().add(controller.getRoot());
    }));
  }

  @Override
  public Node getRoot() {
    return replayDetailRoot;
  }

  public void onCloseButtonClicked() {
    ((Pane) replayDetailRoot.getParent()).getChildren().remove(replayDetailRoot);
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
}
