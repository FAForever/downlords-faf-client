package com.faforever.client.vault.replay;

import com.faforever.client.fx.Controller;
import com.faforever.client.replay.ReplayService;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;

public class WatchButtonController implements Controller<Node> {
  private final ReplayService replayService;
  public MenuButton watchButton;
  private int gameId;
  private int playerId;

  public WatchButtonController(ReplayService replayService) {
    this.replayService = replayService;
  }

  @Override
  public Node getRoot() {
    return watchButton;
  }

  public void onWatchClicked() {
    replayService.runLiveReplay(gameId, playerId);
  }

  public void setPlayerId(int playerId) {
    this.playerId = playerId;
  }

  public void setGameId(int gameId) {
    this.gameId = gameId;
  }
}
