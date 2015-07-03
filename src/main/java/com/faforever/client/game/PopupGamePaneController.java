package com.faforever.client.game;


import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Map;

public class PopupGamePaneController {

  public Node teamListPane;
  public Label modsListLabel;

  public Node PopGamePaneController(GameInfoBean gameInfoBean){
    for(Map.Entry<String, List<String>> entry: gameInfoBean.getTeams().entrySet()){
      PopupTeamCardController teamCard = new PopupTeamCardController();
      teamCard.setTeam(entry.getValue(),Integer.parseInt(entry.getKey()));
    }
    return teamListPane;
  }
}
