package com.faforever.client.game;


import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import com.google.common.base.Joiner;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@RequiredArgsConstructor
public class GameTooltipController implements Controller<Node> {

  private final UiService uiService;

  public TitledPane modsPane;
  public TilePane teamsPane;
  public Label modsLabel;
  public VBox gameTooltipRoot;
  private InvalidationListener teamInvalidationListener;
  private InvalidationListener simModsInvalidationListener;
  private int maxPrefColumns;
  private GameBean game;
  private boolean showMods;

  public void initialize() {
    JavaFxUtil.bindManagedToVisible(modsPane);
    modsPane.visibleProperty().bind(modsLabel.textProperty().isNotEmpty());
    modsLabel.setText("");
    maxPrefColumns = teamsPane.getPrefColumns();
    showMods = true;
  }

  public void setGame(GameBean game) {
    this.game = game;
  }

  public void displayGame() {
    resetListeners();
    if (game == null) {
      return;
    }
    WeakInvalidationListener weakTeamInvalidationListener = new WeakInvalidationListener(teamInvalidationListener);
    JavaFxUtil.addAndTriggerListener(game.teamsProperty(), weakTeamInvalidationListener);
    if (showMods) {
      WeakInvalidationListener weakModInvalidationListener = new WeakInvalidationListener(simModsInvalidationListener);
      JavaFxUtil.addAndTriggerListener(game.simModsProperty(), weakModInvalidationListener);
    }
  }

  private void resetListeners() {
    teamInvalidationListener = observable -> createTeams();
    simModsInvalidationListener = observable -> createModsList();
  }

  private void createTeams() {
    if (game != null) {
      List<Node> teamCardPanes = new ArrayList<>();
      for (Map.Entry<Integer, Set<PlayerBean>> entry : game.getTeams().entrySet()) {
        Integer team = entry.getKey();
        if (team != null) {

          TeamCardController teamCardController = uiService.loadFxml("theme/team_card.fxml");
          teamCardController.setPlayersInTeam(team, entry.getValue(),
              player -> RatingUtil.getLeaderboardRating(player, game.getLeaderboard()), null, RatingPrecision.ROUNDED);
          teamCardPanes.add(teamCardController.getRoot());
        }
      }

      JavaFxUtil.runLater(() -> {
        teamsPane.getChildren().setAll(teamCardPanes);
        teamsPane.setPrefColumns(Math.min(game.getTeams().size(), maxPrefColumns));
      });
    }
  }

  private void createModsList() {
    String stringSimMods;
    if (game != null) {
      stringSimMods = Joiner.on(System.getProperty("line.separator")).join(game.getSimMods().values());
    } else {
      stringSimMods = "";
    }
    JavaFxUtil.runLater(() -> modsLabel.setText(stringSimMods));
  }

  public void setShowMods(boolean showMods) {
    this.showMods = showMods;
  }

  public Node getRoot() {
    return gameTooltipRoot;
  }
}
