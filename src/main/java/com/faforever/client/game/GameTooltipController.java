package com.faforever.client.game;


import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.google.common.base.Joiner;
import javafx.application.Platform;
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

import java.util.List;

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
  private ObservableMap<String, List<String>> lastTeams;
  private ObservableMap<String, String> lastSimMods;
  @SuppressWarnings("FieldCanBeLocal")
  private InvalidationListener teamChangedListener;
  @SuppressWarnings("FieldCanBeLocal")
  private InvalidationListener simModsChangedListener;
  private WeakInvalidationListener weakTeamChangeListener;
  private WeakInvalidationListener weakModChangeListener;
  private int maxPrefColumns;

  public void initialize() {
    modsPane.managedProperty().bind(modsPane.visibleProperty());
    maxPrefColumns = teamsPane.getPrefColumns();
  }

  public void setGame(Game game) {
    teamChangedListener = change -> createTeams(game.getTeams());
    simModsChangedListener = change -> createModsList(game.getSimMods());

    if (lastTeams != null && weakTeamChangeListener != null) {
      lastTeams.removeListener(weakTeamChangeListener);
    }

    if (lastSimMods != null && weakModChangeListener != null) {
      lastSimMods.removeListener(weakModChangeListener);
    }

    lastSimMods = game.getSimMods();
    lastTeams = game.getTeams();
    createTeams(game.getTeams());
    createModsList(game.getSimMods());
    weakTeamChangeListener = new WeakInvalidationListener(teamChangedListener);
    JavaFxUtil.addListener(game.getTeams(),weakTeamChangeListener);
    weakModChangeListener = new WeakInvalidationListener(simModsChangedListener);
    JavaFxUtil.addListener(game.getSimMods(),weakModChangeListener);
  }

  private void createTeams(ObservableMap<? extends String, ? extends List<String>> teamsList) {
    Platform.runLater(() -> {
      synchronized (teamsList) {
        teamsPane.getChildren().clear();
        TeamCardController.createAndAdd(teamsList, playerService, uiService, teamsPane);
        teamsPane.setPrefColumns(teamsList.size() < maxPrefColumns ? teamsList.size() : maxPrefColumns);
      }
    });
  }

  private void createModsList(ObservableMap<? extends String, ? extends String> simMods) {
    String stringSimMods = Joiner.on(System.getProperty("line.separator")).join(simMods.values());
    Platform.runLater(() -> {
      if (simMods.isEmpty()) {
        modsPane.setVisible(false);
        return;
      }

      modsLabel.setText(stringSimMods);
      modsPane.setVisible(true);
    });
  }

  public Node getRoot() {
    return gameTooltipRoot;
  }
}
