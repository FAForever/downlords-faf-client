package com.faforever.client.map;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.MatchmakerQueueBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapPoolAssignmentBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.MatchmakerQueueBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.MapMapper;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mapstruct.MatchmakerMapper;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
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
import com.faforever.commons.api.dto.Map;
import com.faforever.commons.api.dto.MapPoolAssignment;
import com.faforever.commons.api.dto.MapVersion;
import com.faforever.commons.api.dto.NeroxisGeneratorParams;
import com.faforever.commons.api.elide.ElideEntity;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.hamcrest.core.IsInstanceOf;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.luaj.vm2.LuaError;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.util.FileSystemUtils;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.instancio.Select.field;
import static org.instancio.Select.scope;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapServiceTest extends PlatformTest {

  @TempDir
  public Path tempDirectory;

  private MapService instance;

  @Mock
  private TaskService taskService;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private ThemeService themeService;
  @Mock
  private AssetService assetService;
  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private MapGeneratorService mapGeneratorService;
  @Mock
  private PlayerService playerService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ObjectFactory<MapUploadTask> mapUploadTaskFactory;
  @Mock
  private ObjectFactory<DownloadMapTask> downloadMapTaskFactory;
  @Mock
  private ObjectFactory<UninstallMapTask> uninstallMapTaskFactory;
  @Mock
  private FileSizeReader fileSizeReader;
  @Spy
  private MapMapper mapMapper = Mappers.getMapper(MapMapper.class);
  @Spy
  private MatchmakerMapper matchmakerMapper = Mappers.getMapper(MatchmakerMapper.class);
  @Spy
  private ClientProperties clientProperties;
  @Spy
  private ForgedAlliancePrefs forgedAlliancePrefs;
  @Spy
  private Preferences preferences;

  private Path mapsDirectory;

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(mapMapper);
    MapperSetup.injectMappers(matchmakerMapper);
    clientProperties.getVault().setMapPreviewUrlFormat("http://127.0.0.1:65534/preview/%s/%s");
    clientProperties.getVault().setMapDownloadUrlFormat("http://127.0.0.1:65534/fakeDownload/%s");
    mapsDirectory = Files.createDirectory(tempDirectory.resolve("maps"));

    forgedAlliancePrefs.setInstallationPath(Path.of("."));
    forgedAlliancePrefs.setVaultBaseDirectory(tempDirectory);

    doAnswer(invocation -> {
      CompletableTask<?> task = invocation.getArgument(0);
      WaitForAsyncUtils.asyncFx(task);
      task.getFuture().join();
      return task;
    }).when(taskService).submitTask(any());

    instance = new MapService(notificationService, taskService, fafApiAccessor, assetService, i18n,
                              themeService, mapGeneratorService, playerService, mapMapper, fileSizeReader,
                              clientProperties, forgedAlliancePrefs, preferences, mapUploadTaskFactory,
                              downloadMapTaskFactory, uninstallMapTaskFactory, fxApplicationThreadExecutor);
    instance.officialMaps = Set.of();
    instance.afterPropertiesSet();
  }

  @Test
  public void testGetLocalMapsNoMaps() {
    assertThat(instance.getInstalledMaps(), hasSize(0));
  }

  @Test
  @Disabled("Unstable test: Map could not be read")
  public void testGetLocalMapsOfficialMap() throws Exception {
    instance.officialMaps = Set.of("SCMP_001");

    Path scmp001 = Files.createDirectory(mapsDirectory.resolve("SCMP_001"));
    Files.copy(getClass().getResourceAsStream("/maps/SCMP_001/SCMP_001_scenario.lua"), scmp001.resolve("SCMP_001_scenario.lua"));

    instance.afterPropertiesSet();

    ObservableList<MapVersionBean> localMapBeans = instance.getInstalledMaps();
    assertThat(localMapBeans, hasSize(1));

    MapVersionBean mapBean = localMapBeans.getFirst();
    assertThat(mapBean, notNullValue());
    assertThat(mapBean.folderName(), is("SCMP_001"));
    assertThat(mapBean.map().displayName(), is("Burial Mounds"));
    assertThat(mapBean.size(), equalTo(MapSize.valueOf(1024, 1024)));
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
    MapVersionBean mapBean = instance.readMap(Path.of(getClass().getResource("/maps/SCMP_001").toURI()));

    assertThat(mapBean, notNullValue());
    assertThat(mapBean.id(), nullValue());
    assertThat(mapBean.description(), startsWith("Initial scans of the planet"));
    assertThat(mapBean.size(), is(MapSize.valueOf(1024, 1024)));
    assertThat(mapBean.version(), is(new ComparableVersion("1")));
    assertThat(mapBean.folderName(), is("SCMP_001"));
  }

  @Test
  public void testInstalledOfficialMapIgnoreCase() throws Exception {
    instance.officialMaps = Set.of("SCMP_001");

    Path scmp001 = Files.createDirectory(mapsDirectory.resolve("SCMP_001"));
    Files.copy(getClass().getResourceAsStream("/maps/SCMP_001/SCMP_001_scenario.lua"), scmp001.resolve("SCMP_001_scenario.lua"));

    instance.afterPropertiesSet();

    assertThat(instance.isInstalled("ScMp_001"), is(true));
  }

  @Test
  public void testLoadPreview() {
    for (PreviewSize previewSize : PreviewSize.values()) {
      Path cacheSubDir = Path.of("maps").resolve(previewSize.folderName);
      when(assetService.loadAndCacheImage(any(URL.class), eq(cacheSubDir), any())).thenReturn(new Image(InputStream.nullInputStream()));
      instance.loadPreview("preview", previewSize);
      verify(assetService).loadAndCacheImage(any(URL.class), eq(cacheSubDir), any());
    }
  }

  @Test
  public void testGetRecommendedMaps() {
    MapVersionBean mapVersionBean = Instancio.create(MapVersionBean.class);
    Map map = mapMapper.map(mapVersionBean.map(), new CycleAvoidingMappingContext());
    map.setLatestVersion(mapMapper.map(mapVersionBean, new CycleAvoidingMappingContext()));
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(List.of(map), 1);
    when(fafApiAccessor.getManyWithPageCount(any(), anyString())).thenReturn(resultMono);
    StepVerifier.create(instance.getRecommendedMapsWithPageCount(10, 0))
                .expectNext(Tuples.of(List.of(mapVersionBean), 1))
                .verifyComplete();
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasFilter(qBuilder().bool("recommended")
                                                                                          .isTrue())), anyString());
  }

  @Test
  public void testGetHighestRatedMaps() {
    MapVersionBean mapVersionBean = Instancio.create(MapVersionBean.class);
    Map map = mapMapper.map(mapVersionBean.map(), new CycleAvoidingMappingContext());
    map.setLatestVersion(mapMapper.map(mapVersionBean, new CycleAvoidingMappingContext()));
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(List.of(map), 1);
    when(fafApiAccessor.getManyWithPageCount(any(), anyString())).thenReturn(resultMono);
    StepVerifier.create(instance.getHighestRatedMapsWithPageCount(10, 0))
                .expectNext(Tuples.of(List.of(mapVersionBean), 1))
                .verifyComplete();
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("reviewsSummary.lowerBound", false)),
                                                anyString());
  }

  @Test
  public void testGetNewestMaps() {
    MapVersionBean mapVersionBean = Instancio.create(MapVersionBean.class);
    Map map = mapMapper.map(mapVersionBean.map(), new CycleAvoidingMappingContext());
    map.setLatestVersion(mapMapper.map(mapVersionBean, new CycleAvoidingMappingContext()));
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(List.of(map), 1);
    when(fafApiAccessor.getManyWithPageCount(any(), anyString())).thenReturn(resultMono);
    StepVerifier.create(instance.getNewestMapsWithPageCount(10, 0))
                .expectNext(Tuples.of(List.of(mapVersionBean), 1))
                .verifyComplete();
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("latestVersion.createTime", false)),
                                                anyString());
  }

  @Test
  public void testGetMostPlayedMaps() {
    MapVersionBean mapVersionBean = Instancio.create(MapVersionBean.class);
    Map map = mapMapper.map(mapVersionBean.map(), new CycleAvoidingMappingContext());
    map.setLatestVersion(mapMapper.map(mapVersionBean, new CycleAvoidingMappingContext()));
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(List.of(map), 1);
    when(fafApiAccessor.getManyWithPageCount(any(), anyString())).thenReturn(resultMono);
    StepVerifier.create(instance.getMostPlayedMapsWithPageCount(10, 0))
                .expectNext(Tuples.of(List.of(mapVersionBean), 1))
                .verifyComplete();
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("gamesPlayed", false)), anyString());
  }

  @Test
  public void testIsOfficialMap() {
    MapVersionBean officialMap = Instancio.create(MapVersionBean.class);
    MapVersionBean customMap = Instancio.create(MapVersionBean.class);
    instance.officialMaps = Set.of(officialMap.folderName());

    assertThat(instance.isOfficialMap(officialMap), is(true));
    assertThat(instance.isOfficialMap(officialMap.folderName()), is(true));
    assertThat(instance.isOfficialMap(customMap), is(false));
    assertThat(instance.isOfficialMap(customMap.folderName()), is(false));
  }

  @Test
  public void testIsCustomMap() {
    MapVersionBean officialMap = Instancio.create(MapVersionBean.class);
    MapVersionBean customMap = Instancio.create(MapVersionBean.class);
    instance.officialMaps = Set.of(officialMap.folderName());

    assertThat(instance.isCustomMap(customMap), is(true));
    assertThat(instance.isCustomMap(officialMap), is(false));
  }

  @Test
  public void testGetLatestVersionMap() {
    MapVersionBean oldestMap = Instancio.create(MapVersionBean.class);
    StepVerifier.create(instance.getMapLatestVersion(oldestMap)).expectNext(oldestMap).verifyComplete();

    MapVersionBean mapVersionBean = Instancio.of(MapVersionBean.class)
                                             .set(field(MapVersionBean::folderName), "palaneum.v0001")
                                             .create();
    Map map = mapMapper.map(mapVersionBean.map(), new CycleAvoidingMappingContext());
    map.setLatestVersion(mapMapper.map(mapVersionBean, new CycleAvoidingMappingContext()));

    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(map));
    StepVerifier.create(instance.getMapLatestVersion(mapVersionBean).map(MapVersionBean::id))
                .expectNext(mapVersionBean.id())
                .verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().string("versions.folderName")
                                                                             .eq(mapVersionBean.folderName()))));

    MapVersionBean newMap = Instancio.create(MapVersionBean.class);
    map = mapMapper.map(mapVersionBean.map(), new CycleAvoidingMappingContext());
    map.setLatestVersion(mapMapper.map(newMap, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(map));
    StepVerifier.create(instance.getMapLatestVersion(mapVersionBean).map(MapVersionBean::id)).expectNext(newMap.id())
                .verifyComplete();
  }

  @Test
  public void testUpdateMapToLatestVersionIfNewVersionExist() throws Exception {
    MapVersionBean outdatedMap = Instancio.of(MapVersionBean.class)
                                          .set(field(MapVersionBean::folderName), "palaneum.v0001")
                                          .create();
    MapVersionBean updatedMap = Instancio.of(MapVersionBean.class)
                                         .set(field(MapVersionBean::folderName), "palaneum.v0002")
                                         .create();
    Map map = mapMapper.map(outdatedMap.map(), new CycleAvoidingMappingContext());
    map.setLatestVersion(mapMapper.map(updatedMap, new CycleAvoidingMappingContext()));

    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(map));

    copyMapsToCustomMapsDirectory(outdatedMap);
    assertThat(checkCustomMapFolderExist(outdatedMap), is(true));
    assertThat(checkCustomMapFolderExist(updatedMap), is(false));
    prepareDownloadMapTask(updatedMap);
    prepareUninstallMapTask(outdatedMap);
    StepVerifier.create(instance.updateLatestVersionIfNecessary(outdatedMap).map(MapVersionBean::id))
                .expectNext(updatedMap.id())
                .verifyComplete();

    assertThat(checkCustomMapFolderExist(outdatedMap), is(false));
    assertThat(checkCustomMapFolderExist(updatedMap), is(true));
  }

  @Test
  public void testUpdateMapToLatestVersionIfOfficalMap() throws Exception {
    MapVersionBean offical = Instancio.create(MapVersionBean.class);
    instance.officialMaps = Set.of(offical.folderName());

    StepVerifier.create(instance.updateLatestVersionIfNecessary(offical)).expectNext(offical).verifyComplete();

    verify(fafApiAccessor, times(0)).getMany(any());
  }

  @Test
  public void testUpdateMapToLatestVersionIfAutoUpdateTurnedOff() throws Exception {
    MapVersionBean map = Instancio.create(MapVersionBean.class);
    preferences.setMapAndModAutoUpdate(false);

    StepVerifier.create(instance.updateLatestVersionIfNecessary(map)).expectNext(map).verifyComplete();

    verify(fafApiAccessor, times(0)).getMany(any());
  }

  @Test
  public void testUpdateMapToLatestVersionIfNoNewVersion() throws Exception {
    MapVersionBean mapVersionBean = Instancio.of(MapVersionBean.class)
                                             .set(field(MapVersionBean::folderName), "palaneum.v0001")
                                             .create();

    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());
    prepareDownloadMapTask(mapVersionBean);

    copyMapsToCustomMapsDirectory(mapVersionBean);
    assertThat(checkCustomMapFolderExist(mapVersionBean), is(true));
    StepVerifier.create(instance.updateLatestVersionIfNecessary(mapVersionBean))
                .expectNext(mapVersionBean)
                .verifyComplete();
    assertThat(checkCustomMapFolderExist(mapVersionBean), is(true));
  }

  @Test
  public void testHideMapVersion() throws Exception {
    MapVersionBean map = Instancio.create(MapVersionBean.class);
    when(fafApiAccessor.patch(any(), any())).thenReturn(Mono.empty());
    when(fafApiAccessor.getOne(any())).thenReturn(Mono.empty());

    StepVerifier.create(instance.hideMapVersion(map)).verifyComplete();

    verify(fafApiAccessor).patch(any(), argThat(mapVersion -> ((MapVersion) mapVersion).getHidden()));
  }

  @Test
  public void testLoadMapNoLargeThumbnailUrl() {
    instance.loadPreview(
        Instancio.of(MapVersionBean.class).set(field(MapVersionBean::thumbnailUrlLarge), null).create(),
        PreviewSize.LARGE);

    verify(assetService).loadAndCacheImage(any(), any(), any());
  }

  @Test
  public void testLoadMapNoSmallThumbnailUrl() {
    instance.loadPreview(
        Instancio.of(MapVersionBean.class).set(field(MapVersionBean::thumbnailUrlSmall), null).create(),
        PreviewSize.SMALL);

    verify(assetService).loadAndCacheImage(any(), any(), any());
  }

  @Test
  public void testFindByMapFolderName() throws Exception {
    MapVersionBean mapVersionBean = Instancio.create(MapVersionBean.class);
    Flux<ElideEntity> resultFlux = Flux.just(mapMapper.map(mapVersionBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.findByMapFolderName("test")).expectNext(mapVersionBean).verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().string("folderName").eq("test"))));
  }

  @Test
  public void testGetMatchMakerMaps() throws Exception {
    MapPoolAssignmentBean mapPoolAssignment1 = Instancio.create(MapPoolAssignmentBean.class);
    MapPoolAssignmentBean mapPoolAssignment2 = Instancio.of(MapPoolAssignmentBean.class)
                                                        .ignore(field(MapPoolAssignmentBean::mapVersion))
                                                        .set(field(MapPoolAssignmentBean::mapParams),
                                                             new NeroxisGeneratorParams().setVersion("0.0.0")
                                                                                         .setSize(512)
                                                                                         .setSpawns(2))
                                                        .create();

    Flux<ElideEntity> resultFlux = Flux.fromIterable(
        matchmakerMapper.mapAssignmentBeans(List.of(mapPoolAssignment1, mapPoolAssignment2),
                                            new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any(), anyString())).thenReturn(resultFlux);
    when(playerService.getCurrentPlayer()).thenReturn(PlayerBeanBuilder.create().defaultValues().get());

    MatchmakerQueueBean matchmakerQueue = MatchmakerQueueBeanBuilder.create().defaultValues().get();
    StepVerifier.create(instance.getMatchmakerMapsWithPageCount(matchmakerQueue, 10, 1)).assertNext(results -> {
      assertThat(results.getT1(), hasSize(2));
      assertThat(results.getT2(), is(1));
    }).verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasDtoClass(MapPoolAssignment.class)), anyString());
  }

  @Test
  public void testGetMatchMakerMapsWithPagination() throws Exception {
    MapPoolAssignmentBean mapPoolAssignment1 = Instancio.of(MapPoolAssignmentBean.class)
                                                        .set(field(MapVersionBean::size).within(
                                                            scope(MapVersionBean.class)), MapSize.valueOf(512, 512))
                                                        .set(field(MapBean::displayName).within(scope(MapBean.class)),
                                                             "a")
                                                        .create();
    MapPoolAssignmentBean mapPoolAssignment2 = Instancio.of(MapPoolAssignmentBean.class)
                                                        .set(field(MapVersionBean::size).within(
                                                            scope(MapVersionBean.class)), MapSize.valueOf(512, 512))
                                                        .set(field(MapBean::displayName).within(scope(MapBean.class)),
                                                             "b")
                                                        .create();
    MapPoolAssignmentBean mapPoolAssignment3 = Instancio.of(MapPoolAssignmentBean.class)
                                                        .set(field(MapVersionBean::size).within(
                                                            scope(MapVersionBean.class)), MapSize.valueOf(1024, 1024))
                                                        .set(field(MapBean::displayName).within(scope(MapBean.class)),
                                                             "c")
                                                        .create();

    Flux<ElideEntity> resultFlux = Flux.fromIterable(matchmakerMapper.mapAssignmentBeans(List.of(mapPoolAssignment1, mapPoolAssignment2, mapPoolAssignment3), new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any(), anyString())).thenReturn(resultFlux);
    when(playerService.getCurrentPlayer()).thenReturn(PlayerBeanBuilder.create().defaultValues().get());

    MatchmakerQueueBean matchmakerQueue = MatchmakerQueueBeanBuilder.create().defaultValues().get();
    StepVerifier.create(instance.getMatchmakerMapsWithPageCount(matchmakerQueue, 1, 2)).assertNext(results -> {
      assertThat(results.getT1(), hasSize(1));
      assertThat(results.getT1().getFirst().id(), is(mapPoolAssignment2.mapVersion().id()));
      assertThat(results.getT2(), is(3));
    }).verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasDtoClass(MapPoolAssignment.class)), anyString());
  }

  @Test
  public void testHasPlayedMap() throws Exception {
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());

    MapVersionBean mapVersion = Instancio.create(MapVersionBean.class);
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    StepVerifier.create(instance.hasPlayedMap(player, mapVersion)).expectNext(false).verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("mapVersion.id")
                                                                             .eq(mapVersion.id())
        .and()
        .intNum("playerStats.player.id")
        .eq(player.getId()))));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasSort("endTime", false)));
  }

  @Test
  public void testGetOwnedMaps() throws Exception {
    MapVersionBean mapVersionBean = Instancio.create(MapVersionBean.class);
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(List.of(mapMapper.map(mapVersionBean, new CycleAvoidingMappingContext())), 1);
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(resultMono);
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    when(playerService.getCurrentPlayer()).thenReturn(player);

    StepVerifier.create(instance.getOwnedMapsWithPageCount(10, 1))
                .expectNext(Tuples.of(List.of(mapVersionBean), 1))
                .verifyComplete();

    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasFilter(qBuilder().string("map.author.id")
        .eq(String.valueOf(player.getId())))));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)));
  }

  @Test
  public void testFindByQuery() throws Exception {
    MapVersionBean mapVersionBean = Instancio.create(MapVersionBean.class);
    Map map = mapMapper.map(mapVersionBean.map(), new CycleAvoidingMappingContext());
    map.setLatestVersion(mapMapper.map(mapVersionBean, new CycleAvoidingMappingContext()));
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(List.of(map), 1);
    when(fafApiAccessor.getManyWithPageCount(any(), anyString())).thenReturn(resultMono);

    SearchConfig searchConfig = new SearchConfig(new SortConfig("testSort", SortOrder.ASC), "testQuery");
    StepVerifier.create(instance.findByQueryWithPageCount(searchConfig, 10, 1))
                .expectNext(Tuples.of(List.of(mapVersionBean), 1))
                .verifyComplete();

    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("testSort", true)), eq("testQuery"));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)), eq("testQuery"));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)), eq("testQuery"));
  }

  @Test
  public void testConvertMapFolderNameToHumanNameIfPossible() {
    assertEquals("dualgap adaptive", instance.convertMapFolderNameToHumanNameIfPossible("dualgap_adaptive.v0012"));
  }

  private void prepareDownloadMapTask(MapVersionBean mapToDownload) {
    StubDownloadMapTask task = new StubDownloadMapTask(forgedAlliancePrefs, i18n, mapsDirectory);
    task.setMapToDownload(mapToDownload);
    when(downloadMapTaskFactory.getObject()).thenReturn(task);
  }

  private void prepareUninstallMapTask(MapVersionBean mapToDelete) {
    UninstallMapTask task = new UninstallMapTask(instance);
    task.setMap(mapToDelete);
    when(uninstallMapTaskFactory.getObject()).thenReturn(task);
  }

  private void copyMapsToCustomMapsDirectory(MapVersionBean... maps) throws Exception {
    for (MapVersionBean map : maps) {
      String folder = map.folderName();
      Path mapPath = Files.createDirectories(mapsDirectory.resolve(folder));
      FileSystemUtils.copyRecursively(Path.of(getClass().getResource("/maps/" + folder).toURI()), mapPath);
    }
  }

  private boolean checkCustomMapFolderExist(MapVersionBean map) throws IOException {
    try (Stream<Path> files = Files.list(mapsDirectory)) {
      return files.anyMatch(path -> path.getFileName().toString().equals(map.folderName()) && path.toFile()
          .isDirectory());
    }
  }
}
