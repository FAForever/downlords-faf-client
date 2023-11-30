package com.faforever.client.game;

import com.faforever.client.domain.GameBean;
import com.faforever.client.filter.CustomGamesFilterController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.ToStringOnlyConverter;
import com.faforever.client.game.GamesTilesContainerController.TilesSortingOrder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.HostGameEvent;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.dialog.Dialog;
import com.faforever.client.ui.preferences.GameDirectoryRequiredHandler;
import com.faforever.client.util.PopupUtil;
import com.faforever.commons.lobby.GameStatus;
import com.faforever.commons.lobby.GameType;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class CustomGamesController extends NodeController<Node> {

  private final UiService uiService;
  private final GameService gameService;
  private final I18n i18n;
  private final Preferences preferences;
  private final GameDirectoryRequiredHandler gameDirectoryRequiredHandler;

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
  public Label filteredGamesCountLabel;

  private CustomGamesFilterController customGamesFilterController;
  private Popup gameFilterPopup;

  private FilteredList<GameBean> filteredGames;
  private final Predicate<GameBean> openGamesPredicate = game -> game.getStatus() == GameStatus.OPEN && game.getGameType() == GameType.CUSTOM;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(chooseSortingTypeChoiceBox, gameDetailPane);

    initializeFilterController();

    createGameButton.disableProperty().bind(gameService.gameRunningProperty().when(showing));

    chooseSortingTypeChoiceBox.getItems().addAll(TilesSortingOrder.values());
    chooseSortingTypeChoiceBox.setConverter(new ToStringOnlyConverter<>(tilesSortingOrder -> tilesSortingOrder == null ? "null" : i18n.get(tilesSortingOrder.getDisplayNameKey())));
    chooseSortingTypeChoiceBox.valueProperty().bindBidirectional(preferences.gameTileSortingOrderProperty());

    filteredGames = new FilteredList<>(gameService.getGames());
    filteredGames.predicateProperty().bind(customGamesFilterController.predicateProperty().when(showing));

    IntegerBinding filteredGameCount = Bindings.size(filteredGames);
    IntegerBinding gameCount = Bindings.size(gameService.getGames().filtered(openGamesPredicate));
    filteredGamesCountLabel.visibleProperty().bind(filteredGameCount.isNotEqualTo(gameCount).when(showing));

    filteredGamesCountLabel.textProperty().bind(Bindings.createStringBinding(() -> {
      int numGames = gameCount.intValue();
      return i18n.get("filteredOutItemsCount", numGames - filteredGameCount.intValue(), numGames);
    }, gameCount, filteredGameCount).when(showing));

    if (tilesButton.getId().equals(preferences.getGamesViewMode())) {
      viewToggleGroup.selectToggle(tilesButton);
      tilesButton.getOnAction().handle(null);
    } else {
      viewToggleGroup.selectToggle(tableButton);
      tableButton.getOnAction().handle(null);
    }

    preferences.gamesViewModeProperty()
        .bind(viewToggleGroup.selectedToggleProperty()
            .map(toggle -> (ToggleButton) toggle)
            .map(ToggleButton::getId)
            .when(showing));

    viewToggleGroup.selectedToggleProperty().when(showing).subscribe((oldValue, newValue) -> {
      if (newValue == null) {
        if (oldValue != null) {
          viewToggleGroup.selectToggle(oldValue);
        } else {
          viewToggleGroup.selectToggle(viewToggleGroup.getToggles().get(0));
        }
      }
    });

    gameDetailPane.visibleProperty().bind(toggleGameDetailPaneButton.selectedProperty().when(showing));

    toggleGameDetailPaneButton.selectedProperty().bindBidirectional(preferences.showGameDetailsSidePaneProperty());
  }

  private void initializeFilterController() {
    customGamesFilterController = uiService.loadFxml("theme/filter/filter.fxml", CustomGamesFilterController.class);
    customGamesFilterController.setDefaultPredicate(openGamesPredicate);
    customGamesFilterController.completeSetting();

    customGamesFilterController.filterActiveProperty().when(showing).subscribe(filterButton::setSelected);
    filterButton.selectedProperty()
                .when(showing)
                .subscribe(() -> filterButton.setSelected(customGamesFilterController.getFilterActive()));
  }

  @Override
  protected void onNavigate(NavigateEvent navigateEvent) {
    if (navigateEvent instanceof HostGameEvent hostGameEvent) {
      onCreateGame(hostGameEvent.getMapFolderName());
    }
  }

  public void onCreateGameButtonClicked() {
    onCreateGame(null);
  }

  private void onCreateGame(@Nullable String mapFolderName) {
    if (preferences.getForgedAlliance().getInstallationPath() == null) {
      CompletableFuture<Path> gameDirectoryFuture = new CompletableFuture<>();
      gameDirectoryRequiredHandler.onChooseGameDirectory(gameDirectoryFuture);
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

  @Override
  public Node getRoot() {
    return gamesRoot;
  }

  public void onTableButtonClicked() {
    GamesTableController gamesTableController = uiService.loadFxml("theme/play/games_table.fxml");
    gamesTableController.initializeGameTable(filteredGames);
    populateContainer(gamesTableController.getRoot());
    gameDetailController.gameProperty().bind(gamesTableController.selectedGameProperty());
    chooseSortingTypeChoiceBox.setVisible(false);
  }

  private void populateContainer(Node root) {
    gameViewContainer.getChildren().setAll(root);
    AnchorPane.setBottomAnchor(root, 0d);
    AnchorPane.setLeftAnchor(root, 0d);
    AnchorPane.setRightAnchor(root, 0d);
    AnchorPane.setTopAnchor(root, 0d);
  }

  public void onTilesButtonClicked() {
    GamesTilesContainerController gamesTilesContainerController = uiService.loadFxml(
        "theme/play/games_tiles_container.fxml");
    gamesTilesContainerController.createTiledFlowPane(filteredGames);
    gamesTilesContainerController.sortingOrderProperty().bind(chooseSortingTypeChoiceBox.valueProperty());
    populateContainer(gamesTilesContainerController.getRoot());
    gameDetailController.gameProperty().bind(gamesTilesContainerController.selectedGameProperty());
    chooseSortingTypeChoiceBox.setVisible(true);
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
