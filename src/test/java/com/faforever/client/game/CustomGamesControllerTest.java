package com.faforever.client.game;

import com.faforever.client.domain.GameBean;
import com.faforever.client.filter.CustomGamesFilterController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.replay.WatchButtonController;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CustomGamesControllerTest extends PlatformTest {

  @InjectMocks
  private CustomGamesController instance;
  @Mock
  private GameService gameService;

  @Mock
  private UiService uiService;
  @Mock
  private GamesTableController gamesTableController;
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
  @Mock
  private GameTooltipController gameTooltipController;
  @Spy
  private Preferences preferences;

  private ObservableList<GameBean> games;

  @BeforeEach
  public void setUp() throws Exception {
    games = FXCollections.observableArrayList();

    preferences.setGamesViewMode("tableButton");
    preferences.setShowGameDetailsSidePane(true);

    when(gameService.getGames()).thenReturn(games);
    when(gameService.gameRunningProperty()).thenReturn(new SimpleBooleanProperty());
    when(uiService.loadFxml("theme/play/games_table.fxml")).thenReturn(gamesTableController);
    when(uiService.loadFxml("theme/play/games_tiles_container.fxml")).thenReturn(gamesTilesContainerController);
    when(gameDetailController.gameProperty()).thenReturn(new SimpleObjectProperty<>());
    when(gamesTilesContainerController.getRoot()).thenReturn(new Pane());
    when(gamesTableController.getRoot()).thenReturn(new Pane());
    when(gamesTableController.selectedGameProperty()).thenReturn(new SimpleObjectProperty<>());
    when(gamesTilesContainerController.selectedGameProperty()).thenReturn(new SimpleObjectProperty<>());
    when(uiService.loadFxml("theme/filter/filter.fxml", CustomGamesFilterController.class)).thenReturn(customGamesFilterController);
    when(customGamesFilterController.filterActiveProperty()).thenReturn(new SimpleBooleanProperty());
    when(customGamesFilterController.predicateProperty()).thenReturn(new SimpleObjectProperty<>(item -> true));
    when(gamesTilesContainerController.sortingOrderProperty()).thenReturn(new SimpleObjectProperty<>());
    when(gameDetailController.getRoot()).thenReturn(new Pane());

    loadFxml("theme/play/custom_games.fxml", clazz -> {
      if (clazz == GamesTableController.class) {
        return gamesTableController;
      }
      if (clazz == GamesTilesContainerController.class) {
        return gamesTilesContainerController;
      }
      if (clazz == GameDetailController.class) {
        return gameDetailController;
      }
      if (clazz == WatchButtonController.class) {
        return watchButtonController;
      }
      if (clazz == GameTooltipController.class) {
        return gameTooltipController;
      }
      return instance;
    });
  }

  @Test
  public void testSetSelectedGameShowsDetailPane() {
    instance.toggleGameDetailPaneButton.setSelected(true);
    assertTrue(instance.gameDetailPane.isVisible());
  }

  @Test
  public void testSetSelectedGameDoesNotShowDetailPaneIfDisabled() {
    instance.toggleGameDetailPaneButton.setSelected(false);
    preferences.setShowGameDetailsSidePane(false);
    assertFalse(instance.gameDetailPane.isVisible());
  }

  @Test
  public void testDisplayTiles() {
    runOnFxThreadAndWait(() -> instance.tilesButton.fire());
    verify(gamesTilesContainerController).createTiledFlowPane(games);
  }

  @Test
  public void testHideSidePane() {
    runOnFxThreadAndWait(() -> instance.toggleGameDetailPaneButton.fire());

    assertFalse(preferences.isShowGameDetailsSidePane());

    assertFalse(instance.gameDetailPane.isManaged());
    assertFalse(instance.gameDetailPane.isVisible());
  }
}
