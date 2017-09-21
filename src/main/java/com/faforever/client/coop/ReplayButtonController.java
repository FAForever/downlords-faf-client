package com.faforever.client.coop;

import com.faforever.client.fx.Controller;
import javafx.scene.control.Button;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReplayButtonController implements Controller<Button> {
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
