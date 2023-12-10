package com.faforever.client.patch;

import com.faforever.client.io.ChecksumMismatchException;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.update.Version;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.ObjectFactory;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.faforever.client.game.KnownFeaturedMod.FAF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameUpdaterImplTest extends ServiceTest {

  @TempDir
  public Path tempDir;
  @InjectMocks
  private GameUpdaterImpl instance;

  @Mock
  private FeaturedModUpdater featuredModUpdater;
  @Mock
  private TaskService taskService;
  @Mock
  private ObjectFactory<GameBinariesUpdateTask> gameBinariesUpdateTaskFactory;
  @Spy
  private DataPrefs dataPrefs;
  @Spy
  private ForgedAlliancePrefs forgedAlliancePrefs;

  @Mock
  private GameBinariesUpdateTaskImpl gameBinariesUpdateTask;

  private Path fafDataDirectory;
  private Path binDirectory;
  private Path replayDataDirectory;
  private Path replayBinDirectory;

  @BeforeEach
  public void setUp() throws Exception {
    Path cwd = Path.of(".");
    forgedAlliancePrefs.setInstallationPath(cwd);
    forgedAlliancePrefs.setVaultBaseDirectory(cwd);
    dataPrefs.setBaseDataDirectory(tempDir.resolve("faf_temp_data"));
    fafDataDirectory = Files.createDirectories(dataPrefs.getBaseDataDirectory());
    binDirectory = Files.createDirectories(dataPrefs.getBinDirectory());
    replayDataDirectory = Files.createDirectories(dataPrefs.getReplayDataDirectory());
    replayBinDirectory = Files.createDirectories(dataPrefs.getReplayBinDirectory());
    lenient().when(gameBinariesUpdateTaskFactory.getObject()).thenReturn(gameBinariesUpdateTask);
    lenient().when(taskService.submitTask(gameBinariesUpdateTask)).thenReturn(gameBinariesUpdateTask);
    lenient().when(gameBinariesUpdateTask.getFuture()).thenReturn(CompletableFuture.completedFuture(null));
    lenient().when(featuredModUpdater.updateMod(any(String.class), any(), eq(false)))
             .thenAnswer(invocation -> {
               String featuredModName = invocation.getArgument(0, String.class);
               Path initFile = binDirectory.resolve(String.format("init_%s", featuredModName));
      Files.createFile(initFile);
      int version = Objects.requireNonNullElse(invocation.getArgument(1, Integer.class), Integer.MAX_VALUE);
      return CompletableFuture.completedFuture(new PatchResult(new ComparableVersion(String.valueOf(version)), initFile));
    });
    lenient().when(featuredModUpdater.updateMod(any(String.class), any(), eq(true)))
             .thenAnswer(invocation -> {
               String featuredModName = invocation.getArgument(0, String.class);
               Path initFile = replayBinDirectory.resolve(String.format("init_%s", featuredModName));
      Files.createFile(initFile);
      int version = Objects.requireNonNullElse(invocation.getArgument(1, Integer.class), Integer.MAX_VALUE);
      return CompletableFuture.completedFuture(new PatchResult(new ComparableVersion(String.valueOf(version)), initFile));
    });
  }

  @Test
  public void badChecksumTest() throws Exception {
    when(featuredModUpdater.updateMod(any(String.class), any(), anyBoolean()))
        .thenAnswer(invocation -> CompletableFuture.failedFuture(new ChecksumMismatchException(
            URI.create("http://google.com").toURL(), "asd", "qwe")));

    CompletionException exception = assertThrows(CompletionException.class,
                                                 () -> instance.update(FAF.getTechnicalName(), Map.of(), 0, false)
                                                               .join());
    assertEquals(ChecksumMismatchException.class, exception.getCause().getClass());
    assertFalse(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertFalse(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
  }

  @Test
  public void nonBaseModUpdateTestEmptyVersions() throws Exception {
    String technicalName = "Test_Mod";

    instance.update(technicalName, Map.of(), 0, false).join();

    verify(taskService).submitTask(gameBinariesUpdateTask);
    verify(gameBinariesUpdateTask).setVersion(new ComparableVersion(String.valueOf(0)));
    verify(featuredModUpdater).updateMod(FAF.getTechnicalName(), 0, false);
    verify(featuredModUpdater).updateMod(technicalName, null, false);
    assertTrue(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertTrue(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
    assertTrue(Files.exists(binDirectory.resolve(String.format("init_%s", technicalName))));
  }

  @Test
  public void nonBaseModUpdateTest() throws Exception {
    String technicalName = "Test_Mod";

    instance.update(technicalName, Map.of("1", 100), 0, false).join();

    verify(taskService).submitTask(gameBinariesUpdateTask);
    verify(gameBinariesUpdateTask).setVersion(new ComparableVersion(String.valueOf(0)));
    verify(featuredModUpdater).updateMod(FAF.getTechnicalName(), 0, false);
    verify(featuredModUpdater).updateMod(technicalName, 100, false);
    assertTrue(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertTrue(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
    assertTrue(Files.exists(binDirectory.resolve(String.format("init_%s", technicalName))));
  }

  @Test
  public void nonBaseModUpdateWithCacheDirectoryTest() {
    String technicalName = "Test_Mod";

    instance.update(technicalName, Map.of("1", 100), 0, true).join();

    verify(gameBinariesUpdateTaskFactory).getObject();
    verify(taskService).submitTask(gameBinariesUpdateTask);
    verify(gameBinariesUpdateTask).setVersion(new ComparableVersion(String.valueOf(0)));
    verify(featuredModUpdater).updateMod(FAF.getTechnicalName(), 0, true);
    verify(featuredModUpdater).updateMod(technicalName, 100, true);
    assertTrue(Files.exists(replayDataDirectory.resolve("fa_path.lua")));
    assertTrue(Files.exists(replayBinDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
    assertTrue(Files.exists(replayBinDirectory.resolve(String.format("init_%s", technicalName))));
  }

  @Test
  public void nonBaseModUpdateTestWithNulls() throws Exception {
    String technicalName = "Test_Mod";

    instance.update(technicalName, null, null, false).join();

    verify(taskService).submitTask(gameBinariesUpdateTask);
    verify(gameBinariesUpdateTask).setVersion(new ComparableVersion(String.valueOf(Integer.MAX_VALUE)));
    verify(featuredModUpdater).updateMod(FAF.getTechnicalName(), null, false);
    verify(featuredModUpdater).updateMod(technicalName, null, false);
    assertTrue(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertTrue(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
    assertTrue(Files.exists(binDirectory.resolve(String.format("init_%s", technicalName))));
  }

  @Test
  public void baseModUpdateTest() throws Exception {
    String technicalName = FAF.getTechnicalName();

    instance.update(technicalName, Map.of(), 0, false).join();

    verify(taskService).submitTask(gameBinariesUpdateTask);
    verify(gameBinariesUpdateTask).setVersion(new ComparableVersion(String.valueOf(0)));
    verify(featuredModUpdater).updateMod(technicalName, 0, false);
    assertTrue(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertTrue(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
    assertTrue(Files.exists(binDirectory.resolve(String.format("init_%s", technicalName))));
  }

  @Test
  public void baseModUpdateWithCacheDirectoryTest() throws Exception {
    String technicalName = FAF.getTechnicalName();

    instance.update(technicalName, Map.of(), 0, true).join();

    verify(gameBinariesUpdateTaskFactory).getObject();
    verify(taskService).submitTask(gameBinariesUpdateTask);
    verify(gameBinariesUpdateTask).setVersion(new ComparableVersion(String.valueOf(0)));
    verify(featuredModUpdater).updateMod(technicalName, 0, true);
    assertTrue(Files.exists(replayDataDirectory.resolve("fa_path.lua")));
    assertTrue(Files.exists(replayBinDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
    assertTrue(Files.exists(replayBinDirectory.resolve(String.format("init_%s", technicalName))));
  }

  @Test
  public void baseModUpdateWithSimModsInstalledTest() throws Exception {
    String technicalName = FAF.getTechnicalName();

    instance.update(technicalName, Map.of(), 0, false).join();

    verify(taskService).submitTask(gameBinariesUpdateTask);
    verify(gameBinariesUpdateTask).setVersion(new ComparableVersion(String.valueOf(0)));
    verify(featuredModUpdater).updateMod(technicalName, 0, false);
    assertTrue(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertTrue(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
    assertTrue(Files.exists(binDirectory.resolve(String.format("init_%s", technicalName))));
  }

  @Test
  public void baseModUpdateWithSimModsNotInstalledTest() throws Exception {
    String technicalName = FAF.getTechnicalName();

    instance.update(technicalName, Map.of(), 0, false).join();

    verify(taskService).submitTask(gameBinariesUpdateTask);
    verify(gameBinariesUpdateTask).setVersion(new ComparableVersion(String.valueOf(0)));
    verify(featuredModUpdater).updateMod(technicalName, 0, false);
    assertTrue(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertTrue(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
    assertTrue(Files.exists(binDirectory.resolve(String.format("init_%s", technicalName))));
  }

  @Test
  public void testCheckFaPathFileContent() throws Exception {
    String gameType = FAF.getTechnicalName();
    Integer gameVersion = 3711;
    String clientVersion = Version.getCurrentVersion();

    instance.update(gameType, Map.of(), gameVersion, false).join();

    String content = Files.readString(fafDataDirectory.resolve("fa_path.lua"));
    assertTrue(content.contains("GameType = \"" + gameType + "\""));
    assertTrue(content.contains("GameVersion = \"" + gameVersion + "\""));
    assertTrue(content.contains("ClientVersion = \"" + clientVersion + "\""));
  }

}
