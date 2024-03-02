package com.faforever.client.game;

import com.faforever.client.builders.GameInfoBuilder;
import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.server.GameInfo;
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
import static org.mockito.Mockito.lenient;

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
    lenient().when(uiService.loadFxml("theme/team_card.fxml")).thenReturn(teamCardController);
    lenient().when(teamCardController.getRoot()).then(invocation -> new Pane());
    lenient().when(teamCardController.playerIdsProperty()).thenReturn(new SimpleObjectProperty<>());
    lenient().when(teamCardController.teamIdProperty()).thenReturn(new SimpleIntegerProperty());
    lenient().when(teamCardController.ratingProviderProperty()).thenReturn(new SimpleObjectProperty<>());
    lenient().when(playerService.getPlayerByNameIfOnline(Mockito.anyString()))
             .thenReturn(Optional.of(PlayerInfoBuilder.create().defaultValues().get()));

    loadFxml("theme/play/game_tooltip.fxml", clazz -> instance);
  }
  
  @Test
  public void testSetGame() {
    GameInfo game = GameInfoBuilder.create().defaultValues().simMods(Map.of()).teams(Map.of()).get();

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
