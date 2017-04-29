package com.faforever.client.game;


import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.Rating;
import com.faforever.client.util.RatingUtil;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TeamCardController implements Controller<Node> {

  private final UiService uiService;
  private final I18n i18n;
  public Pane teamPaneRoot;
  public VBox teamPane;
  public Label teamNameLabel;

  @Inject
  public TeamCardController(UiService uiService, I18n i18n) {
    this.uiService = uiService;
    this.i18n = i18n;
  }

  /**
   * Creates a new {@link TeamCardController} and adds its root to the specified {@code teamsPane}.
   *
   * @param teamsList a mapping of team name (e.g. "2") to a list of player names that are in that team
   * @param playerService the service to use to look up players by name
   */
  public static void createAndAdd(ObservableMap<? extends String, ? extends List<String>> teamsList, PlayerService playerService, UiService uiService, Pane teamsPane) {
    for (Map.Entry<? extends String, ? extends List<String>> entry : teamsList.entrySet()) {
      List<Player> players = entry.getValue().stream()
          .map(playerService::getPlayerForUsername)
          .collect(Collectors.toList());

      TeamCardController teamCardController = uiService.loadFxml("theme/team_card.fxml");
      teamCardController.setPlayersInTeam(entry.getKey(), players,
          player -> new Rating(player.getGlobalRatingMean(), player.getGlobalRatingDeviation()));
      teamsPane.getChildren().add(teamCardController.getRoot());
    }
  }

  public void setPlayersInTeam(String team, List<Player> playerList, Function<Player, Rating> ratingProvider) {
    int totalRating = 0;
    for (Player player : playerList) {
      // If the server wasn't bugged, this would never be the case.
      if (player == null) {
        continue;
      }
      PlayerCardTooltipController playerCardTooltipController = uiService.loadFxml("theme/player_card_tooltip.fxml");
      playerCardTooltipController.setPlayer(player);

      teamPane.getChildren().add(playerCardTooltipController.getRoot());
      totalRating += RatingUtil.getRating(ratingProvider.apply(player));
    }

    String teamTitle;
    if ("1".equals(team) || "-1".equals(team)) {
      teamTitle = i18n.get("game.tooltip.teamTitleNoTeam");
    } else if ("null".equals(team)) {
      teamTitle = i18n.get("game.tooltip.observers");
    } else {
      teamTitle = i18n.get("game.tooltip.teamTitle", Integer.valueOf(team) - 1, totalRating);
    }
    teamNameLabel.setText(teamTitle);
  }

  public Node getRoot() {
    return teamPaneRoot;
  }
}
