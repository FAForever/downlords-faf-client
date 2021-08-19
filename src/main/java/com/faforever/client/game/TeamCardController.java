package com.faforever.client.game;


import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.replay.Replay.PlayerStats;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import com.faforever.commons.api.dto.Faction;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class TeamCardController implements Controller<Node> {
  private final UiService uiService;
  private final I18n i18n;
  public Pane teamPaneRoot;
  public VBox teamPane;
  public Label teamNameLabel;
  private final Map<Integer, RatingChangeLabelController> ratingChangeControllersByPlayerId;

  public TeamCardController(UiService uiService, I18n i18n) {
    this.uiService = uiService;
    this.i18n = i18n;
    ratingChangeControllersByPlayerId = new HashMap<>();
  }

  /**
   * Creates a new {@link TeamCardController} and adds its root to the specified {@code teamsPane}.
   *
   * @param game the game to create teams from
   * @param playerService the service to use to look up players by name
   */
  static void createAndAdd(Game game, PlayerService playerService, UiService uiService, Pane teamsPane) {
    List<Node> teamCardPanes = new ArrayList<>();
    if (game != null) {
      for (Map.Entry<? extends String, ? extends List<String>> entry : game.getTeams().entrySet()) {
        String team = entry.getKey();
        if (team != null) {
          List<Player> players = entry.getValue().stream()
              .flatMap(playerName -> playerService.getPlayerByNameIfOnline(playerName).stream())
              .collect(Collectors.toList());

          TeamCardController teamCardController = uiService.loadFxml("theme/team_card.fxml");
          teamCardController.setPlayersInTeam(team, players,
              player -> RatingUtil.getLeaderboardRating(player, game.getRatingType()), null, RatingPrecision.ROUNDED);
          teamCardPanes.add(teamCardController.getRoot());
        }
      }
    }
    JavaFxUtil.runLater(() -> teamsPane.getChildren().setAll(teamCardPanes));
  }

  public void setPlayersInTeam(String team, List<Player> playerList, Function<Player, Integer> ratingProvider, Function<Player, Faction> playerFactionProvider, RatingPrecision ratingPrecision) {
    int totalRating = 0;
    for (Player player : playerList) {
      // If the server wasn't bugged, this would never be the case.
      if (player == null) {
        continue;
      }
      PlayerCardTooltipController playerCardTooltipController = uiService.loadFxml("theme/player_card_tooltip.fxml");
      Integer playerRating = ratingProvider.apply(player);
      if (playerRating != null) {
        totalRating += playerRating;

        if (ratingPrecision == RatingPrecision.ROUNDED) {
          playerRating = RatingUtil.getRoundedRating(playerRating);
        }
      }
      Faction faction = null;
      if (playerFactionProvider != null) {
        faction = playerFactionProvider.apply(player);
      }
      playerCardTooltipController.setPlayer(player, playerRating, faction);

      RatingChangeLabelController ratingChangeLabelController = uiService.loadFxml("theme/rating_change_label.fxml");
      ratingChangeControllersByPlayerId.put(player.getId(), ratingChangeLabelController);
      HBox container = new HBox(playerCardTooltipController.getRoot(), ratingChangeLabelController.getRoot());
      JavaFxUtil.runLater(() -> teamPane.getChildren().add(container));
    }

    String teamTitle;
    if (team != null) {
      if (Game.NO_TEAM.equals(team)) {
        teamTitle = i18n.get("game.tooltip.teamTitleNoTeam");
      } else if (Game.OBSERVERS_TEAM.equals(team)) {
        teamTitle = i18n.get("game.tooltip.observers");
      } else {
        try {
          teamTitle = i18n.get("game.tooltip.teamTitle", Integer.parseInt(team) - 1, totalRating);
        } catch (NumberFormatException e) {
          teamTitle = "";
          log.warn("Received unknown team in server message: team `{}`", team);
        }
      }
    } else {
      teamTitle = i18n.get("game.tooltip.teamTitleNoTeam");
    }
    String finalTeamTitle = teamTitle;
    JavaFxUtil.runLater(() -> teamNameLabel.setText(finalTeamTitle));
  }

  public void showRatingChange(Map<String, List<PlayerStats>> teams) {
    teams.values().stream()
        .flatMap(List::stream)
        .filter(playerStats -> ratingChangeControllersByPlayerId.containsKey(playerStats.getPlayerId()))
        .forEach(playerStats -> ratingChangeControllersByPlayerId.get(playerStats.getPlayerId()).setRatingChange(playerStats));
  }

  public Node getRoot() {
    return teamPaneRoot;
  }
}
