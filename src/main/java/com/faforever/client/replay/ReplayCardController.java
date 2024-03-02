package com.faforever.client.replay;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GamePlayerStatsBean;
import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.domain.ReplayReviewsSummaryBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.SimpleChangeListener;
import com.faforever.client.game.PlayerCardController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.rating.RatingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.VaultEntityCardController;
import com.faforever.client.vault.review.StarsController;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ReplayCardController extends VaultEntityCardController<ReplayBean> {

  private final UiService uiService;
  private final ReplayService replayService;
  private final TimeService timeService;
  private final MapService mapService;
  private final RatingService ratingService;
  private final NotificationService notificationService;
  private final ImageViewHelper imageViewHelper;
  private final I18n i18n;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public Label dateLabel;
  public ImageView mapThumbnailImageView;
  public Label gameTitleLabel;
  public Node replayTileRoot;
  public Label timeLabel;
  public Label modLabel;
  public Label tickDurationLabel;
  public Label realTimeDurationLabel;
  public Label playerCountLabel;
  public Label ratingLabel;
  public Label qualityLabel;
  public Label numberOfReviewsLabel;
  public HBox teamsContainer;
  public Label onMapLabel;
  public Button watchButton;
  public Button deleteButton;
  public TextField replayIdField;
  public StarsController starsController;

  private Consumer<ReplayBean> onOpenDetailListener;
  private Runnable onDeleteListener;
  private final ObjectProperty<Map<String, List<GamePlayerStatsBean>>> teams = new SimpleObjectProperty<>();
  private final SimpleChangeListener<Map<String, List<GamePlayerStatsBean>>> teamsListener = this::populatePlayers;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(deleteButton, tickDurationLabel, realTimeDurationLabel);

    ObservableValue<MapVersionBean> mapVersionObservable = entity.map(ReplayBean::mapVersion);
    onMapLabel.textProperty().bind(mapVersionObservable.map(MapVersionBean::map).map(MapBean::displayName)
            .map(displayName -> i18n.get("game.onMapFormat", displayName))
            .when(showing));

    mapThumbnailImageView.imageProperty().bind(mapVersionObservable.map(MapVersionBean::folderName)
            .map(folderName -> mapService.loadPreview(folderName, PreviewSize.SMALL))
            .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable)
            .when(showing));

    deleteButton.visibleProperty().bind(entity.map(ReplayBean::replayFile).map(Objects::nonNull).when(showing));
    watchButton.disableProperty().bind(entity.map(replayBean -> !replayBean.replayAvailable()).when(showing));
    gameTitleLabel.textProperty().bind(entity.map(ReplayBean::title).when(showing));
    ObservableValue<OffsetDateTime> startTimeObservable = entity.map(ReplayBean::startTime);
    dateLabel.textProperty().bind(startTimeObservable.map(timeService::asDate).when(showing));
    timeLabel.textProperty().bind(startTimeObservable.map(timeService::asShortTime).when(showing));
    modLabel.textProperty().bind(entity.map(ReplayBean::featuredMod).map(FeaturedModBean::displayName)
            .when(showing));
    playerCountLabel.textProperty().bind(entity.map(ReplayBean::numPlayers).map(i18n::number).when(showing));
    qualityLabel.textProperty()
        .bind(entity.map(ratingService::calculateQuality)
            .map(quality -> !Double.isNaN(quality) ? i18n.get("percentage", Math.round(quality * 100)) : i18n.get("gameQuality.undefined"))
            .when(showing));
    ratingLabel.textProperty().bind(entity.map(ReplayBean::averageRating).map(i18n::number).orElse("-").when(showing));
    tickDurationLabel.visibleProperty()
        .bind(tickDurationLabel.textProperty().isNotEmpty().and(realTimeDurationLabel.visibleProperty().not()));
    tickDurationLabel.textProperty().bind(entity.map(ReplayBean::replayTicks)
            .map(ticks -> Duration.ofMillis(ticks * 100))
            .map(timeService::shortDuration)
            .when(showing));
    realTimeDurationLabel.visibleProperty().bind(realTimeDurationLabel.textProperty().isNotEmpty());
    realTimeDurationLabel.textProperty().bind(entity.map(replayBean -> {
      OffsetDateTime startTime = replayBean.startTime();
      OffsetDateTime endTime = replayBean.endTime();
      return startTime == null || endTime == null ? null : Duration.between(startTime, endTime);
    }).map(timeService::shortDuration).orElse(i18n.get("notAvailable")).when(showing));
    numberOfReviewsLabel.textProperty()
                        .bind(entity.map(ReplayBean::gameReviewsSummary).map(ReplayReviewsSummaryBean::numReviews)
            .orElse(0)
            .map(i18n::number)
            .when(showing));
    replayIdField.textProperty()
                 .bind(entity.map(ReplayBean::id).map(id -> i18n.get("game.idFormat", id)).when(showing));
    starsController.valueProperty().bind(entity.map(ReplayBean::gameReviewsSummary)
                    .map(reviewsSummary -> reviewsSummary.score() / reviewsSummary.numReviews())
            .when(showing));

    teams.bind(entity.map(ReplayBean::teamPlayerStats).when(showing));
    teams.orElse(Map.of()).addListener(teamsListener);
  }

  private void populatePlayers(Map<String, List<GamePlayerStatsBean>> newValue) {
    teamsContainer.getChildren().clear();
    CompletableFuture.supplyAsync(() -> createTeams(newValue)).thenAcceptAsync(teamCards -> 
      teamsContainer.getChildren().setAll(teamCards), fxApplicationThreadExecutor);
  }

  private List<VBox> createTeams(Map<String, List<GamePlayerStatsBean>> teams) {
    if (teams.size() < 3) {
      return teams.entrySet().stream().map(entry -> {
        String team = entry.getKey();
        List<GamePlayerStatsBean> playerStats = entry.getValue();
        VBox teamCard = new VBox();

        String teamLabelText = team.equals("1") ? i18n.get("replay.noTeam") : i18n.get("replay.team", Integer.parseInt(team) - 1);
        Label teamLabel = new Label(teamLabelText);
        teamLabel.getStyleClass().add("replay-card-team-label");
        teamLabel.setPadding(new Insets(0, 0, 5, 0));
        teamCard.getChildren().add(teamLabel);

        playerStats.forEach(player -> {
          PlayerCardController controller = uiService.loadFxml("theme/player_card.fxml");
          controller.setPlayer(player.player());
          controller.setFaction(player.faction());
          controller.removeAvatar();
          teamCard.getChildren().add(controller.getRoot());
        });

      return teamCard;
    }).toList();
    }
    VBox helperLabel = new VBox(); 
    helperLabel.getChildren().add(new Label("Click for teams"));
    ArrayList<VBox> helpers = new ArrayList<VBox>();
    helpers.add(helperLabel);
    return helpers;
  }

  @Override
  public Node getRoot() {
    return replayTileRoot;
  }

  public void setOnOpenDetailListener(Consumer<ReplayBean> onOpenDetailListener) {
    this.onOpenDetailListener = onOpenDetailListener;
  }

  public void setOnDeleteListener(Runnable onDeleteListener) {
    this.onDeleteListener = onDeleteListener;
  }

  public void onShowReplayDetail() {
    onOpenDetailListener.accept(entity.get());
  }

  public void onWatchButtonClicked() {
    replayService.runReplay(entity.get());
  }

  public void onDeleteButtonClicked() {
    notificationService.addNotification(new ImmediateNotification(i18n.get("replay.deleteNotification.heading", entity.get()
                                                                                                                      .title()),
                                                                  i18n.get("replay.deleteNotification.info"),
                                                                  Severity.INFO,
                                                                  Arrays.asList(new Action(i18n.get("cancel")),
                                                                                new Action(i18n.get("delete"),
                                                                                           this::deleteReplay))));
  }

  private void deleteReplay() {
    if (replayService.deleteReplayFile(entity.get().replayFile()) && onDeleteListener != null) {
      onDeleteListener.run();
    }
  }
}
