package com.faforever.client.game;


import com.google.common.base.Joiner;
import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

public class GameTooltipController {

  @FXML
  public TitledPane modsPane;
  @FXML
  public Pane teamsPane;
  @FXML
  public Label modsLabel;
  @FXML
  public VBox gameTooltipRoot;

  @Resource
  ApplicationContext applicationContext;

  @FXML
  void initialize() {
    modsPane.managedProperty().bind(modsPane.visibleProperty());
  }

  public void setGameInfoBean(GameInfoBean gameInfoBean) {
    createTeams(gameInfoBean.getTeams());
    createModsList(gameInfoBean.getSimMods());
    gameInfoBean.getTeams().addListener((MapChangeListener<String, List<String>>) change -> createTeams(change.getMap()));
    gameInfoBean.getSimMods().addListener((MapChangeListener<String, String>) change -> createModsList(change.getMap()));
  }

  private void createTeams(ObservableMap<? extends String, ? extends List<String>> teamsList) {
    Platform.runLater(() -> {
      synchronized (teamsList) {
        teamsPane.getChildren().clear();
        for (Map.Entry<? extends String, ? extends List<String>> entry : teamsList.entrySet()) {
          TeamCardController teamCardController = applicationContext.getBean(TeamCardController.class);
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
