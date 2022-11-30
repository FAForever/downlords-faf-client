package com.faforever.client.game;

import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.faforever.commons.lobby.GameStatus;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class GamesTilesContainerController implements Controller<Node> {

  private final UiService uiService;
  private final PreferencesService preferencesService;

  public FlowPane tiledFlowPane;
  public ScrollPane tiledScrollPane;


  private Comparator<Node> appliedComparator;
  @VisibleForTesting
  Map<Integer, Node> gameIdToGameCard = new HashMap<>();
  private GameTooltipController gameTooltipController;
  private Tooltip tooltip;
  private ObservableList<GameBean> games;
  private ComboBox<TilesSortingOrder> sortingTypeChoiceBox;

  private final ObjectProperty<GameBean> selectedGame = new SimpleObjectProperty<>();

  private ChangeListener<? super TilesSortingOrder> sortingListener;
  private ListChangeListener<GameBean> gameListChangeListener;
  private final InvalidationListener tooltipShowingListener = observable -> {
    if (tooltip.isShowing()) {
      gameTooltipController.displayGame();
    } else {
      gameTooltipController.setGame(null);
    }
  };

  private void sortNodes() {
    ObservableList<Node> sortedChildren = tiledFlowPane.getChildren().sorted(appliedComparator);
    tiledFlowPane.getChildren().setAll(sortedChildren);
  }

  public void initialize() {
    gameTooltipController = uiService.loadFxml("theme/play/game_tooltip.fxml");
    tooltip = JavaFxUtil.createCustomTooltip(gameTooltipController.getRoot());
    JavaFxUtil.addListener(tooltip.showingProperty(), tooltipShowingListener);
    JavaFxUtil.fixScrollSpeed(tiledScrollPane);

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
        change.getRemoved().forEach(this::removeGameCard);
        change.getAddedSubList().forEach(GamesTilesContainerController.this::addGameCard);
        sortNodes();
      }
    });
  }

  ReadOnlyObjectProperty<GameBean> selectedGameProperty() {
    return this.selectedGame;
  }

  public void createTiledFlowPane(ObservableList<GameBean> games, ComboBox<TilesSortingOrder> sortingTypeChoiceBox) {
    this.games = games;
    this.sortingTypeChoiceBox = sortingTypeChoiceBox;
    initializeChoiceBox();
    JavaFxUtil.addListener(games, gameListChangeListener);
    games.forEach(this::addGameCard);
    sortNodes();
    selectFirstGame();
  }

  private void initializeChoiceBox() {
    sortingTypeChoiceBox.setVisible(true);
    JavaFxUtil.addListener(sortingTypeChoiceBox.getSelectionModel().selectedItemProperty(), sortingListener);
    sortingTypeChoiceBox.getSelectionModel().select(preferencesService.getPreferences().getGameTileSortingOrder());
  }

  private void selectFirstGame() {
    ObservableList<Node> cards = tiledFlowPane.getChildren();
    selectedGame.set(!cards.isEmpty() ? (GameBean) cards.get(0).getUserData() : null);
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

  private void removeGameCard(GameBean game) {
    Node card = gameIdToGameCard.remove(game.getId());
    if (card != null) {
      Tooltip.uninstall(card, tooltip);
      if (!tiledFlowPane.getChildren().remove(card)) {
        log.warn("Tried to remove game tile that did not exist in UI.");
      } else {
        clearSelectedGame(game);
      }
    } else {
      log.warn("Tried to remove game tile that did not exist.");
    }
  }

  private void clearSelectedGame(GameBean game) {
    if (game.equals(selectedGame.getValue()) && game.getStatus() != GameStatus.OPEN) {
      selectFirstGame();
    }
  }

  public void recreateTile(String mapName) {
    games.stream()
        .filter(game -> game.getMapFolderName().equals(mapName))
        .findFirst()
        .ifPresentOrElse(game -> JavaFxUtil.runLater(() -> {
          removeGameCard(game);
          addGameCard(game);
          sortNodes();
        }), () -> log.warn("No tile with {} map to recreate", mapName));
  }

  public Node getRoot() {
    return tiledScrollPane;
  }

  public void removeListeners() {
    JavaFxUtil.removeListener(games, gameListChangeListener);
    JavaFxUtil.removeListener(tooltip.showingProperty(), tooltipShowingListener);
    JavaFxUtil.removeListener(sortingTypeChoiceBox.getSelectionModel().selectedItemProperty(), sortingListener);
  }

  public enum TilesSortingOrder {
    PLAYER_DES(Comparator.comparingInt(o -> ((GameBean) o.getUserData()).getNumActivePlayers()), true, "tiles.comparator.playersDescending"),
    PLAYER_ASC(Comparator.comparingInt(o -> ((GameBean) o.getUserData()).getNumActivePlayers()), false, "tiles.comparator.playersAscending"),
    AVG_RATING_DES(Comparator.comparingDouble(o -> ((GameBean) o.getUserData()).getAverageRating()), true, "tiles.comparator.averageRatingDescending"),
    AVG_RATING_ASC(Comparator.comparingDouble(o -> ((GameBean) o.getUserData()).getAverageRating()), false, "tiles.comparator.averageRatingAscending"),
    NAME_DES(Comparator.comparing(o -> ((GameBean) o.getUserData()).getTitle()
        .toLowerCase(Locale.US)), true, "tiles.comparator.nameDescending"),
    NAME_ASC(Comparator.comparing(o -> ((GameBean) o.getUserData()).getTitle()
        .toLowerCase(Locale.US)), false, "tiles.comparator.nameAscending");

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
