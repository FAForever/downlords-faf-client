package com.faforever.client.coop;

import com.faforever.client.fx.NodeController;
import javafx.scene.control.Button;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReplayButtonController extends NodeController<Button> {
  public Button replayButtonRoot;
  private String replayId;
  private Consumer<ReplayButtonController> onReplayButtonClicked;

  void setOnClickedAction(Consumer<ReplayButtonController> onReplayButtonClicked) {
    this.onReplayButtonClicked = onReplayButtonClicked;
  }

  public void onClicked() {
    onReplayButtonClicked.accept(this);
  }

  public String getReplayId() {
    return replayId;
  }

  public void setReplayId(String replayId) {
    this.replayId = replayId;
  }

  @Override
  public Button getRoot() {
    return replayButtonRoot;
  }
}
