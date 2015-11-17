package com.faforever.client.game;


import com.google.common.base.Joiner;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

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
  public VBox teamListRoot;

  @Autowired
  ApplicationContext applicationContext;

  public void setGameInfoBean(GameInfoBean gameInfoBean) {
    createTeam(gameInfoBean.getTeams());
    createModsList(gameInfoBean.getSimMods());
    gameInfoBean.getTeams().addListener((MapChangeListener<String, List<String>>) change -> createTeam(change.getMap()));
    gameInfoBean.getSimMods().addListener((MapChangeListener<String, String>) change -> createModsList(change.getMap()));
  }

  private void createTeam(ObservableMap<? extends String, ? extends List<String>> teamsList) {
    teamsPane.getChildren().clear();
    for (Map.Entry<? extends String, ? extends List<String>> entry : teamsList.entrySet()) {
      TeamCardController teamCardController = applicationContext.getBean(TeamCardController.class);
      boolean teamCardSuccess = teamCardController.setTeam(entry.getValue(), entry.getKey());
      if (teamCardSuccess) {
        teamsPane.getChildren().add(teamCardController.getRoot());
      }
    }
  }

  private void createModsList(ObservableMap<? extends String, ? extends String> simMods) {
    if (simMods.isEmpty()) {
      modsPane.setVisible(false);
      return;
    }

    String stringSimMods = Joiner.on(System.getProperty("line.separator")).join(simMods.values());
    modsLabel.setText(stringSimMods);
    modsPane.setVisible(true);
  }

  public Node getRoot() {
    return teamListRoot;
  }
}
