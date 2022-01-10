package com.faforever.client.fa;

import com.faforever.client.builders.GameLaunchMessageBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.logging.LoggingService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.lobby.GameLaunchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ForgedAllianceServiceTest extends ServiceTest {
  @InjectMocks
  private ForgedAllianceService instance;
  @Mock
  private PlayerService playerService ;
  @Mock
  private LoggingService loggingService;
  @Mock
  private PreferencesService preferencesService;

  @BeforeEach
  public void setUp() throws Exception {
    Preferences preferences = PreferencesBuilder.create().defaultValues()
        .dataPrefs()
        .dataDirectory(Path.of("."))
        .then()
        .get();
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(playerService.getCurrentPlayer()).thenReturn(PlayerBeanBuilder.create().defaultValues().get());
  }

  @Test
  public void testStartGameOffline() throws Exception {
    IOException throwable = assertThrows(IOException.class, () -> instance.startGameOffline("test"));
    assertThat(throwable.getCause().getMessage(), containsString("error=2"));

    verify(loggingService).getNewGameLogFile(0);
    verify(preferencesService, times(3)).getPreferences();
  }

  @Test
  public void testStartGameOnline() throws Exception {
    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();
    IOException throwable = assertThrows(IOException.class, () -> instance.startGameOnline(gameLaunchMessage, 0, 0, false));
    assertThat(throwable.getCause().getMessage(), containsString("error=2"));

    verify(playerService).getCurrentPlayer();
    verify(loggingService).getNewGameLogFile(gameLaunchMessage.getUid());
    verify(preferencesService, times(3)).getPreferences();
  }

  @Test
  public void testStartReplay() throws Exception {
    IOException throwable = assertThrows(IOException.class, () -> instance.startReplay(Path.of("."), 0));
    assertThat(throwable.getCause().getMessage(), containsString("error=2"));

    verify(loggingService).getNewGameLogFile(0);
    verify(preferencesService, times(3)).getPreferences();
  }

  @Test
  public void testStartOnlineReplay() throws Exception {
    IOException throwable = assertThrows(IOException.class, () -> instance.startReplay(URI.create("google.com"), 0));
    assertThat(throwable.getCause().getMessage(), containsString("error=2"));

    verify(playerService).getCurrentPlayer();
    verify(loggingService).getNewGameLogFile(0);
    verify(preferencesService, times(3)).getPreferences();
  }
}
