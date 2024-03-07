package com.faforever.client.game;

import com.faforever.client.domain.server.GameInfo;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CustomGamesControllerTest extends PlatformTest {

  @InjectMocks
  private CustomGamesController instance;
  @Mock
  private GameService gameService;
  @Mock
  private GameRunner gameRunner;
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

  private ObservableList<GameInfo> games;

  @BeforeEach
  public void setUp() throws Exception {
    games = FXCollections.observableArrayList();

    preferences.setGamesViewMode("tableButton");
    preferences.setShowGameDetailsSidePane(true);

    lenient().when(gameService.getGames()).thenReturn(games);
    lenient().when(gameRunner.runningProperty()).thenReturn(new SimpleBooleanProperty());
    lenient().when(gameDetailController.gameProperty()).thenReturn(new SimpleObjectProperty<>());
    lenient().when(gamesTilesContainerController.getRoot()).thenReturn(new Pane());
    lenient().when(gamesTableController.getRoot()).thenReturn(new Pane());
    lenient().when(gamesTableController.selectedGameProperty()).thenReturn(new SimpleObjectProperty<>());
    lenient().when(uiService.loadFxml("theme/filter/filter.fxml", CustomGamesFilterController.class))
             .thenReturn(customGamesFilterController);
    lenient().when(uiService.loadFxml("theme/play/games_table.fxml")).thenReturn(gamesTableController);
    lenient().when(uiService.loadFxml("theme/play/games_tiles_container.fxml"))
             .thenReturn(gamesTilesContainerController);
    lenient().when(customGamesFilterController.filterActiveProperty()).thenReturn(new SimpleBooleanProperty());
    lenient().when(customGamesFilterController.predicateProperty())
             .thenReturn(new SimpleObjectProperty<>(item -> true));
    lenient().when(gamesTilesContainerController.sortingOrderProperty()).thenReturn(new SimpleObjectProperty<>());
    lenient().when(gameDetailController.getRoot()).thenReturn(new Pane());

    loadFxml("theme/play/custom_games.fxml", clazz -> {
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
    when(gamesTilesContainerController.selectedGameProperty()).thenReturn(new SimpleObjectProperty<>());

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
