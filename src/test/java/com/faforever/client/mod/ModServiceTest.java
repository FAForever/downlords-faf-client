package com.faforever.client.mod;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.domain.api.ModType;
import com.faforever.client.domain.api.ModVersion;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GamePrefsService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mapstruct.ModMapper;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.remote.AssetService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.ApiTestUtil;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.util.FileSizeReader;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.client.vault.search.SearchController.SortOrder;
import com.faforever.commons.api.dto.Mod;
import com.faforever.commons.api.elide.ElideEntity;
import com.faforever.commons.io.ByteCopier;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.hamcrest.Matchers;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.core.io.ClassPathResource;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModServiceTest extends PlatformTest {

  public static final String BLACK_OPS_UNLEASHED_DIRECTORY_NAME = "BlackOpsUnleashed";
  private static final ClassPathResource BLACKOPS_UNLEASHED_MOD_INFO = new ClassPathResource(
      "/mods/blackops_unleashed_mod_info.lua");
  private static final long TIMEOUT = 5000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

  @TempDir
  public Path tempDirectory;

  @Mock
  private GamePrefsService gamePrefsService;
  @Mock
  private ThemeService themeService;
  @Mock
  private TaskService taskService;
  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private AssetService assetService;
  @Mock
  private PlatformService platformService;
  @Mock
  private FileSizeReader fileSizeReader;
  @Mock
  private ObjectFactory<ModUploadTask> modUploadTaskFactory;
  @Mock
  private ObjectFactory<DownloadModTask> downloadModTaskFactory;
  @Mock
  private ObjectFactory<UninstallModTask> uninstallModTaskFactory;
  @Spy
  private ModMapper modMapper = Mappers.getMapper(ModMapper.class);
  @Spy
  private ForgedAlliancePrefs forgedAlliancePrefs;
  @Spy
  private DataPrefs dataPrefs;
  @Spy
  private Preferences preferences;

  private ModService instance;

  private Path modsDirectory;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new ModService(fafApiAccessor, gamePrefsService, taskService, notificationService, i18n, platformService,
                              assetService,
                              themeService, fileSizeReader, modMapper, forgedAlliancePrefs, preferences,
                              modUploadTaskFactory, downloadModTaskFactory, uninstallModTaskFactory,
                              fxApplicationThreadExecutor);
    MapperSetup.injectMappers(modMapper);
    modsDirectory = tempDirectory.resolve("mods");
    Files.createDirectories(modsDirectory);
    forgedAlliancePrefs.setVaultBaseDirectory(tempDirectory);

    doAnswer(invocation -> {
      CompletableTask<?> task = invocation.getArgument(0);
      WaitForAsyncUtils.asyncFx(task);
      task.getFuture().join();
      return task;
    }).when(taskService).submitTask(any());

    copyMod(BLACK_OPS_UNLEASHED_DIRECTORY_NAME, BLACKOPS_UNLEASHED_MOD_INFO);

    instance.afterPropertiesSet();
  }

  private void copyMod(String directoryName, ClassPathResource classPathResource) throws IOException {
    Path targetDir = Files.createDirectories(modsDirectory.resolve(directoryName));

    try (InputStream inputStream = classPathResource.getInputStream(); OutputStream outputStream = Files.newOutputStream(
        targetDir.resolve("mod_info.lua"))) {
      ByteCopier.from(inputStream).to(outputStream).copy();
    }
  }

  @Test
  public void testPostConstructLoadInstalledMods() {
    ObservableList<ModVersion> installedModVersions = instance.getInstalledMods();

    assertThat(installedModVersions.size(), is(1));
  }

  @Test
  public void testDownloadAndInstallModWithProperties() throws Exception {
    assertThat(instance.getInstalledMods().size(), is(1));

    DownloadModTask task = stubDownloadModTask();
    task.getFuture().complete(null);

    when(downloadModTaskFactory.getObject()).thenReturn(task);

    StringProperty stringProperty = new SimpleStringProperty();
    DoubleProperty doubleProperty = new SimpleDoubleProperty();

    StepVerifier.create(
                    instance.downloadIfNecessary(Instancio.create(ModVersion.class), doubleProperty, stringProperty))
                .verifyComplete();

    assertThat(stringProperty.isBound(), is(true));
    assertThat(doubleProperty.isBound(), is(true));
  }

  @Test
  public void testDownloadAndInstallModInfoBeanWithProperties() throws Exception {
    assertThat(instance.getInstalledMods().size(), is(1));

    DownloadModTask task = stubDownloadModTask();
    task.getFuture().complete(null);

    when(downloadModTaskFactory.getObject()).thenReturn(task);

    assertThat(instance.getInstalledMods().size(), is(1));

    StringProperty stringProperty = new SimpleStringProperty();
    DoubleProperty doubleProperty = new SimpleDoubleProperty();

    ModVersion modVersion = Instancio.create(ModVersion.class);
    StepVerifier.create(instance.downloadIfNecessary(modVersion, doubleProperty, stringProperty)).verifyComplete();

    assertThat(stringProperty.isBound(), is(true));
    assertThat(doubleProperty.isBound(), is(true));
  }

  @Test
  public void testExtractModInfo() throws Exception {
    WaitForAsyncUtils.waitForFxEvents();

    ArrayList<ModVersion> installedMods = new ArrayList<>(instance.getInstalledMods());
    installedMods.sort(Comparator.comparing(modVersionBean -> modVersionBean.mod().displayName()));

    ModVersion modVersion = installedMods.getFirst();

    assertThat(modVersion.mod().displayName(), is("BlackOps Unleashed"));
    assertThat(modVersion.version(), is(new ComparableVersion("8")));
    assertThat(modVersion.mod().author(), is("Lt_hawkeye"));
    assertThat(modVersion.description(),
               is("Version 5.2. BlackOps Unleased Unitpack contains several new units and game changes. Have fun"));
    assertThat(modVersion.id(), is(nullValue()));
    assertThat(modVersion.uid(), is("9e8ea941-c306-4751-b367-a11000000502"));
    assertThat(modVersion.modType(), equalTo(ModType.SIM));
  }

  @Test
  public void testLoadInstalledModWithoutModInfo() throws Exception {
    Files.createDirectories(modsDirectory.resolve("foobar"));

    assertThat(instance.getInstalledMods().size(), is(1));
  }

  @Test
  public void testIsModInstalled() {
    assertThat(instance.isInstalled("9e8ea941-c306-4751-b367-a11000000502"), is(true));
    assertThat(instance.isInstalled("9e8ea941-c306-4751-b367-f00000000005"), is(false));
  }

  @Test
  public void testUninstallMod() {
    prepareUninstallModTask(instance.getInstalledMods().getFirst());

    instance.uninstallMod(instance.getInstalledMods().getFirst());

    verify(taskService).submitTask(any(UninstallModTask.class));
  }

  @Test
  public void testGetPathForMod() {
    assertThat(instance.getInstalledMods(), hasSize(1));

    Path actual = instance.getPathForMod(instance.getInstalledMods().getFirst());

    Path expected = modsDirectory.resolve(BLACK_OPS_UNLEASHED_DIRECTORY_NAME);
    assertThat(actual, is(expected));
  }

  @Test
  public void testGetPathForModUnknownModReturnsNull() {
    assertThat(instance.getInstalledMods(), hasSize(1));
    assertThat(instance.getPathForMod(Instancio.create(ModVersion.class)),
               Matchers.nullValue());
  }

  @Test
  public void testUploadMod() {
    ModUploadTask modUploadTask = mock(ModUploadTask.class);
    when(modUploadTask.getFuture()).thenReturn(CompletableFuture.completedFuture(null));

    when(modUploadTaskFactory.getObject()).thenReturn(modUploadTask);

    Path modPath = Path.of(".");

    instance.uploadMod(modPath);

    verify(modUploadTask).setModPath(modPath);
    verify(taskService).submitTask(modUploadTask);
  }

  @Test
  public void testLoadThumbnail() throws MalformedURLException {
    ModVersion modVersion = Instancio.create(ModVersion.class);
    instance.loadThumbnail(modVersion);
    verify(assetService).loadAndCacheImage(eq(modVersion.thumbnailUrl()), eq(Path.of("mods")), any());
  }

  @Test
  public void testUpdateModsWithUpdatedMod() throws IOException, ExecutionException, InterruptedException {
    ModVersion modVersion = Instancio.create(ModVersion.class);

    com.faforever.commons.api.dto.ModVersion dto = modMapper.map(modVersion, new CycleAvoidingMappingContext());

    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(dto));

    StepVerifier.create(instance.updateAndActivateModVersions(List.of(modVersion)))
                .expectNext(List.of(modVersion))
                .verifyComplete();

    verify(taskService, times(0)).submitTask(any(DownloadModTask.class));
  }

  @Test
  public void testUpdateModsWithAutoUpdateTurnedOff() throws IOException, ExecutionException, InterruptedException {
    ModVersion modVersion = Instancio.create(ModVersion.class);
    preferences.setMapAndModAutoUpdate(false);

    StepVerifier.create(instance.updateAndActivateModVersions(List.of(modVersion)))
                .expectNext(List.of(modVersion))
                .verifyComplete();

    verify(fafApiAccessor, times(0)).getMany(any());
  }

  @Test
  @Disabled("flaky")
  public void testUpdateModsWithOutdatedMod() throws IOException, ExecutionException, InterruptedException {
    ModVersion latestVersion = Instancio.create(ModVersion.class);
    ModVersion modVersion = Instancio.create(ModVersion.class);

    com.faforever.commons.api.dto.ModVersion dto = modMapper.map(modVersion, new CycleAvoidingMappingContext());

    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(dto));

    when(downloadModTaskFactory.getObject()).thenReturn(stubDownloadModTask());

    StepVerifier.create(instance.updateAndActivateModVersions(List.of(modVersion)))
                .expectNext(List.of(latestVersion))
                .verifyComplete();

    verify(taskService, times(2)).submitTask(any());
  }

  private DownloadModTask stubDownloadModTask() {
    return new DownloadModTask(i18n, dataPrefs, forgedAlliancePrefs) {
      @Override
      protected Void call() {
        return null;
      }
    };
  }

  @Test
  public void testGetRecommendedMods() {
    ModVersion modVersion = Instancio.create(ModVersion.class);
    Mod mod = modMapper.map(modVersion.mod(), new CycleAvoidingMappingContext());
    mod.setLatestVersion(modMapper.map(modVersion, new CycleAvoidingMappingContext()));
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(List.of(mod), 1);
    when(fafApiAccessor.getManyWithPageCount(any(), anyString())).thenReturn(resultMono);
    StepVerifier.create(instance.getRecommendedModsWithPageCount(10, 0)).expectNextCount(1)
                .verifyComplete();
    verify(fafApiAccessor).getManyWithPageCount(
        argThat(ElideMatchers.hasFilter(qBuilder().bool("recommended").isTrue())), anyString());
  }

  @Test
  public void testFindByQuery() throws Exception {
    ModVersion modVersion = Instancio.create(ModVersion.class);
    Mod mod = modMapper.map(modVersion.mod(), new CycleAvoidingMappingContext());
    mod.setLatestVersion(modMapper.map(modVersion, new CycleAvoidingMappingContext()));
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(List.of(mod), 1);
    when(fafApiAccessor.getManyWithPageCount(any(), anyString())).thenReturn(resultMono);

    SearchConfig searchConfig = new SearchConfig(new SortConfig("testSort", SortOrder.ASC), "testQuery");
    StepVerifier.create(instance.findByQueryWithPageCount(searchConfig, 10, 1)).expectNextCount(1)
                .verifyComplete();

    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("testSort", true)), eq("testQuery"));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)), eq("testQuery"));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)), eq("testQuery"));
  }

  @Test
  public void testGetHighestRated() {
    ModVersion modVersion = Instancio.create(ModVersion.class);
    Mod mod = modMapper.map(modVersion.mod(), new CycleAvoidingMappingContext());
    mod.setLatestVersion(modMapper.map(modVersion, new CycleAvoidingMappingContext()));
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(List.of(mod), 1);
    when(fafApiAccessor.getManyWithPageCount(any(), anyString())).thenReturn(resultMono);
    StepVerifier.create(instance.getHighestRatedModsWithPageCount(10, 1)).expectNextCount(1)
                .verifyComplete();
    verify(fafApiAccessor).getManyWithPageCount(
        argThat(ElideMatchers.hasFilter(qBuilder().string("latestVersion.type").eq("SIM"))), anyString());
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("reviewsSummary.lowerBound", false)),
                                                anyString());
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)), anyString());
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)), anyString());
  }

  @Test
  public void testGetHighestRatedUI() {
    ModVersion modVersion = Instancio.create(ModVersion.class);
    Mod mod = modMapper.map(modVersion.mod(), new CycleAvoidingMappingContext());
    mod.setLatestVersion(modMapper.map(modVersion, new CycleAvoidingMappingContext()));
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(List.of(mod), 1);
    when(fafApiAccessor.getManyWithPageCount(any(), anyString())).thenReturn(resultMono);
    StepVerifier.create(instance.getHighestRatedUiModsWithPageCount(10, 1)).expectNextCount(1)
                .verifyComplete();
    verify(fafApiAccessor).getManyWithPageCount(
        argThat(ElideMatchers.hasFilter(qBuilder().string("latestVersion.type").eq("UI"))), anyString());
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("reviewsSummary.lowerBound", false)),
                                                anyString());
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)), anyString());
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)), anyString());
  }

  @Test
  public void testGetNewest() {
    ModVersion modVersion = Instancio.create(ModVersion.class);
    Mod mod = modMapper.map(modVersion.mod(), new CycleAvoidingMappingContext());
    mod.setLatestVersion(modMapper.map(modVersion, new CycleAvoidingMappingContext()));
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(List.of(mod), 1);
    when(fafApiAccessor.getManyWithPageCount(any(), anyString())).thenReturn(resultMono);
    StepVerifier.create(instance.getNewestModsWithPageCount(10, 1)).expectNextCount(1)
                .verifyComplete();
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("latestVersion.createTime", false)),
                                                anyString());
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)), anyString());
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)), anyString());
  }

  private void prepareUninstallModTask(ModVersion modToDelete) {
    UninstallModTask task = new UninstallModTask(instance);
    task.setMod(modToDelete);
    when(uninstallModTaskFactory.getObject()).thenReturn(task);
  }
}
