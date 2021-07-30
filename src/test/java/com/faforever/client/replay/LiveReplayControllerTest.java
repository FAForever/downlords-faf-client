package com.faforever.client.replay;

import com.faforever.client.game.Game;
import com.faforever.client.game.GameBuilder;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.replay.LiveReplayController;
import com.faforever.commons.lobby.GameStatus;
import javafx.collections.FXCollections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class LiveReplayControllerTest extends UITest {

  @Mock
  private GameService gameService;
  @Mock
  private UiService uiService;
  @Mock
  private I18n i18n;
  @Mock
  private MapService mapService;
  @Mock
  private TimeService timeService;

  private LiveReplayController instance;

  private final Game openedGame = GameBuilder.create().defaultValues().id(1).status(GameStatus.OPEN).get();
  private final Game livingGame = GameBuilder.create().defaultValues().id(2).status(GameStatus.PLAYING).get();
  private final List<Game> games = List.of(openedGame, livingGame);

  @BeforeEach
  public void setUp() throws IOException {
    Mockito.when(gameService.getGames()).thenReturn(FXCollections.observableArrayList(games));

    instance = new LiveReplayController(gameService, uiService, i18n, mapService, timeService);
    loadFxml("theme/vault/replay/live_replays.fxml", clazz -> instance);
  }

  @Test
  public void testOnDisplay() {
    assertThat(instance.liveReplayControllerRoot.getItems().size(), is(1));
    assertThat(instance.liveReplayControllerRoot.getItems().get(0).getId(), is(2));
  }
}
