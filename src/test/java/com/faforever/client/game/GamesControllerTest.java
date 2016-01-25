package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.collections.FXCollections;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class GamesControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  I18n i18n;
  private GamesController instance;
  @Mock
  private EnterPasswordController enterPasswordController;
  @Mock
  private CreateGameController createGameController;
  @Mock
  private GameService gameService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private Preferences preferences;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private GamesTableController gamesTableController;
  @Mock
  private GamesTilesContainerController gamesTilesContainerController;
  @Mock
  private MapService mapService;

  @Before
  public void setUp() throws Exception {
    instance = loadController("games.fxml");
    instance.enterPasswordController = enterPasswordController;
    instance.createGameController = createGameController;
    instance.gameService = gameService;
    instance.preferencesService = preferencesService;
    instance.applicationContext = applicationContext;
    instance.mapService = mapService;
    instance.i18n = i18n;

    when(enterPasswordController.getRoot()).thenReturn(new Pane());
    when(createGameController.getRoot()).thenReturn(new Pane());
    when(gameService.getGameInfoBeans()).thenReturn(FXCollections.observableArrayList());
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getGamesViewMode()).thenReturn("tableButton");
    when(applicationContext.getBean(GamesTilesContainerController.class)).thenReturn(gamesTilesContainerController);
    when(applicationContext.getBean(GamesTableController.class)).thenReturn(gamesTableController);
    when(gamesTilesContainerController.getRoot()).thenReturn(new Pane());

    instance.postConstruct();
  }

  @Test
  public void testSetSelectedGameShowsDetailPane() throws Exception {
    assertFalse(instance.gameDetailPane.isVisible());
    instance.setSelectedGame(GameInfoBeanBuilder.create().defaultValues().get());
    assertTrue(instance.gameDetailPane.isVisible());
  }

  @Test
  public void testSetSelectedGameNullHidesDetailPane() throws Exception {
    instance.setSelectedGame(GameInfoBeanBuilder.create().defaultValues().get());
    instance.setSelectedGame(null);
    assertFalse(instance.gameDetailPane.isVisible());
  }
}