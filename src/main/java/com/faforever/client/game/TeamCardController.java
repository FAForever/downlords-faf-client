package com.faforever.client.game;


import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import javafx.scene.Node;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TeamCardController implements Controller<Node> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TitledPane teamPaneRoot;
  public VBox teamPane;

  @Inject
  PlayerService playerService;
  @Inject
  UiService uiService;
  @Inject
  I18n i18n;

  public void setPlayersInTeam(String team, List<String> playerList) {
    int totalRating = 0;
    for (String player : playerList) {
      Player playerInfoBean = playerService.getPlayerForUsername(player);
      if (playerInfoBean == null) {
        logger.warn("{} is not returned by playerService", player);
        continue;
      }
      PlayerCardTooltipController playerCardTooltipController = uiService.loadFxml("theme/player_card_tooltip.fxml");
      playerCardTooltipController.setPlayer(playerInfoBean);

      teamPane.getChildren().add(playerCardTooltipController.getRoot());
      totalRating += RatingUtil.getRoundedGlobalRating(playerInfoBean);
    }

    String teamTitle;
    if ("1".equals(team) || "-1".equals(team)) {
      teamTitle = i18n.get("game.tooltip.teamTitleNoTeam");
    } else if ("null".equals(team)) {
      teamTitle = i18n.get("game.tooltip.observers");
    } else {
      teamTitle = i18n.get("game.tooltip.teamTitle", Integer.valueOf(team) - 1, totalRating);
    }
    teamPaneRoot.setText(teamTitle);
  }

  public Node getRoot() {
    return teamPaneRoot;
  }
}
