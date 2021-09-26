package com.faforever.client.mod;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.FeaturedModBeanBuilder;
import com.faforever.client.builders.ModBeanBuilder;
import com.faforever.client.builders.ModVersionBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.ModBean;
import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.domain.ModVersionBean.ModType;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mapstruct.ModMapper;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.ApiTestUtil;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.UITest;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.client.vault.search.SearchController.SortOrder;
import com.faforever.commons.api.dto.FeaturedModFile;
import com.faforever.commons.api.dto.ModVersion;
import com.faforever.commons.io.ByteCopier;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModServiceTest extends UITest {

  public static final String BLACK_OPS_UNLEASHED_DIRECTORY_NAME = "BlackOpsUnleashed";
  private static final ClassPathResource BLACKOPS_SUPPORT_MOD_INFO = new ClassPathResource("/mods/blackops_support_mod_info.lua");
  private static final ClassPathResource BLACKOPS_UNLEASHED_MOD_INFO = new ClassPathResource("/mods/blackops_unleashed_mod_info.lua");
  private static final ClassPathResource ECO_MANAGER_MOD_INFO = new ClassPathResource("/mods/eco_manager_mod_info.lua");
  private static final long TIMEOUT = 5000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

  @TempDir
  public Path faDataDirectory;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private ApplicationContext applicationContext;
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

  private Path modsDirectory;
  private ModService instance;
  private ModMapper modMapper = Mappers.getMapper(ModMapper.class);
  private Path gamePrefsPath;

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(modMapper);
    modsDirectory = faDataDirectory.resolve("mods");
    Files.createDirectories(modsDirectory);
    gamePrefsPath = faDataDirectory.resolve("game.prefs");
    Preferences preferences = PreferencesBuilder.create().defaultValues()
        .forgedAlliancePrefs()
        .preferencesFile(gamePrefsPath)
        .modsDirectory(modsDirectory)
        .then()
        .get();

    instance = new ModService(fafApiAccessor, preferencesService, taskService, applicationContext, notificationService, i18n,
        platformService, assetService, modMapper);

    when(fafApiAccessor.getMaxPageSize()).thenReturn(100);
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(taskService.submitTask(any(CompletableTask.class))).then(invocation -> {
      CompletableTask<?> completableTask = invocation.getArgument(0);
      completableTask.run();
      completableTask.get(2, TimeUnit.SECONDS);
      return completableTask;
    });

    copyMod(BLACK_OPS_UNLEASHED_DIRECTORY_NAME, BLACKOPS_UNLEASHED_MOD_INFO);
    instance.afterPropertiesSet();
  }

  private void copyMod(String directoryName, ClassPathResource classPathResource) throws IOException {
    Path targetDir = Files.createDirectories(modsDirectory.resolve(directoryName));

    try (InputStream inputStream = classPathResource.getInputStream();
         OutputStream outputStream = Files.newOutputStream(targetDir.resolve("mod_info.lua"))) {
      ByteCopier.from(inputStream)
          .to(outputStream)
          .copy();
    }
  }

  @Test
  public void testPostConstructLoadInstalledMods() {
    ObservableList<ModVersionBean> installedModVersions = instance.getInstalledModVersions();

    assertThat(installedModVersions.size(), is(1));
  }

  @Test
  public void testLoadInstalledModsLoadsMods() throws Exception {
    assertThat(instance.getInstalledModVersions().size(), is(1));
    copyMod("BlackopsSupport", BLACKOPS_SUPPORT_MOD_INFO);
    assertThat(instance.getInstalledModVersions().size(), is(1));
    instance.loadInstalledMods();
    assertThat(instance.getInstalledModVersions().size(), is(2));
  }

  @Test
  public void testDownloadAndInstallMod() throws Exception {
    assertThat(instance.getInstalledModVersions().size(), is(1));

    InstallModTask task = stubInstallModTask();
    task.getFuture().complete(null);

    when(applicationContext.getBean(InstallModTask.class)).thenReturn(task);

    URL modUrl = new URL("http://example.com/some/mod.zip");

    copyMod("BlackopsSupport", BLACKOPS_SUPPORT_MOD_INFO);
    assertThat(instance.getInstalledModVersions().size(), is(1));

    instance.downloadAndInstallMod(modUrl).toCompletableFuture().get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(instance.getInstalledModVersions().size(), is(2));
  }

  @Test
  public void testDownloadAndInstallModWithProperties() throws Exception {
    assertThat(instance.getInstalledModVersions().size(), is(1));

    InstallModTask task = stubInstallModTask();
    task.getFuture().complete(null);

    when(applicationContext.getBean(InstallModTask.class)).thenReturn(task);

    URL modUrl = new URL("http://example.com/some/mod.zip");

    copyMod("BlackopsSupport", BLACKOPS_SUPPORT_MOD_INFO);
    assertThat(instance.getInstalledModVersions().size(), is(1));

    StringProperty stringProperty = new SimpleStringProperty();
    DoubleProperty doubleProperty = new SimpleDoubleProperty();

    instance.downloadAndInstallMod(modUrl, doubleProperty, stringProperty).toCompletableFuture().get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(stringProperty.isBound(), is(true));
    assertThat(doubleProperty.isBound(), is(true));

    assertThat(instance.getInstalledModVersions().size(), is(2));
  }

  @Test
  public void testDownloadAndInstallModInfoBeanWithProperties() throws Exception {
    assertThat(instance.getInstalledModVersions().size(), is(1));

    InstallModTask task = stubInstallModTask();
    task.getFuture().complete(null);

    when(applicationContext.getBean(InstallModTask.class)).thenReturn(task);

    URL modUrl = new URL("http://example.com/some/modVersion.zip");

    copyMod("BlackopsSupport", BLACKOPS_SUPPORT_MOD_INFO);
    assertThat(instance.getInstalledModVersions().size(), is(1));

    StringProperty stringProperty = new SimpleStringProperty();
    DoubleProperty doubleProperty = new SimpleDoubleProperty();

    ModVersionBean modVersion = ModVersionBeanBuilder.create().defaultValues().downloadUrl(modUrl).get();
    instance.downloadAndInstallMod(modVersion, doubleProperty, stringProperty).toCompletableFuture().get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(stringProperty.isBound(), is(true));
    assertThat(doubleProperty.isBound(), is(true));

    assertThat(instance.getInstalledModVersions().size(), is(2));
  }

  @Test
  public void testGetInstalledModUids() throws Exception {
    copyMod("BlackopsSupport", BLACKOPS_SUPPORT_MOD_INFO);
    instance.loadInstalledMods();

    Set<String> installedModUids = instance.getInstalledModUids();

    assertThat(installedModUids, containsInAnyOrder("9e8ea941-c306-4751-b367-f00000000005", "9e8ea941-c306-4751-b367-a11000000502"));
  }

  @Test
  public void testGetInstalledUiModsUids() throws Exception {
    assertThat(instance.getInstalledModVersions().size(), is(1));

    copyMod("EM", ECO_MANAGER_MOD_INFO);

    instance.loadInstalledMods();
    assertThat(instance.getInstalledModVersions().size(), is(2));

    Set<String> installedUiModsUids = instance.getInstalledUiModsUids();

    assertThat(installedUiModsUids, contains("b2cde810-15d0-4bfa-af66-ec2d6ecd561b"));
  }

  @Test
  public void testEnableSimModsClean() throws Exception {
    Files.createFile(gamePrefsPath);

    HashSet<String> simMods = new HashSet<>();
    simMods.add("9e8ea941-c306-4751-b367-f00000000005");
    simMods.add("9e8ea941-c306-4751-b367-a11000000502");
    instance.enableSimMods(simMods);

    List<String> lines = Files.readAllLines(gamePrefsPath);

    assertThat(lines, contains(
        "active_mods = {",
        "    ['9e8ea941-c306-4751-b367-f00000000005'] = true,",
        "    ['9e8ea941-c306-4751-b367-a11000000502'] = true",
        "}"
    ));
  }

  @Test
  public void testEnableSimModsModDisableUnselectedMods() throws Exception {
    Iterable<? extends CharSequence> lines = Arrays.asList(
        "active_mods = {",
        "    ['9e8ea941-c306-4751-b367-f00000000005'] = true,",
        "    ['9e8ea941-c306-4751-b367-a11000000502'] = true",
        "}"
    );
    Files.write(gamePrefsPath, lines);

    HashSet<String> simMods = new HashSet<>();
    simMods.add("9e8ea941-c306-4751-b367-a11000000502");
    instance.enableSimMods(simMods);

    lines = Files.readAllLines(gamePrefsPath);

    assertThat(lines, contains(
        "active_mods = {",
        "    ['9e8ea941-c306-4751-b367-a11000000502'] = true",
        "}"
    ));
  }

  @Test
  public void testExtractModInfo() throws Exception {
    copyMod("BlackopsSupport", BLACKOPS_SUPPORT_MOD_INFO);
    copyMod("EM", ECO_MANAGER_MOD_INFO);
    instance.loadInstalledMods();

    ArrayList<ModVersionBean> installedModVersions = new ArrayList<>(instance.getInstalledModVersions());
    installedModVersions.sort(Comparator.comparing(modVersionBean -> modVersionBean.getMod().getDisplayName()));

    ModVersionBean modVersion = installedModVersions.get(0);

    assertThat(modVersion.getMod().getDisplayName(), is("BlackOps Global Icon Support Mod"));
    assertThat(modVersion.getVersion(), is(new ComparableVersion("5")));
    assertThat(modVersion.getMod().getAuthor(), is("Exavier Macbeth, DeadMG"));
    assertThat(modVersion.getDescription(), is("Version 5.0. This mod provides global icon support for any mod that places their icons in the proper folder structure. See Readme"));
    assertThat(modVersion.getImagePath(), nullValue());
    assertThat(modVersion.getSelectable(), is(true));
    assertThat(modVersion.getId(), is(nullValue()));
    assertThat(modVersion.getUid(), is("9e8ea941-c306-4751-b367-f00000000005"));
    assertThat(modVersion.getModType(), equalTo(ModType.SIM));

    modVersion = installedModVersions.get(1);

    assertThat(modVersion.getMod().getDisplayName(), is("BlackOps Unleashed"));
    assertThat(modVersion.getVersion(), is(new ComparableVersion("8")));
    assertThat(modVersion.getMod().getAuthor(), is("Lt_hawkeye"));
    assertThat(modVersion.getDescription(), is("Version 5.2. BlackOps Unleased Unitpack contains several new units and game changes. Have fun"));
    assertThat(modVersion.getImagePath(), is(modsDirectory.resolve("BlackOpsUnleashed/icons/yoda_icon.bmp")));
    assertThat(modVersion.getSelectable(), is(true));
    assertThat(modVersion.getId(), is(nullValue()));
    assertThat(modVersion.getUid(), is("9e8ea941-c306-4751-b367-a11000000502"));
    assertThat(modVersion.getModType(), equalTo(ModType.SIM));
    assertThat(modVersion.getMountPoints(), hasSize(10));
    assertThat(modVersion.getMountPoints().get(3).getFile(), is(Paths.get("effects")));
    assertThat(modVersion.getMountPoints().get(3).getMountPoint(), is("/effects"));
    assertThat(modVersion.getHookDirectories(), contains("/blackops"));

    modVersion = installedModVersions.get(2);

    assertThat(modVersion.getMod().getDisplayName(), is("EcoManager"));
    assertThat(modVersion.getVersion(), is(new ComparableVersion("3")));
    assertThat(modVersion.getMod().getAuthor(), is("Crotalus"));
    assertThat(modVersion.getDescription(), is("EcoManager v3, more efficient energy throttling"));
    assertThat(modVersion.getImagePath(), nullValue());
    assertThat(modVersion.getSelectable(), is(true));
    assertThat(modVersion.getId(), is(nullValue()));
    assertThat(modVersion.getUid(), is("b2cde810-15d0-4bfa-af66-ec2d6ecd561b"));
    assertThat(modVersion.getModType(), equalTo(ModType.UI));
  }

  @Test
  public void testLoadInstalledModWithoutModInfo() throws Exception {
    Files.createDirectories(modsDirectory.resolve("foobar"));

    assertThat(instance.getInstalledModVersions().size(), is(1));

    instance.loadInstalledMods();

    assertThat(instance.getInstalledModVersions().size(), is(1));
  }

  @Test
  public void testIsModInstalled() {
    assertThat(instance.isModInstalled("9e8ea941-c306-4751-b367-a11000000502"), is(true));
    assertThat(instance.isModInstalled("9e8ea941-c306-4751-b367-f00000000005"), is(false));
  }

  @Test
  public void testUninstallMod() {
    UninstallModTask uninstallModTask = mock(UninstallModTask.class);
    when(applicationContext.getBean(UninstallModTask.class)).thenReturn(uninstallModTask);
    assertThat(instance.getInstalledModVersions(), hasSize(1));

    instance.uninstallMod(instance.getInstalledModVersions().get(0));

    verify(taskService).submitTask(uninstallModTask);
  }

  @Test
  public void testGetPathForMod() {
    assertThat(instance.getInstalledModVersions(), hasSize(1));

    Path actual = instance.getPathForMod(instance.getInstalledModVersions().get(0));

    Path expected = modsDirectory.resolve(BLACK_OPS_UNLEASHED_DIRECTORY_NAME);
    assertThat(actual, is(expected));
  }

  @Test
  public void testGetPathForModUnknownModReturnsNull() {
    assertThat(instance.getInstalledModVersions(), hasSize(1));
    assertThat(instance.getPathForMod(ModVersionBeanBuilder.create().defaultValues().uid("1").get()), Matchers.nullValue());
  }

  @Test
  public void testUploadMod() {
    ModUploadTask modUploadTask = mock(ModUploadTask.class);

    when(applicationContext.getBean(ModUploadTask.class)).thenReturn(modUploadTask);

    Path modPath = Paths.get(".");

    instance.uploadMod(modPath);

    verify(applicationContext).getBean(ModUploadTask.class);
    verify(modUploadTask).setModPath(modPath);
    verify(taskService).submitTask(modUploadTask);
  }

  @Test
  public void testLoadThumbnail() throws MalformedURLException {
    ModVersionBean modVersion = ModVersionBeanBuilder.create().defaultValues()
        .thumbnailUrl(new URL("http://127.0.0.1:65534/thumbnail.png"))
        .get();
    instance.loadThumbnail(modVersion);
    verify(assetService).loadAndCacheImage(eq(modVersion.getThumbnailUrl()), eq(Paths.get("mods")), any());
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


    List<ModVersionBean> modVersions = instance.updateAndActivateModVersions(List.of(modVersion)).get();

    verify(taskService, times(0)).submitTask(any(InstallModTask.class));

    assertThat(modVersions, contains(modVersion));
  }

  @Test
  public void testUpdateModsWithAutoUpdateTurnedOff() throws IOException, ExecutionException, InterruptedException {
    ModVersionBean modVersion = ModVersionBeanBuilder.create().defaultValues().get();
    Preferences preferences = new Preferences();
    when(preferencesService.getPreferences()).thenReturn(preferences);
    preferences.setMapAndModAutoUpdate(false);

    List<ModVersionBean> modVersions = instance.updateAndActivateModVersions(List.of(modVersion)).get();

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

    when(applicationContext.getBean(any(Class.class)))
        .thenReturn(mock(InstallModTask.class));

    InstallModTask installModTask = mock(InstallModTask.class);
    when(taskService.submitTask(any(InstallModTask.class)))
        .thenReturn(installModTask);
    when(installModTask.getFuture())
        .thenReturn(CompletableFuture.completedFuture(null));

    List<ModVersionBean> modVersions = instance.updateAndActivateModVersions(List.of(modVersion)).get();

    verify(taskService, times(2)).submitTask(any());

    assertThat(modVersions, contains(latestVersion));
  }

  private InstallModTask stubInstallModTask() {
    return new InstallModTask(preferencesService, i18n) {
      @Override
      protected Void call() {
        return null;
      }
    };
  }

  @Test
  public void testGetRecommendedMods() {
    ModVersionBean modVersionBean = ModVersionBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(ApiTestUtil.apiPageOf(List.of(modMapper.map(modVersionBean.getMod(), new CycleAvoidingMappingContext())), 1));
    List<ModVersionBean> results = instance.getRecommendedModsWithPageCount(10, 0).join().getT1();
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasFilter(qBuilder().bool("recommended").isTrue())));
    assertThat(results, contains(modVersionBean));
  }

  @Test
  public void testGetFeaturedFiles() {
    FeaturedModBean featuredMod = FeaturedModBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.getMany(eq(FeaturedModFile.class), anyString(), anyInt(), any())).thenReturn(Flux.just(new FeaturedModFile()));

    instance.getFeaturedModFiles(featuredMod, 0);
    verify(fafApiAccessor).getMany(eq(FeaturedModFile.class), eq(String.format("/featuredMods/%s/files/%s", featuredMod.getId(), 0)), eq(100), any());
  }

  @Test
  public void testGetFeaturedMod() {
    FeaturedModBean featuredMod = FeaturedModBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(modMapper.map(featuredMod, new CycleAvoidingMappingContext())));
    FeaturedModBean result = instance.getFeaturedMod("test").join();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().string("technicalName").eq("test"))));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasSort("order", true)));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasPageSize(1)));
    assertThat(result, is(featuredMod));
  }

  @Test
  public void testFindByQuery() throws Exception {
    ModVersionBean modVersionBean = ModVersionBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.getManyWithPageCount(any(), anyString())).thenReturn(ApiTestUtil.apiPageOf(List.of(modMapper.map(modVersionBean.getMod(), new CycleAvoidingMappingContext())), 1));

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
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(ApiTestUtil.apiPageOf(List.of(modMapper.map(modVersionBean.getMod(), new CycleAvoidingMappingContext())), 1));
    List<ModVersionBean> results = instance.getHighestRatedModsWithPageCount(10, 1).join().getT1();
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasFilter(qBuilder().string("latestVersion.type").eq("SIM"))));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("reviewsSummary.lowerBound", false)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)));
    assertThat(results, contains(modVersionBean));
  }

  @Test
  public void testGetHighestRatedUI() {
    ModVersionBean modVersionBean = ModVersionBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(ApiTestUtil.apiPageOf(List.of(modMapper.map(modVersionBean.getMod(), new CycleAvoidingMappingContext())), 1));
    List<ModVersionBean> results = instance.getHighestRatedUiModsWithPageCount(10, 1).join().getT1();
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasFilter(qBuilder().string("latestVersion.type").eq("UI"))));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("reviewsSummary.lowerBound", false)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)));
    assertThat(results, contains(modVersionBean));
  }

  @Test
  public void testGetNewest() {
    ModVersionBean modVersionBean = ModVersionBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(ApiTestUtil.apiPageOf(List.of(modMapper.map(modVersionBean.getMod(), new CycleAvoidingMappingContext())), 1));
    List<ModVersionBean> results = instance.getNewestModsWithPageCount(10, 1).join().getT1();
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("latestVersion.createTime", false)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)));
    assertThat(results, contains(modVersionBean));
  }
}
