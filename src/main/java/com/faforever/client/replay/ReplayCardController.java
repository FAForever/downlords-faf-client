package com.faforever.client.replay;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.rating.RatingService;
import com.faforever.client.util.RatingUtil;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.Review;
import com.faforever.client.vault.review.StarsController;
import com.jfoenix.controls.JFXRippler;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ReplayCardController implements Controller<Node> {

  private final ReplayService replayService;
  private final TimeService timeService;
  private final MapService mapService;
  private final RatingService ratingService;
  private final I18n i18n;
  public Label dateLabel;
  public ImageView mapThumbnailImageView;
  public Label gameTitleLabel;
  public Node replayTileRoot;
  public Label timeLabel;
  public Label modLabel;
  public Label durationLabel;
  public Label playerCountLabel;
  public Label ratingLabel;
  public Label qualityLabel;
  public Label numberOfReviewsLabel;
  public HBox teamsContainer;
  public Label onMapLabel;
  public Button watchButton;
  public StarsController starsController;
  private Replay replay;
  private final InvalidationListener reviewsChangedListener = observable -> populateReviews();
  private Consumer<Replay> onOpenDetailListener;
  private JFXRippler jfxRippler;

  @Override
  public void initialize() {
    jfxRippler = new JFXRippler(replayTileRoot);
  }

  public void setReplay(Replay replay) {
    this.replay = replay;

    Optional<MapBean> optionalMap = Optional.ofNullable(replay.getMap());
    if (optionalMap.isPresent()) {
      MapBean map = optionalMap.get();
      Image image = mapService.loadPreview(map.getFolderName(), PreviewSize.SMALL);
      mapThumbnailImageView.setImage(image);
      onMapLabel.setText(i18n.get("game.onMapFormat", map.getDisplayName()));
    } else {
      onMapLabel.setText(i18n.get("game.onUnknownMap"));
    }

    gameTitleLabel.setText(replay.getTitle());
    dateLabel.setText(timeService.asDate(replay.getStartTime()));
    timeLabel.setText(timeService.asShortTime(replay.getStartTime()));
    modLabel.setText(replay.getFeaturedMod().getDisplayName());
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
        .ifPresentOrElse(averageRating -> ratingLabel.setText(i18n.number((int) averageRating)),
            () -> ratingLabel.setText("-"));

    Integer replayTicks = replay.getReplayTicks();
    if (replayTicks != null) {
      durationLabel.setText(timeService.shortDuration(Duration.ofMillis(replayTicks * 100)));
      // FIXME which icon was added in https://github.com/FAForever/downlords-faf-client/commit/58357c603eafead218ef7cceb8907e86c5d864b6#r40460680
//      durationLabel.getGraphic().getStyleClass().remove("duration-icon");
//      durationLabel.getGraphic().getStyleClass().remove("time-icon");
    } else {
      durationLabel.setText(Optional.ofNullable(replay.getEndTime())
          .map(endTime -> timeService.shortDuration(Duration.between(replay.getStartTime(), endTime)))
          .orElse(i18n.get("notAvailable")));
    }

    replay.getTeams()
        .forEach((id, team) -> {
          VBox teamBox = new VBox();

          String teamLabelText = id.equals("1") ? i18n.get("replay.noTeam") : i18n.get("replay.team", Integer.parseInt(id) - 1);
          Label teamLabel = new Label(teamLabelText);
          teamLabel.getStyleClass().add("replay-card-team-label");
          teamLabel.setPadding(new Insets(0, 0, 5, 0));
          teamBox.getChildren().add(teamLabel);
          team.forEach(player -> teamBox.getChildren().add(new Label(player)));

          teamsContainer.getChildren().add(teamBox);
        });

    ObservableList<Review> reviews = replay.getReviews();
    JavaFxUtil.addListener(reviews, new WeakInvalidationListener(reviewsChangedListener));
    reviewsChangedListener.invalidated(reviews);
  }

  private void populateReviews() {
    ObservableList<Review> reviews = replay.getReviews();
    Platform.runLater(() -> {
      numberOfReviewsLabel.setText(i18n.number(reviews.size()));
      starsController.setValue((float) reviews.stream().mapToInt(Review::getScore).average().orElse(0d));
    });
  }

  public Node getRoot() {
    return jfxRippler;
  }

  public void setOnOpenDetailListener(Consumer<Replay> onOpenDetailListener) {
    this.onOpenDetailListener = onOpenDetailListener;
  }

  public void onShowReplayDetail() {
    onOpenDetailListener.accept(replay);
  }

  public void onWatchButtonClicked() {
    replayService.runReplay(replay);
  }
}
