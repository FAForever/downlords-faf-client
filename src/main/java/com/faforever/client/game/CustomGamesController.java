package com.faforever.client.game;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.GamesTilesContainerController.TilesSortingOrder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.HostGameEvent;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXDialog.DialogTransition;
import com.jfoenix.controls.JFXDialogLayout;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class CustomGamesController extends AbstractViewController<Node> {

  private static final Collection<String> HIDDEN_FEATURED_MODS = Arrays.asList(
      KnownFeaturedMod.COOP.getTechnicalName(),
      KnownFeaturedMod.LADDER_1V1.getTechnicalName(),
      KnownFeaturedMod.GALACTIC_WAR.getTechnicalName(),
      KnownFeaturedMod.MATCHMAKER.getTechnicalName()
  );

  private static final Predicate<Game> OPEN_CUSTOM_GAMES_PREDICATE = gameInfoBean ->
      gameInfoBean.getStatus() == GameStatus.OPEN
          && !HIDDEN_FEATURED_MODS.contains(gameInfoBean.getFeaturedMod());
  private final UiService uiService;
  private final GameService gameService;
  private final PreferencesService preferencesService;
  private final EventBus eventBus;
  private final I18n i18n;

  @SuppressWarnings("WeakerAccess")
  public GameDetailController gameDetailController;
  private GamesTableController gamesTableController;

  public ToggleButton tableButton;
  public ToggleButton tilesButton;
  public ToggleGroup viewToggleGroup;
  public Button createGameButton;
  public Pane gameViewContainer;
  public StackPane gamesRoot;
  public ScrollPane gameDetailPane;
  public ComboBox<TilesSortingOrder> chooseSortingTypeChoiceBox;

  @VisibleForTesting
  FilteredList<Game> filteredItems;

  public CheckBox showModdedGamesCheckBox;
  public CheckBox showPasswordProtectedGamesCheckBox;
  private final ChangeListener<Boolean> filterConditionsChangedListener = (observable, oldValue, newValue) -> updateFilteredItems();
  private GamesTilesContainerController gamesTilesContainerController;

  @Inject
  public CustomGamesController(UiService uiService, GameService gameService, PreferencesService preferencesService,
                               EventBus eventBus, I18n i18n) {
    this.uiService = uiService;
    this.gameService = gameService;
    this.preferencesService = preferencesService;
    this.eventBus = eventBus;
    this.i18n = i18n;
  }

  public void initialize() {
    JavaFxUtil.bind(createGameButton.disableProperty(), gameService.gameRunningProperty());
    getRoot().sceneProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        createGameButton.disableProperty().unbind();
      }
    });

    chooseSortingTypeChoiceBox.setVisible(false);
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

    ObservableList<Game> games = gameService.getGames();

    filteredItems = new FilteredList<>(games);
    JavaFxUtil.bindBidirectional(showModdedGamesCheckBox.selectedProperty(), preferencesService.getPreferences().showModdedGamesProperty());
    JavaFxUtil.bindBidirectional(showPasswordProtectedGamesCheckBox.selectedProperty(), preferencesService.getPreferences().showPasswordProtectedGamesProperty());

    updateFilteredItems();
    JavaFxUtil.addListener(preferencesService.getPreferences().showModdedGamesProperty(), new WeakChangeListener<>(filterConditionsChangedListener));
    JavaFxUtil.addListener(preferencesService.getPreferences().showPasswordProtectedGamesProperty(), new WeakChangeListener<>(filterConditionsChangedListener));

    if (tilesButton.getId().equals(preferencesService.getPreferences().getGamesViewMode())) {
      viewToggleGroup.selectToggle(tilesButton);
      tilesButton.getOnAction().handle(null);
    } else {
      viewToggleGroup.selectToggle(tableButton);
      tableButton.getOnAction().handle(null);
      gamesTableController.setModsColumnVisibility(showModdedGamesCheckBox.selectedProperty().getValue());
      gamesTableController.setPasswordProtectionColumnVisibility(showPasswordProtectedGamesCheckBox.selectedProperty().getValue());
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
      preferencesService.getPreferences().setGamesViewMode(((ToggleButton) newValue).getId());
      preferencesService.storeInBackground();
    });

    setSelectedGame(null);
    eventBus.register(this);
  }

  @Override
  public void onDisplay(NavigateEvent navigateEvent) {
    if (navigateEvent instanceof HostGameEvent) {
      onCreateGame(((HostGameEvent) navigateEvent).getMapFolderName());
    }
  }

  private void updateFilteredItems() {
    preferencesService.storeInBackground();

    boolean showPasswordProtectedGames = showPasswordProtectedGamesCheckBox.isSelected();
    boolean showModdedGames = showModdedGamesCheckBox.isSelected();

    filteredItems.setPredicate(OPEN_CUSTOM_GAMES_PREDICATE.and(gameInfoBean ->
        (showPasswordProtectedGames || !gameInfoBean.getPasswordProtected())
            && (showModdedGames || gameInfoBean.getSimMods().isEmpty())));
    if (tableButton.isSelected()) {
      gamesTableController.setModsColumnVisibility(showModdedGamesCheckBox.selectedProperty().getValue());
      gamesTableController.setPasswordProtectionColumnVisibility(showPasswordProtectedGamesCheckBox.selectedProperty().getValue());
    }
  }

  public void onCreateGameButtonClicked() {
    onCreateGame(null);
  }

  private void onCreateGame(@Nullable String mapFolderName) {
    if (preferencesService.getPreferences().getForgedAlliance().getPath() == null) {
      CompletableFuture<Path> gameDirectoryFuture = new CompletableFuture<>();
      eventBus.post(new GameDirectoryChooseEvent(gameDirectoryFuture));
      gameDirectoryFuture.thenAccept(path -> Optional.ofNullable(path).ifPresent(path1 -> onCreateGame(null)));
      return;
    }

    CreateGameController createGameController = uiService.loadFxml("theme/play/create_game.fxml");

    if (mapFolderName != null && !createGameController.selectMap(mapFolderName)) {
      log.warn("Map with folder name '{}' could not be found in map list", mapFolderName);
    }

    Pane root = createGameController.getRoot();
    JFXDialogLayout dialogLayout = new JFXDialogLayout();
    dialogLayout.setHeading(new Label(i18n.get("games.create")));
    dialogLayout.setBody(root);

    JFXDialog dialog = new JFXDialog();
    dialog.setContent(dialogLayout);
    dialog.setTransitionType(DialogTransition.TOP);

    createGameController.setOnClosedButtonClickedListener(dialog::close);
    gamesRoot.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.ESCAPE) {
        dialog.close();
      }
    });

    dialog.show(gamesRoot);

    root.requestFocus();
  }

  public Node getRoot() {
    return gamesRoot;
  }

  public void onTableButtonClicked() {
    gamesTableController = uiService.loadFxml("theme/play/games_table.fxml");
    gamesTableController.selectedGameProperty().addListener((observable, oldValue, newValue) -> setSelectedGame(newValue));
    Platform.runLater(() -> {
      gamesTableController.initializeGameTable(filteredItems);

      Node root = gamesTableController.getRoot();
      populateContainer(root);
    });
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
    gamesTilesContainerController = uiService.loadFxml("theme/play/games_tiles_container.fxml");
    gamesTilesContainerController.selectedGameProperty().addListener((observable, oldValue, newValue) -> setSelectedGame(newValue));
    chooseSortingTypeChoiceBox.getItems().clear();

    Platform.runLater(() -> {
      Node root = gamesTilesContainerController.getRoot();
      populateContainer(root);
      gamesTilesContainerController.createTiledFlowPane(filteredItems, chooseSortingTypeChoiceBox);
    });
  }

  @VisibleForTesting
  void setSelectedGame(Game game) {
    gameDetailController.setGame(game);
    if (game == null) {
      gameDetailPane.setVisible(false);
      return;
    }

    gameDetailPane.setVisible(true);
  }

  @VisibleForTesting
  void setFilteredList(ObservableList<Game> games) {
    filteredItems = new FilteredList<>(games, s -> true);
  }
}
