package com.faforever.client.fa;

import com.faforever.client.builders.GameParametersBuilder;
import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.game.error.GameLaunchException;
import com.faforever.client.logging.LoggingService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.net.URI;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ForgedAllianceLaunchServiceTest extends ServiceTest {
  @InjectMocks
  @Spy
  private ForgedAllianceLaunchService instance;
  @Mock
  private PlayerService playerService ;
  @Mock
  private LoggingService loggingService;

  @Spy
  private ForgedAlliancePrefs forgedAlliancePrefs;
  @Spy
  private DataPrefs dataPrefs;

  @BeforeEach
  public void setUp() throws Exception {
    dataPrefs.setBaseDataDirectory(Path.of("."));
  }

  @Test
  public void testStartGameOffline() throws Exception {
    GameLaunchException throwable = assertThrows(GameLaunchException.class, () -> instance.launchOfflineGame("test"));
    assertThat(throwable.getCause().getMessage(), containsString("error=2"));

    verify(loggingService).getNewGameLogFile(0);
  }

  @Test
  public void testStartGameOnline() throws Exception {
    when(playerService.getCurrentPlayer()).thenReturn(PlayerInfoBuilder.create().defaultValues().get());
    GameParameters gameParameters = GameParametersBuilder.create().defaultValues().get();
    GameLaunchException throwable = assertThrows(GameLaunchException.class,
                                                 () -> instance.launchOnlineGame(gameParameters, 0, 0));
    assertThat(throwable.getCause().getMessage(), containsString("error=2"));

    verify(playerService).getCurrentPlayer();
    verify(loggingService).getNewGameLogFile(gameParameters.uid());
  }

  @Test
  public void testStartReplay() throws Exception {
    GameLaunchException throwable = assertThrows(GameLaunchException.class,
                                                 () -> instance.startReplay(Path.of("."), 0));
    assertThat(throwable.getCause().getMessage(), containsString("error=2"));

    verify(loggingService).getNewReplayLogFile(0);
    verify(instance).getReplayExecutablePath();
  }

  @Test
  public void testStartOnlineReplay() throws Exception {
    when(playerService.getCurrentPlayer()).thenReturn(PlayerInfoBuilder.create().defaultValues().get());
    GameLaunchException throwable = assertThrows(GameLaunchException.class,
                                                 () -> instance.startReplay(URI.create("google.com"), 0));
    assertThat(throwable.getCause().getMessage(), containsString("error=2"));

    verify(playerService).getCurrentPlayer();
    verify(loggingService).getNewReplayLogFile(0);
    verify(instance).getReplayExecutablePath();
  }
}
