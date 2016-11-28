package com.faforever.client.replay;

import com.faforever.client.map.MapService;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.util.TimeService;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReplayCardController {

  private final ReplayService replayService;
  private final TimeService timeService;
  private final MapService mapService;
  public Label dateLabel;
  public ImageView thumbnailImageView;
  public Label gameTitleLabel;
  public Label playsLabel;
  public Node replayTileRoot;
  private Replay replay;
  private Consumer<Replay> onOpenDetailListener;

  @Inject
  public ReplayCardController(ReplayService replayService, TimeService timeService, MapService mapService) {
    this.replayService = replayService;
    this.timeService = timeService;
    this.mapService = mapService;
  }

  public void setReplay(Replay replay) {
    this.replay = replay;

    Image image = mapService.loadPreview(replay.getMap(), PreviewSize.SMALL);
    thumbnailImageView.setImage(image);

    gameTitleLabel.setText(replay.getTitle());
    playsLabel.setText(String.format("%d", replay.getViews()));
    dateLabel.setText(timeService.asDate(replay.getEndTime()));
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
