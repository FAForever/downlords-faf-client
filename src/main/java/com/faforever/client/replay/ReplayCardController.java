package com.faforever.client.replay;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.util.TimeService;
import com.google.common.base.Joiner;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReplayCardController implements Controller<Node> {

  private final TimeService timeService;
  private final MapService mapService;
  public Label dateLabel;
  public ImageView thumbnailImageView;
  public Label gameTitleLabel;
  public Node replayTileRoot;
  public Label timeLabel;
  public Label modLabel;
  public Label durationLabel;
  public Label playerCountLabel;
  public Label ratingLabel;
  public Label qualityLabel;
  public Label numberOfReviewsLabel;
  public Label playerListLabel;

  private Replay replay;
  private Consumer<Replay> onOpenDetailListener;
  private I18n i18n;

  @Inject
  public ReplayCardController(TimeService timeService, MapService mapService, I18n i18n) {
    this.timeService = timeService;
    this.mapService = mapService;
    this.i18n = i18n;
  }

  public void setReplay(Replay replay) {
    this.replay = replay;

    Optional.ofNullable(replay.getMap()).ifPresent(map -> {
      Image image = mapService.loadPreview(map, PreviewSize.SMALL);
      thumbnailImageView.setImage(image);
    });

    gameTitleLabel.setText(replay.getTitle());
    dateLabel.setText(timeService.asDate(replay.getStartTime()));
    timeLabel.setText(timeService.asShortTime(replay.getStartTime()));
    modLabel.setText(replay.getFeaturedMod().getDisplayName());
    playerCountLabel.setText(i18n.number(replay.getTeams().values().stream().mapToInt(List::size).sum()));

    // FIXME implement
    ratingLabel.setText("n/a");
    qualityLabel.setText("n/a");
    numberOfReviewsLabel.setText("0");

    Instant endTime = replay.getEndTime();
    if (endTime != null) {
      durationLabel.setText(timeService.shortDuration(Duration.between(endTime, replay.getStartTime())));
    } else {
      durationLabel.setText(i18n.get("notAvailable"));
    }

    String players = replay.getTeams().values().stream()
        .map(team -> Joiner.on(i18n.get("textSeparator")).join(team))
        .collect(Collectors.joining(i18n.get("vsSeparator")));
    playerListLabel.setText(players);
  }

  public Node getRoot() {
    return replayTileRoot;
  }

  public void setOnOpenDetailListener(Consumer<Replay> onOpenDetailListener) {
    this.onOpenDetailListener = onOpenDetailListener;
  }

  public void onShowReplayDetail() {
    onOpenDetailListener.accept(replay);
  }
}
