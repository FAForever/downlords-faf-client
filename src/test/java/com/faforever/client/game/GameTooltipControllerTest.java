package com.faforever.client.game;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class GameTooltipControllerTest extends PlatformTest {

  @Mock
  private UiService uiService;
  @Mock
  private PlayerService playerService;

  @Mock
  private TeamCardController teamCardController;
  @InjectMocks
  private GameTooltipController instance;
  
  @BeforeEach
  public void setUp() throws Exception {
    when(uiService.loadFxml("theme/team_card.fxml")).thenReturn(teamCardController);
    when(teamCardController.getRoot()).then(invocation -> new Pane());
    when(teamCardController.playerIdsProperty()).thenReturn(new SimpleObjectProperty<>());
    when(teamCardController.teamIdProperty()).thenReturn(new SimpleIntegerProperty());
    when(teamCardController.ratingProviderProperty()).thenReturn(new SimpleObjectProperty<>());
    when(playerService.getPlayerByNameIfOnline(Mockito.anyString())).thenReturn(Optional.of(PlayerBeanBuilder.create().defaultValues().get()));

    loadFxml("theme/play/game_tooltip.fxml", clazz -> instance);
  }
  
  @Test
  public void testSetGame() {
    GameBean game = GameBeanBuilder.create().defaultValues().simMods(Map.of()).teams(Map.of()).get();

    instance.setGame(game);
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.modsPane.isVisible());
    assertThat(instance.teamsPane.getPrefColumns(), is(0));

    game.setTeams(Map.of(1, List.of(1)));
    WaitForAsyncUtils.waitForFxEvents();
    assertThat(instance.teamsPane.getPrefColumns(), is(1));

    game.setSimMods(Map.of("mod1", "mod1"));
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.modsPane.isVisible());
  }
  
  @Test
  public void testSetGameNull() {
    instance.setGame(null);
    WaitForAsyncUtils.waitForFxEvents();
  }
}
