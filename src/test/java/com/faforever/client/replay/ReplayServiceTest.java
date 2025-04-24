package com.faforever.client.replay;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.domain.api.Replay;
import com.faforever.client.domain.api.ReviewsSummary;
import com.faforever.client.featuredmod.FeaturedModService;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameService;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mapstruct.ReplayMapper;
import com.faforever.client.mapstruct.ReviewMapper;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ReplayHistoryPrefs;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.ApiTestUtil;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.user.LoginService;
import com.faforever.client.util.FileSizeReader;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.client.vault.search.SearchController.SortOrder;
import com.faforever.commons.api.dto.GameReviewsSummary;
import com.faforever.commons.api.elide.ElideEntity;
import com.faforever.commons.replay.GameOption;
import com.faforever.commons.replay.ReplayDataParser;
import com.faforever.commons.replay.ReplayMetadata;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.ObjectFactory;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
  public Path tempDirectory;
  public Path cacheDirectory;
  public Path replayDirectory;

  @InjectMocks
  private ReplayService instance;
  @Mock
  private I18n i18n;

  @Mock
  private ReplayRunner replayRunner;
  @Mock
  private ReplayFileReader replayFileReader;
  @Mock
  private NotificationService notificationService;
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
  private FeaturedModService featuredModService;
  @Mock
  private MapService mapService;
  @Mock
  private MapGeneratorService mapGeneratorService;
  @Mock
  private LoginService loginService;
  @Mock
  private ReplayDataParser replayDataParser;
  @Mock
  private FileSizeReader fileSizeReader;
  @Mock
  private ObjectFactory<ReplayDownloadTask> replayDownloadTaskFactory;
  @Spy
  private ReplayMapper replayMapper = Mappers.getMapper(ReplayMapper.class);
  @Spy
  private ReviewMapper reviewMapper = Mappers.getMapper(ReviewMapper.class);
  @Spy
  private ClientProperties clientProperties;
  @Spy
  private DataPrefs dataPrefs;
  @Spy
  private ReplayHistoryPrefs replayHistory;

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(replayMapper);
    MapperSetup.injectMappers(reviewMapper);

    dataPrefs.setBaseDataDirectory(tempDirectory);

    cacheDirectory = Files.createDirectories(dataPrefs.getCacheDirectory());
    replayDirectory = Files.createDirectories(dataPrefs.getReplaysDirectory());

    lenient().when(fileSizeReader.getFileSize(any())).thenReturn(CompletableFuture.completedFuture(1024));

    ReplayMetadata replayMetadata = new ReplayMetadata();
    replayMetadata.setUid(123);
    replayMetadata.setSimMods(emptyMap());
    replayMetadata.setFeaturedModVersions(emptyMap());
    replayMetadata.setFeaturedMod("faf");
    replayMetadata.setMapname(TEST_MAP_NAME);

    lenient().when(replayFileReader.parseReplay(any())).thenReturn(replayDataParser);
    lenient().when(replayDataParser.getMetadata()).thenReturn(replayMetadata);
    lenient().when(replayDataParser.getData()).thenReturn(REPLAY_FIRST_BYTES);
    lenient().when(replayDataParser.getChatMessages()).thenReturn(List.of());
    lenient().when(replayDataParser.getGameOptions()).thenReturn(List.of());
    lenient().when(replayDataParser.getMods()).thenReturn(Map.of());
    lenient().when(replayDataParser.getMap()).thenReturn(TEST_MAP_PATH);
    lenient().when(replayDataParser.getReplayPatchFieldId()).thenReturn(TEST_VERSION_STRING);
    lenient().doAnswer(invocation -> invocation.getArgument(0)).when(taskService).submitTask(any());
  }

  @Test
  public void testParseSupComVersion() throws Exception {
    when(replayDataParser.getReplayPatchFieldId()).thenReturn(TEST_VERSION_STRING);
    Integer version = ReplayService.parseSupComVersion(replayDataParser);

    assertEquals((Integer) 3599, version);
  }

  @Test
  public void testParseMapFolderNamePrefersScenarioFile() throws Exception {
    when(replayDataParser.getMap()).thenReturn(BAD_MAP_PATH);
    when(replayDataParser.getGameOptions()).thenReturn(List.of(new GameOption("ScenarioFile", COOP_MAP_PATH)));

    String mapName = ReplayService.parseMapFolderName(replayDataParser);
    assertEquals(COOP_MAP_NAME, mapName);
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

    StepVerifier.create(instance.loadLocalReplayPage(2, 1)).expectNext(Tuples.of(List.of(), 0));

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
    when(featuredModService.getFeaturedMod(any())).thenReturn(Mono.empty());
    when(mapService.findByMapFolderName(any())).thenReturn(Mono.just(Instancio.create(MapVersion.class)));

    StepVerifier.create(instance.loadLocalReplayPage(1, 1)).assertNext(result -> {
      List<Replay> localReplays = result.getT1();
      assertThat(localReplays, hasSize(1));
      assertThat(localReplays.getFirst().id(), is(123));
      assertThat(localReplays.getFirst().title(), is("title"));
    }).verifyComplete();
  }

  @Test
  public void testRunFafReplayFile() throws Exception {
    Path replayFile = Files.createFile(replayDirectory.resolve("replay.fafreplay"));

    Replay replay = Instancio.of(Replay.class).set(field(Replay::replayFile), replayFile).create();

    instance.runReplay(replay);

    verify(replayRunner).runWithReplay(any(), eq(123), eq("faf"), eq(3599), eq(emptyMap()), eq(emptySet()),
                                       eq(TEST_MAP_NAME));
    verifyNoInteractions(notificationService);
  }

  @Test
  public void testRunFafReplayFileGeneratedMap() throws Exception {
    Path replayFile = Files.createFile(replayDirectory.resolve("replay.fafreplay"));

    Replay replay = Instancio.of(Replay.class).set(field(Replay::replayFile), replayFile).create();

    when(replayDataParser.getMap()).thenReturn(TEST_MAP_PATH_GENERATED);

    instance.runReplay(replay);

    verify(replayRunner).runWithReplay(any(), eq(123), eq("faf"), eq(3599), eq(emptyMap()), eq(emptySet()),
                                       eq(TEST_MAP_NAME_GENERATED));
    verifyNoInteractions(notificationService);
  }

  @Test
  public void testRunScFaReplayFile() throws Exception {
    Path replayFile = Files.createFile(replayDirectory.resolve("replay.scfareplay"));

    Replay replay = Instancio.of(Replay.class).set(field(Replay::replayFile), replayFile).create();

    when(replayFileReader.parseReplay(replayFile)).thenReturn(replayDataParser);

    instance.runReplay(replay);

    verify(replayRunner).runWithReplay(any(), eq(null), eq("faf"), eq(3599), eq(emptyMap()), eq(emptySet()),
                                       eq(TEST_MAP_NAME));
    verifyNoInteractions(notificationService);
  }

  @Test
  public void testRunReplayFileExceptionTriggersNotification() throws Exception {
    Path replayFile = Files.createFile(replayDirectory.resolve("replay.fafreplay"));

    doThrow(new FakeTestException()).when(replayFileReader).parseReplay(replayFile);

    Replay replay = Instancio.of(Replay.class).set(field(Replay::replayFile), replayFile).create();

    instance.runReplay(replay);
    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString());
  }

  @Test
  public void testRunFafReplayFileExceptionTriggersNotification() throws Exception {
    Path replayFile = Files.createFile(replayDirectory.resolve("replay.fafreplay"));

    doThrow(new FakeTestException()).when(replayFileReader).parseReplay(replayFile);

    Replay replay = Instancio.of(Replay.class).set(field(Replay::replayFile), replayFile).create();

    instance.runReplay(replay);
    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString());
  }

  @Test
  public void testOwnReplays() throws Exception {
    Replay replay = Instancio.create(Replay.class);
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(List.of(replayMapper.map(replay)), 1);
    when(fafApiAccessor.getManyWithPageCount(any(), anyString())).thenReturn(resultMono);

    when(loginService.getUserId()).thenReturn(47);
    StepVerifier.create(instance.getOwnReplaysWithPageCount(100, 1)).expectNextCount(1)
                .verifyComplete();
    Condition expectedCondition = qBuilder().intNum("playerStats.player.id")
                                            .eq(47)
                                            .and()
                                            .instant("endTime")
                                            .after(
                                                Instant.now().minus(365, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                                                false);
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasFilter(expectedCondition)), anyString());
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("endTime", false)), anyString());
  }

  @Test
  public void testRunFafOnlineReplay() throws Exception {
    Path replayFile = Files.createFile(replayDirectory.resolve("replay.fafreplay"));

    ReplayDownloadTask replayDownloadTask = mock(ReplayDownloadTask.class);
    when(replayDownloadTask.getFuture()).thenReturn(CompletableFuture.completedFuture(replayFile));
    when(replayDownloadTaskFactory.getObject()).thenReturn(replayDownloadTask);
    Replay replay = Instancio.of(Replay.class).ignore(field(Replay::replayFile)).create();

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
    verify(replayRunner).runWithReplay(any(), eq(123), eq("faf"), eq(3599), eq(emptyMap()), eq(emptySet()),
                                       eq(TEST_MAP_NAME));
    verifyNoInteractions(notificationService);
  }

  @Test
  public void testRunScFaOnlineReplay() throws Exception {
    Path replayFile = Files.createFile(replayDirectory.resolve("replay.scfareplay"));

    ReplayDownloadTask replayDownloadTask = mock(ReplayDownloadTask.class);
    when(replayDownloadTask.getFuture()).thenReturn(CompletableFuture.completedFuture(replayFile));

    when(replayDownloadTaskFactory.getObject()).thenReturn(replayDownloadTask);
    Replay replay = Instancio.of(Replay.class).ignore(field(Replay::replayFile)).create();

    when(replayFileReader.parseReplay(replayFile)).thenReturn(replayDataParser);

    instance.runReplay(replay);

    verify(taskService).submitTask(replayDownloadTask);
    verify(replayRunner).runWithReplay(replayFile, null, "faf", 3599, emptyMap(), emptySet(), TEST_MAP_NAME);
    verifyNoInteractions(notificationService);
  }

  @Test
  public void testRunScFaOnlineReplayExceptionTriggersNotification() throws Exception {
    Path replayFile = Files.createFile(replayDirectory.resolve("replay.scfareplay"));
    doThrow(new FakeTestException()).when(replayFileReader).parseReplay(replayFile);

    ReplayDownloadTask replayDownloadTask = mock(ReplayDownloadTask.class);
    when(replayDownloadTask.getFuture()).thenReturn(CompletableFuture.completedFuture(replayFile));
    when(replayDownloadTaskFactory.getObject()).thenReturn(replayDownloadTask);
    Replay replay = Instancio.of(Replay.class).ignore(field(Replay::replayFile)).create();

    instance.runReplay(replay);

    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString(), anyInt());
    verifyNoMoreInteractions(gameService);
  }

  @Test
  public void testEnrich() throws Exception {
    Path path = Path.of("foo.bar");
    when(replayFileReader.parseReplay(path)).thenReturn(replayDataParser);

    instance.loadReplayDetails(path);

    verify(replayDataParser).getChatMessages();
    verify(replayDataParser, times(2)).getGameOptions();
  }

  @Test
  public void testGetNewest() {
    Replay replay = Instancio.create(Replay.class);
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(List.of(replayMapper.map(replay)), 1);
    when(fafApiAccessor.getManyWithPageCount(any(), anyString())).thenReturn(resultMono);

    StepVerifier.create(instance.getNewestReplaysWithPageCount(10, 1)).expectNextCount(1)
                .verifyComplete();
    Condition expectedCondition = qBuilder().instant("endTime")
                                            .after(Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                                                   false);
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasFilter(expectedCondition)), anyString());
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("endTime", false)), anyString());
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)), anyString());
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)), anyString());
  }

  @Test
  public void testGetHighestRated() {
    ReviewsSummary replayReviewsSummary = Instancio.create(ReviewsSummary.class);
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(
        List.of(reviewMapper.mapToGame(replayReviewsSummary)), 1);
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(resultMono);

    StepVerifier.create(instance.getHighestRatedReplaysWithPageCount(10, 1)).expectNextCount(1)
                .verifyComplete();
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasDtoClass(GameReviewsSummary.class)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("lowerBound", false)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)));
  }

  @Test
  public void testGetReplaysForPlayer() {
    Replay replay = Instancio.create(Replay.class);
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(List.of(replayMapper.map(replay)), 1);
    when(fafApiAccessor.getManyWithPageCount(any(), anyString())).thenReturn(resultMono);

    StepVerifier.create(instance.getReplaysForPlayerWithPageCount(0, 10, 1)).expectNextCount(1)
                .verifyComplete();
    Condition expectedCondition = qBuilder().intNum("playerStats.player.id")
                                            .eq(0)
                                            .and()
                                            .instant("endTime")
                                            .after(
                                                Instant.now().minus(365, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                                                false);
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasFilter(expectedCondition)), anyString());
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("endTime", false)), anyString());
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)), anyString());
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)), anyString());
  }

  @Test
  public void testFindByQuery() {
    Replay replay = Instancio.create(Replay.class);
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = ApiTestUtil.apiPageOf(List.of(replayMapper.map(replay)), 1);
    when(fafApiAccessor.getManyWithPageCount(any(), anyString())).thenReturn(resultMono);

    SearchConfig searchConfig = new SearchConfig(new SortConfig("testSort", SortOrder.ASC), "testQuery");
    StepVerifier.create(instance.findByQueryWithPageCount(searchConfig, 10, 1)).expectNextCount(1)
                .verifyComplete();
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasSort("testSort", true)), eq("testQuery"));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(10)), eq("testQuery"));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageNumber(1)), eq("testQuery"));
  }

  @Test
  public void testFindById() {
    Replay replay = Instancio.create(Replay.class);
    Mono<ElideEntity> resultMono = Mono.just(replayMapper.map(replay));
    when(fafApiAccessor.getOne(any())).thenReturn(resultMono);
    StepVerifier.create(instance.findById(0)).expectNextCount(1).verifyComplete();
    verify(fafApiAccessor).getOne(any());
  }
}
