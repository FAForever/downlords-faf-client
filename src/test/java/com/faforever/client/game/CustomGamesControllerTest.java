package com.faforever.client.game;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.filter.GameFilterController;
import com.faforever.client.i18n.I18n;
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
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
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
  private GameFilterController gameFilterController;

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
    when(uiService.loadFxml("theme/filter/filter.fxml", GameFilterController.class)).thenReturn(gameFilterController);
    when(gameFilterController.getFilterStateProperty()).thenReturn(new SimpleBooleanProperty());
    when(gameFilterController.getPredicateProperty()).thenReturn(new SimpleObjectProperty<>(item -> true));

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
    verify(gameDetailController).setGame(null);
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

    assertFalse(instance.gameDetailPane.isManaged());
    assertFalse(instance.gameDetailPane.isVisible());
  }
}
