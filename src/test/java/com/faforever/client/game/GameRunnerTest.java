package com.faforever.client.game;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.GameLaunchMessageBuilder;
import com.faforever.client.builders.LeagueEntryBeanBuilder;
import com.faforever.client.builders.NewGameInfoBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.discord.DiscordRichPresenceService;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fa.ForgedAllianceLaunchService;
import com.faforever.client.fa.GameParameters;
import com.faforever.client.fa.relay.ice.CoturnService;
import com.faforever.client.fa.relay.ice.IceAdapter;
import com.faforever.client.featuredmod.FeaturedModService;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.logging.LoggingService;
import com.faforever.client.map.MapService;
import com.faforever.client.mapstruct.GameMapper;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mod.ModService;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.LastGamePrefs;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.replay.ReplayServer;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameType;
import com.faforever.commons.lobby.NoticeInfo;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.test.publisher.TestPublisher;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameRunnerTest extends ServiceTest {

  private static final long TIMEOUT = 5000;
  private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
  private static final Integer GPG_PORT = 1234;
  private static final int LOCAL_REPLAY_PORT = 15111;
  private static final String LADDER_1v1_RATING_TYPE = "ladder_1v1";

  @InjectMocks
  private GameRunner instance;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private MapService mapService;
  @Mock
  private ForgedAllianceLaunchService forgedAllianceLaunchService;
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
  private FeaturedModService featuredModService;
  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private GameService gameService;
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
  private GamePathHandler gamePathHandler;
  @Spy
  private GameMapper gameMapper = Mappers.getMapper(GameMapper.class);
  @Spy
  private ClientProperties clientProperties;
  @Spy
  private LastGamePrefs lastGamePrefs;
  @Spy
  private NotificationPrefs notificationPrefs;

  @Captor
  private ArgumentCaptor<Set<String>> simModsCaptor;

  private final TestPublisher<NoticeInfo> testNoticePublisher = TestPublisher.create();

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(gameMapper);
    PlayerBean junitPlayer = PlayerBeanBuilder.create().defaultValues().get();

    lenient().doAnswer(invocation -> {
      Runnable runnable = invocation.getArgument(0);
      runnable.run();
      return null;
    }).when(fxApplicationThreadExecutor).execute(any());
    lenient().when(fafServerAccessor.getEvents(NoticeInfo.class)).thenReturn(testNoticePublisher.flux());
    lenient().when(coturnService.getSelectedCoturns(anyInt())).thenReturn(completedFuture(List.of()));
    lenient().when(preferencesService.isValidGamePath()).thenReturn(true);
    lenient().when(fafServerAccessor.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>());
    lenient().when(replayServer.start(anyInt())).thenReturn(completedFuture(LOCAL_REPLAY_PORT));
    lenient().when(iceAdapter.start(anyInt())).thenReturn(completedFuture(GPG_PORT));
    lenient().when(playerService.getCurrentPlayer()).thenReturn(junitPlayer);
    lenient().when(process.pid()).thenReturn(10L);

    lenient().doAnswer(invocation -> {
      try {
        ((Runnable) invocation.getArgument(0)).run();
      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }).when(executorService).execute(any());

    instance.afterPropertiesSet();

    testNoticePublisher.assertSubscribers(1);
  }

  private void mockStartGameProcess(GameLaunchResponse gameLaunchResponse) throws IOException {
    String mapName = gameLaunchResponse.getMapName();
    if (mapName != null) {
      lenient().when(mapService.downloadIfNecessary(any())).thenReturn(completedFuture(null));
    }
    lenient().when(forgedAllianceLaunchService.launchOnlineGame(any(), anyInt(), anyInt())).thenReturn(process);
    lenient().when(replayServer.start(anyInt())).thenReturn(completedFuture(LOCAL_REPLAY_PORT));
    lenient().when(iceAdapter.start(anyInt())).thenReturn(completedFuture(GPG_PORT));
    lenient().when(coturnService.getSelectedCoturns(anyInt())).thenReturn(completedFuture(List.of()));
    lenient().when(process.onExit()).thenReturn(new CompletableFuture<>());
    lenient().when(process.exitValue()).thenReturn(0);
    lenient().when(process.isAlive()).thenReturn(true);
    lenient().when(gameService.getByUid(anyInt()))
             .thenAnswer(invocation -> Optional.of(
                 GameBeanBuilder.create().id(invocation.getArgument(0, Integer.class)).get()));
  }

  @Test
  public void testStartOnlineGlobalGame() throws Exception {
    GameLaunchResponse gameLaunchResponse = GameLaunchMessageBuilder.create().defaultValues().get();
    mockStartGameProcess(gameLaunchResponse);

    CompletableFuture<Process> exitFuture = new CompletableFuture<>();

    when(process.onExit()).thenReturn(exitFuture);

    instance.startOnlineGame(gameLaunchResponse);

    ArgumentCaptor<GameParameters> captor = ArgumentCaptor.forClass(GameParameters.class);
    verify(forgedAllianceLaunchService).launchOnlineGame(captor.capture(), eq(GPG_PORT), eq(LOCAL_REPLAY_PORT));
    GameParameters gameParameters = captor.getValue();
    Integer uid = gameParameters.uid();

    verify(fafServerAccessor).setPingIntervalSeconds(5);
    verify(leaderboardService, never()).getActiveLeagueEntryForPlayer(any(), any());
    verify(mapService, never()).downloadIfNecessary(any());
    verify(replayServer).start(uid);
    verify(iceAdapter).start(uid);
    verify(coturnService).getSelectedCoturns(uid);
    verify(iceAdapter).setIceServers(anyCollection());
    assertTrue(instance.isRunning());
    assertEquals(uid, instance.getRunningGame().getId());
    assertEquals(10L, instance.getRunningProcessId());
    assertNotNull(instance.getRunningGame());

    assertNull(gameParameters.league());

    exitFuture.complete(process);

    verify(iceAdapter).stop();
    verify(replayServer).stop();
    verify(fafServerAccessor).notifyGameEnded();
    verify(fafServerAccessor).setPingIntervalSeconds(25);
    verify(notificationService).addNotification(any(PersistentNotification.class));
    assertFalse(instance.isRunning());
    assertNull(instance.getRunningProcessId());
    assertNull(instance.getRunningGame());
  }

  @Test
  public void testStartOnlineMatchmakerGameNoLeaderboardEntry() throws Exception {
    GameLaunchResponse gameLaunchResponse = GameLaunchMessageBuilder.create()
                                                                    .defaultValues()
                                                                    .mapName("test")
                                                                    .ratingType("ladder_1v1")
                                                                    .get();
    mockStartGameProcess(gameLaunchResponse);
    when(leaderboardService.getActiveLeagueEntryForPlayer(any(), any())).thenReturn(completedFuture(Optional.empty()));

    instance.startOnlineGame(gameLaunchResponse);

    ArgumentCaptor<GameParameters> captor = ArgumentCaptor.forClass(GameParameters.class);
    verify(forgedAllianceLaunchService).launchOnlineGame(captor.capture(), eq(GPG_PORT), eq(LOCAL_REPLAY_PORT));
    GameParameters gameParameters = captor.getValue();

    verify(leaderboardService).getActiveLeagueEntryForPlayer(any(), eq(gameParameters.leaderboard()));
    verify(mapService).downloadIfNecessary(gameParameters.mapName());

    assertNotNull(gameParameters.league());
    assertEquals("unlisted", gameParameters.league().division());
    assertNull(gameParameters.league().subDivision());
  }

  @Test
  public void testStartOnlineMatchmakerGameWithLeaderboardEntry() throws Exception {
    GameLaunchResponse gameLaunchResponse = GameLaunchMessageBuilder.create()
                                                                    .defaultValues()
                                                                    .mapName("test")
                                                                    .ratingType("ladder_1v1")
                                                                    .get();
    mockStartGameProcess(gameLaunchResponse);
    LeagueEntryBean leagueEntry = LeagueEntryBeanBuilder.create().defaultValues().get();
    when(leaderboardService.getActiveLeagueEntryForPlayer(any(), any())).thenReturn(
        completedFuture(Optional.of(leagueEntry)));

    instance.startOnlineGame(gameLaunchResponse);

    ArgumentCaptor<GameParameters> captor = ArgumentCaptor.forClass(GameParameters.class);
    verify(forgedAllianceLaunchService).launchOnlineGame(captor.capture(), eq(GPG_PORT), eq(LOCAL_REPLAY_PORT));
    GameParameters gameParameters = captor.getValue();

    verify(leaderboardService).getActiveLeagueEntryForPlayer(any(), eq(gameParameters.leaderboard()));
    verify(mapService).downloadIfNecessary(gameParameters.mapName());

    assertNotNull(gameParameters.league());
    assertEquals(leagueEntry.getSubdivision().getDivision().getNameKey(), gameParameters.league().division());
    assertEquals(leagueEntry.getSubdivision().getNameKey(), gameParameters.league().subDivision());
  }

  private void mockJoinGame(GameLaunchResponse gameLaunchResponse) throws IOException {
    lenient().when(featuredModService.updateFeaturedModToLatest(any(), anyBoolean())).thenReturn(completedFuture(null));
    lenient().when(fafServerAccessor.requestJoinGame(anyInt(), any())).thenReturn(completedFuture(gameLaunchResponse));
    lenient().when(modService.installAndEnableMods(anySet())).thenReturn(completedFuture(null));
    lenient().when(mapService.downloadIfNecessary(any())).thenReturn(completedFuture(null));
    mockStartGameProcess(gameLaunchResponse);
  }

  private void mockHostGame(GameLaunchResponse gameLaunchResponse) throws IOException {
    lenient().when(featuredModService.updateFeaturedModToLatest(any(), anyBoolean())).thenReturn(completedFuture(null));
    lenient().when(fafServerAccessor.requestHostGame(any())).thenReturn(completedFuture(gameLaunchResponse));
    lenient().when(modService.installAndEnableMods(anySet())).thenReturn(completedFuture(null));
    lenient().when(mapService.downloadIfNecessary(any())).thenReturn(completedFuture(null));
    lenient().when(gameService.getByUid(anyInt()))
             .thenReturn(Optional.of(GameBeanBuilder.create().defaultValues().get()));
    mockStartGameProcess(gameLaunchResponse);
  }

  private void mockMatchmakerGame(GameLaunchResponse gameLaunchResponse) throws IOException {
    lenient().when(featuredModService.updateFeaturedModToLatest(any(), anyBoolean())).thenReturn(completedFuture(null));
    lenient().when(fafServerAccessor.startSearchMatchmaker()).thenReturn(completedFuture(gameLaunchResponse));
    lenient().when(modService.installAndEnableMods(anySet())).thenReturn(completedFuture(null));
    mockStartGameProcess(gameLaunchResponse);
  }

  @Test
  public void testHostGame() throws Exception {
    GameLaunchResponse gameLaunchResponse = GameLaunchMessageBuilder.create().defaultValues().get();
    mockHostGame(gameLaunchResponse);

    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    instance.host(newGameInfo);

    int uid = gameLaunchResponse.getUid();
    GameParameters gameParameters = gameMapper.map(gameLaunchResponse, null);

    verify(modService).installAndEnableMods(newGameInfo.simMods());
    verify(mapService).downloadIfNecessary(newGameInfo.map());
    verify(fafServerAccessor).requestHostGame(newGameInfo);
    verify(featuredModService).updateFeaturedModToLatest(newGameInfo.featuredModName(), false);
    verify(replayServer).start(uid);
    verify(iceAdapter).start(uid);
    verify(coturnService).getSelectedCoturns(uid);
    verify(iceAdapter).setIceServers(anyCollection());
    verify(forgedAllianceLaunchService).launchOnlineGame(gameParameters, GPG_PORT, LOCAL_REPLAY_PORT);
    assertEquals(uid, instance.getRunningGame().getId());
  }

  @Test
  public void testJoinGameWithSimMods() throws Exception {
    GameBean game = GameBeanBuilder.create().defaultValues().get();

    ObservableMap<String, String> simMods = FXCollections.observableHashMap();
    simMods.put("123-456-789", "Fake mod name");

    game.setSimMods(simMods);

    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();

    mockJoinGame(gameLaunchMessage);

    instance.join(game);

    verify(modService).installAndEnableMods(simMods.keySet());
  }

  @Test
  public void testAddOnGameStartedListener() throws Exception {
    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    GameLaunchResponse gameLaunchResponse = GameLaunchMessageBuilder.create().defaultValues().get();

    mockHostGame(gameLaunchResponse);

    instance.host(newGameInfo);

    GameParameters gameParameters = gameMapper.map(gameLaunchResponse, null);

    verify(forgedAllianceLaunchService).launchOnlineGame(gameParameters, GPG_PORT, LOCAL_REPLAY_PORT);
    verify(replayServer).start(eq(gameLaunchResponse.getUid()));
  }

  @Test
  public void testRestoreGameSessionListener() throws Exception {
    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();

    mockHostGame(gameLaunchMessage);

    SimpleObjectProperty<ConnectionState> connectionState = new SimpleObjectProperty<>();
    when(fafServerAccessor.connectionStateProperty()).thenReturn(connectionState);

    instance.afterPropertiesSet();
    instance.host(newGameInfo);

    connectionState.set(ConnectionState.DISCONNECTED);
    connectionState.set(ConnectionState.CONNECTED);

    verify(fafServerAccessor).restoreGameSession(anyInt());
  }

  @Test
  public void testStartSearchLadder1v1() throws Exception {
    int uid = 123;
    String map = "scmp_037";
    GameLaunchResponse gameLaunchResponse = new GameLaunchMessageBuilder().defaultValues()
                                                                          .uid(uid)
                                                                          .mod("FAF")
                                                                          .mapName(map)
                                                                         .expectedPlayers(2)
                                                                         .faction(
                                                                             com.faforever.commons.lobby.Faction.CYBRAN)
                                                                         .gameType(GameType.MATCHMAKER)
                                                                         .mapPosition(4)
                                                                         .team(1)
                                                                         .ratingType(LADDER_1v1_RATING_TYPE)
                                                                         .get();

    mockMatchmakerGame(gameLaunchResponse);

    instance.startSearchMatchmaker();

    GameParameters gameParameters = gameMapper.map(gameLaunchResponse, null);

    verify(fafServerAccessor).startSearchMatchmaker();
    verify(mapService).downloadIfNecessary(map);
    verify(replayServer).start(eq(uid));
    verify(forgedAllianceLaunchService).launchOnlineGame(gameParameters, GPG_PORT, LOCAL_REPLAY_PORT);
  }

  @Test
  public void testStartSearchLadder1v1WithLeagueEntry() throws Exception {
    int uid = 123;
    String map = "scmp_037";
    GameLaunchResponse gameLaunchResponse = new GameLaunchMessageBuilder().defaultValues()
                                                                          .uid(uid)
                                                                          .mod("FAF")
                                                                          .mapName(map)
                                                                         .expectedPlayers(2)
                                                                         .faction(
                                                                             com.faforever.commons.lobby.Faction.CYBRAN)
                                                                         .gameType(GameType.MATCHMAKER)
                                                                         .mapPosition(4)
                                                                         .team(1)
                                                                         .ratingType(LADDER_1v1_RATING_TYPE)
                                                                         .get();

    mockMatchmakerGame(gameLaunchResponse);

    instance.startSearchMatchmaker();

    GameParameters gameParameters = gameMapper.map(gameLaunchResponse, null);

    verify(fafServerAccessor).startSearchMatchmaker();
    verify(mapService).downloadIfNecessary(map);
    verify(replayServer).start(eq(uid));
    verify(forgedAllianceLaunchService).launchOnlineGame(gameParameters, GPG_PORT, LOCAL_REPLAY_PORT);
  }

  @Test
  public void testStartSearchLadderTwiceReturnsSameFutureWhenSearching() throws Exception {
    int uid = 123;
    String map = "scmp_037";
    GameLaunchResponse gameLaunchMessage = new GameLaunchMessageBuilder().defaultValues()
                                                                         .uid(uid)
                                                                         .mod("FAF")
                                                                         .mapName(map)
                                                                         .expectedPlayers(2)
                                                                         .faction(
                                                                             com.faforever.commons.lobby.Faction.CYBRAN)
                                                                         .gameType(GameType.MATCHMAKER)
                                                                         .mapPosition(4)
                                                                         .team(1)
                                                                         .ratingType(LADDER_1v1_RATING_TYPE)
                                                                         .get();

    mockMatchmakerGame(gameLaunchMessage);

    instance.startSearchMatchmaker();

    verify(fafServerAccessor).startSearchMatchmaker();
  }

  @Test
  public void testStartSearchMatchmakerGameRunningDoesNothing() throws Exception {
    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();

    mockHostGame(gameLaunchMessage);
    CountDownLatch gameRunningLatch = new CountDownLatch(1);
    instance.runningProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        gameRunningLatch.countDown();
      }
    });

    instance.host(newGameInfo);
    gameRunningLatch.await(TIMEOUT, TIME_UNIT);

    instance.startSearchMatchmaker();

    verify(notificationService).addImmediateWarnNotification("game.gameRunning");
  }

  @Test
  public void testAskForRatingAtEndOfGame() throws Exception {
    notificationPrefs.setAfterGameReviewEnabled(true);

    mockHostGame(GameLaunchMessageBuilder.create().defaultValues().get());

    instance.host(NewGameInfoBuilder.create().defaultValues().get());

    verify(notificationService).addNotification(any(PersistentNotification.class));
  }

  @Test
  public void testNoAskForRatingAtEndOfGame() throws Exception {
    notificationPrefs.setAfterGameReviewEnabled(false);

    mockHostGame(GameLaunchMessageBuilder.create().defaultValues().get());

    instance.host(NewGameInfoBuilder.create().defaultValues().get());

    verify(notificationService, never()).addNotification(any(PersistentNotification.class));
  }

  @Test
  public void testNoAskForRatingAtEndOfGameIfExitCode() throws Exception {
    notificationPrefs.setAfterGameReviewEnabled(true);

    mockHostGame(GameLaunchMessageBuilder.create().defaultValues().get());

    when(process.exitValue()).thenReturn(-1);

    instance.host(NewGameInfoBuilder.create().defaultValues().get());

    verify(notificationService, never()).addNotification(any(PersistentNotification.class));
  }

  @Test
  public void testGameHostIfNoGameSet() {
    when(preferencesService.isValidGamePath()).thenReturn(false);
    when(gamePathHandler.chooseAndValidateGameDirectory()).thenReturn(new CompletableFuture<>());
    instance.host(null);
    verify(gamePathHandler).chooseAndValidateGameDirectory();
  }

  @Test
  public void testGameHost() throws IOException {
    when(preferencesService.isValidGamePath()).thenReturn(true);
    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    GameLaunchResponse gameLaunchResponse = GameLaunchMessageBuilder.create().defaultValues().get();
    mockHostGame(gameLaunchResponse);

    instance.host(newGameInfo);

    GameParameters gameParameters = gameMapper.map(gameLaunchResponse, null);

    verify(forgedAllianceLaunchService).launchOnlineGame(gameParameters, GPG_PORT, LOCAL_REPLAY_PORT);
  }

  @Test
  public void testOfflineGame() throws IOException {
    when(preferencesService.isValidGamePath()).thenReturn(true);
    when(forgedAllianceLaunchService.launchOfflineGame(any())).thenReturn(process);
    when(process.onExit()).thenReturn(completedFuture(process));
    instance.startOffline();
    verify(forgedAllianceLaunchService).launchOfflineGame(any());
  }

  @Test
  public void testOfflineGameInvalidPath() throws IOException {
    when(preferencesService.isValidGamePath()).thenReturn(false);
    when(gamePathHandler.chooseAndValidateGameDirectory()).thenReturn(new CompletableFuture<>());
    instance.startOffline();
    verify(gamePathHandler).chooseAndValidateGameDirectory();
  }

  @Test
  public void startSearchMatchmakerIfNoGameSet() {
    when(preferencesService.isValidGamePath()).thenReturn(false);
    when(gamePathHandler.chooseAndValidateGameDirectory()).thenReturn(new CompletableFuture<>());
    instance.startSearchMatchmaker();
    verify(gamePathHandler).chooseAndValidateGameDirectory();
  }

  @Test
  public void startSearchMatchmakerWithGameOptions() throws IOException {
    when(preferencesService.isValidGamePath()).thenReturn(true);
    Map<String, String> gameOptions = new LinkedHashMap<>();
    gameOptions.put("Share", "ShareUntilDeath");
    gameOptions.put("UnitCap", "500");
    GameLaunchResponse gameLaunchResponse = GameLaunchMessageBuilder.create()
                                                                   .defaultValues()
                                                                   .team(1)
                                                                   .expectedPlayers(4)
                                                                   .mapPosition(3)
                                                                   .gameOptions(gameOptions)
                                                                   .get();

    mockMatchmakerGame(gameLaunchResponse);

    instance.startSearchMatchmaker();

    GameParameters gameParameters = gameMapper.map(gameLaunchResponse, null);

    verify(forgedAllianceLaunchService).launchOnlineGame(gameParameters, GPG_PORT, LOCAL_REPLAY_PORT);
  }

  @Test
  public void startSearchMatchmakerThenCancelledWithGame() throws IOException {
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

    mockMatchmakerGame(gameLaunchMessage);

    instance.startSearchMatchmaker();

    instance.stopSearchMatchmaker();

    verify(notificationService).addServerNotification(any());
  }

  @Test
  public void startSearchMatchmakerThenCancelledNoGame() throws IOException {
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
    mockMatchmakerGame(gameLaunchMessage);


    instance.startSearchMatchmaker();
    instance.stopSearchMatchmaker();

    verify(notificationService, never()).addServerNotification(any());
  }

  @Test
  public void joinGameIfNoGameSet() {
    when(preferencesService.isValidGamePath()).thenReturn(false);
    when(gamePathHandler.chooseAndValidateGameDirectory()).thenReturn(new CompletableFuture<>());
    instance.join(null);
    verify(gamePathHandler).chooseAndValidateGameDirectory();
  }

  @Test
  public void launchTutorialIfNoGameSet() {
    when(preferencesService.isValidGamePath()).thenReturn(false);
    when(gamePathHandler.chooseAndValidateGameDirectory()).thenReturn(new CompletableFuture<>());
    instance.launchTutorial(null, null);
    verify(gamePathHandler).chooseAndValidateGameDirectory();
  }

  @Test
  public void iceCloseOnError() throws Exception {
    GameBean game = GameBeanBuilder.create().defaultValues().get();

    game.setMapFolderName("map");

    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();

    mockJoinGame(gameLaunchMessage);

    when(replayServer.start(anyInt())).thenReturn(failedFuture(new Throwable()));

    instance.join(game);

    verify(mapService).downloadIfNecessary(any());
    verify(replayServer).start(eq(game.getId()));
    verify(iceAdapter).stop();
  }

  @Test
  public void onKillNoticeStopsGame() throws Exception {
    when(process.isAlive()).thenReturn(true);

    mockJoinGame(GameLaunchMessageBuilder.create().defaultValues().get());

    instance.join(GameBeanBuilder.create().defaultValues().get());

    testNoticePublisher.next(new NoticeInfo("kill", null));

    verify(iceAdapter).onGameCloseRequested();
    verify(process).destroy();
  }

  /**
   * Ensure that the user is allowed to choose the GameDirectory if no path is provided
   */
  @Test
  public void testJoinGameMissingGamePathUserSelectsValidPath() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(preferencesService.isValidGamePath()).thenReturn(false).thenReturn(true);

    when(gamePathHandler.chooseAndValidateGameDirectory()).thenReturn(completedFuture(null));

    instance.join(game);

    verify(gamePathHandler).chooseAndValidateGameDirectory();
  }

  /**
   * Ensure that the user is allowed to choose the GameDirectory if no path is provided
   */
  @Test
  public void testJoinGameMissingGamePathUserSelectsInvalidPath() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();

    when(preferencesService.isValidGamePath()).thenReturn(false);

    when(gamePathHandler.chooseAndValidateGameDirectory()).thenReturn(completedFuture(null),
                                                                      failedFuture(new CancellationException()));

    instance.join(game);

    verify(gamePathHandler, times(2)).chooseAndValidateGameDirectory();
  }

  /**
   * Ensure that the user is _not_ notified about his rating if ignoreRating is true
   */
  @Test
  public void testJoinGameIgnoreRatings() throws Exception {
    GameBean game = GameBeanBuilder.create().defaultValues()
                                   .ratingMax(-100)
                                   .get();

    mockJoinGame(GameLaunchMessageBuilder.create().defaultValues().get());

    instance.join(game);

    ArgumentCaptor<ImmediateNotification> captor = ArgumentCaptor.forClass(ImmediateNotification.class);

    verify(notificationService).addNotification(captor.capture());

    ImmediateNotification value = captor.getValue();

    value.getActions().getFirst().call(null);

    verify(fafServerAccessor).requestJoinGame(anyInt(), any());
  }

  /**
   * Ensure that the user is notified about his rating being to low
   */
  @Test
  public void testJoinGameRatingToLow() {
    GameBean game = GameBeanBuilder.create().defaultValues()
                                   .ratingMin(5000)
                                   .get();
    instance.join(game);
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  /**
   * Ensure that the user is notified about his rating being to high
   */
  @Test
  public void testJoinGameRatingToHigh() {
    GameBean game = GameBeanBuilder.create().defaultValues()
                                   .ratingMax(-100)
                                   .get();
    instance.join(game);
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }
}
