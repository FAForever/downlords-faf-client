package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.ModService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class CustomGamesControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  I18n i18n;
  private CustomGamesController instance;
  @Mock
  private GameService gameService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private Preferences preferences;
  @Mock
  private UiService uiService;
  @Mock
  private GamesTableController gamesTableController;
  @Mock
  private MapService mapService;
  @Mock
  private ModService modService;
  @Mock
  private EventBus eventBus;
  @Mock
  private PlayerService playerService;

  @Before
  public void setUp() throws Exception {
    instance = new CustomGamesController(uiService, i18n, gameService, mapService, preferencesService, modService, eventBus, playerService);

    when(gameService.getGames()).thenReturn(FXCollections.observableArrayList());
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getGamesViewMode()).thenReturn("tableButton");
    when(uiService.loadFxml("theme/play/games_table.fxml")).thenReturn(gamesTableController);
    when(gamesTableController.selectedGameProperty()).thenReturn(new SimpleObjectProperty<>());

    loadFxml("theme/play/custom_games.fxml", clazz -> instance);
  }

  @Test
  public void testSetSelectedGameShowsDetailPane() throws Exception {
    assertFalse(instance.gameDetailPane.isVisible());
    instance.setSelectedGame(GameBuilder.create().defaultValues().get());
    assertTrue(instance.gameDetailPane.isVisible());
  }

  @Test
  public void testSetSelectedGameNullHidesDetailPane() throws Exception {
    instance.setSelectedGame(GameBuilder.create().defaultValues().get());
    assertTrue(instance.gameDetailPane.isVisible());
    instance.setSelectedGame(null);
    assertFalse(instance.gameDetailPane.isVisible());
  }
}
