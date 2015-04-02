package com.faforever.client.games;

import com.faforever.client.ladder.GameService;
import javafx.fxml.FXML;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;

public class GamesController {

  @FXML
  ToggleGroup ladderButtons;

  @FXML
  GameService gameService;

  @FXML
  void initialize() {
    ladderButtons.selectedToggleProperty().addListener((observable, oldValue, newValue) -> onLadderButtonSelected(newValue));
  }

  private void onLadderButtonSelected(Toggle button) {
    gameService.publishPotentialPlayer();
  }
}
