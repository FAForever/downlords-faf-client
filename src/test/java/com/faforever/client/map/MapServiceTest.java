package com.faforever.client.map;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesBuilder;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.luaj.vm2.LuaError;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.util.FileSystemUtils;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapServiceTest extends AbstractPlainJavaFxTest {

  @TempDir
  public Path cacheDirectory;
  @TempDir
  public Path customMapsDirectory;
  @TempDir
  public Path gameDirectory;

  private MapService instance;
  private Path mapsDirectory;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private TaskService taskService;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private AssetService assetService;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private FafService fafService;
  @Mock
  private MapGeneratorService mapGeneratorService;
  @Mock
  private PlayerService playerService;
  @Mock
  private EventBus eventBus;

  @BeforeEach
  public void setUp() throws Exception {
    ClientProperties clientProperties = new ClientProperties();
    clientProperties.getVault().setMapPreviewUrlFormat("http://127.0.0.1:65534/preview/%s/%s");
    clientProperties.getVault().setMapDownloadUrlFormat("http://127.0.0.1:65534/fakeDownload/%s");

    Preferences preferences = PreferencesBuilder.create().defaultValues()
        .forgedAlliancePrefs()
        .customMapsDirectory(customMapsDirectory)
        .installationPath(gameDirectory)
        .then()
        .get();

    mapsDirectory = Files.createDirectories(gameDirectory.resolve("maps"));
    when(preferencesService.getPreferences()).thenReturn(preferences);
    instance = new MapService(preferencesService, taskService, applicationContext,
        fafService, assetService, i18n, uiService, mapGeneratorService, clientProperties, eventBus, playerService);
    instance.afterPropertiesSet();

    doAnswer(invocation -> {
      CompletableTask<?> task = invocation.getArgument(0);
      WaitForAsyncUtils.asyncFx(task);
      task.getFuture().get();
      return task;
    }).when(taskService).submitTask(any());

    instance.officialMaps = ImmutableSet.of();
    instance.afterPropertiesSet();
  }

  @Test
  public void testGetLocalMapsNoMaps() {
    assertThat(instance.getInstalledMaps(), hasSize(0));
  }

  @Test
  public void testGetLocalMapsOfficialMap() throws Exception {
    instance.officialMaps = ImmutableSet.of("SCMP_001");

    Path scmp001 = Files.createDirectory(mapsDirectory.resolve("SCMP_001"));
    Files.copy(getClass().getResourceAsStream("/maps/SCMP_001/SCMP_001_scenario.lua"), scmp001.resolve("SCMP_001_scenario.lua"));

    instance.afterPropertiesSet();

    ObservableList<MapBean> localMapBeans = instance.getInstalledMaps();
    assertThat(localMapBeans, hasSize(1));

    MapBean mapBean = localMapBeans.get(0);
    assertThat(mapBean, notNullValue());
    assertThat(mapBean.getFolderName(), is("SCMP_001"));
    assertThat(mapBean.getDisplayName(), is("Burial Mounds"));
    assertThat(mapBean.getSize(), equalTo(MapSize.valueOf(1024, 1024)));
  }

  @Test
  public void testReadMapOfNonFolderThrowsException() {
    assertThat(assertThrows(MapLoadException.class, () -> instance.readMap(mapsDirectory.resolve("something"))).getMessage(), containsString("Not a folder"));
  }

  @Test
  public void testReadMapInvalidMap() throws Exception {
    Path corruptMap = Files.createDirectory(mapsDirectory.resolve("corruptMap"));
    Files.writeString(corruptMap.resolve("corruptMap_scenario.lua"), "{\"This is invalid\", \"}");

    assertThat(assertThrows(MapLoadException.class, () -> instance.readMap(corruptMap)).getCause(), IsInstanceOf.instanceOf(LuaError.class));
  }

  @Test
  public void testReadMap() throws Exception {
    MapBean mapBean = instance.readMap(Paths.get(getClass().getResource("/maps/SCMP_001").toURI()));

    assertThat(mapBean, notNullValue());
    assertThat(mapBean.getId(), isEmptyOrNullString());
    assertThat(mapBean.getDescription(), startsWith("Initial scans of the planet"));
    assertThat(mapBean.getSize(), is(MapSize.valueOf(1024, 1024)));
    assertThat(mapBean.getVersion(), is(new ComparableVersion("1")));
    assertThat(mapBean.getFolderName(), is("SCMP_001"));
  }

  @Test
  public void testInstalledOfficialMapIgnoreCase() throws Exception {
    instance.officialMaps = ImmutableSet.of("SCMP_001");

    Path scmp001 = Files.createDirectory(mapsDirectory.resolve("SCMP_001"));
    Files.copy(getClass().getResourceAsStream("/maps/SCMP_001/SCMP_001_scenario.lua"), scmp001.resolve("SCMP_001_scenario.lua"));

    instance.afterPropertiesSet();

    assertThat(instance.isInstalled("ScMp_001"), is(true));
  }

  @Test
  public void testLoadPreview() {
    for (PreviewSize previewSize : PreviewSize.values()) {
      Path cacheSubDir = Paths.get("maps").resolve(previewSize.folderName);
      when(assetService.loadAndCacheImage(any(URL.class), eq(cacheSubDir), any())).thenReturn(new Image("theme/images/unknown_map.png"));
      instance.loadPreview("preview", previewSize);
      verify(assetService).loadAndCacheImage(any(URL.class), eq(cacheSubDir), any());
    }
  }

  @Test
  public void testGetRecommendedMaps() {
    instance.getRecommendedMapsWithPageCount(10, 0);
    verify(fafService).getRecommendedMapsWithPageCount(10, 0);
  }

  @Test
  public void testGetHighestRatedMaps() {
    when(fafService.getHighestRatedMapsWithPageCount(10, 0)).thenReturn(CompletableFuture.completedFuture(null));
    instance.getHighestRatedMapsWithPageCount(10, 0);
    verify(fafService).getHighestRatedMapsWithPageCount(10, 0);
  }

  @Test
  public void testGetNewestMaps() {
    when(fafService.getNewestMapsWithPageCount(10, 0)).thenReturn(CompletableFuture.completedFuture(null));
    instance.getNewestMapsWithPageCount(10, 0);
    verify(fafService).getNewestMapsWithPageCount(10, 0);
  }

  @Test
  public void testGetMostPlayedMaps() {
    when(fafService.getMostPlayedMapsWithPageCount(10, 0)).thenReturn(CompletableFuture.completedFuture(null));
    instance.getMostPlayedMapsWithPageCount(10, 0);
    verify(fafService).getMostPlayedMapsWithPageCount(10, 0);
  }

  @Test
  public void testIsOfficialMap() {
    instance.officialMaps = ImmutableSet.of("SCMP_001");

    MapBean officialMap = MapBeanBuilder.create().displayName("official map").folderName("SCMP_001").get();
    MapBean customMap = MapBeanBuilder.create().displayName("custom map").folderName("customMap.v0001").get();
    assertThat(instance.isOfficialMap(officialMap), is(true));
    assertThat(instance.isOfficialMap(officialMap.getFolderName()), is(true));
    assertThat(instance.isOfficialMap(customMap), is(false));
    assertThat(instance.isOfficialMap(customMap.getFolderName()), is(false));
  }

  @Test
  public void testIsCustomMap() {
    instance.officialMaps = ImmutableSet.of("SCMP_001");

    MapBean officialMap = MapBeanBuilder.create().displayName("official map").folderName("SCMP_001").get();
    MapBean customMap = MapBeanBuilder.create().displayName("custom map").folderName("customMap.v0001").get();

    assertThat(instance.isCustomMap(customMap), is(true));
    assertThat(instance.isCustomMap(officialMap), is(false));
  }

  @Test
  public void testGetLatestVersionMap() {
    MapBean oldestMap = MapBeanBuilder.create().folderName("unitMap v1").version(null).get();
    assertThat(instance.getMapLatestVersion(oldestMap).join(), is(oldestMap));

    MapBean map = MapBeanBuilder.create().folderName("junit_map1.v0003").version(3).get();
    MapBean sameMap = MapBeanBuilder.create().folderName("junit_map1.v0003").version(3).get();
    when(fafService.getMapLatestVersion(map.getFolderName()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(sameMap)));
    assertThat(instance.getMapLatestVersion(map).join(), is(sameMap));

    MapBean outdatedMap = MapBeanBuilder.create().folderName("junit_map2.v0001").version(1).get();
    MapBean newMap = MapBeanBuilder.create().folderName("junit_map2.v0002").version(2).get();
    when(fafService.getMapLatestVersion(outdatedMap.getFolderName()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(newMap)));
    assertThat(instance.getMapLatestVersion(outdatedMap).join(), is(newMap));
  }

  @Test
  public void testUpdateMapToLatestVersionIfNewVersionExist() throws Exception {
    MapBean outdatedMap = MapBeanBuilder.create().displayName("test map").folderName("palaneum.v0001").version(1).get();
    MapBean updatedMap = MapBeanBuilder.create().displayName("test map").folderName("palaneum.v0002").version(2).get();

    when(fafService.getMapLatestVersion(outdatedMap.getFolderName())).thenReturn(CompletableFuture.completedFuture(Optional.of(updatedMap)));

    copyMapsToCustomMapsDirectory(outdatedMap);
    assertThat(checkCustomMapFolderExist(outdatedMap), is(true));
    assertThat(checkCustomMapFolderExist(updatedMap), is(false));
    prepareDownloadMapTask(updatedMap);
    prepareUninstallMapTask(outdatedMap);
    assertThat(instance.updateLatestVersionIfNecessary(outdatedMap).join(), is(updatedMap));
    assertThat(checkCustomMapFolderExist(outdatedMap), is(false));
    assertThat(checkCustomMapFolderExist(updatedMap), is(true));
  }

  @Test
  public void testUpdateMapToLatestVersionIfOfficalMap() throws Exception {
    MapBean offical = MapBeanBuilder.create().displayName("offical map").folderName("SCMP_001").version(1).get();

    instance.updateLatestVersionIfNecessary(offical);

    verify(fafService, times(0)).getMapLatestVersion(any());
  }

  @Test
  public void testUpdateMapToLatestVersionIfAutoUpdateTurnedOff() throws Exception {
    MapBean map = MapBeanBuilder.create().displayName("none offical map").folderName("bla.v0001").version(1).get();
    Preferences preferences = new Preferences();
    when(preferencesService.getPreferences()).thenReturn(preferences);
    preferences.setMapAndModAutoUpdate(false);

    instance.updateLatestVersionIfNecessary(map);

    verify(fafService, times(0)).getMapLatestVersion(any());
  }

  @Test
  public void testUpdateMapToLatestVersionIfNoNewVersion() throws Exception {
    MapBean map = MapBeanBuilder.create().displayName("test map").folderName("palaneum.v0001").version(1).get();

    when(fafService.getMapLatestVersion(map.getFolderName())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    copyMapsToCustomMapsDirectory(map);
    assertThat(checkCustomMapFolderExist(map), is(true));
    assertThat(instance.updateLatestVersionIfNecessary(map).join(), is(map));
    assertThat(checkCustomMapFolderExist(map), is(true));
  }

  private void prepareDownloadMapTask(MapBean mapToDownload) {
    StubDownloadMapTask task = new StubDownloadMapTask(preferencesService, i18n, customMapsDirectory);
    task.setMapToDownload(mapToDownload);
    when(applicationContext.getBean(DownloadMapTask.class)).thenReturn(task);
  }

  private void prepareUninstallMapTask(MapBean mapToDelete) {
    UninstallMapTask task = new UninstallMapTask(instance);
    task.setMap(mapToDelete);
    when(applicationContext.getBean(UninstallMapTask.class)).thenReturn(task);
  }

  private void copyMapsToCustomMapsDirectory(MapBean... maps) throws Exception {
    for (MapBean map : maps) {
      String folder = map.getFolderName();
      Path mapPath = Files.createDirectories(customMapsDirectory.resolve(folder));
      FileSystemUtils.copyRecursively(
          Paths.get(getClass().getResource("/maps/" + folder).toURI()),
          mapPath
      );
      instance.addInstalledMap(mapPath);
    }
  }

  private boolean checkCustomMapFolderExist(MapBean map) throws IOException {
    return Files.list(customMapsDirectory)
        .anyMatch(path -> path.getFileName().toString().equals(map.getFolderName()) && path.toFile().isDirectory());
  }
}
