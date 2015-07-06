package com.faforever.client.game;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
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

  @FXML
  public ScrollPane tiledScrollPane;

  @Autowired
  GamesController gamesController;

  @Autowired
  ApplicationContext applicationContext;

  private FilteredList<GameInfoBean> filteredItems;
  private Map<Integer, Node> uidToGameCard;

  //FIXME if password checkbox is clicked as a passworded game is added possible bug
  public void createTiledFlowPane(ObservableMap<Integer, GameInfoBean> gameInfoBeans) {
    ObservableList<GameInfoBean> tilePaneItems = FXCollections.observableArrayList();
    if (gamesController.isFirstGeneratedPane()) {
      filteredItems = new FilteredList<>(tilePaneItems);
      gamesController.setFilteredList(filteredItems);
    } else {
      filteredItems = gamesController.returnFilteredList();
    }
    tilePaneItems.addAll(gameInfoBeans.values());

    uidToGameCard = new HashMap<>();
    for (GameInfoBean gameInfoBean : filteredItems) {
      addGameCard(gameInfoBean);
    }
    //FIXME gameTiles passworded games don't get restored when unchecked
    //FIXME Exception in thread "JavaFX Application Thread" java.util.NoSuchElementException
    filteredItems.addListener((ListChangeListener<GameInfoBean>) change -> {
      while (change.next()) {
        if (change.wasRemoved()) {
          tilePaneItems.removeAll(change.getList());
          for (GameInfoBean gameInfoBean : change.getList()) {
            tiledFlowPane.getChildren().remove(uidToGameCard.get(gameInfoBean.getUid()));
          }
        } else {
          tilePaneItems.addAll(change.getList());
          //FIXME isEmpty is workaround for errors
          if(!change.getList().isEmpty()) {
            for (GameInfoBean gameInfoBean : change.getList()) {
              addGameCard(gameInfoBean);
            }
          }
        }
      }
    });

    gameInfoBeans.addListener((MapChangeListener<Integer, GameInfoBean>) change -> {
      if (change.wasRemoved()) {
        tilePaneItems.remove(change.getValueRemoved());
        tiledFlowPane.getChildren().remove(uidToGameCard.get(change.getValueRemoved().getUid()));
      } else {
        tilePaneItems.add(change.getValueAdded());
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

  public Node getRoot() {
    return tiledScrollPane;
  }

}
