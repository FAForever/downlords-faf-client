package com.faforever.client.game;


import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.google.common.base.Joiner;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@RequiredArgsConstructor
public class GameTooltipController implements Controller<Node> {

  private final UiService uiService;
  private final PlayerService playerService;

  public TitledPane modsPane;
  public TilePane teamsPane;
  public Label modsLabel;
  public VBox gameTooltipRoot;
  private InvalidationListener teamInvalidationListener;
  private InvalidationListener simModsInvalidationListener;
  private int maxPrefColumns;
  private Game game;
  private boolean showMods;

  public void initialize() {
    modsPane.managedProperty().bind(modsPane.visibleProperty());
    maxPrefColumns = teamsPane.getPrefColumns();
    showMods = true;
  }

  public void setGame(Game game) {
    this.game = game;
  }

  public void displayGame() {
    if (game == null) {
      return;
    }
    resetListeners();
    WeakInvalidationListener weakTeamInvalidationListener = new WeakInvalidationListener(teamInvalidationListener);
    JavaFxUtil.addAndTriggerListener(game.getTeams(), weakTeamInvalidationListener);
    if (showMods) {
      WeakInvalidationListener weakModInvalidationListener = new WeakInvalidationListener(simModsInvalidationListener);
      JavaFxUtil.addAndTriggerListener(game.getSimMods(), weakModInvalidationListener);
    } else {
      JavaFxUtil.runLater(() -> modsPane.setVisible(false));
    }
  }

  private void resetListeners() {
    teamInvalidationListener = change -> createTeams();
    simModsInvalidationListener = change -> createModsList(game.getSimMods());
  }

  private void createTeams() {
    TeamCardController.createAndAdd(game, playerService, uiService, teamsPane);
    JavaFxUtil.runLater(() -> teamsPane.setPrefColumns(Math.min(game.getTeams().size(), maxPrefColumns)));
  }

  private void createModsList(ObservableMap<? extends String, ? extends String> simMods) {
    String stringSimMods;
    synchronized (simMods) {
      stringSimMods = Joiner.on(System.getProperty("line.separator")).join(simMods.values());
    }
    JavaFxUtil.runLater(() -> {
      if (simMods.isEmpty()) {
        modsPane.setVisible(false);
        return;
      }

      modsLabel.setText(stringSimMods);
      modsPane.setVisible(true);
    });
  }

  public void setShowMods(boolean showMods) {
    this.showMods = showMods;
  }

  public Node getRoot() {
    return gameTooltipRoot;
  }
}
