package com.faforever.client.game;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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

public class GamesTilesContainerController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @FXML
  public FlowPane tiledFlowPane;

  @FXML
  public ScrollPane tiledScrollPane;

  @Autowired
  ApplicationContext applicationContext;

  private Map<Integer, Node> uidToGameCard;

  public void createTiledFlowPane(ObservableList<GameInfoBean> gameInfoBeans) {
    uidToGameCard = new HashMap<>();
    gameInfoBeans.forEach(this::addGameCard);

    gameInfoBeans.addListener((ListChangeListener<GameInfoBean>) change -> {
      while (change.next()) {
        change.getRemoved().forEach(gameInfoBean -> tiledFlowPane.getChildren().remove(uidToGameCard.get(gameInfoBean.getUid())));
        change.getAddedSubList().forEach(GamesTilesContainerController.this::addGameCard);
      }
    });
  }

  private void addGameCard(GameInfoBean gameInfoBean) {
    GameTileController gameTileController = applicationContext.getBean(GameTileController.class);
    gameTileController.setGameInfoBean(gameInfoBean);
    tiledFlowPane.getChildren().add(gameTileController.getRoot());
    uidToGameCard.put(gameInfoBean.getUid(), gameTileController.getRoot());
  }

  public Node getRoot() {
    return tiledScrollPane;
  }

}
