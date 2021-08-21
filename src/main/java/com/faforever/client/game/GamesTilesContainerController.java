package com.faforever.client.game;

import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class GamesTilesContainerController implements Controller<Node> {

  private final UiService uiService;
  private final ListChangeListener<GameBean> gameListChangeListener;
  private final PreferencesService preferencesService;
  public FlowPane tiledFlowPane;
  public ScrollPane tiledScrollPane;
  private final ChangeListener<? super TilesSortingOrder> sortingListener;
  private final ObjectProperty<GameBean> selectedGame;
  private Comparator<Node> appliedComparator;
  @VisibleForTesting
  Map<Integer, Node> gameIdToGameCard;
  private GameTooltipController gameTooltipController;
  private Tooltip tooltip;

  public GamesTilesContainerController(UiService uiService, PreferencesService preferencesService) {
    this.uiService = uiService;
    this.preferencesService = preferencesService;
    selectedGame = new SimpleObjectProperty<>();

    sortingListener = (observable, oldValue, newValue) -> {
      if (newValue == null) {
        return;
      }
      preferencesService.getPreferences().setGameTileSortingOrder(newValue);
      preferencesService.storeInBackground();
      appliedComparator = newValue.getComparator();
      sortNodes();
    };

    gameListChangeListener = change -> JavaFxUtil.runLater(() -> {
      while (change.next()) {
        change.getRemoved().forEach(game -> {
          Node card = gameIdToGameCard.remove(game.getId());
          if (card != null) {
            Tooltip.uninstall(card, tooltip);
            boolean remove = tiledFlowPane.getChildren().remove(card);
            if (!remove) {
              log.error("Tried to remove game tile that did not exist in UI.");
            }
          } else {
            log.error("Tried to remove game tile that did not exist.");
          }
        });
        change.getAddedSubList().forEach(GamesTilesContainerController.this::addGameCard);
        sortNodes();
      }
    });
  }

  private void sortNodes() {
    ObservableList<Node> sortedChildren = tiledFlowPane.getChildren().sorted(appliedComparator);
    tiledFlowPane.getChildren().setAll(sortedChildren);
  }

  public void initialize() {
    gameTooltipController = uiService.loadFxml("theme/play/game_tooltip.fxml");
    tooltip = JavaFxUtil.createCustomTooltip(gameTooltipController.getRoot());
    tooltip.showingProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        gameTooltipController.displayGame();
      } else {
        gameTooltipController.setGame(null);
      }
    });

    JavaFxUtil.fixScrollSpeed(tiledScrollPane);
  }

  ReadOnlyObjectProperty<GameBean> selectedGameProperty() {
    return this.selectedGame;
  }

  @VisibleForTesting
  void createTiledFlowPane(ObservableList<GameBean> games, ComboBox<TilesSortingOrder> choseSortingTypeChoiceBox) {
    JavaFxUtil.assertApplicationThread();
    initializeChoiceBox(choseSortingTypeChoiceBox);
    gameIdToGameCard = new HashMap<>();

    JavaFxUtil.addListener(games, new WeakListChangeListener<>(gameListChangeListener));
    games.forEach(this::addGameCard);

    selectFirstGame();
    sortNodes();
  }

  private void initializeChoiceBox(ComboBox<TilesSortingOrder> sortingTypeChoiceBox) {
    sortingTypeChoiceBox.setVisible(true);
    sortingTypeChoiceBox.getSelectionModel().selectedItemProperty().addListener(new WeakChangeListener<>(sortingListener));
    sortingTypeChoiceBox.getSelectionModel().select(preferencesService.getPreferences().getGameTileSortingOrder());
  }

  private void selectFirstGame() {
    ObservableList<Node> cards = tiledFlowPane.getChildren();
    if (!cards.isEmpty()) {
      selectedGame.set((GameBean) cards.get(0).getUserData());
    }
  }

  private void addGameCard(GameBean game) {
    GameTileController gameTileController = uiService.loadFxml("theme/play/game_card.fxml");
    gameTileController.setGame(game);
    gameTileController.setOnSelectedListener(selectedGame::set);

    Node root = gameTileController.getRoot();
    root.setUserData(game);
    tiledFlowPane.getChildren().add(root);
    gameIdToGameCard.put(game.getId(), root);
    
    root.setOnMouseEntered(event -> {
      gameTooltipController.setGame(game);
      if (tooltip.isShowing()) {
        gameTooltipController.displayGame();
      }
    });
    Tooltip.install(root, tooltip);
  }

  public Node getRoot() {
    return tiledScrollPane;
  }

  public enum TilesSortingOrder {
    PLAYER_DES(Comparator.comparingInt(o -> ((GameBean) o.getUserData()).getNumPlayers()), true, "tiles.comparator.playersDescending"),
    PLAYER_ASC(Comparator.comparingInt(o -> ((GameBean) o.getUserData()).getNumPlayers()), false, "tiles.comparator.playersAscending"),
    AVG_RATING_DES(Comparator.comparingDouble(o -> ((GameBean) o.getUserData()).getAverageRating()), true, "tiles.comparator.averageRatingDescending"),
    AVG_RATING_ASC(Comparator.comparingDouble(o -> ((GameBean) o.getUserData()).getAverageRating()), false, "tiles.comparator.averageRatingAscending"),
    NAME_DES(Comparator.comparing(o -> ((GameBean) o.getUserData()).getTitle().toLowerCase(Locale.US)), true, "tiles.comparator.nameDescending"),
    NAME_ASC(Comparator.comparing(o -> ((GameBean) o.getUserData()).getTitle().toLowerCase(Locale.US)), false, "tiles.comparator.nameAscending");

    @Getter
    private final Comparator<Node> comparator;
    @Getter
    private final String displayNameKey;

    TilesSortingOrder(Comparator<Node> comparator, boolean reversed, String displayNameKey) {
      this.displayNameKey = displayNameKey;
      this.comparator = reversed ? comparator.reversed() : comparator;
    }
  }
}
