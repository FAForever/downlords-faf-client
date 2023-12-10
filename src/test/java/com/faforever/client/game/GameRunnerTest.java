package com.faforever.client.game;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.GameLaunchMessageBuilder;
import com.faforever.client.builders.NewGameInfoBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.discord.DiscordRichPresenceService;
import com.faforever.client.domain.GameBean;
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
import com.faforever.client.ui.preferences.GameDirectoryRequiredHandler;
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
import java.nio.file.Path;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
  private GameDirectoryRequiredHandler gameDirectoryRequiredHandler;
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

  private GameParameters mockStartGameProcess(GameLaunchResponse gameLaunchResponse) throws IOException {
    GameParameters gameParameters = gameMapper.map(gameLaunchResponse);
    gameParameters.setLocalGpgPort(GPG_PORT);
    gameParameters.setLocalReplayPort(LOCAL_REPLAY_PORT);

    String mapName = gameLaunchResponse.getMapName();
    if (mapName != null) {
      lenient().when(mapService.downloadIfNecessary(any())).thenReturn(completedFuture(null));
    }
    String leaderboard = gameLaunchResponse.getLeaderboard();
    if (leaderboard != null && !"global".equals(leaderboard)) {
      lenient().when(leaderboardService.getActiveLeagueEntryForPlayer(any(), any())).thenReturn(
          completedFuture(Optional.empty()));
      gameParameters.setDivision("unlisted");
    }
    lenient().when(forgedAllianceLaunchService.startGameOnline(any())).thenReturn(process);
    lenient().when(replayServer.start(anyInt())).thenReturn(completedFuture(LOCAL_REPLAY_PORT));
    lenient().when(iceAdapter.start(anyInt())).thenReturn(completedFuture(GPG_PORT));
    lenient().when(coturnService.getSelectedCoturns(anyInt())).thenReturn(completedFuture(List.of()));
    lenient().when(process.onExit()).thenReturn(completedFuture(process));
    lenient().when(gameService.getByUid(anyInt()))
             .thenReturn(Optional.of(GameBeanBuilder.create().defaultValues().get()));
    return gameParameters;
  }

  private GameParameters mockJoinGame(GameLaunchResponse gameLaunchResponse) throws IOException {
    lenient().when(featuredModService.updateFeaturedModToLatest(any(), anyBoolean())).thenReturn(completedFuture(null));
    lenient().when(fafServerAccessor.requestJoinGame(anyInt(), any())).thenReturn(completedFuture(gameLaunchResponse));
    lenient().when(modService.installAndEnableMods(anySet())).thenReturn(completedFuture(null));
    lenient().when(mapService.downloadIfNecessary(any())).thenReturn(completedFuture(null));
    return mockStartGameProcess(gameLaunchResponse);
  }

  private GameParameters mockHostGame(GameLaunchResponse gameLaunchResponse) throws IOException {
    lenient().when(featuredModService.updateFeaturedModToLatest(any(), anyBoolean())).thenReturn(completedFuture(null));
    lenient().when(fafServerAccessor.requestHostGame(any())).thenReturn(completedFuture(gameLaunchResponse));
    lenient().when(modService.installAndEnableMods(anySet())).thenReturn(completedFuture(null));
    lenient().when(mapService.downloadIfNecessary(any())).thenReturn(completedFuture(null));
    lenient().when(gameService.getByUid(anyInt()))
             .thenReturn(Optional.of(GameBeanBuilder.create().defaultValues().get()));
    return mockStartGameProcess(gameLaunchResponse);
  }

  private GameParameters mockMatchmakerGame(GameLaunchResponse gameLaunchResponse) throws IOException {
    lenient().when(featuredModService.updateFeaturedModToLatest(any(), anyBoolean())).thenReturn(completedFuture(null));
    lenient().when(fafServerAccessor.startSearchMatchmaker()).thenReturn(completedFuture(gameLaunchResponse));
    lenient().when(modService.installAndEnableMods(anySet())).thenReturn(completedFuture(null));
    return mockStartGameProcess(gameLaunchResponse);
  }

  @Test
  public void testJoinGameWithSimMods() throws Exception {
    GameBean game = GameBeanBuilder.create().defaultValues().get();

    ObservableMap<String, String> simMods = FXCollections.observableHashMap();
    simMods.put("123-456-789", "Fake mod name");

    game.setSimMods(simMods);

    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();

    GameParameters gameParameters = mockJoinGame(gameLaunchMessage);

    instance.join(game);
    verify(mapService).downloadIfNecessary(any());
    verify(replayServer).start(eq(game.getId()));
    verify(modService).installAndEnableMods(simMods.keySet());

    verify(forgedAllianceLaunchService).startGameOnline(gameParameters);
  }

  @Test
  public void testModEnabling() throws Exception {
    GameBean game = GameBeanBuilder.create().defaultValues().get();

    ObservableMap<String, String> simMods = FXCollections.observableHashMap();
    simMods.put("123-456-789", "Fake mod name");

    game.setSimMods(simMods);
    game.setMapFolderName("map");

    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();

    mockJoinGame(gameLaunchMessage);

    instance.join(game);

    verify(modService).installAndEnableMods(simModsCaptor.capture());
    assertEquals(simModsCaptor.getValue().iterator().next(), "123-456-789");
  }

  @Test
  public void testAddOnGameStartedListener() throws Exception {
    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();

    GameParameters gameParameters = mockHostGame(gameLaunchMessage);

    instance.host(newGameInfo);

    verify(forgedAllianceLaunchService).startGameOnline(gameParameters);
    verify(replayServer).start(eq(gameLaunchMessage.getUid()));
  }

  @Test
  public void testStartSearchLadder1v1() throws Exception {
    int uid = 123;
    String map = "scmp_037";
    GameLaunchResponse gameLaunchMessage = new GameLaunchMessageBuilder().defaultValues()
                                                                         .uid(uid).mod("FAF").mapname(map)
                                                                         .expectedPlayers(2)
                                                                         .faction(
                                                                             com.faforever.commons.lobby.Faction.CYBRAN)
                                                                         .gameType(GameType.MATCHMAKER)
                                                                         .mapPosition(4)
                                                                         .team(1)
                                                                         .ratingType(LADDER_1v1_RATING_TYPE)
                                                                         .get();

    GameParameters gameParameters = mockMatchmakerGame(gameLaunchMessage);

    instance.startSearchMatchmaker();

    gameParameters.setDivision("unlisted");

    verify(fafServerAccessor).startSearchMatchmaker();
    verify(mapService).downloadIfNecessary(map);
    verify(replayServer).start(eq(uid));
    verify(forgedAllianceLaunchService).startGameOnline(gameParameters);
  }

  @Test
  public void testStartSearchLadder1v1WithLeagueEntry() throws Exception {
    int uid = 123;
    String map = "scmp_037";
    GameLaunchResponse gameLaunchMessage = new GameLaunchMessageBuilder().defaultValues()
                                                                         .uid(uid).mod("FAF").mapname(map)
                                                                         .expectedPlayers(2)
                                                                         .faction(
                                                                             com.faforever.commons.lobby.Faction.CYBRAN)
                                                                         .gameType(GameType.MATCHMAKER)
                                                                         .mapPosition(4)
                                                                         .team(1)
                                                                         .ratingType(LADDER_1v1_RATING_TYPE)
                                                                         .get();

    GameParameters gameParameters = mockMatchmakerGame(gameLaunchMessage);

    instance.startSearchMatchmaker();

    verify(fafServerAccessor).startSearchMatchmaker();
    verify(mapService).downloadIfNecessary(map);
    verify(replayServer).start(eq(uid));
    verify(forgedAllianceLaunchService).startGameOnline(gameParameters);
  }

  @Test
  public void testStartSearchLadderTwiceReturnsSameFutureWhenSearching() throws Exception {
    int uid = 123;
    String map = "scmp_037";
    GameLaunchResponse gameLaunchMessage = new GameLaunchMessageBuilder().defaultValues()
                                                                         .uid(uid).mod("FAF").mapname(map)
                                                                         .expectedPlayers(2)
                                                                         .faction(
                                                                             com.faforever.commons.lobby.Faction.CYBRAN)
                                                                         .gameType(GameType.MATCHMAKER)
                                                                         .mapPosition(4)
                                                                         .team(1)
                                                                         .ratingType(LADDER_1v1_RATING_TYPE)
                                                                         .get();

    GameParameters gameParameters = mockMatchmakerGame(gameLaunchMessage);

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

    when(process.isAlive()).thenReturn(true);
    when(process.onExit()).thenReturn(new CompletableFuture<>());

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
    when(gameDirectoryRequiredHandler.onChooseGameDirectory()).thenReturn(new CompletableFuture<>());
    instance.host(null);
    verify(gameDirectoryRequiredHandler).onChooseGameDirectory();
  }

  @Test
  public void testGameHost() throws IOException {
    when(preferencesService.isValidGamePath()).thenReturn(true);
    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();
    GameParameters gameParameters = mockHostGame(gameLaunchMessage);

    instance.host(newGameInfo);

    verify(forgedAllianceLaunchService).startGameOnline(gameParameters);
  }

  @Test
  public void testOfflineGame() throws IOException {
    when(preferencesService.isValidGamePath()).thenReturn(true);
    when(forgedAllianceLaunchService.startGameOffline(any())).thenReturn(process);
    when(process.onExit()).thenReturn(completedFuture(process));
    instance.startGameOffline();
    verify(forgedAllianceLaunchService).startGameOffline(any());
  }

  @Test
  public void testOfflineGameInvalidPath() throws IOException {
    when(preferencesService.isValidGamePath()).thenReturn(false);
    when(gameDirectoryRequiredHandler.onChooseGameDirectory()).thenReturn(new CompletableFuture<>());
    instance.startGameOffline();
    verify(gameDirectoryRequiredHandler).onChooseGameDirectory();
  }

  @Test
  public void startSearchMatchmakerIfNoGameSet() {
    when(preferencesService.isValidGamePath()).thenReturn(false);
    when(gameDirectoryRequiredHandler.onChooseGameDirectory()).thenReturn(new CompletableFuture<>());
    instance.startSearchMatchmaker();
    verify(gameDirectoryRequiredHandler).onChooseGameDirectory();
  }

  @Test
  public void startSearchMatchmakerWithGameOptions() throws IOException {
    when(preferencesService.isValidGamePath()).thenReturn(true);
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

    GameParameters gameParameters = mockMatchmakerGame(gameLaunchMessage);

    instance.startSearchMatchmaker();

    verify(forgedAllianceLaunchService).startGameOnline(gameParameters);
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

    when(process.onExit()).thenReturn(new CompletableFuture<>());
    when(process.isAlive()).thenReturn(true);

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
    when(gameDirectoryRequiredHandler.onChooseGameDirectory()).thenReturn(new CompletableFuture<>());
    instance.join(null);
    verify(gameDirectoryRequiredHandler).onChooseGameDirectory();
  }

  @Test
  public void launchTutorialIfNoGameSet() {
    when(preferencesService.isValidGamePath()).thenReturn(false);
    when(gameDirectoryRequiredHandler.onChooseGameDirectory()).thenReturn(new CompletableFuture<>());
    instance.launchTutorial(null, null);
    verify(gameDirectoryRequiredHandler).onChooseGameDirectory();
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
    when(process.onExit()).thenReturn(new CompletableFuture<>());

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

    when(gameDirectoryRequiredHandler.onChooseGameDirectory()).thenReturn(completedFuture(Path.of(".")));

    instance.join(game);

    verify(gameDirectoryRequiredHandler).onChooseGameDirectory();
  }

  /**
   * Ensure that the user is allowed to choose the GameDirectory if no path is provided
   */
  @Test
  public void testJoinGameMissingGamePathUserSelectsInvalidPath() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();

    when(preferencesService.isValidGamePath()).thenReturn(false);

    when(gameDirectoryRequiredHandler.onChooseGameDirectory()).thenReturn(completedFuture(Path.of(".")),
                                                                          failedFuture(new CancellationException()));

    instance.join(game);

    verify(gameDirectoryRequiredHandler, times(2)).onChooseGameDirectory();
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
