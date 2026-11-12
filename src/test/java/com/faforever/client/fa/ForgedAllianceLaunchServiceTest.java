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
  @Mock
  private DesyncReports desyncReports; // returns no arguments: the original exe in these tests

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
    assertThrows(GameLaunchException.class, () -> instance.launchOfflineGame("test"));

    verify(loggingService).getNewGameLogFile(0);
  }

  @Test
  public void testRestartAfterACrashLaunchesWithTheReturnArguments() throws Exception {
    when(playerService.getCurrentPlayer()).thenReturn(PlayerInfoBuilder.create().defaultValues().get());
    when(desyncReports.launchArguments(org.mockito.ArgumentMatchers.anyInt())).thenReturn(java.util.List.of());
    GameParameters gameParameters = GameParametersBuilder.create().defaultValues().get();

    // No exe in the test's data folder, so the start fails after the command is built.
    ch.qos.logback.classic.Logger logger =
        (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ForgedAllianceLaunchService.class);
    ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> logged =
        new ch.qos.logback.core.read.ListAppender<>();
    logged.start();
    logger.addAppender(logged);
    try {
      assertThrows(GameLaunchException.class,
                   () -> instance.relaunchOnlineGame(gameParameters, 0, java.util.List.of("/rejoin", "9=0", "1", "0")));
    } finally {
      logger.detachAppender(logged);
    }

    verify(desyncReports).launchArguments(gameParameters.uid());
    verify(loggingService).getNewGameLogFile(gameParameters.uid());
    // The command it tried to start, as logged just before: the return arguments are in it, in order. Tests run in
    // parallel and share the logger, so only this thread's events count (the launch logs on the calling thread),
    // and of those the command with /rejoin.
    String thisThread = Thread.currentThread().getName();
    java.util.List<?> command = logged.list.stream()
        .filter(event -> thisThread.equals(event.getThreadName()))
        .filter(event -> event.getMessage().startsWith("Starting Forged Alliance with command"))
        .map(event -> (java.util.List<?>) event.getArgumentArray()[0])
        .filter(logged1 -> logged1.contains("/rejoin"))
        .findFirst().orElseThrow(() -> new AssertionError("no launch command with /rejoin was logged"));
    int at = command.indexOf("/rejoin");
    org.junit.jupiter.api.Assertions.assertTrue(at >= 0, String.valueOf(command));
    org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("/rejoin", "9=0", "1", "0"), command.subList(at, at + 4));
  }

  @Test
  public void testStartOfflineGameFailsWithoutAnExe() throws Exception {
    assertThrows(GameLaunchException.class, () -> instance.launchOfflineGame("scmp_009"));
  }

  @Test
  public void testStartGameOnline() throws Exception {
    when(playerService.getCurrentPlayer()).thenReturn(PlayerInfoBuilder.create().defaultValues().get());
    GameParameters gameParameters = GameParametersBuilder.create().defaultValues().get();
    assertThrows(GameLaunchException.class, () -> instance.launchOnlineGame(gameParameters, 0, 0, java.util.List.of()));

    verify(playerService).getCurrentPlayer();
    verify(loggingService).getNewGameLogFile(gameParameters.uid());
  }

  @Test
  public void testStartReplay() throws Exception {
    assertThrows(GameLaunchException.class, () -> instance.startReplay(Path.of("."), 0));

    verify(loggingService).getNewReplayLogFile(0);
    verify(instance).getReplayExecutablePath();
  }

  @Test
  public void testStartOnlineReplay() throws Exception {
    when(playerService.getCurrentPlayer()).thenReturn(PlayerInfoBuilder.create().defaultValues().get());
    assertThrows(GameLaunchException.class, () -> instance.startReplay(URI.create("google.com"), 0));

    verify(playerService).getCurrentPlayer();
    verify(loggingService).getNewReplayLogFile(0);
    verify(instance).getReplayExecutablePath();
  }
}
