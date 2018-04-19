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
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class GameTooltipController implements Controller<Node> {

  private final UiService uiService;
  private final PlayerService playerService;

  public TitledPane modsPane;
  public Pane teamsPane;
  public Label modsLabel;
  public VBox gameTooltipRoot;

  @SuppressWarnings("FieldCanBeLocal")
  private InvalidationListener teamInvalidationListener;
  @SuppressWarnings("FieldCanBeLocal")
  private InvalidationListener simModsInvalidationListener;

  @Inject
  public GameTooltipController(UiService uiService, PlayerService playerService) {
    this.uiService = uiService;
    this.playerService = playerService;
  }

  public void initialize() {
    modsPane.managedProperty().bind(modsPane.visibleProperty());
  }

  public void setGame(Game game) {
    createTeams(game.getTeams());
    createModsList(game.getSimMods());
    teamInvalidationListener = change -> createTeams(game.getTeams());
    JavaFxUtil.addListener(game.getTeams(), new WeakInvalidationListener(teamInvalidationListener));

    simModsInvalidationListener = change -> createModsList(game.getSimMods());
    JavaFxUtil.addListener(game.getSimMods(), new WeakInvalidationListener(simModsInvalidationListener));
  }

  private void createTeams(ObservableMap<? extends String, ? extends List<String>> teamsList) {
    Platform.runLater(() -> {
      synchronized (teamsList) {
        teamsPane.getChildren().clear();
        TeamCardController.createAndAdd(teamsList, playerService, uiService, teamsPane);
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
