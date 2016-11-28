package com.faforever.client.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.theme.UiService;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class GamesTilesContainerController implements Controller<Node> {

  private final UiService uiService;
  private final ListChangeListener<Game> gameListChangeListener;
  public FlowPane tiledFlowPane;
  public ScrollPane tiledScrollPane;
  private Map<Integer, Node> uidToGameCard;
  private ObjectProperty<Game> selectedGame;

  @Inject
  public GamesTilesContainerController(UiService uiService) {
    this.uiService = uiService;
    selectedGame = new SimpleObjectProperty<>();

    gameListChangeListener = change -> Platform.runLater(() -> {
      synchronized (change.getList()) {
        while (change.next()) {
          change.getRemoved().forEach(gameInfoBean -> tiledFlowPane.getChildren().remove(uidToGameCard.remove(gameInfoBean.getId())));
          change.getAddedSubList().forEach(GamesTilesContainerController.this::addGameCard);
        }
      }
    });
  }

  ReadOnlyObjectProperty<Game> selectedGameProperty() {
    return this.selectedGame;
  }

  void createTiledFlowPane(ObservableList<Game> games) {
    uidToGameCard = new HashMap<>();
    games.forEach(this::addGameCard);

    games.addListener(new WeakListChangeListener<>(gameListChangeListener));
    selectFirstGame();
  }

  private void selectFirstGame() {
    ObservableList<Node> cards = tiledFlowPane.getChildren();
    if (!cards.isEmpty()) {
      selectedGame.set((Game) cards.get(0).getUserData());
    }
  }

  private void addGameCard(Game game) {
    GameCardController gameCardController = uiService.loadFxml("theme/play/game_card.fxml");
    gameCardController.setGame(game);
    gameCardController.setOnSelectedListener(selection -> selectedGame.set(selection));

    Node root = gameCardController.getRoot();
    root.setUserData(game);
    tiledFlowPane.getChildren().add(root);
    uidToGameCard.put(game.getId(), root);
  }

  public Node getRoot() {
    return tiledScrollPane;
  }

}
