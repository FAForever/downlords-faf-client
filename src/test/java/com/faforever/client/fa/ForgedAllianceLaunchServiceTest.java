package com.faforever.client.fa;

import com.faforever.client.builders.GameParametersBuilder;
import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.fa.rendering.RenderingWrapperService;
import com.faforever.client.game.error.GameLaunchException;
import com.faforever.client.logging.LoggingService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.RenderingBackend;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ForgedAllianceLaunchServiceTest extends ServiceTest {
  @InjectMocks
  @Spy
  private ForgedAllianceLaunchService instance;
  @Mock
  private PlayerService playerService;
  @Mock
  private LoggingService loggingService;
  @Mock
  private RenderingWrapperService renderingWrapperService;

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
  public void testStartGameOfflineWithWrapperInjection(@TempDir Path tempDir) throws Exception {
    dataPrefs.setBaseDataDirectory(tempDir);
    forgedAlliancePrefs.setExecutionDirectory(tempDir.resolve("exec"));
    Files.createDirectories(tempDir.resolve("exec"));
    forgedAlliancePrefs.setRenderingBackend(RenderingBackend.DIRECTX_12);

    when(renderingWrapperService.ensureWrapperAvailable(RenderingBackend.DIRECTX_12)).thenReturn(true);
    when(renderingWrapperService.getWrapperDirectory(RenderingBackend.DIRECTX_12))
        .thenReturn(tempDir.resolve("bin/rendering/directx_12"));

    Path wrapperDir = tempDir.resolve("bin/rendering/directx_12");
    Files.createDirectories(wrapperDir);
    Files.createFile(wrapperDir.resolve("d3d9.dll"));

    assertThrows(GameLaunchException.class, () -> instance.launchOfflineGame("test"));

    // Wrapper is injected, process fails to start, wrapper is cleaned up
    assertFalse(Files.exists(tempDir.resolve("exec/d3d9.dll")));
  }

  @Test
  public void testStartGameOfflineWithWrapperDownloadFailure(@TempDir Path tempDir) throws Exception {
    dataPrefs.setBaseDataDirectory(tempDir);
    forgedAlliancePrefs.setExecutionDirectory(tempDir.resolve("exec"));
    Files.createDirectories(tempDir.resolve("exec"));
    forgedAlliancePrefs.setRenderingBackend(RenderingBackend.VULKAN_DXVK);

    when(renderingWrapperService.ensureWrapperAvailable(RenderingBackend.VULKAN_DXVK)).thenReturn(false);

    // Game should still launch (falls back to DX9), but will fail because no executable
    assertThrows(GameLaunchException.class, () -> instance.launchOfflineGame("test"));

    // No DLLs should have been injected
    assertFalse(Files.exists(tempDir.resolve("exec/d3d9.dll")));
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
  public void testStartOnlineReplay() throws Exception {
    when(playerService.getCurrentPlayer()).thenReturn(PlayerInfoBuilder.create().defaultValues().get());
    assertThrows(GameLaunchException.class, () -> instance.startReplay(URI.create("google.com"), 0));

    verify(playerService).getCurrentPlayer();
    verify(loggingService).getNewReplayLogFile(0);
    verify(instance).getReplayExecutablePath();
  }
}
