package com.faforever.client.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.game.GamesTilesContainerController.TilesSortingOrder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.HostMapInCustomGameEvent;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
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
public class CustomGamesController implements Controller<Node> {

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

  public ToggleButton tableButton;
  public ToggleButton tilesButton;
  public ToggleGroup viewToggleGroup;
  public Button createGameButton;
  public Pane gameViewContainer;
  public Pane gamesRoot;
  public ScrollPane gameDetailPane;
  public GameDetailController gameDetailController;
  public ChoiceBox<TilesSortingOrder> chooseSortingTypeChoiceBox;

  private FilteredList<Game> filteredItems;

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
    chooseSortingTypeChoiceBox.setVisible(false);
    chooseSortingTypeChoiceBox.setConverter(new StringConverter<TilesSortingOrder>() {
      @Override
      public String toString(TilesSortingOrder tilesSortingOrder) {
        return i18n.get(tilesSortingOrder.getDisplayNameKey());
      }

      @Override
      public TilesSortingOrder fromString(String string) {
        throw new UnsupportedOperationException("Not supported");
      }
    });

    ObservableList<Game> games = gameService.getGames();

    filteredItems = new FilteredList<>(games);
    filteredItems.setPredicate(OPEN_CUSTOM_GAMES_PREDICATE);

    if (tilesButton.getId().equals(preferencesService.getPreferences().getGamesViewMode())) {
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
      preferencesService.getPreferences().setGamesViewMode(((ToggleButton) newValue).getId());
      preferencesService.storeInBackground();
    });

    setSelectedGame(null);
    eventBus.register(this);
  }

  public void onShowPrivateGames(ActionEvent actionEvent) {
    CheckBox checkBox = (CheckBox) actionEvent.getSource();
    boolean selected = checkBox.isSelected();
    if (selected) {
      filteredItems.setPredicate(OPEN_CUSTOM_GAMES_PREDICATE);
    } else {
      filteredItems.setPredicate(OPEN_CUSTOM_GAMES_PREDICATE.and(gameInfoBean -> !gameInfoBean.getPasswordProtected()));
    }
  }

  public void onCreateGameButtonClicked() {
    if (preferencesService.getPreferences().getForgedAlliance().getPath() == null) {
      CompletableFuture<Path> gameDirectoryFuture = new CompletableFuture<>();
      eventBus.post(new GameDirectoryChooseEvent(gameDirectoryFuture));
      gameDirectoryFuture.thenAccept(path -> Optional.ofNullable(path).ifPresent(path1 -> onCreateGameButtonClicked()));
      return;
    }

    CreateGameController createGameController = uiService.loadFxml("theme/play/create_game.fxml");

    Pane createGameRoot = createGameController.getRoot();
    gamesRoot.getChildren().add(createGameRoot);
    AnchorPane.setTopAnchor(createGameRoot, 0d);
    AnchorPane.setRightAnchor(createGameRoot, 0d);
    AnchorPane.setBottomAnchor(createGameRoot, 0d);
    AnchorPane.setLeftAnchor(createGameRoot, 0d);
    createGameRoot.requestFocus();
  }

  @Subscribe
  public void onHostMapInCustomGameEvent(HostMapInCustomGameEvent event) {
    createGameButton.fire();
  }

  public Node getRoot() {
    return gamesRoot;
  }

  public void onTableButtonClicked() {
    GamesTableController gamesTableController = uiService.loadFxml("theme/play/games_table.fxml");
    gamesTableController.selectedGameProperty()
        .addListener((observable, oldValue, newValue) -> setSelectedGame(newValue));
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
    GamesTilesContainerController gamesTilesContainerController = uiService.loadFxml("theme/play/games_tiles_container.fxml");
    gamesTilesContainerController.selectedGameProperty()
        .addListener((observable, oldValue, newValue) -> setSelectedGame(newValue));
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
}
