package com.faforever.client.game;

import com.faforever.client.domain.GameBean;
import com.faforever.client.filter.CustomGamesFilterController;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.GamesTilesContainerController.TilesSortingOrder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.HostGameEvent;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.dialog.Dialog;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.faforever.client.util.PopupUtil;
import com.faforever.commons.lobby.GameStatus;
import com.faforever.commons.lobby.GameType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class CustomGamesController extends AbstractViewController<Node> {

  private final UiService uiService;
  private final GameService gameService;
  private final PreferencesService preferencesService;
  private final EventBus eventBus;
  private final I18n i18n;

  @SuppressWarnings("WeakerAccess")
  public GameDetailController gameDetailController;

  public ToggleButton tableButton;
  public ToggleButton tilesButton;
  public ToggleButton toggleGameDetailPaneButton;
  public ToggleGroup viewToggleGroup;
  public Button createGameButton;
  public ToggleButton filterButton;
  public Pane gameViewContainer;
  public StackPane gamesRoot;
  public ScrollPane gameDetailPane;
  public ComboBox<TilesSortingOrder> chooseSortingTypeChoiceBox;

  private FilteredList<GameBean> filteredItems;
  private Preferences preferences;
  private CustomGamesFilterController customGamesFilterController;
  private Popup gameFilterPopup;
  private GamesTableController gamesTableController;
  private GamesTilesContainerController gamesTilesContainerController;

  private final ChangeListener<GameBean> gameChangeListener = (observable, oldValue, newValue) -> setSelectedGame(newValue);

  public void initialize() {
    preferences = preferencesService.getPreferences();

    initializeFilterController();

    JavaFxUtil.bind(createGameButton.disableProperty(), gameService.gameRunningProperty());
    getRoot().sceneProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        createGameButton.disableProperty().unbind();
      }
    });

    chooseSortingTypeChoiceBox.setVisible(false);
    chooseSortingTypeChoiceBox.getItems().addAll(TilesSortingOrder.values());
    chooseSortingTypeChoiceBox.setConverter(new StringConverter<>() {
      @Override
      public String toString(TilesSortingOrder tilesSortingOrder) {
        return tilesSortingOrder == null ? "null" : i18n.get(tilesSortingOrder.getDisplayNameKey());
      }

      @Override
      public TilesSortingOrder fromString(String string) {
        throw new UnsupportedOperationException("Not supported");
      }
    });

    ObservableList<GameBean> games = gameService.getGames();
    filteredItems = new FilteredList<>(games);
    JavaFxUtil.addAndTriggerListener(customGamesFilterController.getPredicateProperty(),
        (observable, oldValue, newValue) -> filteredItems.setPredicate(newValue));

    if (tilesButton.getId().equals(preferences.getGamesViewMode())) {
      viewToggleGroup.selectToggle(tilesButton);
      tilesButton.getOnAction().handle(null);
    } else {
      viewToggleGroup.selectToggle(tableButton);
      tableButton.getOnAction().handle(null);
    }
    viewToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        if (oldValue != null) {
          viewToggleGroup.selectToggle(oldValue);
        } else {
          viewToggleGroup.selectToggle(viewToggleGroup.getToggles().get(0));
        }
        return;
      }
      preferences.setGamesViewMode(((ToggleButton) newValue).getId());
      preferencesService.storeInBackground();
    });

    JavaFxUtil.bind(gameDetailPane.visibleProperty(), toggleGameDetailPaneButton.selectedProperty());
    JavaFxUtil.bind(gameDetailPane.managedProperty(), gameDetailPane.visibleProperty());

    setSelectedGame(null);

    toggleGameDetailPaneButton.selectedProperty().addListener(observable -> {
      preferences.setShowGameDetailsSidePane(toggleGameDetailPaneButton.isSelected());
      preferencesService.storeInBackground();
    });
    toggleGameDetailPaneButton.setSelected(preferences.isShowGameDetailsSidePane());

    eventBus.register(this);
  }

  private void initializeFilterController() {
    customGamesFilterController = uiService.loadFxml("theme/filter/filter.fxml", CustomGamesFilterController.class);
    customGamesFilterController.setDefaultPredicate(game -> game.getStatus() == GameStatus.OPEN && game.getGameType() == GameType.CUSTOM);
    JavaFxUtil.bindBidirectional(customGamesFilterController.getPrivateGamesProperty(), preferences.hidePrivateGamesProperty());
    JavaFxUtil.bindBidirectional(customGamesFilterController.getSimsModsProperty(), preferences.hideModdedGamesProperty());
    JavaFxUtil.bindBidirectional(customGamesFilterController.getMapFolderNameBlackListProperty(), preferences.getFilters().mapNameBlacklistProperty());
    JavaFxUtil.addListener(preferences.hideModdedGamesProperty(), observable -> preferencesService.storeInBackground());
    JavaFxUtil.addListener(preferences.hidePrivateGamesProperty(), observable -> preferencesService.storeInBackground());
    JavaFxUtil.addListener(preferences.getFilters().mapNameBlacklistProperty(), (InvalidationListener) observable -> preferencesService.storeInBackground());

    customGamesFilterController.completeSetting();

    JavaFxUtil.addAndTriggerListener(customGamesFilterController.getFilterStateProperty(), (observable, oldValue, newValue) -> filterButton.setSelected(newValue));
    JavaFxUtil.addAndTriggerListener(filterButton.selectedProperty(), observable -> filterButton.setSelected(customGamesFilterController.getFilterState()));
  }

  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    if (navigateEvent instanceof HostGameEvent) {
      onCreateGame(((HostGameEvent) navigateEvent).getMapFolderName());
    }
  }

  public void onCreateGameButtonClicked() {
    onCreateGame(null);
  }

  private void onCreateGame(@Nullable String mapFolderName) {
    if (preferences.getForgedAlliance().getInstallationPath() == null) {
      CompletableFuture<Path> gameDirectoryFuture = new CompletableFuture<>();
      eventBus.post(new GameDirectoryChooseEvent(gameDirectoryFuture));
      gameDirectoryFuture.thenAccept(path -> Optional.ofNullable(path).ifPresent(path1 -> onCreateGame(null)));
      return;
    }

    CreateGameController createGameController = uiService.loadFxml("theme/play/create_game.fxml");
    createGameController.setGamesRoot(gamesRoot);

    if (mapFolderName != null && !createGameController.selectMap(mapFolderName)) {
      log.warn("Map with folder name '{}' could not be found in map list", mapFolderName);
    }

    Pane root = createGameController.getRoot();
    Dialog dialog = uiService.showInDialog(gamesRoot, root, i18n.get("games.create"));
    createGameController.setOnCloseButtonClickedListener(dialog::close);

    root.requestFocus();
  }

  public Node getRoot() {
    return gamesRoot;
  }

  public void onTableButtonClicked() {
    disposeGamesContainer();

    gamesTableController = uiService.loadFxml("theme/play/games_table.fxml");
    JavaFxUtil.addListener(gamesTableController.selectedGameProperty(), gameChangeListener);
    gamesTableController.initializeGameTable(filteredItems);
    populateContainer(gamesTableController.getRoot());
  }

  private void populateContainer(Node root) {
    chooseSortingTypeChoiceBox.setVisible(false);
    gameViewContainer.getChildren().setAll(root);
    AnchorPane.setBottomAnchor(root, 0d);
    AnchorPane.setLeftAnchor(root, 0d);
    AnchorPane.setRightAnchor(root, 0d);
    AnchorPane.setTopAnchor(root, 0d);
  }

  public void onTilesButtonClicked() {
    disposeGamesContainer();

    gamesTilesContainerController = uiService.loadFxml("theme/play/games_tiles_container.fxml");
    JavaFxUtil.addListener(gamesTilesContainerController.selectedGameProperty(), gameChangeListener);
    populateContainer(gamesTilesContainerController.getRoot());
    gamesTilesContainerController.createTiledFlowPane(filteredItems, chooseSortingTypeChoiceBox);
  }

  private void disposeGamesContainer() {
    if (gamesTilesContainerController != null) {
      JavaFxUtil.removeListener(gamesTilesContainerController.selectedGameProperty(), gameChangeListener);
      gamesTilesContainerController.removeListeners();
    }

    if (gamesTableController != null) {
      JavaFxUtil.removeListener(gamesTableController.selectedGameProperty(), gameChangeListener);
      gamesTableController.removeListeners();
    }
  }

  @VisibleForTesting
  void setSelectedGame(GameBean game) {
    gameDetailController.getRoot().setVisible(true);
    gameDetailController.setGame(game);
  }

  public void onFilterButtonClicked() {
    if (gameFilterPopup == null) {
      gameFilterPopup = PopupUtil.createPopup(AnchorLocation.CONTENT_TOP_LEFT, customGamesFilterController.getRoot());
    }

    if (gameFilterPopup.isShowing()) {
      gameFilterPopup.hide();
    } else {
      Bounds screenBounds = filterButton.localToScreen(filterButton.getBoundsInLocal());
      gameFilterPopup.show(filterButton.getScene().getWindow(), screenBounds.getMinX(), screenBounds.getMaxY() + 10);
    }
  }
}
