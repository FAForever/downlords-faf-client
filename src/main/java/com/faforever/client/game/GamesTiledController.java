package com.faforever.client.game;

import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public class GamesTiledController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @FXML
  public FlowPane tiledFlowPane;
  public ScrollPane tiledScrollPane;


  @Autowired
  ApplicationContext applicationContext;

  private Map<Integer, Node> uidToGameCard;

  public void createTiledFlowPane(ObservableMap<Integer, GameInfoBean> gameInfoBeans){
    uidToGameCard = new HashMap<>();
    for (GameInfoBean gameInfoBean : gameInfoBeans.values()) {
      addGameCard(gameInfoBean);
    }
    gameInfoBeans.addListener((MapChangeListener<Integer, GameInfoBean>) change -> {
      if (change.wasRemoved()) {
        tiledFlowPane.getChildren().remove(uidToGameCard.get(change.getValueRemoved().getUid()));
      } else {
        addGameCard(change.getValueAdded());
      }
    });
  }

  private void addGameCard(GameInfoBean gameInfoBean) {
    GameCardController gameCardController = applicationContext.getBean(GameCardController.class);
    gameCardController.setGameInfoBean(gameInfoBean);
    tiledFlowPane.getChildren().add(gameCardController.getRoot());
    uidToGameCard.put(gameInfoBean.getUid(), gameCardController.getRoot());
  }

  public ScrollPane getRoot(){
    return tiledScrollPane;
  }
}
