package com.faforever.client.replay;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.domain.ReplayReviewsSummaryBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.SimpleChangeListener;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.DeleteLocalReplayEvent;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.rating.RatingService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.StarsController;
import com.google.common.eventbus.EventBus;
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
import java.util.Map;
import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
// TODO: Add tests
public class ReplayCardController implements Controller<Node> {

  private final ReplayService replayService;
  private final TimeService timeService;
  private final MapService mapService;
  private final RatingService ratingService;
  private final NotificationService notificationService;
  private final ImageViewHelper imageViewHelper;
  private final I18n i18n;
  private final EventBus eventBus;

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

  private final ObjectProperty<ReplayBean> replay = new SimpleObjectProperty<>();

  private Consumer<ReplayBean> onOpenDetailListener;

  public void initialize() {
    JavaFxUtil.bindManagedToVisible(deleteButton, tickDurationLabel, realTimeDurationLabel);

    ObservableValue<MapVersionBean> mapVersionObservable = replay.flatMap(ReplayBean::mapVersionProperty);
    onMapLabel.textProperty()
        .bind(mapVersionObservable.flatMap(MapVersionBean::mapProperty)
            .flatMap(MapBean::displayNameProperty)
            .map(displayName -> i18n.get("game.onMapFormat", displayName)));

    mapThumbnailImageView.imageProperty()
        .bind(mapVersionObservable.flatMap(MapVersionBean::folderNameProperty)
            .map(folderName -> mapService.loadPreview(folderName, PreviewSize.SMALL))
            .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable));

    deleteButton.visibleProperty().bind(replay.flatMap(replayBean -> replayBean.replayFileProperty().isNotNull()));
    watchButton.disableProperty().bind(replay.flatMap(replayBean -> replayBean.replayAvailableProperty().not()));
    gameTitleLabel.textProperty().bind(replay.flatMap(ReplayBean::titleProperty));
    ObservableValue<OffsetDateTime> startTimeObservable = replay.flatMap(ReplayBean::startTimeProperty);
    dateLabel.textProperty().bind(startTimeObservable.map(timeService::asDate));
    timeLabel.textProperty().bind(startTimeObservable.map(timeService::asShortTime));
    modLabel.textProperty()
        .bind(replay.flatMap(ReplayBean::featuredModProperty).flatMap(FeaturedModBean::displayNameProperty));
    playerCountLabel.textProperty().bind(replay.flatMap(ReplayBean::numPlayersProperty).map(i18n::number));
    qualityLabel.textProperty()
        .bind(replay.map(ratingService::calculateQuality)
            .map(quality -> !Double.isNaN(quality) ? i18n.get("percentage", Math.round(quality * 100)) : i18n.get("gameQuality.undefined")));
    ratingLabel.textProperty().bind(replay.flatMap(ReplayBean::averageRatingProperty).map(i18n::number).orElse("-"));
    tickDurationLabel.visibleProperty().bind(tickDurationLabel.textProperty().isNotEmpty());
    tickDurationLabel.textProperty()
        .bind(replay.flatMap(ReplayBean::replayTicksProperty)
            .map(ticks -> Duration.ofMillis(ticks * 100))
            .map(timeService::shortDuration));
    realTimeDurationLabel.visibleProperty().bind(tickDurationLabel.textProperty().isEmpty());
    realTimeDurationLabel.textProperty()
        .bind(replay.flatMap(replayBean -> replayBean.endTimeProperty()
            .flatMap(endTime -> replayBean.startTimeProperty()
                .map(startTime -> Duration.between(startTime, endTime))
                .map(timeService::shortDuration))).orElse(i18n.get("notAvailable")));
    numberOfReviewsLabel.textProperty()
        .bind(replay.flatMap(ReplayBean::gameReviewsSummaryProperty)
            .flatMap(ReplayReviewsSummaryBean::numReviewsProperty)
            .orElse(0)
            .map(i18n::number));
    starsController.valueProperty()
        .bind(replay.flatMap(ReplayBean::gameReviewsSummaryProperty)
            .flatMap(reviewsSummary -> reviewsSummary.scoreProperty().divide(reviewsSummary.numReviewsProperty())));

    replay.flatMap(ReplayBean::teamsProperty).addListener((SimpleChangeListener<Map<String, List<String>>>) this::onTeamsChanged);
  }

  public void setReplay(ReplayBean replay) {
    this.replay.set(replay);
  }

  private void onTeamsChanged(Map<String, List<String>> teams) {
    teamsContainer.getChildren().clear();
    teams.forEach((id, team) -> {
      VBox teamBox = new VBox();

      String teamLabelText = id.equals("1") ? i18n.get("replay.noTeam") : i18n.get("replay.team", Integer.parseInt(id) - 1);
      Label teamLabel = new Label(teamLabelText);
      teamLabel.getStyleClass().add("replay-card-team-label");
      teamLabel.setPadding(new Insets(0, 0, 5, 0));
      teamBox.getChildren().add(teamLabel);
      team.forEach(player -> teamBox.getChildren().add(new Label(player)));

      teamsContainer.getChildren().add(teamBox);
    });
  }

  public Node getRoot() {
    return replayTileRoot;
  }

  public void setOnOpenDetailListener(Consumer<ReplayBean> onOpenDetailListener) {
    this.onOpenDetailListener = onOpenDetailListener;
  }

  public void onShowReplayDetail() {
    onOpenDetailListener.accept(replay.get());
  }

  public void onWatchButtonClicked() {
    replayService.runReplay(replay.get());
  }

  public void onDeleteButtonClicked() {
    notificationService.addNotification(new ImmediateNotification(i18n.get("replay.deleteNotification.heading", replay.get().getTitle()), i18n.get("replay.deleteNotification.info"), Severity.INFO, Arrays.asList(new Action(i18n.get("cancel")), new Action(i18n.get("delete"), event -> deleteReplay()))));
  }

  private void deleteReplay() {
    eventBus.post(new DeleteLocalReplayEvent(replay.get().getReplayFile()));
  }
}
