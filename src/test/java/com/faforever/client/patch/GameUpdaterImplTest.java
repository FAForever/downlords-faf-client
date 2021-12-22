package com.faforever.client.patch;

import com.faforever.client.builders.FeaturedModBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.ServiceTest;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameUpdaterImplTest extends ServiceTest {

  private GameUpdaterImpl instance;
  @TempDir
  public Path tempDir;
  @Mock
  private ModService modService;
  @Mock
  private SimpleHttpFeaturedModUpdater simpleHttpFeaturedModUpdater;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private TaskService taskService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private GameBinariesUpdateTaskImpl gameBinariesUpdateTask;

  private Path fafDataDirectory;
  private Path binDirectory;

  @BeforeEach
  public void setUp() throws Exception {
    fafDataDirectory = tempDir.resolve("faf_temp_data");
    binDirectory = fafDataDirectory.resolve("bin");
    Files.createDirectories(fafDataDirectory);
    Files.createDirectories(binDirectory);
    Preferences preferences = PreferencesBuilder.create().defaultValues().get();
    when(preferencesService.getFafDataDirectory()).thenReturn(fafDataDirectory);
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(applicationContext.getBean(GameBinariesUpdateTaskImpl.class)).thenReturn(gameBinariesUpdateTask);
    when(taskService.submitTask(gameBinariesUpdateTask)).thenReturn(gameBinariesUpdateTask);
    when(gameBinariesUpdateTask.getFuture()).thenReturn(CompletableFuture.completedFuture(null));
    when(simpleHttpFeaturedModUpdater.updateMod(any(FeaturedModBean.class), any())).thenAnswer(invocation -> {
      FeaturedModBean featuredMod = invocation.getArgument(0, FeaturedModBean.class);
      Path initFile = binDirectory.resolve(String.format("init_%s", featuredMod.getTechnicalName()));
      Files.createFile(initFile);
      int version = Objects.requireNonNullElse(invocation.getArgument(1, Integer.class), Integer.MAX_VALUE);
      return CompletableFuture.completedFuture(new PatchResult(new ComparableVersion(String.valueOf(version)), initFile));
    });
    instance = new GameUpdaterImpl(modService, applicationContext, taskService, preferencesService, notificationService);
  }

  @Test
  public void noUpdatersTest() throws Exception {
    CompletionException exception = assertThrows(CompletionException.class, () -> instance.update(FeaturedModBeanBuilder.create().defaultValues().get(), 0, Set.of()).join());
    assertEquals(UnsupportedOperationException.class, exception.getCause().getClass());
    assertFalse(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertFalse(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
  }

  @Test
  public void noCompatibleUpdatersTest() throws Exception {
    when(simpleHttpFeaturedModUpdater.canUpdate(any())).thenReturn(false);
    instance.addFeaturedModUpdater(simpleHttpFeaturedModUpdater);
    CompletionException exception = assertThrows(CompletionException.class, () -> instance.update(FeaturedModBeanBuilder.create().defaultValues().get(), 0, Set.of()).join());
    assertEquals(UnsupportedOperationException.class, exception.getCause().getClass());
    assertFalse(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertFalse(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
  }

  @Test
  public void nonBaseModUpdateTest() throws Exception {
    when(simpleHttpFeaturedModUpdater.canUpdate(any())).thenReturn(true);
    FeaturedModBean baseMod = FeaturedModBeanBuilder.create().defaultValues().get();
    when(modService.getFeaturedMod(FAF.getTechnicalName())).thenReturn(CompletableFuture.completedFuture(baseMod));
    String technicalName = "Test_Mod";
    FeaturedModBean updatedMod = FeaturedModBeanBuilder.create().defaultValues().technicalName(technicalName).get();
    when(modService.getFeaturedMod(technicalName)).thenReturn(CompletableFuture.completedFuture(updatedMod));

    instance.addFeaturedModUpdater(simpleHttpFeaturedModUpdater);
    instance.update(updatedMod, 0, Set.of()).join();

    verify(applicationContext).getBean(GameBinariesUpdateTaskImpl.class);
    verify(taskService).submitTask(gameBinariesUpdateTask);
    verify(gameBinariesUpdateTask).setVersion(new ComparableVersion(String.valueOf(Integer.MAX_VALUE)));
    verify(modService).getFeaturedMod(FAF.getTechnicalName());
    verify(simpleHttpFeaturedModUpdater).updateMod(baseMod, null);
    verify(simpleHttpFeaturedModUpdater).updateMod(updatedMod, 0);
    assertTrue(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertTrue(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
    assertTrue(Files.exists(binDirectory.resolve(String.format("init_%s", technicalName))));
  }

  @Test
  public void baseModUpdateTest() throws Exception {
    when(simpleHttpFeaturedModUpdater.canUpdate(any())).thenReturn(true);
    String technicalName = FAF.getTechnicalName();
    FeaturedModBean updatedMod = FeaturedModBeanBuilder.create().defaultValues().technicalName(technicalName).get();
    when(modService.getFeaturedMod(technicalName)).thenReturn(CompletableFuture.completedFuture(updatedMod));

    instance.addFeaturedModUpdater(simpleHttpFeaturedModUpdater);
    instance.update(updatedMod, 0, Set.of()).join();

    verify(applicationContext).getBean(GameBinariesUpdateTaskImpl.class);
    verify(taskService).submitTask(gameBinariesUpdateTask);
    verify(gameBinariesUpdateTask).setVersion(new ComparableVersion(String.valueOf(0)));
    verify(simpleHttpFeaturedModUpdater).updateMod(updatedMod, 0);
    assertTrue(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertTrue(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
    assertTrue(Files.exists(binDirectory.resolve(String.format("init_%s", technicalName))));
  }

  @Test
  public void baseModUpdateWithSimModsInstalledTest() throws Exception {
    when(simpleHttpFeaturedModUpdater.canUpdate(any())).thenReturn(true);
    String technicalName = FAF.getTechnicalName();
    FeaturedModBean updatedMod = FeaturedModBeanBuilder.create().defaultValues().technicalName(technicalName).get();
    when(modService.getFeaturedMod(technicalName)).thenReturn(CompletableFuture.completedFuture(updatedMod));
    String modUID = "abc";
    when(modService.isModInstalled(modUID)).thenReturn(true);

    instance.addFeaturedModUpdater(simpleHttpFeaturedModUpdater);
    instance.update(updatedMod, 0, Set.of(modUID)).join();

    verify(modService, never()).downloadAndInstallMod(modUID);
    verify(applicationContext).getBean(GameBinariesUpdateTaskImpl.class);
    verify(taskService).submitTask(gameBinariesUpdateTask);
    verify(gameBinariesUpdateTask).setVersion(new ComparableVersion(String.valueOf(0)));
    verify(simpleHttpFeaturedModUpdater).updateMod(updatedMod, 0);
    assertTrue(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertTrue(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
    assertTrue(Files.exists(binDirectory.resolve(String.format("init_%s", technicalName))));
  }

  @Test
  public void baseModUpdateWithSimModsNotInstalledTest() throws Exception {
    when(simpleHttpFeaturedModUpdater.canUpdate(any())).thenReturn(true);
    String technicalName = FAF.getTechnicalName();
    FeaturedModBean updatedMod = FeaturedModBeanBuilder.create().defaultValues().technicalName(technicalName).get();
    when(modService.getFeaturedMod(technicalName)).thenReturn(CompletableFuture.completedFuture(updatedMod));
    String modUID = "abc";
    when(modService.isModInstalled(modUID)).thenReturn(false);
    when(modService.downloadAndInstallMod(modUID)).thenReturn(CompletableFuture.completedFuture(null));

    instance.addFeaturedModUpdater(simpleHttpFeaturedModUpdater);
    instance.update(updatedMod, 0, Set.of(modUID)).join();

    verify(modService).downloadAndInstallMod(modUID);
    verify(applicationContext).getBean(GameBinariesUpdateTaskImpl.class);
    verify(taskService).submitTask(gameBinariesUpdateTask);
    verify(gameBinariesUpdateTask).setVersion(new ComparableVersion(String.valueOf(0)));
    verify(simpleHttpFeaturedModUpdater).updateMod(updatedMod, 0);
    assertTrue(Files.exists(fafDataDirectory.resolve("fa_path.lua")));
    assertTrue(Files.exists(binDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)));
    assertTrue(Files.exists(binDirectory.resolve(String.format("init_%s", technicalName))));
  }



}
