package com.faforever.client.game;

import com.faforever.client.builders.GameInfoBuilder;
import com.faforever.client.builders.GameLaunchMessageBuilder;
import com.faforever.client.builders.NewGameInfoBuilder;
import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.discord.DiscordRichPresenceService;
import com.faforever.client.domain.api.LeagueEntry;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fa.ForgedAllianceLaunchService;
import com.faforever.client.fa.GameParameters;
import com.faforever.client.fa.relay.ice.CoturnService;
import com.faforever.client.fa.relay.ice.IceAdapter;
import com.faforever.client.featuredmod.FeaturedModService;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.ObservableConstant;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.EnterPasswordController.PasswordEnteredListener;
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
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameType;
import com.faforever.commons.lobby.NoticeInfo;
import javafx.beans.property.SimpleObjectProperty;
import javafx.stage.Stage;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.publisher.TestPublisher;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.CompletableFuture.completedFuture;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameRunnerTest extends ServiceTest {

  private static final Integer GPG_PORT = 1234;
  private static final int LOCAL_REPLAY_PORT = 15111;

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
  private UiService uiService;
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

  @Mock
  private EnterPasswordController enterPasswordController;

  private final TestPublisher<NoticeInfo> testNoticePublisher = TestPublisher.create();

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(gameMapper);
    PlayerInfo junitPlayer = PlayerInfoBuilder.create().defaultValues().get();

    lenient().doAnswer(invocation -> {
      Runnable runnable = invocation.getArgument(0);
      runnable.run();
      return null;
    }).when(fxApplicationThreadExecutor).execute(any());
    lenient().when(fafServerAccessor.getEvents(NoticeInfo.class)).thenReturn(testNoticePublisher.flux());
    lenient().when(coturnService.getSelectedCoturns(anyInt())).thenReturn(Flux.empty());
    lenient().when(preferencesService.hasValidGamePath()).thenReturn(true);
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
      lenient().when(mapService.downloadIfNecessary(any())).thenReturn(Mono.empty());
    }
    lenient().when(forgedAllianceLaunchService.launchOnlineGame(any(), anyInt(), anyInt())).thenReturn(process);
    lenient().when(replayServer.start(anyInt())).thenReturn(completedFuture(LOCAL_REPLAY_PORT));
    lenient().when(iceAdapter.start(anyInt())).thenReturn(completedFuture(GPG_PORT));
    lenient().when(coturnService.getSelectedCoturns(anyInt())).thenReturn(Flux.empty());
    lenient().when(process.onExit()).thenReturn(new CompletableFuture<>());
    lenient().when(process.exitValue()).thenReturn(0);
    lenient().when(process.isAlive()).thenReturn(true);
    lenient().when(gameService.observeByUid(anyInt())).thenAnswer(invocation -> ObservableConstant.valueOf(
                 GameInfoBuilder.create().id(invocation.getArgument(0, Integer.class)).get()));
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
    verify(fafServerAccessor).setTimeoutLoginReconnectSeconds(5);
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
    verify(fafServerAccessor).setTimeoutLoginReconnectSeconds(30);
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
    when(leaderboardService.getActiveLeagueEntryForPlayer(any(), any())).thenReturn(Mono.empty());

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
    LeagueEntry leagueEntry = Instancio.create(LeagueEntry.class);
    when(leaderboardService.getActiveLeagueEntryForPlayer(any(), any())).thenReturn(Mono.just(leagueEntry));

    instance.startOnlineGame(gameLaunchResponse);

    ArgumentCaptor<GameParameters> captor = ArgumentCaptor.forClass(GameParameters.class);
    verify(forgedAllianceLaunchService).launchOnlineGame(captor.capture(), eq(GPG_PORT), eq(LOCAL_REPLAY_PORT));
    GameParameters gameParameters = captor.getValue();

    verify(leaderboardService).getActiveLeagueEntryForPlayer(any(), eq(gameParameters.leaderboard()));
    verify(mapService).downloadIfNecessary(gameParameters.mapName());

    assertNotNull(gameParameters.league());
    assertEquals(leagueEntry.subdivision().division().nameKey(), gameParameters.league().division());
    assertEquals(leagueEntry.subdivision().nameKey(), gameParameters.league().subDivision());
  }

  @Test
  public void testRestoreGameSessionListener() throws Exception {
    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();

    mockStartGameProcess(gameLaunchMessage);

    SimpleObjectProperty<ConnectionState> connectionState = new SimpleObjectProperty<>();
    when(fafServerAccessor.connectionStateProperty()).thenReturn(connectionState);

    instance.afterPropertiesSet();
    instance.startOnlineGame(gameLaunchMessage);

    connectionState.set(ConnectionState.DISCONNECTED);
    connectionState.set(ConnectionState.CONNECTED);

    verify(fafServerAccessor).restoreGameSession(anyInt());
  }

  @Test
  public void testStartOnlineGameBadKnownExit() throws Exception {
    GameLaunchResponse gameLaunchResponse = GameLaunchMessageBuilder.create().defaultValues().get();
    mockStartGameProcess(gameLaunchResponse);

    CompletableFuture<Process> exitFuture = new CompletableFuture<>();

    when(process.onExit()).thenReturn(exitFuture);

    instance.startOnlineGame(gameLaunchResponse);

    when(process.exitValue()).thenReturn(-1073741515);
    exitFuture.complete(process);

    verify(notificationService).addImmediateWarnNotification(anyString());
  }

  @Test
  public void testStartOnlineGameBadUnknownExit() throws Exception {
    GameLaunchResponse gameLaunchResponse = GameLaunchMessageBuilder.create().defaultValues().get();
    mockStartGameProcess(gameLaunchResponse);

    CompletableFuture<Process> exitFuture = new CompletableFuture<>();

    when(process.onExit()).thenReturn(exitFuture);

    instance.startOnlineGame(gameLaunchResponse);

    when(process.exitValue()).thenReturn(-1);
    exitFuture.complete(process);

    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  @Test
  public void testStartOnlineGameThenKill() throws Exception {
    GameLaunchResponse gameLaunchResponse = GameLaunchMessageBuilder.create().defaultValues().get();
    mockStartGameProcess(gameLaunchResponse);

    CompletableFuture<Process> exitFuture = new CompletableFuture<>();

    when(process.onExit()).thenReturn(exitFuture);

    instance.startOnlineGame(gameLaunchResponse);

    when(process.exitValue()).thenReturn(-1);
    doAnswer(invocation -> {
      exitFuture.complete(process);
      return null;
    }).when(process).destroy();

    testNoticePublisher.next(new NoticeInfo("kill", "test"));

    verify(iceAdapter).stop();
    verify(process).destroy();
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  @Test
  public void testPrepareGameWithoutMapOrMods() throws Exception {
    GameLaunchResponse gameLaunchResponse = GameLaunchMessageBuilder.create().defaultValues().get();
    mockStartGameProcess(gameLaunchResponse);
    when(featuredModService.updateFeaturedModToLatest(any(), anyBoolean())).thenReturn(completedFuture(null));

    instance.prepareAndLaunchGameWhenReady("faf", Set.of(), null, () -> completedFuture(gameLaunchResponse));

    verify(featuredModService).updateFeaturedModToLatest("faf", false);
    verify(modService, never()).downloadAndEnableMods(any());
    verify(mapService, never()).downloadIfNecessary(any());
  }

  @Test
  public void testPrepareGameWitMap() throws Exception {
    GameLaunchResponse gameLaunchResponse = GameLaunchMessageBuilder.create().defaultValues().get();
    mockStartGameProcess(gameLaunchResponse);
    when(featuredModService.updateFeaturedModToLatest(any(), anyBoolean())).thenReturn(completedFuture(null));
    when(mapService.downloadIfNecessary(any())).thenReturn(Mono.empty());

    instance.prepareAndLaunchGameWhenReady("faf", Set.of(), "testMap", () -> completedFuture(gameLaunchResponse));

    verify(featuredModService).updateFeaturedModToLatest("faf", false);
    verify(mapService).downloadIfNecessary(any());
    verify(modService, never()).downloadAndEnableMods(any());
  }

  @Test
  public void testPrepareGameWitMods() throws Exception {
    GameLaunchResponse gameLaunchResponse = GameLaunchMessageBuilder.create().defaultValues().get();
    mockStartGameProcess(gameLaunchResponse);
    when(featuredModService.updateFeaturedModToLatest(any(), anyBoolean())).thenReturn(completedFuture(null));
    when(modService.downloadAndEnableMods(any())).thenReturn(Mono.empty());

    instance.prepareAndLaunchGameWhenReady("faf", Set.of("uid"), null, () -> completedFuture(gameLaunchResponse));

    verify(featuredModService).updateFeaturedModToLatest("faf", false);
    verify(modService).downloadAndEnableMods(any());
    verify(mapService, never()).downloadIfNecessary(any());
  }

  private void mockJoinGame(GameInfo game) throws IOException {
    GameLaunchResponse gameLaunchResponse = GameLaunchMessageBuilder.create()
                                                                    .defaultValues()
                                                                    .gameType(game.getGameType())
                                                                    .mapName(game.getMapFolderName())
                                                                    .featuredMod(game.getFeaturedMod())
                                                                    .get();

    lenient().when(featuredModService.updateFeaturedModToLatest(any(), anyBoolean())).thenReturn(completedFuture(null));
    lenient().when(fafServerAccessor.requestJoinGame(anyInt(), any())).thenReturn(completedFuture(gameLaunchResponse));
    lenient().when(modService.downloadAndEnableMods(anySet())).thenReturn(Mono.empty());
    lenient().when(mapService.downloadIfNecessary(any())).thenReturn(Mono.empty());
    mockStartGameProcess(gameLaunchResponse);
  }

  @Test
  public void testJoinGame() throws Exception {
    GameInfo game = GameInfoBuilder.create().defaultValues().get();

    mockJoinGame(game);

    instance.join(game);

    verify(forgedAllianceLaunchService).launchOnlineGame(any(), anyInt(), anyInt());
    verify(fafServerAccessor).requestJoinGame(anyInt(), any());
  }

  @Test
  public void testJoinGameWhileRunning() throws Exception {
    GameInfo game = GameInfoBuilder.create().defaultValues().get();

    mockJoinGame(game);
    when(process.onExit()).thenReturn(new CompletableFuture<>());

    instance.join(game);
    instance.join(game);

    verify(notificationService).addImmediateWarnNotification(anyString());
    verify(forgedAllianceLaunchService).launchOnlineGame(any(), anyInt(), anyInt());
    verify(fafServerAccessor).requestJoinGame(eq(game.getId()), any());
  }

  @Test
  public void testJoinGameNoValidPath() throws Exception {
    GameInfo game = GameInfoBuilder.create().defaultValues().get();

    mockJoinGame(game);
    when(preferencesService.hasValidGamePath()).thenReturn(false);

    CompletableFuture<Void> pathChosenFuture = new CompletableFuture<>();
    when(gamePathHandler.chooseAndValidateGameDirectory()).thenReturn(pathChosenFuture);

    instance.join(game);

    verify(gamePathHandler).chooseAndValidateGameDirectory();

    when(preferencesService.hasValidGamePath()).thenReturn(true);
    pathChosenFuture.complete(null);

    verify(forgedAllianceLaunchService).launchOnlineGame(any(), anyInt(), anyInt());
    verify(fafServerAccessor).requestJoinGame(anyInt(), any());
  }

  @Test
  public void testJoinGameWaitingForMatchmaker() throws Exception {
    mockMatchmakerGame(GameLaunchMessageBuilder.create().defaultValues().get());
    when(fafServerAccessor.startSearchMatchmaker()).thenReturn(new CompletableFuture<>());

    instance.startSearchMatchmaker();

    instance.join(GameInfoBuilder.create().defaultValues().get());

    verify(fafServerAccessor, never()).requestJoinGame(anyInt(), any());
  }

  @Test
  public void testJoinGameRatingTooLow() throws Exception {
    GameInfo game = GameInfoBuilder.create().defaultValues()
                                   .ratingMax(-100)
                                   .get();

    mockJoinGame(game);

    instance.join(game);

    ArgumentCaptor<ImmediateNotification> captor = ArgumentCaptor.forClass(ImmediateNotification.class);

    verify(notificationService).addNotification(captor.capture());

    ImmediateNotification value = captor.getValue();

    value.actions().getFirst().run();

    verify(fafServerAccessor).requestJoinGame(anyInt(), any());
  }

  @Test
  public void testJoinGameRatingTooHigh() throws Exception {
    GameInfo game = GameInfoBuilder.create().defaultValues()
                                   .ratingMin(5000)
                                   .get();

    mockJoinGame(game);

    instance.join(game);

    ArgumentCaptor<ImmediateNotification> captor = ArgumentCaptor.forClass(ImmediateNotification.class);

    verify(notificationService).addNotification(captor.capture());

    ImmediateNotification value = captor.getValue();

    value.actions().getFirst().run();

    verify(fafServerAccessor).requestJoinGame(anyInt(), any());
  }

  @Test
  public void testJoinGameWithPassword() throws Exception {
    StageHolder.setStage(mock(Stage.class));
    when(uiService.loadFxml("theme/enter_password.fxml")).thenReturn(enterPasswordController);

    GameInfo game = GameInfoBuilder.create().defaultValues().passwordProtected(true).get();

    mockJoinGame(game);
    instance.join(game);

    verify(enterPasswordController).showPasswordDialog(any());
    verify(fafServerAccessor, never()).requestJoinGame(anyInt(), any());

    ArgumentCaptor<PasswordEnteredListener> captor = ArgumentCaptor.forClass(PasswordEnteredListener.class);
    verify(enterPasswordController).setPasswordEnteredListener(captor.capture());

    PasswordEnteredListener listener = captor.getValue();
    listener.onPasswordEntered(game, "test", false);

    verify(forgedAllianceLaunchService).launchOnlineGame(any(), anyInt(), anyInt());
    verify(fafServerAccessor).requestJoinGame(game.getId(), "test");
  }

  private void mockHostGame(NewGameInfo newGameInfo) throws IOException {
    GameLaunchResponse gameLaunchResponse = GameLaunchMessageBuilder.create()
                                                                    .defaultValues()
                                                                    .featuredMod(newGameInfo.featuredModName())
                                                                    .mapName(newGameInfo.map())
                                                                    .gameType(GameType.CUSTOM)
                                                                    .get();

    lenient().when(featuredModService.updateFeaturedModToLatest(any(), anyBoolean())).thenReturn(completedFuture(null));
    lenient().when(fafServerAccessor.requestHostGame(any())).thenReturn(completedFuture(gameLaunchResponse));
    lenient().when(modService.downloadAndEnableMods(anySet())).thenReturn(Mono.empty());
    lenient().when(mapService.downloadIfNecessary(any())).thenReturn(Mono.empty());
    lenient().when(gameService.observeByUid(anyInt()))
             .thenReturn(ObservableConstant.valueOf(GameInfoBuilder.create().defaultValues().get()));
    mockStartGameProcess(gameLaunchResponse);
  }

  @Test
  public void testHostGame() throws Exception {
    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    mockHostGame(newGameInfo);

    instance.host(newGameInfo);

    verify(fafServerAccessor).requestHostGame(newGameInfo);
    verify(forgedAllianceLaunchService).launchOnlineGame(any(), eq(GPG_PORT), eq(LOCAL_REPLAY_PORT));
  }

  @Test
  public void testHostGameWhileRunning() throws Exception {
    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    mockHostGame(newGameInfo);
    when(process.onExit()).thenReturn(new CompletableFuture<>());

    instance.host(newGameInfo);
    instance.host(newGameInfo);

    verify(notificationService).addImmediateWarnNotification(anyString());
    verify(forgedAllianceLaunchService).launchOnlineGame(any(), anyInt(), anyInt());
    verify(fafServerAccessor).requestHostGame(newGameInfo);
  }

  @Test
  public void testHostGameNoValidPath() throws Exception {
    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    mockHostGame(newGameInfo);
    when(preferencesService.hasValidGamePath()).thenReturn(false);

    CompletableFuture<Void> pathChosenFuture = new CompletableFuture<>();
    when(gamePathHandler.chooseAndValidateGameDirectory()).thenReturn(pathChosenFuture);

    instance.host(newGameInfo);

    verify(gamePathHandler).chooseAndValidateGameDirectory();

    when(preferencesService.hasValidGamePath()).thenReturn(true);
    pathChosenFuture.complete(null);

    verify(forgedAllianceLaunchService).launchOnlineGame(any(), anyInt(), anyInt());
    verify(fafServerAccessor).requestHostGame(newGameInfo);
  }

  @Test
  public void testHostGameWaitingForMatchmaker() throws Exception {
    when(fafServerAccessor.startSearchMatchmaker()).thenReturn(new CompletableFuture<>());

    mockMatchmakerGame(GameLaunchMessageBuilder.create().defaultValues().get());
    when(fafServerAccessor.startSearchMatchmaker()).thenReturn(new CompletableFuture<>());

    instance.startSearchMatchmaker();

    instance.host(NewGameInfoBuilder.create().defaultValues().get());

    verify(fafServerAccessor, never()).requestHostGame(any());
  }

  private void mockMatchmakerGame(GameLaunchResponse gameLaunchResponse) throws IOException {
    lenient().when(featuredModService.updateFeaturedModToLatest(any(), anyBoolean())).thenReturn(completedFuture(null));
    lenient().when(fafServerAccessor.startSearchMatchmaker()).thenReturn(completedFuture(gameLaunchResponse));
    lenient().when(modService.downloadAndEnableMods(anySet())).thenReturn(Mono.empty());
    mockStartGameProcess(gameLaunchResponse);
  }

  @Test
  public void testStartSearch() throws Exception {
    mockMatchmakerGame(new GameLaunchMessageBuilder().defaultValues().get());

    instance.startSearchMatchmaker();

    verify(fafServerAccessor).startSearchMatchmaker();
    verify(forgedAllianceLaunchService).launchOnlineGame(any(), anyInt(), anyInt());
  }

  @Test
  public void testStartSearchWhileRunning() throws Exception {
    mockMatchmakerGame(GameLaunchMessageBuilder.create().defaultValues().get());
    when(process.onExit()).thenReturn(new CompletableFuture<>());

    instance.startSearchMatchmaker();
    instance.startSearchMatchmaker();

    verify(notificationService).addImmediateWarnNotification(anyString());
    verify(forgedAllianceLaunchService).launchOnlineGame(any(), anyInt(), anyInt());
    verify(fafServerAccessor).startSearchMatchmaker();
  }

  @Test
  public void testStartSearchNoValidPath() throws Exception {
    mockMatchmakerGame(GameLaunchMessageBuilder.create().defaultValues().get());
    when(preferencesService.hasValidGamePath()).thenReturn(false);

    CompletableFuture<Void> pathChosenFuture = new CompletableFuture<>();
    when(gamePathHandler.chooseAndValidateGameDirectory()).thenReturn(pathChosenFuture);

    instance.startSearchMatchmaker();

    verify(gamePathHandler).chooseAndValidateGameDirectory();

    when(preferencesService.hasValidGamePath()).thenReturn(true);
    pathChosenFuture.complete(null);

    verify(forgedAllianceLaunchService).launchOnlineGame(any(), anyInt(), anyInt());
    verify(fafServerAccessor).startSearchMatchmaker();
  }

  @Test
  public void testStartSearchWaitingForMatchmaker() throws Exception {
    when(fafServerAccessor.startSearchMatchmaker()).thenReturn(new CompletableFuture<>());

    mockMatchmakerGame(GameLaunchMessageBuilder.create().defaultValues().get());

    instance.startSearchMatchmaker();
    instance.startSearchMatchmaker();

    verify(fafServerAccessor).startSearchMatchmaker();
  }

  @Test
  public void testStartStopStartSearchWaitingForMatchmaker() throws Exception {
    when(fafServerAccessor.startSearchMatchmaker()).thenReturn(new CompletableFuture<>());

    mockMatchmakerGame(GameLaunchMessageBuilder.create().defaultValues().get());
    when(fafServerAccessor.startSearchMatchmaker()).thenReturn(new CompletableFuture<>());

    instance.startSearchMatchmaker();
    instance.stopSearchMatchmaker();
    instance.startSearchMatchmaker();

    verify(fafServerAccessor, times(2)).startSearchMatchmaker();
  }

  @Test
  public void testStopSearchForMatchmakerWhileRunning() throws Exception {
    when(fafServerAccessor.startSearchMatchmaker()).thenReturn(new CompletableFuture<>());

    mockMatchmakerGame(GameLaunchMessageBuilder.create().defaultValues().get());

    instance.startSearchMatchmaker();
    instance.stopSearchMatchmaker();

    verify(fafServerAccessor).startSearchMatchmaker();
    verify(process).destroy();
  }

  @Test
  public void testLaunchTutorial() {
    when(preferencesService.hasValidGamePath()).thenReturn(true);
    when(mapService.downloadIfNecessary(any())).thenReturn(Mono.empty());
    when(forgedAllianceLaunchService.launchOfflineGame(any())).thenReturn(process);
    when(featuredModService.updateFeaturedModToLatest(any(), anyBoolean())).thenReturn(completedFuture(null));
    when(process.onExit()).thenReturn(new CompletableFuture<>());
    when(process.isAlive()).thenReturn(true);

    MapVersion mapVersion = Instancio.create(MapVersion.class);
    instance.launchTutorial(mapVersion, "tut");

    verify(mapService).downloadIfNecessary(mapVersion.folderName());
    verify(featuredModService).updateFeaturedModToLatest(KnownFeaturedMod.TUTORIALS.getTechnicalName(), false);
    verify(forgedAllianceLaunchService).launchOfflineGame("tut");
    assertEquals(10L, instance.getRunningProcessId());
  }

  @Test
  public void testLaunchTutorialGameRunning() {
    when(preferencesService.hasValidGamePath()).thenReturn(true);
    when(mapService.downloadIfNecessary(any())).thenReturn(Mono.empty());
    when(forgedAllianceLaunchService.launchOfflineGame(any())).thenReturn(process);
    when(featuredModService.updateFeaturedModToLatest(any(), anyBoolean())).thenReturn(completedFuture(null));
    when(process.onExit()).thenReturn(new CompletableFuture<>());
    when(process.isAlive()).thenReturn(true);

    MapVersion mapVersion = Instancio.create(MapVersion.class);
    instance.launchTutorial(mapVersion, "tut");
    instance.launchTutorial(mapVersion, "tut");

    verify(forgedAllianceLaunchService).launchOfflineGame("tut");
  }

  @Test
  public void testLaunchTutorialIfNoGameSet() {
    when(preferencesService.hasValidGamePath()).thenReturn(false);
    when(gamePathHandler.chooseAndValidateGameDirectory()).thenReturn(new CompletableFuture<>());

    instance.launchTutorial(Instancio.create(MapVersion.class), "tut");

    verify(gamePathHandler).chooseAndValidateGameDirectory();
  }

  @Test
  @Disabled("Flaky Test")
  public void testOfflineGame() throws IOException {
    when(preferencesService.hasValidGamePath()).thenReturn(true);
    when(forgedAllianceLaunchService.launchOfflineGame(any())).thenReturn(process);

    instance.startOffline();

    verify(forgedAllianceLaunchService).launchOfflineGame(null);
  }

  @Test
  @Disabled("Race condition")
  public void testOfflineGameRunning() throws IOException {
    when(preferencesService.hasValidGamePath()).thenReturn(true);
    when(forgedAllianceLaunchService.launchOfflineGame(any())).thenReturn(process);
    when(process.onExit()).thenReturn(new CompletableFuture<>());
    when(process.isAlive()).thenReturn(true);

    instance.startOffline();
    instance.startOffline();

    verify(forgedAllianceLaunchService).launchOfflineGame(null);
  }

  @Test
  public void testOfflineGameInvalidPath() {
    when(preferencesService.hasValidGamePath()).thenReturn(false);
    when(gamePathHandler.chooseAndValidateGameDirectory()).thenReturn(new CompletableFuture<>());

    instance.startOffline();

    verify(gamePathHandler).chooseAndValidateGameDirectory();
  }
}
