package com.faforever.client.game;

import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class GameTooltipControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private UiService uiService;
  @Mock
  private PlayerService playerService;
  
  @Mock
  private Game game;
  
  @Mock
  private TeamCardController teamCardController;
  private GameTooltipController instance;
  
  @Before
  public void setUp() throws Exception {
    when(uiService.loadFxml("theme/team_card.fxml")).thenReturn(teamCardController);
    when(teamCardController.getRoot()).thenReturn(new Pane());
    when(playerService.getPlayerByNameIfOnline(Mockito.anyString())).thenReturn(Optional.of(new Player("")));
    
    instance = new GameTooltipController(uiService, playerService);
    loadFxml("theme/play/game_tooltip.fxml", clazz -> instance);
  }
  
  @Test
  public void testSetGame() {
    ObservableMap<String, String> simMods = FXCollections.observableHashMap();
    when(game.getSimMods()).thenReturn(simMods);
    ObservableMap<String, List<String>> teams = FXCollections.observableHashMap();
    when(game.getTeams()).thenReturn(teams);
    
    instance.setGame(game);
    instance.displayGame();
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.modsPane.isVisible());
    assertThat(instance.teamsPane.getPrefColumns(), is(0));
    
    teams.put("team1", List.of("Bob"));
    instance.setGame(game);
    instance.displayGame();
    WaitForAsyncUtils.waitForFxEvents();
    assertThat(instance.teamsPane.getPrefColumns(), is(1));
    
    simMods.put("mod1", "mod1");
    instance.setGame(game);
    instance.displayGame();
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.modsPane.isVisible());
  }
  
  @Test
  public void testSetGameNull() {
    instance.setGame(null);
    instance.displayGame();
    WaitForAsyncUtils.waitForFxEvents();
  }
}
