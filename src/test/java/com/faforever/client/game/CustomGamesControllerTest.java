package com.faforever.client.game;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.replay.WatchButtonController;
import com.google.common.eventbus.EventBus;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.testfx.util.WaitForAsyncUtils;

import static javafx.beans.binding.Bindings.createBooleanBinding;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CustomGamesControllerTest extends AbstractPlainJavaFxTest {

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

  private ObservableList<Game> games;
  private Preferences preferences;

  @Before
  public void setUp() throws Exception {
    instance = new CustomGamesController(uiService, gameService, preferencesService, eventBus, i18n);

    games = FXCollections.observableArrayList();

    preferences = new Preferences();
    preferences.setGamesViewMode("tableButton");
    preferences.setShowGameDetailsSidePane(true);

    when(gameService.getGames()).thenReturn(games);
    when(gameService.gameRunningProperty()).thenReturn(new SimpleBooleanProperty());
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(uiService.loadFxml("theme/play/games_table.fxml")).thenReturn(gamesTableController);
    when(uiService.loadFxml("theme/play/games_tiles_container.fxml")).thenReturn(gamesTilesContainerController);
    when(gamesTilesContainerController.getRoot()).thenReturn(new Pane());
    when(gamesTableController.getRoot()).thenReturn(new Pane());
    when(gamesTableController.selectedGameProperty()).thenReturn(new SimpleObjectProperty<>());
    when(gamesTilesContainerController.selectedGameProperty()).thenReturn(new SimpleObjectProperty<>());

    loadFxml("theme/play/custom_games.fxml", clazz -> {
      if (clazz == GameDetailController.class) {
        return gameDetailController;
      }
      if (clazz == WatchButtonController.class) {
        return watchButtonController;
      }
      return instance;
    });
    verify(gameDetailController).setGame(null);
  }

  @Test
  public void testSetSelectedGameShowsDetailPane() {
    instance.toggleGameDetailPaneButton.setSelected(true);
    instance.setSelectedGame(GameBuilder.create().defaultValues().get());
    assertTrue(instance.gameDetailPane.isVisible());
  }

  @Test
  public void testSetSelectedGameDoesNotShowDetailPaneIfDisabled() {
    instance.toggleGameDetailPaneButton.setSelected(false);
    preferences.setShowGameDetailsSidePane(false);
    instance.setSelectedGame(GameBuilder.create().defaultValues().get());
    assertFalse(instance.gameDetailPane.isVisible());
  }

  @Test
  public void testUpdateFilters() {
    Game game = GameBuilder.create().defaultValues().get();
    Game gameWithMod = GameBuilder.create().defaultValues().get();
    Game gameWithPW = GameBuilder.create().defaultValues().get();
    Game gameWithModAndPW = GameBuilder.create().defaultValues().get();

    ObservableMap<String, String> simMods = FXCollections.observableHashMap();
    simMods.put("123-456-789", "Fake mod name");
    gameWithMod.setSimMods(simMods);
    gameWithModAndPW.setSimMods(simMods);

    gameWithPW.setPassword("password");
    gameWithPW.passwordProtectedProperty().set(true);
    gameWithModAndPW.setPassword("password");
    gameWithModAndPW.passwordProtectedProperty().set(true);

    ObservableList<Game> games = FXCollections.observableArrayList();
    games.addAll(game, gameWithMod, gameWithPW, gameWithModAndPW);
    instance.setFilteredList(games);

    instance.showModdedGamesCheckBox.setSelected(true);
    instance.showPasswordProtectedGamesCheckBox.setSelected(true);
    assertEquals(4, instance.filteredItems.size());

    instance.showModdedGamesCheckBox.setSelected(false);
    assertEquals(2, instance.filteredItems.size());

    instance.showPasswordProtectedGamesCheckBox.setSelected(false);
    assertEquals(1, instance.filteredItems.size());

    instance.showModdedGamesCheckBox.setSelected(true);
    assertEquals(2, instance.filteredItems.size());
  }

  @Test
  public void testTiles() {
    instance.tilesButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    verify(gamesTilesContainerController).createTiledFlowPane(games, instance.chooseSortingTypeChoiceBox);
  }

  @Test
  public void testHideSidePane() {
    instance.toggleGameDetailPaneButton.fire();
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(preferencesService.getPreferences().isShowGameDetailsSidePane());
    verify(preferencesService, atLeast(2)).storeInBackground();

    assertThat(instance.toggleGameDetailPaneButton.getPseudoClassStates(), hasItem(CustomGamesController.PSEUDO_CLASS_CLOSED));
    assertFalse(instance.gameDetailPane.isManaged());
    assertFalse(instance.gameDetailPane.isVisible());
  }
}
