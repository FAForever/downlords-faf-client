package com.faforever.client.game;

import com.faforever.client.builders.FeaturedModBeanBuilder;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.GameInfoMessageBuilder;
import com.faforever.client.builders.GameLaunchMessageBuilder;
import com.faforever.client.builders.LeagueEntryBeanBuilder;
import com.faforever.client.builders.NewGameInfoBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.discord.DiscordRichPresenceService;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fa.ForgedAllianceService;
import com.faforever.client.fa.GameParameters;
import com.faforever.client.fa.relay.ice.CoturnService;
import com.faforever.client.fa.relay.ice.IceAdapter;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.logging.LoggingService;
import com.faforever.client.map.MapService;
import com.faforever.client.mapstruct.GameMapper;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.patch.GameUpdater;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.LastGamePrefs;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.replay.ReplayServer;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.ui.preferences.GameDirectoryRequiredHandler;
import com.faforever.commons.lobby.GameInfo;
import com.faforever.commons.lobby.GameInfo.TeamIds;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameType;
import com.faforever.commons.lobby.NoticeInfo;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.publisher.TestPublisher;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.faforever.client.game.KnownFeaturedMod.FAF;
import static com.faforever.commons.lobby.GameStatus.CLOSED;
import static com.faforever.commons.lobby.GameStatus.OPEN;
import static com.faforever.commons.lobby.GameStatus.PLAYING;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameServiceTest extends ServiceTest {

  private static final long TIMEOUT = 5000;
  private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
  private static final Integer GPG_PORT = 1234;
  private static final int LOCAL_REPLAY_PORT = 15111;
  private static final String LADDER_1v1_RATING_TYPE = "ladder_1v1";

  @InjectMocks
  @Spy
  private GameService instance;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private MapService mapService;
  @Mock
  private ForgedAllianceService forgedAllianceService;
  @Mock
  private GameUpdater gameUpdater;
  @Mock
  private PlayerService playerService;
  @Mock
  private ExecutorService executorService;
  @Mock
  private ReplayServer replayServer;
  @Mock
  private IceAdapter iceAdapter;
  @Mock
  private ModService modService;
  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private PlatformService platformService;
  @Mock
  private DiscordRichPresenceService discordRichPresenceService;
  @Mock
  private LoggingService loggingService;
  @Mock
  private Process process;
  @Mock
  private CoturnService coturnService;
  @Mock
  private FxApplicationThreadExecutor fxApplicationThreadExecutor;
  @Mock
  private GameDirectoryRequiredHandler gameDirectoryRequiredHandler;
  @Spy
  private GameMapper gameMapper = Mappers.getMapper(GameMapper.class);
  @Spy
  private ClientProperties clientProperties;
  @Spy
  private ForgedAlliancePrefs forgedAlliancePrefs;
  @Spy
  private LastGamePrefs lastGamePrefs;
  @Spy
  private NotificationPrefs notificationPrefs;

  @Captor
  private ArgumentCaptor<Set<String>> simModsCaptor;

  private PlayerBean junitPlayer;

  private final TestPublisher<GameInfo> testGamePublisher = TestPublisher.create();
  private final TestPublisher<NoticeInfo> testNoticePublisher = TestPublisher.create();

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(gameMapper);
    junitPlayer = PlayerBeanBuilder.create().defaultValues().get();

    when(fxApplicationThreadExecutor.asScheduler()).thenReturn(Schedulers.immediate());
    when(fafServerAccessor.getEvents(GameInfo.class)).thenReturn(testGamePublisher.flux());
    when(fafServerAccessor.getEvents(NoticeInfo.class)).thenReturn(testNoticePublisher.flux());
    when(coturnService.getSelectedCoturns(anyInt())).thenReturn(completedFuture(List.of()));
    when(preferencesService.isValidGamePath()).thenReturn(true);
    when(fafServerAccessor.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>());
    when(replayServer.start(anyInt(), any())).thenReturn(completedFuture(LOCAL_REPLAY_PORT));
    when(iceAdapter.start(anyInt())).thenReturn(completedFuture(GPG_PORT));
    when(playerService.getCurrentPlayer()).thenReturn(junitPlayer);

    doAnswer(invocation -> {
      try {
        ((Runnable) invocation.getArgument(0)).run();
      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }).when(executorService).execute(any());

    instance.afterPropertiesSet();

    testGamePublisher.assertSubscribers(1);
    testNoticePublisher.assertSubscribers(1);
  }

  private void mockStartGameProcess(GameParameters gameParameters) throws IOException {
    gameParameters.setLocalGpgPort(GPG_PORT);
    gameParameters.setLocalReplayPort(LOCAL_REPLAY_PORT);
    when(forgedAllianceService.startGameOnline(gameParameters)
    ).thenReturn(process);
  }

  private void mockStartReplayProcess(Path path, int id) throws IOException {
    when(forgedAllianceService.startReplay(path, id)).thenReturn(process);
  }

  private void mockStartLiveReplayProcess(URI replayUrl, int gameId) throws IOException {
    when(forgedAllianceService.startReplay(replayUrl, gameId)).thenReturn(process);
    when(instance.getByUid(gameId)).thenReturn(GameBeanBuilder.create().defaultValues().get());
  }


  private void mockMatchmakerChain() {
    when(modService.getFeaturedMod(FAF.getTechnicalName()))
        .thenReturn(Mono.just(FeaturedModBeanBuilder.create().defaultValues().get()));
    when(gameUpdater.update(any(), any(), any(), any(), anyBoolean())).thenReturn(completedFuture(null));
    when(fafServerAccessor.startSearchMatchmaker()).thenReturn(new CompletableFuture<>());
  }

  @Test
  public void testRetryFlux() {
    doThrow(new RuntimeException()).when(gameMapper).update(any(), any());

    testGamePublisher.next(GameInfoMessageBuilder.create(1).defaultValues().get());

    testGamePublisher.assertSubscribers(1);
  }

  @Test
  public void testJoinGameMapIsAvailable() throws Exception {
    GameBean game = GameBeanBuilder.create().defaultValues().get();

    ObservableMap<String, String> simMods = FXCollections.observableHashMap();
    simMods.put("123-456-789", "Fake mod name");

    game.setSimMods(simMods);

    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();
    GameParameters gameParameters = gameMapper.map(gameLaunchMessage);

    mockStartGameProcess(gameParameters);
    when(mapService.isInstalled(game.getMapFolderName())).thenReturn(true);
    when(fafServerAccessor.requestJoinGame(game.getId(), null)).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any(), anyBoolean())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(Mono.just(FeaturedModBeanBuilder.create()
        .defaultValues()
        .get()));

    CompletableFuture<Void> future = instance.joinGame(game, null).toCompletableFuture();

    assertNull(future.get(TIMEOUT, TIME_UNIT));
    verify(mapService, never()).download(any());
    verify(replayServer).start(eq(game.getId()), any());

    verify(forgedAllianceService).startGameOnline(gameParameters);
  }

  @Test
  public void testStartReplayWhileInGameAllowed() throws Exception {
    GameBean game = GameBeanBuilder.create().defaultValues().get();

    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();
    Path replayPath = Path.of("temp.scfareplay");
    int replayId = 1234;
    GameParameters gameParameters = gameMapper.map(gameLaunchMessage);

    mockStartGameProcess(gameParameters);
    when(mapService.isInstalled(anyString())).thenReturn(true);
    when(fafServerAccessor.requestJoinGame(anyInt(), isNull())).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any(), anyBoolean())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(anyString())).thenReturn(Mono.just(FeaturedModBeanBuilder.create()
        .defaultValues()
        .get()));
    when(process.isAlive()).thenReturn(false);

    CompletableFuture<Void> future = instance.joinGame(game, null).toCompletableFuture();
    future.join();
    mockStartReplayProcess(replayPath, replayId);
    when(process.isAlive()).thenReturn(true);
    future = instance.runWithReplay(replayPath, replayId, "", null, null, null, "map");
    future.join();

    verify(replayServer).start(eq(game.getId()), any());
    verify(forgedAllianceService).startGameOnline(gameParameters);
    verify(forgedAllianceService).startReplay(replayPath, replayId);
  }

  @Test
  public void testStartLiveReplayWhileInGameAllowed() throws Exception {
    GameBean game = GameBeanBuilder.create().defaultValues().get();

    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();
    URI replayUrl = new URI("gpgnet://example.com/123/456.scfareplay");
    int gameId = 1234;
    GameParameters gameParameters = gameMapper.map(gameLaunchMessage);

    mockStartGameProcess(gameParameters);
    when(mapService.isInstalled(anyString())).thenReturn(true);
    when(fafServerAccessor.requestJoinGame(anyInt(), isNull())).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any(), anyBoolean())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(anyString())).thenReturn(Mono.just(FeaturedModBeanBuilder.create()
        .defaultValues()
        .get()));
    when(process.isAlive()).thenReturn(false);

    CompletableFuture<Void> future = instance.joinGame(game, null).toCompletableFuture();
    future.join();
    mockStartLiveReplayProcess(replayUrl, gameId);
    when(process.isAlive()).thenReturn(true);
    future = instance.runWithLiveReplay(replayUrl, gameId, "faf", "map");
    future.join();

    verify(replayServer).start(eq(game.getId()), any());
    verify(forgedAllianceService).startGameOnline(gameParameters);
    verify(forgedAllianceService).startReplay(replayUrl, gameId);
  }

  @Test
  public void testModEnabling() throws Exception {
    GameBean game = GameBeanBuilder.create().defaultValues().get();

    ObservableMap<String, String> simMods = FXCollections.observableHashMap();
    simMods.put("123-456-789", "Fake mod name");

    game.setSimMods(simMods);
    game.setMapFolderName("map");

    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();
    GameParameters gameParameters = new GameParameters();

    mockStartGameProcess(gameParameters);
    when(mapService.isInstalled("map")).thenReturn(true);
    when(fafServerAccessor.requestJoinGame(game.getId(), null)).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any(), anyBoolean())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(Mono.just(FeaturedModBeanBuilder.create()
        .defaultValues()
        .get()));

    instance.joinGame(game, null).toCompletableFuture().get();
    verify(modService).enableSimMods(simModsCaptor.capture());
    assertEquals(simModsCaptor.getValue().iterator().next(), "123-456-789");
  }

  @Test
  public void testAddOnGameStartedListener() throws Exception {
    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();
    GameParameters gameParameters = gameMapper.map(gameLaunchMessage);

    mockStartGameProcess(gameParameters);
    when(process.onExit()).thenReturn(CompletableFuture.completedFuture(process));
    when(gameUpdater.update(any(), any(), any(), any(), anyBoolean())).thenReturn(completedFuture(null));
    when(fafServerAccessor.requestHostGame(newGameInfo)).thenReturn(completedFuture(gameLaunchMessage));
    when(mapService.download(newGameInfo.getMap())).thenReturn(completedFuture(null));

    CountDownLatch gameStartedLatch = new CountDownLatch(1);
    CountDownLatch gameTerminatedLatch = new CountDownLatch(1);
    instance.gameRunningProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        gameStartedLatch.countDown();
      } else {
        gameTerminatedLatch.countDown();
      }
    });

    CountDownLatch processLatch = new CountDownLatch(1);

    instance.hostGame(newGameInfo).toCompletableFuture().get(TIMEOUT, TIME_UNIT);
    gameStartedLatch.await(TIMEOUT, TIME_UNIT);
    processLatch.countDown();

    gameTerminatedLatch.await(TIMEOUT, TIME_UNIT);
    verify(forgedAllianceService).startGameOnline(gameParameters);
    verify(replayServer).start(eq(gameLaunchMessage.getUid()), any());
  }

  @Test
  public void testWaitForProcessTerminationInBackground() throws Exception {
    instance.gameRunning.set(true);

    CompletableFuture<Void> disconnectedFuture = new CompletableFuture<>();

    instance.gameRunningProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        disconnectedFuture.complete(null);
      }
    });

    Process process = mock(Process.class);
    when(process.onExit()).thenReturn(CompletableFuture.completedFuture(process));

    instance.spawnTerminationListener(process, true);

    disconnectedFuture.get(5000, TimeUnit.MILLISECONDS);

    verify(process).onExit();
    verify(process).exitValue();
  }

  @Test
  public void testPlayerLeftOpenGame() {
    PlayerBean player1 = PlayerBeanBuilder.create().defaultValues().id(1).get();
    PlayerBean player2 = PlayerBeanBuilder.create().defaultValues().id(2).get();

    when(playerService.getPlayerByIdIfOnline(1)).thenReturn(Optional.of(player1));
    when(playerService.getPlayerByIdIfOnline(2)).thenReturn(Optional.of(player2));

    List<TeamIds> teamIds = List.of(new TeamIds(1, List.of(1)), new TeamIds(2, List.of(2)));

    testGamePublisher.next(GameInfoMessageBuilder.create(0).defaultValues().teamIds(teamIds).get());
    GameBean game = instance.getByUid(0);

    assertThat(player1.getGame(), is(game));
    assertThat(player2.getGame(), is(game));

    game.setTeams(Map.of(2, List.of(2)));

    assertThat(player1.getGame(), is(CoreMatchers.nullValue()));
    assertThat(player2.getGame(), is(game));
  }

  @Test
  public void testOnGames() {
    assertThat(instance.getGames(), empty());

    GameInfo multiGameInfo = GameInfoMessageBuilder.create(1)
        .games(List.of(GameInfoMessageBuilder.create(1).defaultValues().get(),
                       GameInfoMessageBuilder.create(2).defaultValues().get())
        ).get();
    testGamePublisher.next(multiGameInfo);


    assertThat(instance.getGames(), hasSize(2));
  }

  @Test
  public void testOnGameInfoAdd() {
    assertThat(instance.getGames(), empty());

    GameInfo gameInfo1 = GameInfoMessageBuilder.create(1).defaultValues().title("Game 1").get();
    testGamePublisher.next(gameInfo1);

    GameInfo gameInfo2 = GameInfoMessageBuilder.create(2).defaultValues().title("Game 2").get();
    testGamePublisher.next(gameInfo2);


    assertThat(instance.getGames(), containsInAnyOrder(
        allOf(
            GameMatchers.hasId(1),
            GameMatchers.hasTitle("Game 1")
        ),
        allOf(
            GameMatchers.hasId(2),
            GameMatchers.hasTitle("Game 2")
        )
    ));
  }

  @Test
  public void testCurrentGameEnhancedWithPassword() {
    lastGamePrefs.setLastGamePassword("banana");
    instance.currentGame.set(null);

    when(playerService.isCurrentPlayerInGame(any())).thenReturn(true);

    GameInfo gameInfo = GameInfoMessageBuilder.create(1)
        .defaultValues()
        .host("me")
        .addTeamMember("1", "me")
        .passwordProtected(true)
        .title("Game 1")
        .get();
    testGamePublisher.next(gameInfo);

    assertThat(instance.currentGame.get().getPassword(), is("banana"));
  }

  @Test
  public void testOnGameInfoSetsCurrentGameIfUserIsInAndStatusOpen() {
    assertThat(instance.getCurrentGame(), nullValue());

    when(playerService.isCurrentPlayerInGame(any())).thenReturn(true);

    GameInfo gameInfo = GameInfoMessageBuilder.create(1234).defaultValues()
        .state(OPEN)
        .addTeamMember("1", "PlayerName").get();
    testGamePublisher.next(gameInfo);


    assertThat(instance.getCurrentGame(), notNullValue());
    assertThat(instance.getCurrentGame().getId(), is(1234));
  }

  @Test
  public void testOnGameInfoDoesntSetCurrentGameIfUserIsInAndStatusNotOpen() {
    assertThat(instance.getCurrentGame(), nullValue());

    when(playerService.isCurrentPlayerInGame(any())).thenReturn(true);

    GameInfo gameInfo = GameInfoMessageBuilder.create(1234).defaultValues()
        .state(PLAYING)
        .addTeamMember("1", "PlayerName").get();
    testGamePublisher.next(gameInfo);

    assertThat(instance.getCurrentGame(), nullValue());
  }

  @Test
  public void testOnGameInfoDoesntSetCurrentGameIfUserDoesntMatch() {
    assertThat(instance.getCurrentGame(), nullValue());

    when(playerService.isCurrentPlayerInGame(any())).thenReturn(false);

    GameInfo gameInfo = GameInfoMessageBuilder.create(1234).defaultValues().addTeamMember("1", "Other").get();
    testGamePublisher.next(gameInfo);

    assertThat(instance.getCurrentGame(), nullValue());
  }

  @Test
  public void testOnGameInfoModify() throws InterruptedException {
    assertThat(instance.getGames(), empty());

    GameInfo gameInfo = GameInfoMessageBuilder.create(1).defaultValues().title("Game 1").state(PLAYING).get();
    testGamePublisher.next(gameInfo);

    GameBean game = instance.getByUid(1);

    gameInfo = GameInfoMessageBuilder.create(1).defaultValues().title("Game 1 modified").state(PLAYING).get();
    testGamePublisher.next(gameInfo);

    assertEquals(gameInfo.getTitle(), game.getTitle());
  }

  @Test
  public void testOnGameInfoRemove() {
    assertThat(instance.getGames(), empty());

    GameInfo gameInfo = GameInfoMessageBuilder.create(1).defaultValues().title("Game 1").get();
    testGamePublisher.next(gameInfo);
    assertThat(instance.getGames(), hasSize(1));

    gameInfo = GameInfoMessageBuilder.create(1).title("Game 1").defaultValues().state(CLOSED).get();
    testGamePublisher.next(gameInfo);

    assertThat(instance.getGames(), empty());
  }

  @Test
  public void testStartSearchLadder1v1() throws Exception {
    int uid = 123;
    String map = "scmp_037";
    GameLaunchResponse gameLaunchMessage = new GameLaunchMessageBuilder().defaultValues()
        .uid(uid).mod("FAF").mapname(map)
        .expectedPlayers(2)
        .faction(com.faforever.commons.lobby.Faction.CYBRAN)
        .gameType(GameType.MATCHMAKER)
        .mapPosition(4)
        .team(1)
        .ratingType(LADDER_1v1_RATING_TYPE)
        .get();

    FeaturedModBean featuredMod = FeaturedModBeanBuilder.create().defaultValues().get();
    GameParameters gameParameters = gameMapper.map(gameLaunchMessage);
    gameParameters.setDivision("unlisted");

    mockStartGameProcess(gameParameters);
    when(leaderboardService.getActiveLeagueEntryForPlayer(junitPlayer, LADDER_1v1_RATING_TYPE)).thenReturn(completedFuture(Optional.empty()));
    when(fafServerAccessor.startSearchMatchmaker()).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(featuredMod, Set.of(), null, null, false)).thenReturn(completedFuture(null));
    when(mapService.isInstalled(map)).thenReturn(false);
    when(mapService.download(map)).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(FAF.getTechnicalName())).thenReturn(Mono.just(featuredMod));
    when(process.onExit()).thenReturn(CompletableFuture.completedFuture(process));

    instance.startSearchMatchmaker();

    verify(fafServerAccessor).startSearchMatchmaker();
    verify(mapService).download(map);
    verify(replayServer).start(eq(uid), any());
    verify(forgedAllianceService).startGameOnline(gameParameters);
  }

  @Test
  public void testStartSearchLadder1v1WithLeagueEntry() throws Exception {
    int uid = 123;
    String map = "scmp_037";
    GameLaunchResponse gameLaunchMessage = new GameLaunchMessageBuilder().defaultValues()
        .uid(uid).mod("FAF").mapname(map)
        .expectedPlayers(2)
        .faction(com.faforever.commons.lobby.Faction.CYBRAN)
        .gameType(GameType.MATCHMAKER)
        .mapPosition(4)
        .team(1)
        .ratingType(LADDER_1v1_RATING_TYPE)
        .get();

    FeaturedModBean featuredMod = FeaturedModBeanBuilder.create().defaultValues().get();
    GameParameters gameParameters = gameMapper.map(gameLaunchMessage);
    gameParameters.setDivision("test_name");
    gameParameters.setSubdivision("I");

    mockStartGameProcess(gameParameters);
    LeagueEntryBean leagueEntry = LeagueEntryBeanBuilder.create().defaultValues().get();
    when(leaderboardService.getActiveLeagueEntryForPlayer(junitPlayer, LADDER_1v1_RATING_TYPE)).thenReturn(completedFuture(Optional.of(leagueEntry)));
    when(fafServerAccessor.startSearchMatchmaker()).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(featuredMod, Set.of(), null, null, false)).thenReturn(completedFuture(null));
    when(mapService.isInstalled(map)).thenReturn(false);
    when(mapService.download(map)).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(FAF.getTechnicalName())).thenReturn(Mono.just(featuredMod));
    when(process.onExit()).thenReturn(CompletableFuture.completedFuture(process));

    instance.startSearchMatchmaker();

    verify(fafServerAccessor).startSearchMatchmaker();
    verify(mapService).download(map);
    verify(replayServer).start(eq(uid), any());
    verify(forgedAllianceService).startGameOnline(gameParameters);
  }

  @Test
  public void testStartSearchLadderTwiceReturnsSameFutureWhenSearching() throws Exception {
    int uid = 123;
    String map = "scmp_037";
    GameLaunchResponse gameLaunchMessage = new GameLaunchMessageBuilder().defaultValues()
        .uid(uid).mod("FAF").mapname(map)
        .expectedPlayers(2)
        .faction(com.faforever.commons.lobby.Faction.CYBRAN)
        .gameType(GameType.MATCHMAKER)
        .mapPosition(4)
        .team(1)
        .ratingType(LADDER_1v1_RATING_TYPE)
        .get();

    FeaturedModBean featuredMod = FeaturedModBeanBuilder.create().defaultValues().get();
    GameParameters gameParameters = gameMapper.map(gameLaunchMessage);
    gameParameters.setDivision("unlisted");

    mockStartGameProcess(gameParameters);
    when(leaderboardService.getActiveLeagueEntryForPlayer(junitPlayer, LADDER_1v1_RATING_TYPE)).thenReturn(completedFuture(Optional.empty()));
    when(fafServerAccessor.startSearchMatchmaker()).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(featuredMod, Set.of(), null, null, false)).thenReturn(completedFuture(null));
    when(mapService.isInstalled(map)).thenReturn(false);
    when(mapService.download(map)).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(FAF.getTechnicalName())).thenReturn(Mono.just(featuredMod));
    when(process.onExit()).thenReturn(CompletableFuture.completedFuture(process).newIncompleteFuture());

    instance.startSearchMatchmaker();

    verify(fafServerAccessor).startSearchMatchmaker();
  }

  @Test
  public void testGameLaunchKillsReplay() throws Exception {
    int uid = 123;
    String map = "scmp_037";
    GameLaunchResponse gameLaunchMessage = new GameLaunchMessageBuilder().defaultValues()
        .uid(uid).mod("FAF").mapname(map)
        .expectedPlayers(2)
        .faction(com.faforever.commons.lobby.Faction.CYBRAN)
        .gameType(GameType.MATCHMAKER)
        .mapPosition(4)
        .team(1)
        .ratingType(LADDER_1v1_RATING_TYPE)
        .get();

    FeaturedModBean featuredMod = FeaturedModBeanBuilder.create().defaultValues().get();
    GameParameters gameParameters = gameMapper.map(gameLaunchMessage);
    gameParameters.setDivision("unlisted");

    Path replayPath = Path.of("temp.scfareplay");
    int replayId = 1234;

    mockStartReplayProcess(replayPath, replayId);
    mockStartGameProcess(gameParameters);
    when(leaderboardService.getActiveLeagueEntryForPlayer(junitPlayer, LADDER_1v1_RATING_TYPE)).thenReturn(completedFuture(Optional.empty()));
    when(fafServerAccessor.startSearchMatchmaker()).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any(), anyBoolean())).thenReturn(completedFuture(null));
    when(mapService.isInstalled(anyString())).thenReturn(true);
    when(modService.getFeaturedMod(anyString())).thenReturn(Mono.just(featuredMod));
    when(process.onExit()).thenReturn(CompletableFuture.completedFuture(process));

    CompletableFuture<Void> future = instance.runWithReplay(replayPath, replayId, "", null, null, null, "map");
    future.join();

    verify(forgedAllianceService).startReplay(replayPath, replayId);

    instance.startSearchMatchmaker();

    verify(fafServerAccessor).startSearchMatchmaker();
    verify(replayServer).start(eq(uid), any());
    verify(forgedAllianceService).startGameOnline(gameParameters);
    assertFalse(instance.isReplayRunning());
  }

  @Test
  public void testStartSearchMatchmakerGameRunningDoesNothing() throws Exception {
    Process process = mock(Process.class);
    when(process.isAlive()).thenReturn(true);

    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();

    when(forgedAllianceService.startGameOnline(any())).thenReturn(process);
    when(gameUpdater.update(any(), any(), any(), any(), anyBoolean())).thenReturn(completedFuture(null));
    when(fafServerAccessor.requestHostGame(newGameInfo)).thenReturn(completedFuture(gameLaunchMessage));
    when(mapService.download(newGameInfo.getMap())).thenReturn(completedFuture(null));

    CountDownLatch gameRunningLatch = new CountDownLatch(1);
    instance.gameRunningProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        gameRunningLatch.countDown();
      }
    });

    instance.hostGame(newGameInfo);
    gameRunningLatch.await(TIMEOUT, TIME_UNIT);

    instance.startSearchMatchmaker();

    verify(notificationService).addImmediateWarnNotification("game.gameRunning");
  }

  @Test
  public void testCurrentGameEndedBehaviour() {
    notificationPrefs.setAfterGameReviewEnabled(true);
    notificationPrefs.setTransientNotificationsEnabled(true);
    when(playerService.isCurrentPlayerInGame(any())).thenReturn(true);
    GameBean game = GameBeanBuilder.create()
        .defaultValues()
        .id(123)
        .status(PLAYING)
        .teams(Map.of(1, List.of(junitPlayer.getId())))
        .get();
    junitPlayer.setGame(game);

    instance.currentGame.set(game);

    verify(notificationService, never()).addNotification(any(PersistentNotification.class));


    game.setStatus(PLAYING);
    game.setStatus(CLOSED);

    verify(notificationService).addNotification(any(PersistentNotification.class));
  }

  @Test
  public void testGameHostIfNoGameSet() {
    when(preferencesService.isValidGamePath()).thenReturn(false);
    instance.hostGame(null);
    verify(gameDirectoryRequiredHandler).onChooseGameDirectory(any());
  }

  @Test
  public void testGameHost() throws IOException {
    when(preferencesService.isValidGamePath()).thenReturn(true);
    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();
    when(gameUpdater.update(newGameInfo.getFeaturedMod(), newGameInfo.getSimMods(), null, null, false)).thenReturn(completedFuture(null));
    when(mapService.download(newGameInfo.getMap())).thenReturn(completedFuture(null));
    when(fafServerAccessor.requestHostGame(newGameInfo)).thenReturn(completedFuture(gameLaunchMessage));
    instance.hostGame(newGameInfo);
    GameParameters parameters = gameMapper.map(gameLaunchMessage);
    parameters.setLocalGpgPort(GPG_PORT);
    parameters.setLocalReplayPort(LOCAL_REPLAY_PORT);
    verify(forgedAllianceService).startGameOnline(parameters);
  }

  @Test
  public void testOfflineGame() throws IOException {
    when(preferencesService.isValidGamePath()).thenReturn(true);
    when(forgedAllianceService.startGameOffline(any())).thenReturn(process);
    when(process.onExit()).thenReturn(CompletableFuture.completedFuture(process));
    instance.startGameOffline();
    verify(forgedAllianceService).startGameOffline(any());
  }

  @Test
  public void testOfflineGameInvalidPath() throws IOException {
    when(preferencesService.isValidGamePath()).thenReturn(false);
    instance.startGameOffline();
    verify(gameDirectoryRequiredHandler).onChooseGameDirectory(any());
  }

  @Test
  public void runWithLiveReplayIfNoGameSet() {
    when(preferencesService.isValidGamePath()).thenReturn(false);
    instance.runWithLiveReplay(null, null, null, null);
    verify(gameDirectoryRequiredHandler).onChooseGameDirectory(any());
  }

  @Test
  public void startSearchMatchmakerIfNoGameSet() {
    when(preferencesService.isValidGamePath()).thenReturn(false);
    instance.startSearchMatchmaker();
    verify(gameDirectoryRequiredHandler).onChooseGameDirectory(any());
  }

  @Test
  public void startSearchMatchmakerWithGameOptions() throws IOException {
    when(preferencesService.isValidGamePath()).thenReturn(true);
    when(modService.getFeaturedMod(FAF.getTechnicalName()))
        .thenReturn(Mono.just(FeaturedModBeanBuilder.create().defaultValues().get()));
    when(process.onExit()).thenReturn(completedFuture(process));
    when(process.exitValue()).thenReturn(0);
    Map<String, String> gameOptions = new LinkedHashMap<>();
    gameOptions.put("Share", "ShareUntilDeath");
    gameOptions.put("UnitCap", "500");
    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create()
        .defaultValues()
        .team(1)
        .expectedPlayers(4)
        .mapPosition(3)
        .gameOptions(gameOptions)
        .get();
    GameParameters gameParameters = gameMapper.map(gameLaunchMessage);
    gameParameters.setDivision("unlisted");
    mockStartGameProcess(gameParameters);
    when(leaderboardService.getActiveLeagueEntryForPlayer(junitPlayer, "global")).thenReturn(completedFuture(Optional.empty()));
    when(gameUpdater.update(any(), any(), any(), any(), anyBoolean())).thenReturn(completedFuture(null));
    when(mapService.download(gameLaunchMessage.getMapName())).thenReturn(completedFuture(null));
    when(fafServerAccessor.startSearchMatchmaker()).thenReturn(completedFuture(gameLaunchMessage));
    instance.startSearchMatchmaker();
    verify(forgedAllianceService).startGameOnline(gameParameters);
  }

  @Test
  public void startSearchMatchmakerThenCancelledWithGame() throws IOException {
    when(preferencesService.isValidGamePath()).thenReturn(true);
    when(modService.getFeaturedMod(FAF.getTechnicalName()))
        .thenReturn(Mono.just(FeaturedModBeanBuilder.create().defaultValues().get()));
    when(process.isAlive()).thenReturn(false);
    when(process.onExit()).thenReturn(new CompletableFuture<>());
    when(process.exitValue()).thenReturn(1);
    Map<String, String> gameOptions = new LinkedHashMap<>();
    gameOptions.put("Share", "ShareUntilDeath");
    gameOptions.put("UnitCap", "500");
    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create()
        .defaultValues()
        .team(1)
        .expectedPlayers(4)
        .mapPosition(3)
        .gameOptions(gameOptions)
        .get();
    GameParameters gameParameters = gameMapper.map(gameLaunchMessage);
    gameParameters.setDivision("unlisted");
    mockStartGameProcess(gameParameters);
    when(leaderboardService.getActiveLeagueEntryForPlayer(junitPlayer, "global")).thenReturn(completedFuture(Optional.empty()));
    when(gameUpdater.update(any(), any(), any(), any(), anyBoolean())).thenReturn(completedFuture(null));
    when(mapService.download(gameLaunchMessage.getMapName())).thenReturn(completedFuture(null));
    when(fafServerAccessor.startSearchMatchmaker()).thenReturn(CompletableFuture.completedFuture(gameLaunchMessage));
    instance.startSearchMatchmaker();
    when(process.isAlive()).thenReturn(true);
    instance.stopSearchMatchmaker();
    verify(notificationService).addServerNotification(any());
  }

  @Test
  public void startSearchMatchmakerThenCancelledNoGame() throws IOException {
    when(preferencesService.isValidGamePath()).thenReturn(true);
    when(modService.getFeaturedMod(FAF.getTechnicalName()))
        .thenReturn(Mono.just(FeaturedModBeanBuilder.create().defaultValues().get()));
    when(process.isAlive()).thenReturn(false);
    when(process.onExit()).thenReturn(new CompletableFuture<>());
    when(process.exitValue()).thenReturn(1);
    Map<String, String> gameOptions = new LinkedHashMap<>();
    gameOptions.put("Share", "ShareUntilDeath");
    gameOptions.put("UnitCap", "500");
    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create()
        .defaultValues()
        .team(1)
        .expectedPlayers(4)
        .mapPosition(3)
        .gameOptions(gameOptions)
        .get();
    mockStartGameProcess(gameMapper.map(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any(), anyBoolean())).thenReturn(completedFuture(null));
    when(mapService.download(gameLaunchMessage.getMapName())).thenReturn(completedFuture(null));
    when(fafServerAccessor.startSearchMatchmaker()).thenReturn(CompletableFuture.completedFuture(gameLaunchMessage));
    instance.startSearchMatchmaker();
    instance.stopSearchMatchmaker();
    verify(notificationService, never()).addServerNotification(any());
  }

  @Test
  public void joinGameIfNoGameSet() {
    when(preferencesService.isValidGamePath()).thenReturn(false);
    instance.joinGame(null, null);
    verify(gameDirectoryRequiredHandler).onChooseGameDirectory(any());
  }

  @Test
  public void runWithReplayIfNoGameSet() {
    when(preferencesService.isValidGamePath()).thenReturn(false);
    instance.runWithReplay(null, null, null, null, null, null, null);
    verify(gameDirectoryRequiredHandler).onChooseGameDirectory(any());
  }

  @Test
  public void runWithReplayInMatchmakerQueue() throws Exception {
    mockMatchmakerChain();
    instance.startSearchMatchmaker();

    Path replayPath = Path.of("temp.scfareplay");
    int replayId = 1234;

    when(mapService.isInstalled(anyString())).thenReturn(true);
    when(gameUpdater.update(any(), any(), any(), any(), anyBoolean())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(anyString())).thenReturn(Mono.just(FeaturedModBeanBuilder.create()
        .defaultValues()
        .get()));

    mockStartReplayProcess(replayPath, replayId);
    CompletableFuture<Void> future = instance.runWithReplay(replayPath, replayId, "", null, null, null, "map");
    future.join();

    verify(forgedAllianceService).startReplay(replayPath, replayId);
    assertTrue(instance.isReplayRunning());
  }

  @Test
  public void runWithLiveReplayInMatchmakerQueue() throws Exception {
    mockMatchmakerChain();
    instance.startSearchMatchmaker();

    URI replayUrl = new URI("gpgnet://example.com/123/456.scfareplay");
    int gameId = 1234;

    when(mapService.isInstalled(anyString())).thenReturn(true);
    when(gameUpdater.update(any(), any(), any(), any(), anyBoolean())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(anyString())).thenReturn(Mono.just(FeaturedModBeanBuilder.create()
        .defaultValues()
        .get()));
    mockStartLiveReplayProcess(replayUrl, gameId);

    CompletableFuture<Void> future = instance.runWithLiveReplay(replayUrl, gameId, "faf", "map");
    future.join();

    verify(forgedAllianceService).startReplay(replayUrl, gameId);
    assertTrue(instance.isReplayRunning());
  }

  @Test
  public void runWithReplayInParty() throws Exception {
    Path replayPath = Path.of("temp.scfareplay");
    int replayId = 1234;

    when(mapService.isInstalled(anyString())).thenReturn(true);
    when(gameUpdater.update(any(), any(), any(), any(), anyBoolean())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(anyString())).thenReturn(Mono.just(FeaturedModBeanBuilder.create()
        .defaultValues()
        .get()));

    mockStartReplayProcess(replayPath, replayId);
    CompletableFuture<Void> future = instance.runWithReplay(replayPath, replayId, "", null, null, null, "map");
    future.join();

    verify(forgedAllianceService).startReplay(replayPath, replayId);
    assertTrue(instance.isReplayRunning());
  }

  @Test
  public void runWithLiveReplayInParty() throws Exception {
    URI replayUrl = new URI("gpgnet://example.com/123/456.scfareplay");
    int gameId = 1234;

    when(mapService.isInstalled(anyString())).thenReturn(true);
    when(gameUpdater.update(any(), any(), any(), any(), anyBoolean())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(anyString())).thenReturn(Mono.just(FeaturedModBeanBuilder.create()
        .defaultValues()
        .get()));
    mockStartLiveReplayProcess(replayUrl, gameId);

    CompletableFuture<Void> future = instance.runWithLiveReplay(replayUrl, gameId, "faf", "map");
    future.join();

    verify(forgedAllianceService).startReplay(replayUrl, gameId);
    assertTrue(instance.isReplayRunning());
  }

  @Test
  public void launchTutorialIfNoGameSet() {
    when(preferencesService.isValidGamePath()).thenReturn(false);
    instance.launchTutorial(null, null);
    verify(gameDirectoryRequiredHandler).onChooseGameDirectory(any());
  }

  @Test
  public void iceCloseOnError() throws Exception {
    GameBean game = GameBeanBuilder.create().defaultValues().get();

    game.setMapFolderName("map");

    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();

    mockStartGameProcess(gameMapper.map(gameLaunchMessage));
    when(mapService.isInstalled("map")).thenReturn(true);
    when(fafServerAccessor.requestJoinGame(game.getId(), null)).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any(), anyBoolean())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(Mono.just(FeaturedModBeanBuilder.create()
        .defaultValues()
        .get()));
    when(replayServer.start(anyInt(), any(Supplier.class))).thenReturn(completedFuture(new Throwable()));

    CompletableFuture<Void> future = instance.joinGame(game, null).toCompletableFuture();

    assertNull(future.get(TIMEOUT, TIME_UNIT));
    verify(mapService, never()).download(any());
    verify(replayServer).start(eq(game.getId()), any());
    verify(iceAdapter).stop();
  }

  @Test
  public void spawnTerminationListenerTest() throws IOException {
    when(process.onExit()).thenReturn(CompletableFuture.completedFuture(process));
    when(process.exitValue()).thenReturn(-1);

    instance.spawnTerminationListener(process, true).join();

    verify(loggingService).getMostRecentGameLogFile();
    verify(notificationService).addNotification(any(ImmediateNotification.class));
    verify(fafServerAccessor).notifyGameEnded();
    verify(iceAdapter).stop();
    verify(replayServer).stop();
  }

  @Test
  public void spawnReplayTerminationListenerTest() {
    when(process.onExit()).thenReturn(CompletableFuture.completedFuture(process));
    when(process.exitValue()).thenReturn(-1);

    instance.spawnReplayTerminationListener(process);

    verify(loggingService).getMostRecentGameLogFile();
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  @Test
  public void onKillNoticeStopsGame() throws Exception {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();
    mockStartGameProcess(gameMapper.map(gameLaunchMessage));
    when(mapService.isInstalled(game.getMapFolderName())).thenReturn(true);
    when(fafServerAccessor.requestJoinGame(game.getId(), null)).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any(), anyBoolean())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(Mono.just(FeaturedModBeanBuilder.create()
        .defaultValues()
        .get()));

    when(process.isAlive()).thenReturn(true);

    NoticeInfo noticeMessage = new NoticeInfo("kill", null);


    CompletableFuture<Void> future = instance.joinGame(game, null).toCompletableFuture();
    future.join();

    testNoticePublisher.next(noticeMessage);

    verify(iceAdapter).onGameCloseRequested();
    verify(process).destroy();
  }
}
