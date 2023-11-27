package com.faforever.client.patch;

import com.faforever.client.builders.FeaturedModBeanBuilder;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.io.ChecksumMismatchException;
import com.faforever.client.mod.ModService;
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
import reactor.core.publisher.Mono;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameUpdaterImplTest extends ServiceTest {

  @InjectMocks
  private GameUpdaterImpl instance;
  @TempDir
  public Path tempDir;
  @Mock
  private ModService modService;
  @Mock
  private SimpleHttpFeaturedModUpdater simpleHttpFeaturedModUpdater;
  @Mock
  private TaskService taskService;
  @Mock
  private ObjectFactory<GameBinariesUpdateTask> gameBinariesUpdateTaskFactory;

  @Mock
  private GameBinariesUpdateTaskImpl gameBinariesUpdateTask;
  @Spy
  private DataPrefs dataPrefs;
  @Spy
  private ForgedAlliancePrefs forgedAlliancePrefs;

  private Path fafDataDirectory;
  private Path binDirectory;
  private Path replayDataDirectory;
  private Path replayBinDirectory;

  @BeforeEach
  public void setUp() throws Exception {
    Path cwd = Path.of(".");
    instance.setFeaturedModUpdater(simpleHttpFeaturedModUpdater);
    forgedAlliancePrefs.setInstallationPath(cwd);
    forgedAlliancePrefs.setVaultBaseDirectory(cwd);
    dataPrefs.setBaseDataDirectory(tempDir.resolve("faf_temp_data"));
    fafDataDirectory = Files.createDirectories(dataPrefs.getBaseDataDirectory());
    binDirectory = Files.createDirectories(dataPrefs.getBinDirectory());
    replayDataDirectory = Files.createDirectories(dataPrefs.getReplayDataDirectory());
    replayBinDirectory = Files.createDirectories(dataPrefs.getReplayBinDirectory());
    when(gameBinariesUpdateTaskFactory.getObject()).thenReturn(gameBinariesUpdateTask);
    when(taskService.submitTask(gameBinariesUpdateTask)).thenReturn(gameBinariesUpdateTask);
    when(gameBinariesUpdateTask.getFuture()).thenReturn(CompletableFuture.completedFuture(null));
    when(simpleHttpFeaturedModUpdater.updateMod(any(FeaturedModBean.class), any(), eq(false))).thenAnswer(invocation -> {
      FeaturedModBean featuredMod = invocation.getArgument(0, FeaturedModBean.class);
      Path initFile = binDirectory.resolve(String.format("init_%s", featuredMod.getTechnicalName()));
      Files.createFile(initFile);
      int version = Objects.requireNonNullElse(invocation.getArgument(1, Integer.class), Integer.MAX_VALUE);
      return CompletableFuture.completedFuture(new PatchResult(new ComparableVersion(String.valueOf(version)), initFile));
    });
    when(simpleHttpFeaturedModUpdater.updateMod(any(FeaturedModBean.class), any(), eq(true))).thenAnswer(invocation -> {
      FeaturedModBean featuredMod = invocation.getArgument(0, FeaturedModBean.class);
      Path initFile = replayBinDirectory.resolve(String.format("init_%s", featuredMod.getTechnicalName()));
      Files.createFile(initFile);
      int version = Objects.requireNonNullElse(invocation.getArgument(1, Integer.class), Integer.MAX_VALUE);
      return CompletableFuture.completedFuture(new PatchResult(new ComparableVersion(String.valueOf(version)), initFile));
    });
  }

  @Test
  public void noUpdatersTest() throws Exception {
    // We need to get rid of the injected updater again
    instance.setFeaturedModUpdater(null);

    CompletionException exception = assertThrows(CompletionException.class, () -> instance.update(FeaturedModBeanBuilder.create().defaultValues().get(), Set.of(), Map.of(), 0, false).join());
    assertEquals(UnsupportedOperationException.class, exception.getCause().getClass());
    assertFalse(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertFalse(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
  }

  @Test
  public void badChecksumTest() throws Exception {
    when(simpleHttpFeaturedModUpdater.updateMod(any(FeaturedModBean.class), any(), anyBoolean()))
        .thenAnswer(invocation -> CompletableFuture.failedFuture(new ChecksumMismatchException(new URL("http://google.com"), "asd", "qwe")));

    CompletionException exception = assertThrows(CompletionException.class, () -> instance.update(FeaturedModBeanBuilder.create().defaultValues().get(), Set.of(), Map.of(), 0, false).join());
    assertEquals(ChecksumMismatchException.class, exception.getCause().getClass());
    assertFalse(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertFalse(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
  }

  @Test
  public void nonBaseModUpdateTestEmptyVersions() throws Exception {
    FeaturedModBean baseMod = FeaturedModBeanBuilder.create().defaultValues().get();
    when(modService.getFeaturedMod(FAF.getTechnicalName())).thenReturn(Mono.just(baseMod));
    String technicalName = "Test_Mod";
    FeaturedModBean updatedMod = FeaturedModBeanBuilder.create().defaultValues().technicalName(technicalName).get();
    when(modService.getFeaturedMod(technicalName)).thenReturn(Mono.just(updatedMod));

    instance.update(updatedMod, Set.of(), Map.of(), 0, false).join();

    verify(taskService).submitTask(gameBinariesUpdateTask);
    verify(gameBinariesUpdateTask).setVersion(new ComparableVersion(String.valueOf(0)));
    verify(modService).getFeaturedMod(FAF.getTechnicalName());
    verify(simpleHttpFeaturedModUpdater).updateMod(baseMod, 0, false);
    verify(simpleHttpFeaturedModUpdater).updateMod(updatedMod, null, false);
    assertTrue(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertTrue(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
    assertTrue(Files.exists(binDirectory.resolve(String.format("init_%s", technicalName))));
  }

  @Test
  public void nonBaseModUpdateTest() throws Exception {
    FeaturedModBean baseMod = FeaturedModBeanBuilder.create().defaultValues().get();
    when(modService.getFeaturedMod(FAF.getTechnicalName())).thenReturn(Mono.just(baseMod));
    String technicalName = "Test_Mod";
    FeaturedModBean updatedMod = FeaturedModBeanBuilder.create().defaultValues().technicalName(technicalName).get();
    when(modService.getFeaturedMod(technicalName)).thenReturn(Mono.just(updatedMod));

    instance.update(updatedMod, Set.of(), Map.of("1", 100), 0, false).join();

    verify(taskService).submitTask(gameBinariesUpdateTask);
    verify(gameBinariesUpdateTask).setVersion(new ComparableVersion(String.valueOf(0)));
    verify(modService).getFeaturedMod(FAF.getTechnicalName());
    verify(simpleHttpFeaturedModUpdater).updateMod(baseMod, 0, false);
    verify(simpleHttpFeaturedModUpdater).updateMod(updatedMod, 100, false);
    assertTrue(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertTrue(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
    assertTrue(Files.exists(binDirectory.resolve(String.format("init_%s", technicalName))));
  }

  @Test
  public void nonBaseModUpdateWithCacheDirectoryTest() throws Exception {
    FeaturedModBean baseMod = FeaturedModBeanBuilder.create().defaultValues().get();
    when(modService.getFeaturedMod(FAF.getTechnicalName())).thenReturn(Mono.just(baseMod));
    String technicalName = "Test_Mod";
    FeaturedModBean updatedMod = FeaturedModBeanBuilder.create().defaultValues().technicalName(technicalName).get();
    when(modService.getFeaturedMod(technicalName)).thenReturn(Mono.just(updatedMod));

    instance.update(updatedMod, Set.of(), Map.of("1", 100), 0, true).join();

    verify(gameBinariesUpdateTaskFactory).getObject();
    verify(taskService).submitTask(gameBinariesUpdateTask);
    verify(gameBinariesUpdateTask).setVersion(new ComparableVersion(String.valueOf(0)));
    verify(modService).getFeaturedMod(FAF.getTechnicalName());
    verify(simpleHttpFeaturedModUpdater).updateMod(baseMod, 0, true);
    verify(simpleHttpFeaturedModUpdater).updateMod(updatedMod, 100, true);
    assertTrue(Files.exists(replayDataDirectory.resolve("fa_path.lua")));
    assertTrue(Files.exists(replayBinDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
    assertTrue(Files.exists(replayBinDirectory.resolve(String.format("init_%s", technicalName))));
  }

  @Test
  public void nonBaseModUpdateTestWithNulls() throws Exception {
    FeaturedModBean baseMod = FeaturedModBeanBuilder.create().defaultValues().get();
    when(modService.getFeaturedMod(FAF.getTechnicalName())).thenReturn(Mono.just(baseMod));
    String technicalName = "Test_Mod";
    FeaturedModBean updatedMod = FeaturedModBeanBuilder.create().defaultValues().id(100).technicalName(technicalName).get();
    when(modService.getFeaturedMod(technicalName)).thenReturn(Mono.just(updatedMod));

    instance.update(updatedMod, Set.of(), null, null, false).join();

    verify(taskService).submitTask(gameBinariesUpdateTask);
    verify(gameBinariesUpdateTask).setVersion(new ComparableVersion(String.valueOf(Integer.MAX_VALUE)));
    verify(modService).getFeaturedMod(FAF.getTechnicalName());
    verify(simpleHttpFeaturedModUpdater).updateMod(baseMod, null, false);
    verify(simpleHttpFeaturedModUpdater).updateMod(updatedMod, null, false);
    assertTrue(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertTrue(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
    assertTrue(Files.exists(binDirectory.resolve(String.format("init_%s", technicalName))));
  }

  @Test
  public void baseModUpdateTest() throws Exception {
    String technicalName = FAF.getTechnicalName();
    FeaturedModBean updatedMod = FeaturedModBeanBuilder.create().defaultValues().technicalName(technicalName).get();
    when(modService.getFeaturedMod(technicalName)).thenReturn(Mono.just(updatedMod));

    instance.update(updatedMod, Set.of(), Map.of(), 0, false).join();

    verify(taskService).submitTask(gameBinariesUpdateTask);
    verify(gameBinariesUpdateTask).setVersion(new ComparableVersion(String.valueOf(0)));
    verify(simpleHttpFeaturedModUpdater).updateMod(updatedMod, 0, false);
    assertTrue(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertTrue(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
    assertTrue(Files.exists(binDirectory.resolve(String.format("init_%s", technicalName))));
  }

  @Test
  public void baseModUpdateWithCacheDirectoryTest() throws Exception {
    String technicalName = FAF.getTechnicalName();
    FeaturedModBean updatedMod = FeaturedModBeanBuilder.create().defaultValues().technicalName(technicalName).get();
    when(modService.getFeaturedMod(technicalName)).thenReturn(Mono.just(updatedMod));

    instance.update(updatedMod, Set.of(), Map.of(), 0, true).join();

    verify(gameBinariesUpdateTaskFactory).getObject();
    verify(taskService).submitTask(gameBinariesUpdateTask);
    verify(gameBinariesUpdateTask).setVersion(new ComparableVersion(String.valueOf(0)));
    verify(simpleHttpFeaturedModUpdater).updateMod(updatedMod, 0, true);
    assertTrue(Files.exists(replayDataDirectory.resolve("fa_path.lua")));
    assertTrue(Files.exists(replayBinDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
    assertTrue(Files.exists(replayBinDirectory.resolve(String.format("init_%s", technicalName))));
  }

  @Test
  public void baseModUpdateWithSimModsInstalledTest() throws Exception {
    String technicalName = FAF.getTechnicalName();
    FeaturedModBean updatedMod = FeaturedModBeanBuilder.create().defaultValues().technicalName(technicalName).get();
    when(modService.getFeaturedMod(technicalName)).thenReturn(Mono.just(updatedMod));
    String modUID = "abc";
    when(modService.isInstalled(modUID)).thenReturn(true);

    instance.update(updatedMod, Set.of(modUID), Map.of(), 0, false).join();

    verify(modService, never()).downloadAndInstallMod(modUID);
    verify(taskService).submitTask(gameBinariesUpdateTask);
    verify(gameBinariesUpdateTask).setVersion(new ComparableVersion(String.valueOf(0)));
    verify(simpleHttpFeaturedModUpdater).updateMod(updatedMod, 0, false);
    assertTrue(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertTrue(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
    assertTrue(Files.exists(binDirectory.resolve(String.format("init_%s", technicalName))));
  }

  @Test
  public void baseModUpdateWithSimModsNotInstalledTest() throws Exception {
    String technicalName = FAF.getTechnicalName();
    FeaturedModBean updatedMod = FeaturedModBeanBuilder.create().defaultValues().technicalName(technicalName).get();
    when(modService.getFeaturedMod(technicalName)).thenReturn(Mono.just(updatedMod));
    String modUID = "abc";
    when(modService.isInstalled(modUID)).thenReturn(false);
    when(modService.downloadAndInstallMod(modUID)).thenReturn(CompletableFuture.completedFuture(null));

    instance.update(updatedMod, Set.of(modUID), Map.of(), 0, false).join();

    verify(modService).downloadAndInstallMod(modUID);
    verify(taskService).submitTask(gameBinariesUpdateTask);
    verify(gameBinariesUpdateTask).setVersion(new ComparableVersion(String.valueOf(0)));
    verify(simpleHttpFeaturedModUpdater).updateMod(updatedMod, 0, false);
    assertTrue(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertTrue(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
    assertTrue(Files.exists(binDirectory.resolve(String.format("init_%s", technicalName))));
  }

  @Test
  public void testCheckFaPathFileContent() throws Exception {
    String gameType = FAF.getTechnicalName();
    Integer gameVersion = 3711;
    String clientVersion = Version.getCurrentVersion();
    FeaturedModBean updatedMod = FeaturedModBeanBuilder.create().defaultValues().technicalName(gameType).get();
    when(modService.getFeaturedMod(gameType)).thenReturn(Mono.just(updatedMod));

    instance.update(updatedMod, Set.of(), Map.of(), gameVersion, false).join();

    String content = Files.readString(fafDataDirectory.resolve("fa_path.lua"));
    assertTrue(content.contains("GameType = \"" + gameType + "\""));
    assertTrue(content.contains("GameVersion = \"" + gameVersion + "\""));
    assertTrue(content.contains("ClientVersion = \"" + clientVersion + "\""));
  }

}
