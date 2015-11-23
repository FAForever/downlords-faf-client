package com.faforever.client.game;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

public class GamesTilesContainerController {

  @FXML
  public FlowPane tiledFlowPane;

  @FXML
  public ScrollPane tiledScrollPane;

  @Resource
  ApplicationContext applicationContext;

  private Map<Integer, Node> uidToGameCard;

  public void createTiledFlowPane(ObservableList<GameInfoBean> gameInfoBeans) {
    uidToGameCard = new HashMap<>();
    gameInfoBeans.forEach(this::addGameCard);

    gameInfoBeans.addListener((ListChangeListener<GameInfoBean>) change -> {
      Platform.runLater(() -> {
        while (change.next()) {
          change.getRemoved().forEach(gameInfoBean -> tiledFlowPane.getChildren().remove(uidToGameCard.get(gameInfoBean.getUid())));
          change.getAddedSubList().forEach(GamesTilesContainerController.this::addGameCard);
        }
      });
    });
  }

  private void addGameCard(GameInfoBean gameInfoBean) {
    GameTileController gameTileController = applicationContext.getBean(GameTileController.class);
    gameTileController.setGameInfoBean(gameInfoBean);

    Node root = gameTileController.getRoot();
    tiledFlowPane.getChildren().add(root);
    uidToGameCard.put(gameInfoBean.getUid(), root);
  }

  public Node getRoot() {
    return tiledScrollPane;
  }

}
