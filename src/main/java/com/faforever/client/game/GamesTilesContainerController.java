package com.faforever.client.game;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
  private ObjectProperty<Game> selectedGame;

  public GamesTilesContainerController() {
    selectedGame = new SimpleObjectProperty<>();
  }

  public ReadOnlyObjectProperty<Game> selectedGameProperty() {
    return this.selectedGame;
  }

  public void createTiledFlowPane(ObservableList<Game> games) {
    uidToGameCard = new HashMap<>();
    games.forEach(this::addGameCard);

    games.addListener((ListChangeListener<Game>) change -> Platform.runLater(() -> {
      synchronized (change.getList()) {
        while (change.next()) {
          change.getRemoved().forEach(gameInfoBean -> tiledFlowPane.getChildren().remove(uidToGameCard.remove(gameInfoBean.getId())));
          change.getAddedSubList().forEach(GamesTilesContainerController.this::addGameCard);
        }
      }
    }));
    selectFirstGame();
  }

  private void selectFirstGame() {
    ObservableList<Node> cards = tiledFlowPane.getChildren();
    if (!cards.isEmpty()) {
      selectedGame.set((Game) cards.get(0).getUserData());
    }
  }

  private void addGameCard(Game game) {
    GameTileController gameTileController = applicationContext.getBean(GameTileController.class);
    gameTileController.setGame(game);
    gameTileController.setOnSelectedListener(gameInfoBean1 -> selectedGame.set(gameInfoBean1));

    Node root = gameTileController.getRoot();
    root.setUserData(game);
    tiledFlowPane.getChildren().add(root);
    uidToGameCard.put(game.getId(), root);
  }

  public Node getRoot() {
    return tiledScrollPane;
  }

}
