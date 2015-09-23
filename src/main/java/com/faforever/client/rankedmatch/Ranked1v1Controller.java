package com.faforever.client.rankedmatch;

import com.faforever.client.game.Faction;
import com.faforever.client.game.GameService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.annotation.Autowired;

public class Ranked1v1Controller {

  @FXML
  Pane ranked1v1Root;

  @Autowired
  GameService gameService;

  public Node getRoot() {
    return ranked1v1Root;
  }

  @FXML
  void onAeonButtonClicked(ActionEvent event) {
    startSearchRanked1v1(Faction.AEON);
  }

  private void startSearchRanked1v1(Faction faction) {
    gameService.startSearchRanked1v1(faction).thenAccept(gameLaunchInfo -> {
      // FIXME implement
    }).exceptionally(throwable -> {
      // FIXME implement
      return null;
    });
  }

  @FXML
  void onUefButtonClicked(ActionEvent event) {
    startSearchRanked1v1(Faction.UEF);
  }

  @FXML
  void onCybranButtonClicked(ActionEvent event) {
    startSearchRanked1v1(Faction.CYBRAN);
  }

  @FXML
  void onSeraphimButtonClicked(ActionEvent event) {
    startSearchRanked1v1(Faction.SERAPHIM);
  }
}
