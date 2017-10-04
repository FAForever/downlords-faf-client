package com.faforever.client.replay;

import com.faforever.client.fx.WindowController;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Optional;

@Component
@Slf4j
public class ExternalReplayInfoGenerator {
  private final UiService uiService;

  @Inject
  public ExternalReplayInfoGenerator(UiService uiService) {
    this.uiService = uiService;
  }

  public void showExternalReplayInfo(Optional<Replay> replay, String replayId) {
    if (!replay.isPresent()) {
      log.warn("Replay with id '{}' could not be found", replayId);
      return;
    }

    ReplayDetailController replayDetailController = uiService.loadFxml("theme/vault/replay/replay_detail.fxml");

    replayDetailController.setReplay(replay.get());

    Node replayDetailRoot = replayDetailController.getRoot();
    replayDetailRoot.setVisible(true);
    replayDetailRoot.requestFocus();

    Stage stage = new Stage(StageStyle.UNDECORATED);
    WindowController windowController = uiService.loadFxml("theme/window.fxml");
    windowController.configure(stage, (Region) ((AnchorPane) replayDetailRoot).getChildren().get(0), true);
    replayDetailController.setOnClosure(stage::close);
    stage.setWidth(((Region) replayDetailRoot).getWidth());
    stage.setHeight(((Region) replayDetailRoot).getHeight());

    Stage mainStage = StageHolder.getStage();
    stage.setX(mainStage.getX());
    stage.setY(mainStage.getY());

    stage.show();
  }
}
