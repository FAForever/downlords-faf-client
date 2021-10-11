package com.faforever.client.replay;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.MapVersionBeanBuilder;
import com.faforever.client.builders.ReplayBeanBuilder;
import com.faforever.client.builders.ReplayReviewsSummaryBeanBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.domain.ReplayReviewsSummaryBean;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameService;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mapstruct.ReplayMapper;
import com.faforever.client.mapstruct.ReviewMapper;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.ApiTestUtil;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.user.UserService;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.client.vault.search.SearchController.SortOrder;
import com.faforever.commons.api.dto.GameReviewsSummary;
import com.faforever.commons.replay.ReplayDataParser;
import com.faforever.commons.replay.ReplayMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ReplayServiceTest extends ServiceTest {
  /**
   * First 64 bytes of a SCFAReplay file with version 3599. ASCII representation:
   * <pre>
   * Supreme Commande
   * r v1.50.3599....
   * Replay v1.9../ma
   * ps/forbidden pas
   * s.v0001/forbidde
   * n pass.scmap....
   * </pre>
   */
  private static final byte[] REPLAY_FIRST_BYTES = new byte[]{
      0x53, 0x75, 0x70, 0x72, 0x65, 0x6D, 0x65, 0x20, 0x43, 0x6F, 0x6D, 0x6D, 0x61, 0x6E, 0x64, 0x65,
      0x72, 0x20, 0x76, 0x31, 0x2E, 0x35, 0x30, 0x2E, 0x33, 0x35, 0x39, 0x39, 0x00, 0x0D, 0x0A, 0x00,
      0x52, 0x65, 0x70, 0x6C, 0x61, 0x79, 0x20, 0x76, 0x31, 0x2E, 0x39, 0x0D, 0x0A, 0x2F, 0x6D, 0x61,
      0x70, 0x73, 0x2F, 0x66, 0x6F, 0x72, 0x62, 0x69, 0x64, 0x64, 0x65, 0x6E, 0x20, 0x70, 0x61, 0x73,
      0x73, 0x2E, 0x76, 0x30, 0x30, 0x30, 0x31, 0x2F, 0x66, 0x6F, 0x72, 0x62, 0x69, 0x64, 0x64, 0x65,
      0x6E, 0x20, 0x70, 0x61, 0x73, 0x73, 0x2E, 0x73, 0x63, 0x6D, 0x61, 0x70, 0x00, 0x0D, 0x0A, 0x1A
  };
  private static final String TEST_VERSION_STRING = "Supreme Commander v1.50.3599";
  private static final String TEST_MAP_PATH = "/maps/forbidden_pass.v0001/forbidden_pass_scenario.lua";
  private static final String TEST_MAP_NAME = "forbidden_pass.v0001";
  private static final String COOP_MAP_PATH = "/maps/scca_coop_r02.v0015/scca_coop_r02_scenario.lua";
  private static final String COOP_MAP_NAME = "scca_coop_r02.v0015";
  private static final String BAD_MAP_PATH = "/maps/forbidden_?pass.v0001/forbidden_pass_?scenario.lua";
  private static final String TEST_MAP_PATH_GENERATED = "/maps/neroxis_map_generator_1.0.0_ABcd/neroxis_map_generator_1.0.0_ABcd_scenario.lua";
  private static final String TEST_MAP_NAME_GENERATED = "neroxis_map_generator_1.0.0_ABcd";

  @TempDir
  public Path replayDirectory;
  @TempDir
  public Path cacheDirectory;
  private ReplayService instance;
  @Mock
  private I18n i18n;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private ReplayFileReader replayFileReader;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private TaskService taskService;
  @Mock
  private GameService gameService;
  @Mock
  private PlayerService playerService;
  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private ReportingService reportingService;
  @Mock
  private PlatformService platformService;
  @Mock
  private ModService modService;
  @Mock
  private MapService mapService;
  @Mock
  private MapGeneratorService mapGeneratorService;
  @Mock
  private UserService userService;
  @Mock
  private ReplayDataParser replayDataParser;

  private ReplayMapper replayMapper = Mappers.getMapper(ReplayMapper.class);
  private ReviewMapper reviewMapper = Mappers.getMapper(ReviewMapper.class);

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(replayMapper);
    MapperSetup.injectMappers(reviewMapper);

    instance = new ReplayService(new ClientProperties(), preferencesService, userService, replayFileReader, notificationService, gameService, playerService,
        taskService, i18n, reportingService, applicationContext, platformService, fafApiAccessor, modService, mapService, replayMapper);

    ReplayMetadata replayMetadata = new ReplayMetadata();
    replayMetadata.setUid(123);
    replayMetadata.setSimMods(emptyMap());
    replayMetadata.setFeaturedModVersions(emptyMap());
    replayMetadata.setFeaturedMod("faf");
    replayMetadata.setMapname(TEST_MAP_NAME);

    when(replayFileReader.parseReplay(any())).thenReturn(replayDataParser);
    when(replayDataParser.getMetadata()).thenReturn(replayMetadata);
    when(replayDataParser.getData()).thenReturn(REPLAY_FIRST_BYTES);
    when(replayDataParser.getChatMessages()).thenReturn(List.of());
    when(replayDataParser.getGameOptions()).thenReturn(List.of());
    when(replayDataParser.getMods()).thenReturn(Map.of());
    when(replayDataParser.getMap()).thenReturn(TEST_MAP_PATH);
    when(replayDataParser.getReplayPatchFieldId()).thenReturn(TEST_VERSION_STRING);
    when(preferencesService.getReplaysDirectory()).thenReturn(replayDirectory);
    Path corruptDirectory = Files.createDirectories(replayDirectory.resolve("corrupt"));
    when(preferencesService.getCorruptedReplaysDirectory()).thenReturn(corruptDirectory);
    when(preferencesService.getCacheDirectory()).thenReturn(cacheDirectory);
    doAnswer(invocation -> invocation.getArgument(0)).when(taskService).submitTask(any());
  }

  @Test
  public void testParseSupComVersion() throws Exception {
    when(replayDataParser.getReplayPatchFieldId()).thenReturn(TEST_VERSION_STRING);
    Integer version = ReplayService.parseSupComVersion(replayDataParser);

    assertEquals((Integer) 3599, version);
  }

  @Test
  public void testParseMapFolderName() throws Exception {
    when(replayDataParser.getMap()).thenReturn(COOP_MAP_PATH);
    String mapName = ReplayService.parseMapFolderName(replayDataParser);
    assertEquals(COOP_MAP_NAME, mapName);
  }

  @Test
  public void testParseBadFolderNameThrowsException() throws Exception {
    when(replayDataParser.getMap()).thenReturn(BAD_MAP_PATH);
    assertThrows(IllegalArgumentException.class, () -> ReplayService.parseMapFolderName(replayDataParser));
  }

  @Test
  public void testGuessModByFileNameModIsMissing() throws Exception {
    String mod = ReplayService.guessModByFileName("110621-2128 Saltrock Colony.SCFAReplay");

    assertEquals(KnownFeaturedMod.DEFAULT.getTechnicalName(), mod);
  }

  @Test
  public void testGuessModByFileNameModIsBlackops() throws Exception {
    String mod = ReplayService.guessModByFileName("110621-2128 Saltrock Colony.blackops.SCFAReplay");

    assertEquals("blackops", mod);
  }

  @Test
  public void testGetLocalReplaysMovesCorruptFiles() throws Exception {
    Path file1 = Files.createFile(replayDirectory.resolve("replay.fafreplay"));
    Path file2 = Files.createFile(replayDirectory.resolve("replay2.fafreplay"));

    doThrow(new FakeTestException()).when(replayFileReader).parseReplay(file1);
    doThrow(new FakeTestException()).when(replayFileReader).parseReplay(file2);

    Collection<ReplayBean> localReplays = new ArrayList<>();
    try {
      localReplays.addAll(instance.loadLocalReplayPage(2, 1).get().getT1());
    } catch (FakeTestException exception) {
      // expected
    }

    assertThat(localReplays, empty());
    verify(notificationService, times(2)).addNotification(any(PersistentNotification.class));

    assertThat(Files.exists(file1), is(false));
    assertThat(Files.exists(file2), is(false));
  }

  @Test
  public void testLoadLocalReplays() throws Exception {
    Path file1 = Files.createFile(replayDirectory.resolve("replay.fafreplay"));

    ReplayMetadata replayMetadata = new ReplayMetadata();
    replayMetadata.setUid(123);
    replayMetadata.setTitle("title");

    when(replayDataParser.getMetadata()).thenReturn(replayMetadata);
    when(replayFileReader.parseReplay(file1)).thenReturn(replayDataParser);
    when(modService.getFeaturedMod(any())).thenReturn(CompletableFuture.completedFuture(null));
    when(mapService.findByMapFolderName(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(MapVersionBeanBuilder.create().defaultValues().get())));

    List<ReplayBean> localReplays = instance.loadLocalReplayPage(1, 1).get().getT1();

    assertThat(localReplays, hasSize(1));
    assertThat(localReplays.get(0).getId(), is(123));
    assertThat(localReplays.get(0).getTitle(), is("title"));
  }

  @Test
  public void testRunFafReplayFile() throws Exception {
    Path replayFile = Files.createFile(replayDirectory.resolve("replay.fafreplay"));

    ReplayBean replay = new ReplayBean();
    replay.setReplayFile(replayFile);

    instance.runReplay(replay);

    verify(gameService).runWithReplay(any(), eq(123), eq("faf"), eq(3599), eq(emptyMap()), eq(emptySet()), eq(TEST_MAP_NAME));
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void testRunFafReplayFileGeneratedMap() throws Exception {
    Path replayFile = Files.createFile(replayDirectory.resolve("replay.fafreplay"));

    ReplayBean replay = new ReplayBean();
    replay.setReplayFile(replayFile);

    when(replayDataParser.getMap()).thenReturn(TEST_MAP_PATH_GENERATED);
    when(mapGeneratorService.isGeneratedMap(TEST_MAP_NAME_GENERATED)).thenReturn(true);

    instance.runReplay(replay);

    verify(gameService).runWithReplay(any(), eq(123), eq("faf"), eq(3599), eq(emptyMap()), eq(emptySet()), eq(TEST_MAP_NAME_GENERATED));
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void testRunScFaReplayFile() throws Exception {
    Path replayFile = Files.createFile(replayDirectory.resolve("replay.scfareplay"));

    ReplayBean replay = new ReplayBean();
    replay.setReplayFile(replayFile);

    when(replayFileReader.parseReplay(replayFile)).thenReturn(replayDataParser);

    instance.runReplay(replay);

    verify(gameService).runWithReplay(any(), eq(null), eq("faf"), eq(3599), eq(emptyMap()), eq(emptySet()), eq(TEST_MAP_NAME));
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void testRunReplayFileExceptionTriggersNotification() throws Exception {
    Path replayFile = Files.createFile(replayDirectory.resolve("replay.fafreplay"));

    doThrow(new FakeTestException()).when(replayFileReader).parseReplay(replayFile);

    ReplayBean replay = new ReplayBean();
    replay.setReplayFile(replayFile);

    instance.runReplay(replay);
    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString());
  }

  @Test
  public void testRunFafReplayFileExceptionTriggersNotification() throws Exception {
    Path replayFile = Files.createFile(replayDirectory.resolve("replay.fafreplay"));

    doThrow(new FakeTestException()).when(replayFileReader).parseReplay(replayFile);

    ReplayBean replay = new ReplayBean();
    replay.setReplayFile(replayFile);

    instance.runReplay(replay);
    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString());
  }

  @Test
  public void testOwnReplays() throws Exception {
    ReplayBean replayBean = ReplayBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(ApiTestUtil.apiPageOf(List.of(replayMapper.map(replayBean, new CycleAvoidingMappingContext())), 1));

    when(userService.getUserId()).thenReturn(47);
    List<ReplayBean> results = instance.getOwnReplaysWithPageCount(100, 1).join().getT1();
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasFilter(qBuilder().intNum("playerStats.player.id").eq(47).and().instant("endTime").after(Instant.now().minus(365, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), false))));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("endTime", false)));
    assertThat(results, contains(replayBean));
  }

  @Test
  public void testRunFafOnlineReplay() throws Exception {
    Path replayFile = Files.createFile(replayDirectory.resolve("replay.fafreplay"));

    ReplayDownloadTask replayDownloadTask = mock(ReplayDownloadTask.class);
    when(replayDownloadTask.getFuture()).thenReturn(CompletableFuture.completedFuture(replayFile));
    when(applicationContext.getBean(ReplayDownloadTask.class)).thenReturn(replayDownloadTask);
    ReplayBean replay = new ReplayBean();

    ReplayMetadata replayMetadata = new ReplayMetadata();
    replayMetadata.setUid(123);
    replayMetadata.setSimMods(emptyMap());
    replayMetadata.setFeaturedModVersions(emptyMap());
    replayMetadata.setFeaturedMod("faf");
    replayMetadata.setMapname(TEST_MAP_NAME);

    when(replayFileReader.parseReplay(replayFile)).thenReturn(replayDataParser);
    when(replayDataParser.getMetadata()).thenReturn(replayMetadata);

    instance.runReplay(replay);

    verify(taskService).submitTask(replayDownloadTask);
    verify(gameService).runWithReplay(any(), eq(123), eq("faf"), eq(3599), eq(emptyMap()), eq(emptySet()), eq(TEST_MAP_NAME));
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void testRunScFaOnlineReplay() throws Exception {
    Path replayFile = Files.createFile(replayDirectory.resolve("replay.scfareplay"));

    ReplayDownloadTask replayDownloadTask = mock(ReplayDownloadTask.class);
    when(replayDownloadTask.getFuture()).thenReturn(CompletableFuture.completedFuture(replayFile));

    when(applicationContext.getBean(ReplayDownloadTask.class)).thenReturn(replayDownloadTask);
    ReplayBean replay = new ReplayBean();

    when(replayFileReader.parseReplay(replayFile)).thenReturn(replayDataParser);

    instance.runReplay(replay);

    verify(taskService).submitTask(replayDownloadTask);
    verify(gameService).runWithReplay(replayFile, null, "faf", 3599, emptyMap(), emptySet(), TEST_MAP_NAME);
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void testRunScFaOnlineReplayExceptionTriggersNotification() throws Exception {
    Path replayFile = Files.createFile(replayDirectory.resolve("replay.scfareplay"));
    doThrow(new FakeTestException()).when(replayFileReader).parseReplay(replayFile);

    ReplayDownloadTask replayDownloadTask = mock(ReplayDownloadTask.class);
    when(replayDownloadTask.getFuture()).thenReturn(CompletableFuture.completedFuture(replayFile));
    when(applicationContext.getBean(ReplayDownloadTask.class)).thenReturn(replayDownloadTask);
    ReplayBean replay = new ReplayBean();

    instance.runReplay(replay);

    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString(), anyInt());
    verifyNoMoreInteractions(gameService);
  }

  @Test
  public void testRunLiveReplay() throws Exception {
    when(gameService.runWithLiveReplay(any(URI.class), anyInt(), anyString(), anyString()))
        .thenReturn(CompletableFuture.completedFuture(null));

    instance.runLiveReplay(new URI("faflive://example.com/123/456.scfareplay?mod=faf&map=map%20name"));

    verify(gameService).runWithLiveReplay(new URI("gpgnet://example.com/123/456.scfareplay"), 123, "faf", "map name");
  }

  @Test
  public void testEnrich() throws Exception {
    Path path = Path.of("foo.bar");
    when(replayFileReader.parseReplay(path)).thenReturn(replayDataParser);

    instance.enrich(new ReplayBean(), path);

    verify(replayDataParser).getChatMessages();
    verify(replayDataParser).getGameOptions();
  }

  @Test
  public void testGetNewest() {
    ReplayBean replayBean = ReplayBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(ApiTestUtil.apiPageOf(List.of(replayMapper.map(replayBean, new CycleAvoidingMappingContext())), 1));

    List<ReplayBean> results = instance.getNewestReplaysWithPageCount(10, 1).join().getT1();
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasFilter(qBuilder().instant("endTime").after(Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), false))));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("endTime", false)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)));
    assertThat(results, contains(replayBean));
  }

  @Test
  public void testGetHighestRated() {
    ReplayReviewsSummaryBean replayReviewsSummaryBean = ReplayReviewsSummaryBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(ApiTestUtil.apiPageOf(List.of(reviewMapper.map(replayReviewsSummaryBean, new CycleAvoidingMappingContext())), 1));

    List<ReplayBean> results = instance.getHighestRatedReplaysWithPageCount(10, 1).join().getT1();
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasDtoClass(GameReviewsSummary.class)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("lowerBound", false)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)));
    assertThat(results, contains(replayReviewsSummaryBean.getReplay()));
  }

  @Test
  public void testGetReplaysForPlayer() {
    ReplayBean replayBean = ReplayBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(ApiTestUtil.apiPageOf(List.of(replayMapper.map(replayBean, new CycleAvoidingMappingContext())), 1));

    List<ReplayBean> results = instance.getReplaysForPlayerWithPageCount(0, 10, 1).join().getT1();
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasFilter(qBuilder().intNum("playerStats.player.id").eq(0).and().instant("endTime").after(Instant.now().minus(365, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), false))));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("endTime", false)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)));
    assertThat(results, contains(replayBean));
  }

  @Test
  public void testFindByQuery() {
    ReplayBean replayBean = ReplayBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.getManyWithPageCount(any(), anyString())).thenReturn(ApiTestUtil.apiPageOf(List.of(replayMapper.map(replayBean, new CycleAvoidingMappingContext())), 1));

    SearchConfig searchConfig = new SearchConfig(new SortConfig("testSort", SortOrder.ASC), "testQuery");
    List<ReplayBean> results = instance.findByQueryWithPageCount(searchConfig, 10, 1).join().getT1();
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("testSort", true)), eq("testQuery"));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)), eq("testQuery"));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)), eq("testQuery"));
    assertThat(results, contains(replayBean));
  }

  @Test
  public void testFindById() {
    ReplayBean replayBean = ReplayBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.getOne(any())).thenReturn(Mono.just(replayMapper.map(replayBean, new CycleAvoidingMappingContext())));
    Optional<ReplayBean> result = instance.findById(0).join();
    verify(fafApiAccessor).getOne(any());
    assertThat(result.orElse(null), is(replayBean));
  }
}
