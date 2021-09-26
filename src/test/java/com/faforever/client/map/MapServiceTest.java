package com.faforever.client.map;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.MapBeanBuilder;
import com.faforever.client.builders.MapVersionBeanBuilder;
import com.faforever.client.builders.MatchmakerQueueBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.MapMapper;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mapstruct.ReplayMapper;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.client.vault.search.SearchController.SortOrder;
import com.faforever.commons.api.dto.Map;
import com.faforever.commons.api.dto.MapVersion;
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
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.util.FileSystemUtils;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapServiceTest extends UITest {

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
  private FafApiAccessor fafApiAccessor;
  @Mock
  private MapGeneratorService mapGeneratorService;
  @Mock
  private PlayerService playerService;
  @Mock
  private EventBus eventBus;

  private MapMapper mapMapper = Mappers.getMapper(MapMapper.class);
  private ReplayMapper replayMapper = Mappers.getMapper(ReplayMapper.class);

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(mapMapper);
    MapperSetup.injectMappers(replayMapper);
    ClientProperties clientProperties = new ClientProperties();
    clientProperties.getVault().setMapPreviewUrlFormat("http://127.0.0.1:65534/preview/%s/%s");
    clientProperties.getVault().setMapDownloadUrlFormat("http://127.0.0.1:65534/fakeDownload/%s");
    mapsDirectory = Files.createDirectories(gameDirectory.resolve("maps"));

    Preferences preferences = PreferencesBuilder.create().defaultValues()
        .forgedAlliancePrefs()
        .customMapsDirectory(mapsDirectory)
        .installationPath(gameDirectory)
        .then()
        .get();

    when(preferencesService.getPreferences()).thenReturn(preferences);
    instance = new MapService(preferencesService, taskService, applicationContext,
        fafApiAccessor, assetService, i18n, uiService, mapGeneratorService, clientProperties, eventBus, playerService,
        mapMapper, replayMapper);
    instance.afterPropertiesSet();

    doAnswer(invocation -> {
      CompletableTask<?> task = invocation.getArgument(0);
      WaitForAsyncUtils.asyncFx(task);
      task.getFuture().join();
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

    ObservableList<MapVersionBean> localMapBeans = instance.getInstalledMaps();
    assertThat(localMapBeans, hasSize(1));

    MapVersionBean mapBean = localMapBeans.get(0);
    assertThat(mapBean, notNullValue());
    assertThat(mapBean.getFolderName(), is("SCMP_001"));
    assertThat(mapBean.getMap().getDisplayName(), is("Burial Mounds"));
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
    MapVersionBean mapBean = instance.readMap(Paths.get(getClass().getResource("/maps/SCMP_001").toURI()));

    assertThat(mapBean, notNullValue());
    assertThat(mapBean.getId(), nullValue());
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
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(Mono.empty());
    instance.getRecommendedMapsWithPageCount(10, 0);
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasFilter(qBuilder().bool("recommended").isTrue())));
  }

  @Test
  public void testGetHighestRatedMaps() {
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(Mono.empty());
    instance.getHighestRatedMapsWithPageCount(10, 0);
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("reviewsSummary.lowerBound", false)));
  }

  @Test
  public void testGetNewestMaps() {
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(Mono.empty());
    instance.getNewestMapsWithPageCount(10, 0);
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("updateTime", false)));
  }

  @Test
  public void testGetMostPlayedMaps() {
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(Mono.empty());
    instance.getMostPlayedMapsWithPageCount(10, 0);
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("gamesPlayed", false)));
  }

  @Test
  public void testIsOfficialMap() {
    instance.officialMaps = ImmutableSet.of("SCMP_001");

    MapVersionBean officialMap = MapVersionBeanBuilder.create().folderName("SCMP_001").get();
    MapVersionBean customMap = MapVersionBeanBuilder.create().folderName("customMap.v0001").get();
    assertThat(instance.isOfficialMap(officialMap), is(true));
    assertThat(instance.isOfficialMap(officialMap.getFolderName()), is(true));
    assertThat(instance.isOfficialMap(customMap), is(false));
    assertThat(instance.isOfficialMap(customMap.getFolderName()), is(false));
  }

  @Test
  public void testIsCustomMap() {
    instance.officialMaps = ImmutableSet.of("SCMP_001");

    MapVersionBean officialMap = MapVersionBeanBuilder.create().folderName("SCMP_001").get();
    MapVersionBean customMap = MapVersionBeanBuilder.create().folderName("customMap.v0001").get();

    assertThat(instance.isCustomMap(customMap), is(true));
    assertThat(instance.isCustomMap(officialMap), is(false));
  }

  @Test
  public void testGetLatestVersionMap() {
    MapVersionBean oldestMap = MapVersionBeanBuilder.create().folderName("unitMap v1").version(null).get();
    assertThat(instance.getMapLatestVersion(oldestMap).join(), is(oldestMap));

    MapBean mapBean = MapBeanBuilder.create().defaultValues().get();
    MapVersionBean mapVersionBean = MapVersionBeanBuilder.create().defaultValues().map(mapBean).folderName("junit_map1.v0003").version(new ComparableVersion("3")).get();
    MapVersionBean sameMap = MapVersionBeanBuilder.create().defaultValues().map(mapBean).folderName("junit_map1.v0003").version(new ComparableVersion("3")).get();
    mapBean.setLatestVersion(sameMap);
    Map map = mapMapper.map(mapBean, new CycleAvoidingMappingContext());

    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(map));
    assertThat(instance.getMapLatestVersion(mapVersionBean).join().getId(), is(sameMap.getId()));

    verify(fafApiAccessor).getMany(argThat(
        ElideMatchers.hasFilter(qBuilder().string("versions.folderName").eq("junit_map1.v0003"))
    ));

    MapVersionBean outdatedMap = MapVersionBeanBuilder.create().defaultValues().map(mapBean).folderName("junit_map2.v0001").version(new ComparableVersion("1")).get();
    MapVersionBean newMap = MapVersionBeanBuilder.create().defaultValues().map(mapBean).folderName("junit_map2.v0002").version(new ComparableVersion("2")).get();
    mapBean.setLatestVersion(newMap);
    map = mapMapper.map(mapBean, new CycleAvoidingMappingContext());
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(map));
    assertThat(instance.getMapLatestVersion(outdatedMap).join().getId(), is(newMap.getId()));
  }

  @Test
  public void testUpdateMapToLatestVersionIfNewVersionExist() throws Exception {
    MapBean mapBean = MapBeanBuilder.create().defaultValues().get();
    MapVersionBean outdatedMap = MapVersionBeanBuilder.create().defaultValues().map(mapBean).folderName("palaneum.v0001").version(new ComparableVersion("1")).get();
    MapVersionBean updatedMap = MapVersionBeanBuilder.create().defaultValues().map(mapBean).folderName("palaneum.v0002").version(new ComparableVersion("2")).get();
    mapBean.setLatestVersion(updatedMap);
    Map map = mapMapper.map(mapBean, new CycleAvoidingMappingContext());

    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(map));

    copyMapsToCustomMapsDirectory(outdatedMap);
    assertThat(checkCustomMapFolderExist(outdatedMap), is(true));
    assertThat(checkCustomMapFolderExist(updatedMap), is(false));
    prepareDownloadMapTask(updatedMap);
    prepareUninstallMapTask(outdatedMap);
    assertThat(instance.updateLatestVersionIfNecessary(outdatedMap).join().getId(), is(updatedMap.getId()));
    assertThat(checkCustomMapFolderExist(outdatedMap), is(false));
    assertThat(checkCustomMapFolderExist(updatedMap), is(true));
  }

  @Test
  public void testUpdateMapToLatestVersionIfOfficalMap() throws Exception {
    MapVersionBean offical = MapVersionBeanBuilder.create().defaultValues().folderName("SCMP_001").version(new ComparableVersion("1")).get();

    instance.updateLatestVersionIfNecessary(offical);

    verify(fafApiAccessor, times(0)).getMany(any());
  }

  @Test
  public void testUpdateMapToLatestVersionIfAutoUpdateTurnedOff() throws Exception {
    MapVersionBean map = MapVersionBeanBuilder.create().defaultValues().folderName("bla.v0001").version(new ComparableVersion("1")).get();
    Preferences preferences = new Preferences();
    when(preferencesService.getPreferences()).thenReturn(preferences);
    preferences.setMapAndModAutoUpdate(false);

    instance.updateLatestVersionIfNecessary(map);

    verify(fafApiAccessor, times(0)).getMany(any());
  }

  @Test
  public void testUpdateMapToLatestVersionIfNoNewVersion() throws Exception {
    MapVersionBean map = MapVersionBeanBuilder.create().defaultValues().folderName("palaneum.v0001").version(new ComparableVersion("1")).get();

    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());

    copyMapsToCustomMapsDirectory(map);
    assertThat(checkCustomMapFolderExist(map), is(true));
    assertThat(instance.updateLatestVersionIfNecessary(map).join(), is(map));
    assertThat(checkCustomMapFolderExist(map), is(true));
  }

  @Test
  public void testHideMapVersion() throws Exception {
    MapVersionBean map = MapVersionBeanBuilder.create().defaultValues().folderName("palaneum.v0001").version(new ComparableVersion("1")).get();
    when(fafApiAccessor.patch(any(), any())).thenReturn(Mono.empty());

    instance.hideMapVersion(map);

    verify(fafApiAccessor).patch(any(), argThat(mapVersion -> ((MapVersion) mapVersion).isHidden()));
  }

  @Test
  public void testUnRankMapVersion() throws Exception {
    MapVersionBean map = MapVersionBeanBuilder.create().defaultValues().folderName("palaneum.v0001").version(new ComparableVersion("1")).get();
    when(fafApiAccessor.patch(any(), any())).thenReturn(Mono.empty());

    instance.unrankMapVersion(map);

    verify(fafApiAccessor).patch(any(), argThat(mapVersion -> !((MapVersion) mapVersion).isRanked()));
  }

  @Test
  public void testFindByMapFolderName() throws Exception {
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());

    instance.findByMapFolderName("test");

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().string("folderName").eq("test"))));
  }

  @Test
  public void testGetMatchMakerMaps() throws Exception {
    when(fafApiAccessor.getMany(any(), anyString())).thenReturn(Flux.empty());
    when(playerService.getCurrentPlayer()).thenReturn(PlayerBeanBuilder.create().defaultValues().get());

    instance.getMatchmakerMapsWithPageCount(MatchmakerQueueBeanBuilder.create().defaultValues().get(), 10, 1);

    verify(fafApiAccessor).getMany(any(), anyString());
  }

  @Test
  public void testHasPlayedMap() throws Exception {
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());

    MapVersionBean mapVersion = MapVersionBeanBuilder.create().defaultValues().get();
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    instance.hasPlayedMap(player, mapVersion);

    verify(fafApiAccessor).getMany(argThat(
        ElideMatchers.hasFilter(qBuilder()
            .intNum("mapVersion.id").eq(mapVersion.getId()).and()
            .intNum("playerStats.player.id").eq(player.getId()))
    ));
    verify(fafApiAccessor).getMany(argThat(
        ElideMatchers.hasSort("endTime", false)
    ));
  }

  @Test
  public void testGetOwnedMaps() throws Exception {
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(Mono.empty());
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    when(playerService.getCurrentPlayer()).thenReturn(player);

    instance.getOwnedMapsWithPageCount(10, 1);

    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasFilter(qBuilder().string("author.id").eq(String.valueOf(player.getId())))));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)));
  }

  @Test
  public void testFindByQuery() throws Exception {
    when(fafApiAccessor.getManyWithPageCount(any(), anyString())).thenReturn(Mono.empty());

    SearchConfig searchConfig = new SearchConfig(new SortConfig("testSort", SortOrder.ASC), "testQuery");
    instance.findByQueryWithPageCount(searchConfig, 10, 1);

    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("testSort", true)), eq("testQuery"));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)), eq("testQuery"));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)), eq("testQuery"));
  }

  private void prepareDownloadMapTask(MapVersionBean mapToDownload) {
    StubDownloadMapTask task = new StubDownloadMapTask(preferencesService, i18n, mapsDirectory);
    task.setMapToDownload(mapToDownload);
    when(applicationContext.getBean(DownloadMapTask.class)).thenReturn(task);
  }

  private void prepareUninstallMapTask(MapVersionBean mapToDelete) {
    UninstallMapTask task = new UninstallMapTask(instance);
    task.setMap(mapToDelete);
    when(applicationContext.getBean(UninstallMapTask.class)).thenReturn(task);
  }

  private void copyMapsToCustomMapsDirectory(MapVersionBean... maps) throws Exception {
    for (MapVersionBean map : maps) {
      String folder = map.getFolderName();
      Path mapPath = Files.createDirectories(mapsDirectory.resolve(folder));
      FileSystemUtils.copyRecursively(
          Paths.get(getClass().getResource("/maps/" + folder).toURI()),
          mapPath
      );
      instance.addInstalledMap(mapPath);
    }
  }

  private boolean checkCustomMapFolderExist(MapVersionBean map) throws IOException {
    return Files.list(mapsDirectory)
        .anyMatch(path -> path.getFileName().toString().equals(map.getFolderName()) && path.toFile().isDirectory());
  }
}
