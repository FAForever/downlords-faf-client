package com.faforever.client.game;

import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.faforever.commons.lobby.GameStatus;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.SortedList;
import javafx.scene.Node;
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
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@RequiredArgsConstructor
public class GamesTilesContainerController extends NodeController<Node> {

  private final UiService uiService;
  private final PlayerService playerService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final Comparator<Node> averageRatingComparator = Comparator.comparingDouble(this::getAverageRatingForGame);
  private final Comparator<Node> titleComparator = Comparator.comparing(o -> ((GameBean) o.getUserData()).getTitle()
                                                                                                         .toLowerCase(
                                                                                                             Locale.US));
  private final Comparator<Node> playerCountComparator = Comparator.comparingInt(
      o -> ((GameBean) o.getUserData()).getNumActivePlayers());

  public FlowPane tiledFlowPane;
  public ScrollPane tiledScrollPane;
  public GameTooltipController gameTooltipController;

  private Tooltip tooltip;

  private final ObservableMap<GameBean, Node> gameToGameCard = FXCollections.synchronizedObservableMap(
      FXCollections.observableHashMap());
  private final SortedList<Node> gameCards = new SortedList<>(
      JavaFxUtil.attachListToMap(FXCollections.observableArrayList(), gameToGameCard));
  private final ObjectProperty<TilesSortingOrder> sortingOrder = new SimpleObjectProperty<>();
  private final ReadOnlyObjectWrapper<GameBean> selectedGame = new ReadOnlyObjectWrapper<>();

  private final ListChangeListener<GameBean> gameListChangeListener = this::onGameListChange;
  private ObservableList<GameBean> games;

  private double getAverageRatingForGame(Node tile) {
    return this.playerService.getAverageRatingForGame((GameBean) tile.getUserData());
  }

  @Override
  protected void onInitialize() {
    tooltip = JavaFxUtil.createCustomTooltip(gameTooltipController.getRoot());
    JavaFxUtil.fixScrollSpeed(tiledScrollPane);

    gameCards.comparatorProperty().bind(sortingOrder.map(order -> switch (order) {
      case PLAYER_DES -> playerCountComparator.reversed();
      case PLAYER_ASC -> playerCountComparator;
      case AVG_RATING_DES -> averageRatingComparator.reversed();
      case AVG_RATING_ASC -> averageRatingComparator;
      case NAME_DES -> titleComparator.reversed();
      case NAME_ASC -> titleComparator;
    }).when(showing));

    Bindings.bindContent(tiledFlowPane.getChildren(), gameCards);
  }

  @Override
  public void onHide() {
    if (games != null) {
      games.removeListener(gameListChangeListener);
    }
  }

  private void onGameListChange(Change<? extends GameBean> change) {
    while (change.next()) {
      if (change.wasRemoved()) {
        change.getRemoved().forEach(this::removeGameCard);
      }

      if (change.wasAdded()) {
        change.getAddedSubList().forEach(this::addGameCard);
      }
    }
  }

  public ReadOnlyObjectProperty<GameBean> selectedGameProperty() {
    return selectedGame.getReadOnlyProperty();
  }

  public void createTiledFlowPane(ObservableList<GameBean> games) {
    if (this.games != null) {
      this.games.removeListener(gameListChangeListener);
    }

    this.games = games;
    this.games.addListener(gameListChangeListener);
    games.forEach(this::addGameCard);
    selectFirstGame();
  }

  private void selectFirstGame() {
    selectedGame.set(!gameCards.isEmpty() ? (GameBean) gameCards.get(0).getUserData() : null);
  }

  private void addGameCard(GameBean game) {
    if (gameToGameCard.containsKey(game)) {
      return;
    }

    CompletableFuture.supplyAsync(() -> createGameCard(game))
                     .thenAcceptAsync(root -> {
                       gameToGameCard.put(game, root);
                       if (selectedGame.get() == null) {
                         selectedGame.set(game);
                       }
                     }, fxApplicationThreadExecutor)
                     .exceptionally(throwable -> {
                       log.error("Unable to create and add game card", throwable);
                       return null;
                     });

  }

  private Node createGameCard(GameBean game) {
    GameTileController gameTileController = uiService.loadFxml("theme/play/game_card.fxml");
    gameTileController.setGame(game);
    gameTileController.setOnSelectedListener(selectedGame::set);

    Node root = gameTileController.getRoot();
    root.setUserData(game);

    root.setOnMouseEntered(event -> gameTooltipController.setGame(game));
    root.setOnMouseExited(event -> {
      if (Objects.equals(game, gameTooltipController.getGame())) {
        gameTooltipController.setGame(null);
      }
    });
    Tooltip.install(root, tooltip);
    return root;
  }

  private void removeGameCard(GameBean game) {
    fxApplicationThreadExecutor.execute(() -> {
      Node card = gameToGameCard.remove(game);
      if (card != null) {
        Tooltip.uninstall(card, tooltip);
        clearSelectedGame(game);
      } else {
        log.warn("Tried to remove game tile that did not exist.");
      }
    });
  }

  private void clearSelectedGame(GameBean game) {
    if (game.equals(selectedGame.getValue()) && game.getStatus() != GameStatus.OPEN) {
      selectFirstGame();
    }
  }

  @Override
  public Node getRoot() {
    return tiledScrollPane;
  }

  public TilesSortingOrder getSortingOrder() {
    return sortingOrder.get();
  }

  public ObjectProperty<TilesSortingOrder> sortingOrderProperty() {
    return sortingOrder;
  }

  public void setSortingOrder(TilesSortingOrder sortingOrder) {
    this.sortingOrder.set(sortingOrder);
  }

  @Getter
  @RequiredArgsConstructor
  public enum TilesSortingOrder {
    PLAYER_DES("tiles.comparator.playersDescending"),
    PLAYER_ASC("tiles.comparator.playersAscending"),
    AVG_RATING_DES("tiles.comparator.averageRatingDescending"),
    AVG_RATING_ASC("tiles.comparator.averageRatingAscending"),
    NAME_DES("tiles.comparator.nameDescending"),
    NAME_ASC("tiles.comparator.nameAscending");

    private final String displayNameKey;
  }
}
