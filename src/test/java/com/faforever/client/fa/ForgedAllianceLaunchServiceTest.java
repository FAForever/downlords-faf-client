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
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockConstruction;
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
  private FafReReleaseService fafReReleaseService;

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
  public void testStartGameOnline() throws Exception {
    when(playerService.getCurrentPlayer()).thenReturn(PlayerInfoBuilder.create().defaultValues().get());
    GameParameters gameParameters = GameParametersBuilder.create().defaultValues().get();
    assertThrows(GameLaunchException.class, () -> instance.launchOnlineGame(gameParameters, 0, 0));

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
  @EnabledOnOs(OS.WINDOWS)
  public void testStartReplayWithFafRe() {
    forgedAlliancePrefs.setUseFafRe(true);
    Path engine = Path.of("faf-re", "main_x64.exe").toAbsolutePath();
    when(fafReReleaseService.getExecutable()).thenReturn(engine);
    Path replay = Path.of("game.scfareplay");

    try (var processes = mockConstruction(ProcessBuilder.class)) {
      instance.startReplay(replay, 0);

      ProcessBuilder process = processes.constructed().getFirst();
      verify(process).directory(dataPrefs.getReplayBinDirectory().toFile());
      verify(process).command(List.of(
          engine.toString(), "/init", dataPrefs.getReplayBinDirectory().resolve("init.lua").toAbsolutePath().toString(),
          "/nobugreport", "/replay", replay.toAbsolutePath().toString(), "/replayid", "0", "/notelemetry"));
    }
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
