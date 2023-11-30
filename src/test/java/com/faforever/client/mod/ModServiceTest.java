package com.faforever.client.mod;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.FeaturedModBeanBuilder;
import com.faforever.client.builders.ModBeanBuilder;
import com.faforever.client.builders.ModVersionBeanBuilder;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.ModBean;
import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.domain.ModVersionBean.ModType;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mapstruct.ModMapper;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.remote.AssetService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.ApiTestUtil;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.FileSizeReader;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.client.vault.search.SearchController.SortOrder;
import com.faforever.commons.api.dto.FeaturedModFile;
import com.faforever.commons.api.dto.ModVersion;
import com.faforever.commons.api.elide.ElideEntity;
import com.faforever.commons.io.ByteCopier;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.core.io.ClassPathResource;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
  private static final ClassPathResource BLACKOPS_SUPPORT_MOD_INFO = new ClassPathResource(
      "/mods/blackops_support_mod_info.lua");
  private static final ClassPathResource BLACKOPS_UNLEASHED_MOD_INFO = new ClassPathResource(
      "/mods/blackops_unleashed_mod_info.lua");
  private static final ClassPathResource ECO_MANAGER_MOD_INFO = new ClassPathResource("/mods/eco_manager_mod_info.lua");
  private static final long TIMEOUT = 5000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

  @TempDir
  public Path tempDirectory;

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
  private ImageViewHelper imageViewHelper;
  @Mock
  private ObjectFactory<ModUploadTask> modUploadTaskFactory;
  @Mock
  private ObjectFactory<DownloadModTask> downloadModTaskFactory;
  @Mock
  private ObjectFactory<UninstallModTask> uninstallModTaskFactory;
  @Mock
  private UiService uiService;
  @Mock
  private ThemeService themeService;
  @Spy
  private ModMapper modMapper = Mappers.getMapper(ModMapper.class);
  @Spy
  private ForgedAlliancePrefs forgedAlliancePrefs;
  @Spy
  private DataPrefs dataPrefs;
  @Spy
  private Preferences preferences;
  @Spy
  private NotificationPrefs notificationPrefs;

  private Path modsDirectory;
  @InjectMocks
  private ModService instance;
  private Path gamePrefsPath;

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(modMapper);
    modsDirectory = tempDirectory.resolve("mods");
    Files.createDirectories(modsDirectory);
    gamePrefsPath = tempDirectory.resolve("game.prefs");

    forgedAlliancePrefs.setPreferencesFile(gamePrefsPath);
    forgedAlliancePrefs.setVaultBaseDirectory(tempDirectory);

    when(fileSizeReader.getFileSize(any())).thenReturn(CompletableFuture.completedFuture(1024));

    when(fafApiAccessor.getMaxPageSize()).thenReturn(100);

    doAnswer(invocation -> {
      CompletableTask<?> task = invocation.getArgument(0);
      WaitForAsyncUtils.asyncFx(task);
      task.getFuture().join();
      return task;
    }).when(taskService).submitTask(any());

    copyMod(BLACK_OPS_UNLEASHED_DIRECTORY_NAME, BLACKOPS_UNLEASHED_MOD_INFO);

    instance = new ModService(fafApiAccessor, taskService, notificationService, i18n, platformService, assetService,
                              uiService, themeService, fileSizeReader, modMapper, forgedAlliancePrefs, preferences,
                              modUploadTaskFactory, downloadModTaskFactory, uninstallModTaskFactory,
                              fxApplicationThreadExecutor);

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
    ObservableList<ModVersionBean> installedModVersions = instance.getInstalledMods();

    assertThat(installedModVersions.size(), is(1));
  }

  @Test
  @Disabled("Flaky Test")
  public void testDownloadAndInstallMod() throws Exception {
    assertThat(instance.getInstalledMods().size(), is(1));

    DownloadModTask task = stubDownloadModTask();
    task.getFuture().complete(null);

    when(downloadModTaskFactory.getObject()).thenReturn(task);


    assertThat(instance.getInstalledMods().size(), is(1));

    URL modUrl = new URL("http://example.com/some/mod.zip");


    copyMod("BlackopsSupport", BLACKOPS_SUPPORT_MOD_INFO);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getInstalledMods().size(), is(2));
  }

  @Test
  public void testDownloadAndInstallModWithProperties() throws Exception {
    assertThat(instance.getInstalledMods().size(), is(1));

    DownloadModTask task = stubDownloadModTask();
    task.getFuture().complete(null);

    when(downloadModTaskFactory.getObject()).thenReturn(task);

    URL modUrl = new URL("http://example.com/some/mod.zip");

    StringProperty stringProperty = new SimpleStringProperty();
    DoubleProperty doubleProperty = new SimpleDoubleProperty();

    instance.downloadAndInstallMod(modUrl, doubleProperty, stringProperty)
            .toCompletableFuture()
            .get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(stringProperty.isBound(), is(true));
    assertThat(doubleProperty.isBound(), is(true));
  }

  @Test
  public void testDownloadAndInstallModInfoBeanWithProperties() throws Exception {
    assertThat(instance.getInstalledMods().size(), is(1));

    DownloadModTask task = stubDownloadModTask();
    task.getFuture().complete(null);

    when(downloadModTaskFactory.getObject()).thenReturn(task);

    URL modUrl = new URL("http://example.com/some/modVersion.zip");

    assertThat(instance.getInstalledMods().size(), is(1));

    StringProperty stringProperty = new SimpleStringProperty();
    DoubleProperty doubleProperty = new SimpleDoubleProperty();

    ModVersionBean modVersion = ModVersionBeanBuilder.create().defaultValues().downloadUrl(modUrl).get();
    instance.downloadAndInstallMod(modVersion, doubleProperty, stringProperty)
            .toCompletableFuture()
            .get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(stringProperty.isBound(), is(true));
    assertThat(doubleProperty.isBound(), is(true));
  }

  @Test
  public void testEnableSimModsClean() throws Exception {
    Files.createFile(gamePrefsPath);

    HashSet<String> simMods = new HashSet<>();
    simMods.add("9e8ea941-c306-4751-b367-f00000000005");
    simMods.add("9e8ea941-c306-4751-b367-a11000000502");
    instance.enableSimMods(simMods);

    List<String> lines = Files.readAllLines(gamePrefsPath);

    assertThat(lines, contains("active_mods = {", "    ['9e8ea941-c306-4751-b367-f00000000005'] = true,",
                               "    ['9e8ea941-c306-4751-b367-a11000000502'] = true", "}"));
  }

  @Test
  public void testEnableSimModsModDisableUnselectedMods() throws Exception {
    Iterable<? extends CharSequence> lines = Arrays.asList("active_mods = {",
                                                           "    ['9e8ea941-c306-4751-b367-f00000000005'] = true,",
                                                           "    ['9e8ea941-c306-4751-b367-a11000000502'] = true", "}");
    Files.write(gamePrefsPath, lines);

    HashSet<String> simMods = new HashSet<>();
    simMods.add("9e8ea941-c306-4751-b367-a11000000502");
    instance.enableSimMods(simMods);

    lines = Files.readAllLines(gamePrefsPath);

    assertThat(lines, contains("active_mods = {", "    ['9e8ea941-c306-4751-b367-a11000000502'] = true", "}"));
  }

  @Test
  public void testExtractModInfo() throws Exception {
    WaitForAsyncUtils.waitForFxEvents();

    ArrayList<ModVersionBean> installedMods = new ArrayList<>(instance.getInstalledMods());
    installedMods.sort(Comparator.comparing(modVersionBean -> modVersionBean.getMod().getDisplayName()));

    ModVersionBean modVersion = installedMods.get(0);

    assertThat(modVersion.getMod().getDisplayName(), is("BlackOps Unleashed"));
    assertThat(modVersion.getVersion(), is(new ComparableVersion("8")));
    assertThat(modVersion.getMod().getAuthor(), is("Lt_hawkeye"));
    assertThat(modVersion.getDescription(),
               is("Version 5.2. BlackOps Unleased Unitpack contains several new units and game changes. Have fun"));
    assertThat(modVersion.getImagePath(), is(modsDirectory.resolve("BlackOpsUnleashed/icons/yoda_icon.bmp")));
    assertThat(modVersion.getSelectable(), is(true));
    assertThat(modVersion.getId(), is(nullValue()));
    assertThat(modVersion.getUid(), is("9e8ea941-c306-4751-b367-a11000000502"));
    assertThat(modVersion.getModType(), equalTo(ModType.SIM));
    assertThat(modVersion.getMountPoints(), hasSize(10));
    assertThat(modVersion.getMountPoints().get(3).getFile(), is(Path.of("effects")));
    assertThat(modVersion.getMountPoints().get(3).getMountPoint(), is("/effects"));
    assertThat(modVersion.getHookDirectories(), contains("/blackops"));
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
    prepareUninstallModTask(instance.getInstalledMods().get(0));

    instance.uninstallMod(instance.getInstalledMods().get(0));

    verify(taskService).submitTask(any(UninstallModTask.class));
  }

  @Test
  public void testGetPathForMod() {
    assertThat(instance.getInstalledMods(), hasSize(1));

    Path actual = instance.getPathForMod(instance.getInstalledMods().get(0));

    Path expected = modsDirectory.resolve(BLACK_OPS_UNLEASHED_DIRECTORY_NAME);
    assertThat(actual, is(expected));
  }

  @Test
  public void testGetPathForModUnknownModReturnsNull() {
    assertThat(instance.getInstalledMods(), hasSize(1));
    assertThat(instance.getPathForMod(ModVersionBeanBuilder.create().defaultValues().uid("1").get()),
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
    ModVersionBean modVersion = ModVersionBeanBuilder.create()
                                                     .defaultValues()
                                                     .thumbnailUrl(new URL("http://127.0.0.1:65534/thumbnail.png"))
                                                     .get();
    instance.loadThumbnail(modVersion);
    verify(assetService).loadAndCacheImage(eq(modVersion.getThumbnailUrl()), eq(Path.of("mods")), any());
  }

  @Test
  public void testUpdateModsWithUpdatedMod() throws IOException, ExecutionException, InterruptedException {
    ModVersionBean modVersion = ModVersionBeanBuilder.create().defaultValues().get();
    ModBean mod = new ModBean();
    modVersion.setMod(mod);
    mod.setLatestVersion(modVersion);

    ModVersion dto = modMapper.map(modVersion, new CycleAvoidingMappingContext());

    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(dto));

    Files.createFile(gamePrefsPath);


    Collection<ModVersionBean> modVersions = instance.updateAndActivateModVersions(List.of(modVersion)).get();

    verify(taskService, times(0)).submitTask(any(DownloadModTask.class));

    assertThat(modVersions, contains(modVersion));
  }

  @Test
  public void testUpdateModsWithAutoUpdateTurnedOff() throws IOException, ExecutionException, InterruptedException {
    ModVersionBean modVersion = ModVersionBeanBuilder.create().defaultValues().get();
    preferences.setMapAndModAutoUpdate(false);

    Collection<ModVersionBean> modVersions = instance.updateAndActivateModVersions(List.of(modVersion)).get();

    verify(fafApiAccessor, times(0)).getMany(any());

    assertThat(modVersions, contains(modVersion));
  }

  @Test
  public void testUpdateModsWithOutdatedMod() throws IOException, ExecutionException, InterruptedException {
    ModVersionBean latestVersion = ModVersionBeanBuilder.create().defaultValues().uid("latest").id(100).get();
    ModBean mod = ModBeanBuilder.create().defaultValues().latestVersion(latestVersion).get();
    ModVersionBean modVersion = ModVersionBeanBuilder.create().defaultValues().mod(mod).get();

    ModVersion dto = modMapper.map(modVersion, new CycleAvoidingMappingContext());

    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(dto));

    Files.createFile(gamePrefsPath);

    when(downloadModTaskFactory.getObject()).thenReturn(stubDownloadModTask());

    prepareUninstallModTask(modVersion);

    Collection<ModVersionBean> modVersions = instance.updateAndActivateModVersions(List.of(modVersion)).get();

    verify(taskService, times(2)).submitTask(any());

    assertThat(modVersions, contains(latestVersion));
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
    ModVersionBean modVersionBean = ModVersionBeanBuilder.create().defaultValues().get();
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(
        List.of(modMapper.map(modVersionBean.getMod(), new CycleAvoidingMappingContext())), 1);
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(resultMono);
    List<ModVersionBean> results = instance.getRecommendedModsWithPageCount(10, 0).join().getT1();
    verify(fafApiAccessor).getManyWithPageCount(
        argThat(ElideMatchers.hasFilter(qBuilder().bool("recommended").isTrue())));
    assertThat(results, contains(modVersionBean));
  }

  @Test
  public void testGetFeaturedFiles() {
    FeaturedModBean featuredMod = FeaturedModBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.getMany(eq(FeaturedModFile.class), anyString(), anyInt(), any())).thenReturn(
        Flux.just(new FeaturedModFile()));

    instance.getFeaturedModFiles(featuredMod, 0);
    verify(fafApiAccessor).getMany(eq(FeaturedModFile.class),
                                   eq(String.format("/featuredMods/%s/files/%s", featuredMod.getId(), 0)), eq(100),
                                   any());
  }

  @Test
  public void testGetFeaturedMod() {
    FeaturedModBean featuredMod = FeaturedModBeanBuilder.create().defaultValues().get();
    Flux<ElideEntity> resultFlux = Flux.just(modMapper.map(featuredMod, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    FeaturedModBean result = instance.getFeaturedMod("test").block();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().string("technicalName").eq("test"))));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasSort("order", true)));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasPageSize(1)));
    assertThat(result, is(featuredMod));
  }

  @Test
  public void testFindByQuery() throws Exception {
    ModVersionBean modVersionBean = ModVersionBeanBuilder.create().defaultValues().get();
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(
        List.of(modMapper.map(modVersionBean.getMod(), new CycleAvoidingMappingContext())), 1);
    when(fafApiAccessor.getManyWithPageCount(any(), anyString())).thenReturn(resultMono);

    SearchConfig searchConfig = new SearchConfig(new SortConfig("testSort", SortOrder.ASC), "testQuery");
    List<ModVersionBean> results = instance.findByQueryWithPageCount(searchConfig, 10, 1).join().getT1();

    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("testSort", true)), eq("testQuery"));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)), eq("testQuery"));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)), eq("testQuery"));
    assertThat(results, contains(modVersionBean));
  }

  @Test
  public void testGetHighestRated() {
    ModVersionBean modVersionBean = ModVersionBeanBuilder.create().defaultValues().get();
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(
        List.of(modMapper.map(modVersionBean.getMod(), new CycleAvoidingMappingContext())), 1);
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(resultMono);
    List<ModVersionBean> results = instance.getHighestRatedModsWithPageCount(10, 1).join().getT1();
    verify(fafApiAccessor).getManyWithPageCount(
        argThat(ElideMatchers.hasFilter(qBuilder().string("latestVersion.type").eq("SIM"))));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("reviewsSummary.lowerBound", false)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)));
    assertThat(results, contains(modVersionBean));
  }

  @Test
  public void testGetHighestRatedUI() {
    ModVersionBean modVersionBean = ModVersionBeanBuilder.create().defaultValues().get();
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(
        List.of(modMapper.map(modVersionBean.getMod(), new CycleAvoidingMappingContext())), 1);
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(resultMono);
    List<ModVersionBean> results = instance.getHighestRatedUiModsWithPageCount(10, 1).join().getT1();
    verify(fafApiAccessor).getManyWithPageCount(
        argThat(ElideMatchers.hasFilter(qBuilder().string("latestVersion.type").eq("UI"))));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("reviewsSummary.lowerBound", false)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)));
    assertThat(results, contains(modVersionBean));
  }

  @Test
  public void testGetNewest() {
    ModVersionBean modVersionBean = ModVersionBeanBuilder.create().defaultValues().get();
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(
        List.of(modMapper.map(modVersionBean.getMod(), new CycleAvoidingMappingContext())), 1);
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(resultMono);
    List<ModVersionBean> results = instance.getNewestModsWithPageCount(10, 1).join().getT1();
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("latestVersion.createTime", false)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)));
    assertThat(results, contains(modVersionBean));
  }

  private void prepareUninstallModTask(ModVersionBean modToDelete) {
    UninstallModTask task = new UninstallModTask(instance);
    task.setMod(modToDelete);
    when(uninstallModTaskFactory.getObject()).thenReturn(task);
  }
}
