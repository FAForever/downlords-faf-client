package com.faforever.client.game;


import com.faforever.client.fx.Controller;
import com.faforever.client.theme.UiService;
import com.google.common.base.Joiner;
import javafx.application.Platform;
import javafx.collections.MapChangeListener;
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
import java.util.Map;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class GameTooltipController implements Controller<Node> {

  private final UiService uiService;
  public TitledPane modsPane;
  public Pane teamsPane;
  public Label modsLabel;
  public VBox gameTooltipRoot;

  @Inject
  public GameTooltipController(UiService uiService) {
    this.uiService = uiService;
  }

  public void initialize() {
    modsPane.managedProperty().bind(modsPane.visibleProperty());
  }

  public void setGameInfoBean(Game game) {
    createTeams(game.getTeams());
    createModsList(game.getSimMods());
    game.getTeams().addListener((MapChangeListener<String, List<String>>) change -> createTeams(change.getMap()));
    game.getSimMods().addListener((MapChangeListener<String, String>) change -> createModsList(change.getMap()));
  }

  private void createTeams(ObservableMap<? extends String, ? extends List<String>> teamsList) {
    Platform.runLater(() -> {
      synchronized (teamsList) {
        teamsPane.getChildren().clear();
        for (Map.Entry<? extends String, ? extends List<String>> entry : teamsList.entrySet()) {
          TeamCardController teamCardController = uiService.loadFxml("theme/team_card.fxml");
          teamCardController.setPlayersInTeam(entry.getKey(), entry.getValue());
          teamsPane.getChildren().add(teamCardController.getRoot());
        }
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
