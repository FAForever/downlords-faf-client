package com.faforever.client.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.ModService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.domain.GameState;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import static javafx.beans.binding.Bindings.createObjectBinding;
import static javafx.beans.binding.Bindings.createStringBinding;

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
      gameInfoBean.getStatus() == GameState.OPEN
          && !HIDDEN_FEATURED_MODS.contains(gameInfoBean.getFeaturedMod());
  private final UiService uiService;
  private final I18n i18n;
  private final GameService gameService;
  private final MapService mapService;
  private final PreferencesService preferencesService;
  private final ModService modService;
  private final EventBus eventBus;
  private final PlayerService playerService;

  public ToggleButton tableButton;
  public ToggleButton tilesButton;
  public ToggleGroup viewToggleGroup;
  public VBox teamListPane;
  public Label mapLabel;
  public Button createGameButton;
  public Pane gameViewContainer;
  public Pane gamesRoot;
  public ImageView mapImageView;
  public Label gameTitleLabel;
  public Label numberOfPlayersLabel;
  public Label hostLabel;
  public Label gameTypeLabel;
  public ScrollPane gameDetailPane;
  private FilteredList<Game> filteredItems;

  private Game currentGame;
  private InvalidationListener teamsChangeListener;


  @Inject
  public CustomGamesController(UiService uiService, I18n i18n, GameService gameService, MapService mapService,
                               PreferencesService preferencesService, ModService modService, EventBus eventBus,
                               PlayerService playerService) {
    this.uiService = uiService;
    this.i18n = i18n;
    this.gameService = gameService;
    this.mapService = mapService;
    this.preferencesService = preferencesService;
    this.modService = modService;
    this.eventBus = eventBus;
    this.playerService = playerService;
  }

  public void initialize() {
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
    gamesTilesContainerController.createTiledFlowPane(filteredItems);

    Platform.runLater(() -> {
      Node root = gamesTilesContainerController.getRoot();
      populateContainer(root);
    });
  }

  @VisibleForTesting
  void setSelectedGame(Game game) {
    if (game == null) {
      gameDetailPane.setVisible(false);
      return;
    }

    gameDetailPane.setVisible(true);

    gameTitleLabel.textProperty().bind(game.titleProperty());

    mapImageView.imageProperty().bind(createObjectBinding(
        () -> mapService.loadPreview(game.getMapFolderName(), PreviewSize.LARGE),
        game.mapFolderNameProperty()
    ));

    numberOfPlayersLabel.textProperty().bind(createStringBinding(
        () -> i18n.get("game.detail.players.format", game.getNumPlayers(), game.getMaxPlayers()),
        game.numPlayersProperty(),
        game.maxPlayersProperty()
    ));

    hostLabel.textProperty().bind(game.hostProperty());
    mapLabel.textProperty().bind(game.mapFolderNameProperty());

    gameTypeLabel.textProperty().bind(createStringBinding(() -> {
      FeaturedMod gameType = modService.getFeaturedMod(game.getFeaturedMod()).get();
      String fullName = gameType != null ? gameType.getDisplayName() : null;
      return StringUtils.defaultString(fullName);
    }, game.featuredModProperty()));

    if (currentGame != null) {
      currentGame.getTeams().removeListener(teamsChangeListener);
    }

    teamsChangeListener = observable -> createTeams(game.getTeams());
    teamsChangeListener.invalidated(game.getTeams());
    game.getTeams().addListener(teamsChangeListener);

    currentGame = game;
  }

  private void createTeams(ObservableMap<? extends String, ? extends List<String>> playersByTeamNumber) {
    teamListPane.getChildren().clear();
    synchronized (playersByTeamNumber) {
      TeamCardController.createAndAdd(playersByTeamNumber, playerService, uiService, teamListPane);
    }
  }
}
