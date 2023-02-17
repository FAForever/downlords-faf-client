package com.faforever.client.game;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.filter.CustomGamesFilterController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.generator.MapGeneratedEvent;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.replay.WatchButtonController;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CustomGamesControllerTest extends UITest {

  @InjectMocks
  private CustomGamesController instance;
  @Mock
  private GameService gameService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private UiService uiService;
  @Mock
  private GamesTableController gamesTableController;
  @Mock
  private EventBus eventBus;
  @Mock
  private GameDetailController gameDetailController;
  @Mock
  private WatchButtonController watchButtonController;
  @Mock
  private I18n i18n;
  @Mock
  private GamesTilesContainerController gamesTilesContainerController;
  @Mock
  private CustomGamesFilterController customGamesFilterController;

  private ObservableList<GameBean> games;
  private Preferences preferences;

  @BeforeEach
  public void setUp() throws Exception {
    games = FXCollections.observableArrayList();

    preferences = PreferencesBuilder.create().defaultValues()
        .gamesViewMode("tableButton")
        .showGameDetailsSidePane(true)
        .get();

    when(gameService.getGames()).thenReturn(games);
    when(gameService.gameRunningProperty()).thenReturn(new SimpleBooleanProperty());
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(uiService.loadFxml("theme/play/games_table.fxml")).thenReturn(gamesTableController);
    when(uiService.loadFxml("theme/play/games_tiles_container.fxml")).thenReturn(gamesTilesContainerController);
    when(gamesTilesContainerController.getRoot()).thenReturn(new Pane());
    when(gamesTableController.getRoot()).thenReturn(new Pane());
    when(gamesTableController.selectedGameProperty()).thenReturn(new SimpleObjectProperty<>());
    when(gamesTilesContainerController.selectedGameProperty()).thenReturn(new SimpleObjectProperty<>());
    when(uiService.loadFxml("theme/filter/filter.fxml", CustomGamesFilterController.class)).thenReturn(customGamesFilterController);
    when(customGamesFilterController.filterStateProperty()).thenReturn(new SimpleBooleanProperty());
    when(customGamesFilterController.predicateProperty()).thenReturn(new SimpleObjectProperty<>(item -> true));

    when(gameDetailController.getRoot()).thenReturn(new Pane());

    loadFxml("theme/play/custom_games.fxml", clazz -> {
      if (clazz == GameDetailController.class) {
        return gameDetailController;
      }
      if (clazz == WatchButtonController.class) {
        return watchButtonController;
      }
      return instance;
    });
  }

  @Test
  public void testSetSelectedGameShowsDetailPane() {
    instance.toggleGameDetailPaneButton.setSelected(true);
    instance.setSelectedGame(GameBeanBuilder.create().defaultValues().get());
    assertTrue(instance.gameDetailPane.isVisible());
  }

  @Test
  public void testSetSelectedGameDoesNotShowDetailPaneIfDisabled() {
    instance.toggleGameDetailPaneButton.setSelected(false);
    preferences.setShowGameDetailsSidePane(false);
    instance.setSelectedGame(GameBeanBuilder.create().defaultValues().get());
    assertFalse(instance.gameDetailPane.isVisible());
  }

  @Test
  public void testDisplayTiles() {
    runOnFxThreadAndWait(() -> instance.tilesButton.fire());
    verify(gamesTilesContainerController).createTiledFlowPane(games, instance.chooseSortingTypeChoiceBox);
  }

  @Test
  public void testHideSidePane() {
    runOnFxThreadAndWait(() -> instance.toggleGameDetailPaneButton.fire());

    assertFalse(preferencesService.getPreferences().isShowGameDetailsSidePane());

    assertFalse(instance.gameDetailPane.isManaged());
    assertFalse(instance.gameDetailPane.isVisible());
  }

  @Test
  public void testOnMapGeneratedEventInTableView() {
    GameBean neroxisGame = GameBeanBuilder.create().defaultValues().mapFolderName("neroxis").get();
    games.add(neroxisGame);

    runOnFxThreadAndWait(() -> instance.onTableButtonClicked());
    instance.onMapGeneratedEvent(new MapGeneratedEvent(neroxisGame.getMapFolderName()));

    verify(gamesTableController).refreshTable();
  }

  @Test
  public void testOnMapGeneratedEventInTilesView() {
    GameBean neroxisGame = GameBeanBuilder.create().defaultValues().mapFolderName("neroxis").get();
    games.add(neroxisGame);

    runOnFxThreadAndWait(() -> instance.onTilesButtonClicked());
    instance.onMapGeneratedEvent(new MapGeneratedEvent(neroxisGame.getMapFolderName()));

    verify(gamesTilesContainerController).recreateTile(neroxisGame.getMapFolderName());
  }

  @Test
  public void testOnMapGeneratedEventWhenNoGeneratedMapInTableView() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    games.add(game);

    runOnFxThreadAndWait(() -> instance.onTableButtonClicked());
    instance.onMapGeneratedEvent(new MapGeneratedEvent("neroxis"));

    verify(gamesTableController, never()).refreshTable();
  }

  @Test
  public void testOnMapGeneratedEventWhenNoGeneratedMapInTilesView() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    games.add(game);

    runOnFxThreadAndWait(() -> instance.onTilesButtonClicked());
    instance.onMapGeneratedEvent(new MapGeneratedEvent("neroxis"));

    verify(gamesTilesContainerController, never()).recreateTile("neroxis");
  }
}
