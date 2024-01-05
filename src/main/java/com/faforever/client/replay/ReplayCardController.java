package com.faforever.client.replay;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.domain.ReplayReviewsSummaryBean;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.SimpleChangeListener;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.rating.RatingService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.VaultEntityCardController;
import com.faforever.client.vault.review.StarsController;
import com.faforever.client.domain.GamePlayerStatsBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.commons.api.dto.Faction;
import com.faforever.client.game.PlayerCardController;
import com.faforever.client.game.RatingPrecision;
import com.faforever.client.util.RatingUtil;
import com.faforever.client.theme.UiService;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.concurrent.CompletableFuture;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
// TODO: Add tests
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
  public StarsController starsController;

  private Consumer<ReplayBean> onOpenDetailListener;
  private Runnable onDeleteListener;
  private final ObjectProperty<Map<String, List<GamePlayerStatsBean>>> teams = new SimpleObjectProperty<>();
  private final SimpleChangeListener<Map<String, List<GamePlayerStatsBean>>> teamsListener = this::populatePlayers;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(deleteButton, tickDurationLabel, realTimeDurationLabel);

    ObservableValue<MapVersionBean> mapVersionObservable = entity.flatMap(ReplayBean::mapVersionProperty);
    onMapLabel.textProperty()
        .bind(mapVersionObservable.flatMap(MapVersionBean::mapProperty)
            .flatMap(MapBean::displayNameProperty)
            .map(displayName -> i18n.get("game.onMapFormat", displayName))
            .when(showing));

    mapThumbnailImageView.imageProperty()
        .bind(mapVersionObservable.flatMap(MapVersionBean::folderNameProperty)
            .map(folderName -> mapService.loadPreview(folderName, PreviewSize.SMALL))
            .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable)
            .when(showing));

    deleteButton.visibleProperty()
        .bind(entity.flatMap(replayBean -> replayBean.replayFileProperty().isNotNull()).when(showing));
    watchButton.disableProperty()
        .bind(entity.flatMap(replayBean -> replayBean.replayAvailableProperty().not()).when(showing));
    gameTitleLabel.textProperty().bind(entity.flatMap(ReplayBean::titleProperty).when(showing));
    ObservableValue<OffsetDateTime> startTimeObservable = entity.flatMap(ReplayBean::startTimeProperty);
    dateLabel.textProperty().bind(startTimeObservable.map(timeService::asDate).when(showing));
    timeLabel.textProperty().bind(startTimeObservable.map(timeService::asShortTime).when(showing));
    modLabel.textProperty()
        .bind(entity.flatMap(ReplayBean::featuredModProperty)
            .flatMap(FeaturedModBean::displayNameProperty)
            .when(showing));
    playerCountLabel.textProperty()
        .bind(entity.flatMap(ReplayBean::numPlayersProperty).map(i18n::number).when(showing));
    qualityLabel.textProperty()
        .bind(entity.map(ratingService::calculateQuality)
            .map(quality -> !Double.isNaN(quality) ? i18n.get("percentage", Math.round(quality * 100)) : i18n.get("gameQuality.undefined"))
            .when(showing));
    ratingLabel.textProperty()
        .bind(entity.flatMap(ReplayBean::averageRatingProperty).map(i18n::number).orElse("-").when(showing));
    tickDurationLabel.visibleProperty()
        .bind(tickDurationLabel.textProperty().isNotEmpty().and(realTimeDurationLabel.visibleProperty().not()));
    tickDurationLabel.textProperty()
        .bind(entity.flatMap(ReplayBean::replayTicksProperty)
            .map(ticks -> Duration.ofMillis(ticks * 100))
            .map(timeService::shortDuration)
            .when(showing));
    realTimeDurationLabel.visibleProperty().bind(realTimeDurationLabel.textProperty().isNotEmpty());
    realTimeDurationLabel.textProperty()
        .bind(entity.flatMap(replayBean -> replayBean.endTimeProperty()
            .flatMap(endTime -> replayBean.startTimeProperty()
                .map(startTime -> Duration.between(startTime, endTime))
                .map(timeService::shortDuration))).orElse(i18n.get("notAvailable")).when(showing));
    numberOfReviewsLabel.textProperty()
        .bind(entity.flatMap(ReplayBean::gameReviewsSummaryProperty)
            .flatMap(ReplayReviewsSummaryBean::numReviewsProperty)
            .orElse(0)
            .map(i18n::number)
            .when(showing));
    starsController.valueProperty()
        .bind(entity.flatMap(ReplayBean::gameReviewsSummaryProperty)
            .flatMap(reviewsSummary -> reviewsSummary.scoreProperty().divide(reviewsSummary.numReviewsProperty()))
            .when(showing));

    teams.bind(entity.flatMap(ReplayBean::teamPlayerStatsProperty).when(showing));
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
          controller.setPlayer(player.getPlayer());
          controller.setFaction(player.getFaction());
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
                                                                                                                      .getTitle()),
                                                                  i18n.get("replay.deleteNotification.info"),
                                                                  Severity.INFO,
                                                                  Arrays.asList(new Action(i18n.get("cancel")),
                                                                                new Action(i18n.get("delete"),
                                                                                           this::deleteReplay))));
  }

  private void deleteReplay() {
    if (replayService.deleteReplayFile(entity.get().getReplayFile()) && onDeleteListener != null) {
      onDeleteListener.run();
    }
  }
}
