package com.faforever.client.game;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testfx.util.WaitForAsyncUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class GameTooltipControllerTest extends UITest {

  @Mock
  private UiService uiService;
  @Mock
  private PlayerService playerService;
  
  @Mock
  private GameBean game;
  
  @Mock
  private TeamCardController teamCardController;
  @InjectMocks
  private GameTooltipController instance;
  
  @BeforeEach
  public void setUp() throws Exception {
    when(uiService.loadFxml("theme/team_card.fxml")).thenReturn(teamCardController);
    when(teamCardController.getRoot()).thenReturn(new Pane());
    when(playerService.getPlayerByNameIfOnline(Mockito.anyString())).thenReturn(Optional.of(PlayerBeanBuilder.create().defaultValues().get()));

    loadFxml("theme/play/game_tooltip.fxml", clazz -> instance);
  }
  
  @Test
  public void testSetGame() {
    Map<String, String> simMods = new HashMap<>();
    when(game.getSimMods()).thenReturn(simMods);
    when(game.simModsProperty()).thenReturn(new SimpleObjectProperty<>(simMods));
    Map<Integer, List<PlayerBean>> teams = new HashMap<>();
    when(game.getTeams()).thenReturn(teams);
    when(game.teamsProperty()).thenReturn(new SimpleObjectProperty<>(teams));

    instance.setGame(game);
    instance.displayGame();
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.modsPane.isVisible());
    assertThat(instance.teamsPane.getPrefColumns(), is(0));

    teams.put(1, List.of(PlayerBeanBuilder.create().defaultValues().get()));
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
