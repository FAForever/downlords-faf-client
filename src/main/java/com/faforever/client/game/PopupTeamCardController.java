package com.faforever.client.game;


import com.faforever.client.player.PlayerService;
import javafx.fxml.FXML;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class PopupTeamCardController {

  @FXML
  TitledPane teamNumberLabel;

  @FXML
  VBox teamPane;

  @Autowired
  PlayerService playerService;

  public void setTeam(List<String> playerList, int teamNumber){

    teamNumberLabel.setText((Integer.toString(teamNumber)));
    for(String player: playerList){
      PopupPlayerCardController playerCard = new PopupPlayerCardController();
      playerCard.setPlayer(playerService.getPlayerForUsername(player));
    }
  }
}
