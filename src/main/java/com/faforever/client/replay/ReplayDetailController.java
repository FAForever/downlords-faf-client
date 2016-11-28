package com.faforever.client.replay;

import com.faforever.client.fx.Controller;
import javafx.scene.Node;

public class ReplayDetailController implements Controller<Node> {
  public Node replayDetailRoot;
  private Replay replay;

  public void setReplay(Replay replay) {
    this.replay = replay;
  }

  @Override
  public Node getRoot() {
    return replayDetailRoot;
  }
}
