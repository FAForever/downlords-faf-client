package com.faforever.client.game;


import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.lang.invoke.MethodHandles;
import java.util.List;

public class PopupTeamCardController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @FXML
  TitledPane teamPaneRoot;

  @FXML
  VBox teamPane;

  @Autowired
  PlayerService playerService;

  @Autowired
  ApplicationContext applicationContext;

  @Autowired
  I18n i18n;

  /**
   *
   * @param playerList
   * @param teamNumber
   * @return whether playerInfoBean is a null parameter
   */
  //TODO remove boolean type when running into null playerInfoBean
  public boolean setTeam(List<String> playerList, int teamNumber) {

    String localizedTeamTile;
    if (teamNumber == 0) {
      localizedTeamTile = i18n.get("game.popup.teamTitleNoTeam");
    } else if (teamNumber == -1) {
      localizedTeamTile = i18n.get("game.popup.observers");
    } else {
      localizedTeamTile = i18n.get("game.popup.teamTitle", teamNumber);
    }
    teamPaneRoot.setText(localizedTeamTile);

    for (String player : playerList) {
      PlayerInfoBean playerInfoBean = playerService.getPlayerForUsername(player);
      if (playerInfoBean == null) {
        logger.warn("{} is not returned by playerService", player);
        return false;
      }
      PopupPlayerCardController popupPlayerCardController = applicationContext.getBean(PopupPlayerCardController.class);
      popupPlayerCardController.setPlayer(playerInfoBean);

      teamPane.getChildren().add(popupPlayerCardController.getRoot());
    }
    return true;
  }

  public Node getRoot() {
    return teamPaneRoot;
  }
}
