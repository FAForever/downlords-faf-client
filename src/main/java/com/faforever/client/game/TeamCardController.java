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
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.List;

public class TeamCardController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @FXML
  TitledPane teamPaneRoot;
  @FXML
  VBox teamPane;

  @Resource
  PlayerService playerService;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  I18n i18n;

  public void setPlayersInTeam(String team, List<String> playerList) {
    String localizedTeamTile;
    if (team == null) {
      localizedTeamTile = i18n.get("game.tooltip.observers");
    } else if (team.equals("0")) {
      // FIXME figure out what's the value for "no team" (instead of "0")
      localizedTeamTile = i18n.get("game.tooltip.teamTitleNoTeam");
    } else {
      localizedTeamTile = i18n.get("game.tooltip.teamTitle", team);
    }
    teamPaneRoot.setText(localizedTeamTile);

    for (String player : playerList) {
      PlayerInfoBean playerInfoBean = playerService.getPlayerForUsername(player);
      if (playerInfoBean == null) {
        logger.warn("{} is not returned by playerService", player);
        continue;
      }
      PlayerCardTooltipController playerCardTooltipController = applicationContext.getBean(PlayerCardTooltipController.class);
      playerCardTooltipController.setPlayer(playerInfoBean);

      teamPane.getChildren().add(playerCardTooltipController.getRoot());
    }
  }

  public Node getRoot() {
    return teamPaneRoot;
  }
}
