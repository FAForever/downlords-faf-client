package com.faforever.client.game;


import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.replay.Replay.PlayerStats;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.Rating;
import com.faforever.client.util.RatingUtil;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  private final Map<Integer, RatingChangeLabelController> ratingChangeControllersByPlayerId;

  public TeamCardController(UiService uiService, I18n i18n) {
    this.uiService = uiService;
    this.i18n = i18n;
    ratingChangeControllersByPlayerId = new HashMap<>();
  }

  /**
   * Creates a new {@link TeamCardController} and adds its root to the specified {@code teamsPane}.
   *
   * @param teamsList a mapping of team name (e.g. "2") to a list of player names that are in that team
   * @param playerService the service to use to look up players by name
   */
  static void createAndAdd(ObservableMap<? extends String, ? extends List<String>> teamsList, PlayerService playerService, UiService uiService, Pane teamsPane) {
    for (Map.Entry<? extends String, ? extends List<String>> entry : teamsList.entrySet()) {
      List<Player> players = entry.getValue().stream()
          .map(playerService::getPlayerForUsername)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(Collectors.toList());

      TeamCardController teamCardController = uiService.loadFxml("theme/team_card.fxml");
      teamCardController.setPlayersInTeam(entry.getKey(), players,
          player -> new Rating(player.getGlobalRatingMean(), player.getGlobalRatingDeviation()), RatingType.ROUNDED);
      teamsPane.getChildren().add(teamCardController.getRoot());
    }
  }

  public void setPlayersInTeam(String team, List<Player> playerList, Function<Player, Rating> ratingProvider, RatingType ratingType) {
    int totalRating = 0;
    for (Player player : playerList) {
      // If the server wasn't bugged, this would never be the case.
      if (player == null) {
        continue;
      }
      PlayerCardTooltipController playerCardTooltipController = uiService.loadFxml("theme/player_card_tooltip.fxml");
      int playerRating = RatingUtil.getRating(ratingProvider.apply(player));
      totalRating += playerRating;

      if (ratingType == RatingType.ROUNDED) {
        playerRating = RatingUtil.getRoundedGlobalRating(player);
      }
      playerCardTooltipController.setPlayer(player, playerRating);

      RatingChangeLabelController ratingChangeLabelController = uiService.loadFxml("theme/rating_change_label.fxml");
      ratingChangeControllersByPlayerId.put(player.getId(), ratingChangeLabelController);
      HBox container = new HBox(playerCardTooltipController.getRoot(), ratingChangeLabelController.getRoot());
      teamPane.getChildren().add(container);
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
