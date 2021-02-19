package com.faforever.client.game;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.discord.DiscordRichPresenceService;
import com.faforever.client.fa.ForgedAllianceService;
import com.faforever.client.fa.relay.LobbyMode;
import com.faforever.client.fa.relay.event.RehostRequestEvent;
import com.faforever.client.fa.relay.ice.IceAdapter;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.patch.GameUpdater;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesBuilder;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.ReconnectTimerService;
import com.faforever.client.remote.domain.GameInfoMessage;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.replay.ReplayServer;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.ReflectionUtils;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.faforever.client.game.Faction.CYBRAN;
import static com.faforever.client.game.KnownFeaturedMod.FAF;
import static com.faforever.client.remote.domain.GameStatus.CLOSED;
import static com.faforever.client.remote.domain.GameStatus.OPEN;
import static com.faforever.client.remote.domain.GameStatus.PLAYING;
import static com.natpryce.hamcrest.reflection.HasAnnotationMatcher.hasAnnotation;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GameServiceTest extends AbstractPlainJavaFxTest {

  private static final long TIMEOUT = 5000;
  private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
  private static final Integer GPG_PORT = 1234;
  private static final int LOCAL_REPLAY_PORT = 15111;
  private static final String GLOBAL_RATING_TYPE = "global";
  private static final String LADDER_1v1_RATING_TYPE = "ladder_1v1";

  private GameService instance;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private FafService fafService;
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
  private ReplayServer replayService;
  @Mock
  private EventBus eventBus;
  @Mock
  private IceAdapter iceAdapter;
  @Mock
  private ModService modService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private ReportingService reportingService;
  @Mock
  private PlatformService platformService;
  @Mock
  private ReconnectTimerService reconnectTimerService;
  @Mock
  private DiscordRichPresenceService discordRichPresenceService;
  @Mock
  private Process process;

  @Captor
  private ArgumentCaptor<Consumer<GameInfoMessage>> gameInfoMessageListenerCaptor;
  @Captor
  private ArgumentCaptor<Set<String>> simModsCaptor;

  private Player junitPlayer;
  private Preferences preferences;

  @Before
  public void setUp() throws Exception {
    junitPlayer = PlayerBuilder.create("JUnit").defaultValues().get();
    preferences = PreferencesBuilder.create().defaultValues().get();

    ClientProperties clientProperties = new ClientProperties();

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferencesService.isGamePathValid()).thenReturn(true);
    when(fafService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>());
    when(replayService.start(anyInt(), any())).thenReturn(completedFuture(LOCAL_REPLAY_PORT));
    when(iceAdapter.start()).thenReturn(completedFuture(GPG_PORT));
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(junitPlayer));

    doAnswer(invocation -> {
      try {
        ((Runnable) invocation.getArgument(0)).run();
      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }).when(executorService).execute(any());

    instance = new GameService(clientProperties, fafService, forgedAllianceService, mapService,
        preferencesService, gameUpdater, notificationService, i18n, executorService, playerService,
        reportingService, eventBus, iceAdapter, modService, platformService, discordRichPresenceService,
        replayService, reconnectTimerService);

    instance.afterPropertiesSet();

    verify(fafService).addOnMessageListener(eq(GameInfoMessage.class), gameInfoMessageListenerCaptor.capture());
  }

  private void mockGlobalStartGameProcess(int uid, String... additionalArgs) throws IOException {
    mockStartGameProcess(uid, GLOBAL_RATING_TYPE, null, false, additionalArgs);
  }

  private void mockStartGameProcess(int uid, String ratingType, Faction faction, boolean rehost, String... additionalArgs) throws IOException {
    when(forgedAllianceService.startGame(
        uid, faction, asList(additionalArgs), ratingType, GPG_PORT, LOCAL_REPLAY_PORT, rehost, junitPlayer)
    ).thenReturn(process);
  }

  private void mockStartReplayProcess(Path path, int id) throws IOException {
    when(forgedAllianceService.startReplay(path, id)).thenReturn(process);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void postConstruct() {
    verify(fafService).addOnMessageListener(eq(GameInfoMessage.class), any(Consumer.class));
  }

  @Test
  public void testJoinGameMapIsAvailable() throws Exception {
    Game game = GameBuilder.create().defaultValues().get();

    ObservableMap<String, String> simMods = FXCollections.observableHashMap();
    simMods.put("123-456-789", "Fake mod name");

    game.setSimMods(simMods);

    GameLaunchMessage gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();

    mockGlobalStartGameProcess(gameLaunchMessage.getUid());
    when(mapService.isInstalled(game.getMapFolderName())).thenReturn(true);
    when(fafService.requestJoinGame(game.getId(), null)).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));

    CompletableFuture<Void> future = instance.joinGame(game, null).toCompletableFuture();

    assertNull(future.get(TIMEOUT, TIME_UNIT));
    verify(mapService, never()).download(any());
    verify(replayService).start(eq(game.getId()), any());

    verify(forgedAllianceService).startGame(
        gameLaunchMessage.getUid(), null, asList(), gameLaunchMessage.getRatingType(),
        GPG_PORT, LOCAL_REPLAY_PORT, false, junitPlayer);
  }

  @Test
  public void testJoinGameNoLaunchNoGameRatingType() throws Exception {
    Game game = GameBuilder.create().defaultValues().ratingType(null).get();

    GameLaunchMessage gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().ratingType(null).get();

    mockGlobalStartGameProcess(gameLaunchMessage.getUid());
    when(mapService.isInstalled(game.getMapFolderName())).thenReturn(true);
    when(fafService.requestJoinGame(game.getId(), null)).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));

    CompletableFuture<Void> future = instance.joinGame(game, null).toCompletableFuture();

    assertNull(future.get(TIMEOUT, TIME_UNIT));
    verify(mapService, never()).download(any());
    verify(replayService).start(eq(game.getId()), any());

    verify(forgedAllianceService).startGame(
        gameLaunchMessage.getUid(), null, asList(), GameService.DEFAULT_RATING_TYPE,
        GPG_PORT, LOCAL_REPLAY_PORT, false, junitPlayer);
  }

  @Test
  public void testJoinGameNoLaunchRatingType() throws Exception {
    Game game = GameBuilder.create().defaultValues().get();

    GameLaunchMessage gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().ratingType(null).get();

    mockGlobalStartGameProcess(gameLaunchMessage.getUid());
    when(mapService.isInstalled(game.getMapFolderName())).thenReturn(true);
    when(fafService.requestJoinGame(game.getId(), null)).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));

    CompletableFuture<Void> future = instance.joinGame(game, null).toCompletableFuture();

    assertNull(future.get(TIMEOUT, TIME_UNIT));
    verify(mapService, never()).download(any());
    verify(replayService).start(eq(game.getId()), any());

    verify(forgedAllianceService).startGame(
        gameLaunchMessage.getUid(), null, asList(), game.getRatingType(),
        GPG_PORT, LOCAL_REPLAY_PORT, false, junitPlayer);
  }

  @Test
  public void testStartReplayWhileInGameAllowed() throws Exception {
    preferences.getForgedAlliance().setAllowReplaysWhileInGame(true);
    Game game = GameBuilder.create().defaultValues().get();

    GameLaunchMessage gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();
    Path replayPath = Paths.get("temp.scfareplay");
    int replayId = 1234;

    mockGlobalStartGameProcess(gameLaunchMessage.getUid());
    when(mapService.isInstalled(anyString())).thenReturn(true);
    when(fafService.requestJoinGame(anyInt(), isNull())).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(anyString())).thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));
    when(process.isAlive()).thenReturn(true);

    CompletableFuture<Void> future = instance.joinGame(game, null).toCompletableFuture();
    future.join();
    mockStartReplayProcess(replayPath, replayId);
    future = instance.runWithReplay(replayPath, replayId, "", null, null, null, "map");
    future.join();

    verify(replayService).start(eq(game.getId()), any());
    verify(forgedAllianceService).startGame(
        gameLaunchMessage.getUid(), null, asList(), GLOBAL_RATING_TYPE,
        GPG_PORT, LOCAL_REPLAY_PORT, false, junitPlayer);
    verify(forgedAllianceService).startReplay(replayPath, replayId);
  }

  @Test
  public void testStartReplayWhileInGameNotAllowed() throws Exception {
    preferences.getForgedAlliance().setAllowReplaysWhileInGame(false);
    Game game = GameBuilder.create().defaultValues().get();

    GameLaunchMessage gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();
    Path replayPath = Paths.get("temp.scfareplay");
    int replayId = 1234;

    mockGlobalStartGameProcess(gameLaunchMessage.getUid());
    when(mapService.isInstalled(anyString())).thenReturn(true);
    when(fafService.requestJoinGame(anyInt(), isNull())).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(anyString())).thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));
    when(process.isAlive()).thenReturn(true);

    CompletableFuture<Void> future = instance.joinGame(game, null).toCompletableFuture();
    future.join();
    mockStartReplayProcess(replayPath, replayId);
    future = instance.runWithReplay(replayPath, replayId, "", null, null, null, "map");
    future.join();

    verify(replayService).start(eq(game.getId()), any());
    verify(forgedAllianceService).startGame(
        gameLaunchMessage.getUid(), null, asList(), GLOBAL_RATING_TYPE,
        GPG_PORT, LOCAL_REPLAY_PORT, false, junitPlayer);
    verify(forgedAllianceService, never()).startReplay(replayPath, replayId);
  }

  @Test
  public void testModEnabling() throws Exception {
    Game game = GameBuilder.create().defaultValues().get();

    ObservableMap<String, String> simMods = FXCollections.observableHashMap();
    simMods.put("123-456-789", "Fake mod name");

    game.setSimMods(simMods);
    game.setMapFolderName("map");

    GameLaunchMessage gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();

    mockGlobalStartGameProcess(gameLaunchMessage.getUid());
    when(mapService.isInstalled("map")).thenReturn(true);
    when(fafService.requestJoinGame(game.getId(), null)).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));

    instance.joinGame(game, null).toCompletableFuture().get();
    verify(modService).enableSimMods(simModsCaptor.capture());
    assertEquals(simModsCaptor.getValue().iterator().next(), "123-456-789");
  }

  @Test
  public void testAddOnGameStartedListener() throws Exception {
    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    GameLaunchMessage gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().args("/foo bar", "/bar foo").get();

    mockGlobalStartGameProcess(gameLaunchMessage.getUid(), "/foo", "bar", "/bar", "foo");
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(fafService.requestHostGame(newGameInfo)).thenReturn(completedFuture(gameLaunchMessage));
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
    verify(forgedAllianceService).startGame(
        gameLaunchMessage.getUid(), null, asList("/foo", "bar", "/bar", "foo"), GLOBAL_RATING_TYPE,
        GPG_PORT, LOCAL_REPLAY_PORT, false, junitPlayer);
    verify(replayService).start(eq(gameLaunchMessage.getUid()), any());
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

    instance.spawnTerminationListener(process);

    disconnectedFuture.get(5000, TimeUnit.MILLISECONDS);

    verify(process).waitFor();
  }

  @Test
  public void testOnGames() {
    assertThat(instance.getGames(), empty());

    GameInfoMessage multiGameInfoMessage = new GameInfoMessage();
    multiGameInfoMessage.setGames(asList(
        GameInfoMessageBuilder.create(1).defaultValues().get(),
        GameInfoMessageBuilder.create(2).defaultValues().get()
    ));

    gameInfoMessageListenerCaptor.getValue().accept(multiGameInfoMessage);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getGames(), hasSize(2));
  }

  @Test
  public void testOnGameInfoAdd() {
    assertThat(instance.getGames(), empty());

    GameInfoMessage gameInfoMessage1 = GameInfoMessageBuilder.create(1).defaultValues().title("Game 1").get();
    gameInfoMessageListenerCaptor.getValue().accept(gameInfoMessage1);

    GameInfoMessage gameInfoMessage2 = GameInfoMessageBuilder.create(2).defaultValues().title("Game 2").get();
    gameInfoMessageListenerCaptor.getValue().accept(gameInfoMessage2);
    WaitForAsyncUtils.waitForFxEvents();

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
  public void testOnGameInfoMessageSetsCurrentGameIfUserIsInAndStatusOpen() {
    assertThat(instance.getCurrentGame(), nullValue());

    when(playerService.getCurrentPlayer()).thenReturn(Optional.ofNullable(PlayerBuilder.create("PlayerName").get()));

    GameInfoMessage gameInfoMessage = GameInfoMessageBuilder.create(1234).defaultValues()
        .state(OPEN)
        .addTeamMember("1", "PlayerName").get();
    gameInfoMessageListenerCaptor.getValue().accept(gameInfoMessage);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getCurrentGame(), notNullValue());
    assertThat(instance.getCurrentGame().getId(), is(1234));
  }

  @Test
  public void testOnGameInfoMessageDoesntSetCurrentGameIfUserIsInAndStatusNotOpen() {
    assertThat(instance.getCurrentGame(), nullValue());

    when(playerService.getCurrentPlayer()).thenReturn(Optional.ofNullable(PlayerBuilder.create("PlayerName").get()));

    GameInfoMessage gameInfoMessage = GameInfoMessageBuilder.create(1234).defaultValues()
        .state(PLAYING)
        .addTeamMember("1", "PlayerName").get();
    gameInfoMessageListenerCaptor.getValue().accept(gameInfoMessage);

    assertThat(instance.getCurrentGame(), nullValue());
  }

  @Test
  public void testOnGameInfoMessageDoesntSetCurrentGameIfUserDoesntMatch() {
    assertThat(instance.getCurrentGame(), nullValue());

    when(playerService.getCurrentPlayer()).thenReturn(Optional.ofNullable(PlayerBuilder.create("PlayerName").get()));

    GameInfoMessage gameInfoMessage = GameInfoMessageBuilder.create(1234).defaultValues().addTeamMember("1", "Other").get();
    gameInfoMessageListenerCaptor.getValue().accept(gameInfoMessage);

    assertThat(instance.getCurrentGame(), nullValue());
  }

  @Test
  public void testOnGameInfoModify() throws InterruptedException {
    assertThat(instance.getGames(), empty());

    GameInfoMessage gameInfoMessage = GameInfoMessageBuilder.create(1).defaultValues().title("Game 1").state(PLAYING).get();
    gameInfoMessageListenerCaptor.getValue().accept(gameInfoMessage);
    WaitForAsyncUtils.waitForFxEvents();

    CountDownLatch changeLatch = new CountDownLatch(1);
    Game game = instance.getGames().iterator().next();
    game.titleProperty().addListener((observable, oldValue, newValue) -> {
      changeLatch.countDown();
    });

    gameInfoMessage = GameInfoMessageBuilder.create(1).defaultValues().title("Game 1 modified").state(PLAYING).get();
    gameInfoMessageListenerCaptor.getValue().accept(gameInfoMessage);

    changeLatch.await();
    assertEquals(gameInfoMessage.getTitle(), game.getTitle());
  }

  @Test
  public void testOnGameInfoRemove() {
    assertThat(instance.getGames(), empty());

    when(playerService.getCurrentPlayer()).thenReturn(Optional.ofNullable(PlayerBuilder.create("PlayerName").get()));

    GameInfoMessage gameInfoMessage = GameInfoMessageBuilder.create(1).defaultValues().title("Game 1").get();
    gameInfoMessageListenerCaptor.getValue().accept(gameInfoMessage);

    gameInfoMessage = GameInfoMessageBuilder.create(1).title("Game 1").defaultValues().state(CLOSED).get();
    gameInfoMessageListenerCaptor.getValue().accept(gameInfoMessage);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getGames(), empty());
  }

  @Test
  public void testStartSearchLadder1v1() throws Exception {
    int uid = 123;
    String map = "scmp_037";
    GameLaunchMessage gameLaunchMessage = new GameLaunchMessageBuilder().defaultValues()
        .uid(uid).mod("FAF").mapname(map)
        .expectedPlayers(2)
        .faction(CYBRAN)
        .initMode(LobbyMode.AUTO_LOBBY)
        .mapPosition(4)
        .team(1)
        .ratingType(LADDER_1v1_RATING_TYPE)
        .get();

    FeaturedMod featuredMod = FeaturedModBeanBuilder.create().defaultValues().get();

    String[] additionalArgs = {"/team", "1", "/players", "2", "/startspot", "4"};
    mockStartGameProcess(uid, LADDER_1v1_RATING_TYPE, CYBRAN, false, additionalArgs);
    when(fafService.startSearchMatchmaker()).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(featuredMod, null, Collections.emptyMap(), Collections.emptySet())).thenReturn(completedFuture(null));
    when(mapService.isInstalled(map)).thenReturn(false);
    when(mapService.download(map)).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(FAF.getTechnicalName())).thenReturn(completedFuture(featuredMod));

    instance.startSearchMatchmaker().toCompletableFuture();

    verify(fafService).startSearchMatchmaker();
    verify(mapService).download(map);
    verify(replayService).start(eq(uid), any());
    verify(forgedAllianceService).startGame(
        uid, CYBRAN, asList(additionalArgs), LADDER_1v1_RATING_TYPE, GPG_PORT, LOCAL_REPLAY_PORT, false, junitPlayer);
  }

  @Test
  public void testStartSearchMatchmakerGameRunningDoesNothing() throws Exception {
    Process process = mock(Process.class);
    when(process.isAlive()).thenReturn(true);

    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    GameLaunchMessage gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();

    when(forgedAllianceService.startGame(anyInt(), any(), any(), any(), anyInt(), eq(LOCAL_REPLAY_PORT), eq(false), eq(junitPlayer))).thenReturn(process);
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(fafService.requestHostGame(newGameInfo)).thenReturn(completedFuture(gameLaunchMessage));
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

    assertThat(instance.getInMatchmakerQueueProperty().get(), is(false));
  }

  @Test
  public void testStopSearchMatchmaker() {
    instance.getInMatchmakerQueueProperty().set(true);
    instance.onMatchmakerSearchStopped();
    assertThat(instance.getInMatchmakerQueueProperty().get(), is(false));
    verify(fafService).stopSearchMatchmaker();
  }

  @Test
  public void testStopSearchMatchmakerNotSearching() {
    instance.getInMatchmakerQueueProperty().set(false);
    instance.onMatchmakerSearchStopped();
    assertThat(instance.getInMatchmakerQueueProperty().get(), is(false));
    verify(fafService, never()).stopSearchMatchmaker();
  }

  @Test
  public void testSubscribeEventBus() {
    verify(eventBus).register(instance);

    assertThat(ReflectionUtils.findMethod(
        instance.getClass(), "onRehostRequest", RehostRequestEvent.class),
        hasAnnotation(Subscribe.class));
  }

  @Test
  public void testRehostIfGameIsNotRunning() throws Exception {
    Game game = GameBuilder.create().defaultValues().get();
    instance.currentGame.set(game);

    mockStartGameProcess(game.getId(), GLOBAL_RATING_TYPE, null, true);
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(fafService.requestHostGame(any())).thenReturn(completedFuture(GameLaunchMessageBuilder.create().defaultValues().get()));
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));
    when(mapService.download(game.getMapFolderName())).thenReturn(completedFuture(null));

    instance.onRehostRequest(new RehostRequestEvent());

    verify(forgedAllianceService).startGame(anyInt(), eq(null), anyList(), eq(GLOBAL_RATING_TYPE), anyInt(), eq(LOCAL_REPLAY_PORT), eq(true), eq(junitPlayer));
  }

  @Test
  public void testRehostIfGameIsRunning() throws Exception {
    instance.gameRunning.set(true);

    Game game = GameBuilder.create().defaultValues().get();
    instance.currentGame.set(game);

    instance.onRehostRequest(new RehostRequestEvent());

    verify(forgedAllianceService, never()).startGame(anyInt(), any(), any(), any(), anyInt(), eq(LOCAL_REPLAY_PORT), anyBoolean(), eq(junitPlayer));
  }

  @Test
  public void testCurrentGameEndedBehaviour() {
    Game game = new Game();
    game.setId(123);
    game.setStatus(PLAYING);

    instance.currentGame.set(game);

    verify(notificationService, never()).addNotification(any(PersistentNotification.class));

    game.setStatus(PLAYING);
    game.setStatus(CLOSED);

    WaitForAsyncUtils.waitForFxEvents();
    verify(notificationService).addNotification(any(PersistentNotification.class));
  }

  @Test
  public void testGameHostIfNoGameSet() {
    when(preferencesService.isGamePathValid()).thenReturn(false);
    instance.hostGame(null);
    verify(eventBus).post(any(GameDirectoryChooseEvent.class));
  }

  @Test
  public void testGameHost() throws IOException {
    when(preferencesService.isGamePathValid()).thenReturn(true);
    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    GameLaunchMessage gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();
    when(gameUpdater.update(newGameInfo.getFeaturedMod(), null, Map.of(), newGameInfo.getSimMods())).thenReturn(completedFuture(null));
    when(mapService.download(newGameInfo.getMap())).thenReturn(completedFuture(null));
    when(fafService.requestHostGame(newGameInfo)).thenReturn(completedFuture(gameLaunchMessage));
    instance.hostGame(newGameInfo);
    verify(forgedAllianceService).startGame(
        gameLaunchMessage.getUid(), null, List.of(), gameLaunchMessage.getRatingType(),
        GPG_PORT, LOCAL_REPLAY_PORT, false, junitPlayer);
  }

  @Test
  public void testGameHostNoGameLaunchRatingType() throws IOException {
    when(preferencesService.isGamePathValid()).thenReturn(true);
    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    GameLaunchMessage gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().ratingType(null).get();
    when(gameUpdater.update(newGameInfo.getFeaturedMod(), null, Map.of(), newGameInfo.getSimMods())).thenReturn(completedFuture(null));
    when(mapService.download(newGameInfo.getMap())).thenReturn(completedFuture(null));
    when(fafService.requestHostGame(newGameInfo)).thenReturn(completedFuture(gameLaunchMessage));
    instance.hostGame(newGameInfo);
    verify(forgedAllianceService).startGame(
        gameLaunchMessage.getUid(), null, List.of(), GameService.DEFAULT_RATING_TYPE,
        GPG_PORT, LOCAL_REPLAY_PORT, false, junitPlayer);
  }

  @Test
  public void runWithLiveReplayIfNoGameSet() {
    when(preferencesService.isGamePathValid()).thenReturn(false);
    instance.runWithLiveReplay(null, null, null, null);
    verify(eventBus).post(any(GameDirectoryChooseEvent.class));
  }

  @Test
  public void startSearchMatchmakerIfNoGameSet() {
    when(preferencesService.isGamePathValid()).thenReturn(false);
    instance.startSearchMatchmaker();
    verify(eventBus).post(any(GameDirectoryChooseEvent.class));
  }

  @Test
  public void startSearchMatchmaker() throws IOException {
    when(preferencesService.isGamePathValid()).thenReturn(true);
    when(modService.getFeaturedMod(FAF.getTechnicalName()))
        .thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));
    GameLaunchMessage gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(mapService.download(gameLaunchMessage.getMapname())).thenReturn(completedFuture(null));
    when(fafService.startSearchMatchmaker()).thenReturn(completedFuture(gameLaunchMessage));
    instance.startSearchMatchmaker();
    verify(forgedAllianceService).startGame(
        gameLaunchMessage.getUid(), null, List.of("/team", "null", "/players", "null", "/startspot", "null"),
        gameLaunchMessage.getRatingType(), GPG_PORT, LOCAL_REPLAY_PORT, false, junitPlayer);
  }

  @Test
  public void startSearchMatchmakerNoLaunchRatingType() throws IOException {
    when(preferencesService.isGamePathValid()).thenReturn(true);
    when(modService.getFeaturedMod(FAF.getTechnicalName()))
        .thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));
    GameLaunchMessage gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().ratingType(null).get();
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(mapService.download(gameLaunchMessage.getMapname())).thenReturn(completedFuture(null));
    when(fafService.startSearchMatchmaker()).thenReturn(completedFuture(gameLaunchMessage));
    instance.setMatchedQueueRatingType("ladder_1v1");
    instance.startSearchMatchmaker();
    verify(forgedAllianceService).startGame(
        gameLaunchMessage.getUid(), null, List.of("/team", "null", "/players", "null", "/startspot", "null"),
        "ladder_1v1", GPG_PORT, LOCAL_REPLAY_PORT, false, junitPlayer);
  }

  @Test
  public void startSearchMatchmakerNoLaunchRatingTypeNoQueueRatingType() throws IOException {
    when(preferencesService.isGamePathValid()).thenReturn(true);
    when(modService.getFeaturedMod(FAF.getTechnicalName()))
        .thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));
    GameLaunchMessage gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().ratingType(null).get();
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(mapService.download(gameLaunchMessage.getMapname())).thenReturn(completedFuture(null));
    when(fafService.startSearchMatchmaker()).thenReturn(completedFuture(gameLaunchMessage));
    instance.setMatchedQueueRatingType(null);
    instance.startSearchMatchmaker();
    verify(forgedAllianceService).startGame(
        gameLaunchMessage.getUid(), null, List.of("/team", "null", "/players", "null", "/startspot", "null"),
        GameService.DEFAULT_RATING_TYPE, GPG_PORT, LOCAL_REPLAY_PORT, false, junitPlayer);
  }

  @Test
  public void joinGameIfNoGameSet() {
    when(preferencesService.isGamePathValid()).thenReturn(false);
    instance.joinGame(null, null);
    verify(eventBus).post(any(GameDirectoryChooseEvent.class));
  }

  @Test
  public void runWithReplayIfNoGameSet() {
    when(preferencesService.isGamePathValid()).thenReturn(false);
    instance.runWithReplay(null, null, null, null, null, null, null);
    verify(eventBus).post(any(GameDirectoryChooseEvent.class));
  }

  @Test
  public void runWithReplayInMatchmakerQueue() {
    instance.getInMatchmakerQueueProperty().setValue(true);
    instance.runWithReplay(null, null, null, null, null, null, null);
    WaitForAsyncUtils.waitForFxEvents();
    verify(notificationService).addImmediateWarnNotification("replay.inQueue");
  }

  @Test
  public void runWithLiveReplayInMatchmakerQueue() {
    instance.getInMatchmakerQueueProperty().setValue(true);
    instance.runWithLiveReplay(null, null, null, null);
    WaitForAsyncUtils.waitForFxEvents();
    verify(notificationService).addImmediateWarnNotification("replay.inQueue");
  }

  @Test
  public void runWithReplayInParty() {
    instance.getInOthersPartyProperty().setValue(true);
    instance.runWithReplay(null, null, null, null, null, null, null);
    WaitForAsyncUtils.waitForFxEvents();
    verify(notificationService).addImmediateWarnNotification("replay.inParty");
  }

  @Test
  public void runWithLiveReplayInParty() {
    instance.getInOthersPartyProperty().setValue(true);
    instance.runWithLiveReplay(null, null, null, null);
    WaitForAsyncUtils.waitForFxEvents();
    verify(notificationService).addImmediateWarnNotification("replay.inParty");
  }

  @Test
  public void launchTutorialIfNoGameSet() {
    when(preferencesService.isGamePathValid()).thenReturn(false);
    instance.launchTutorial(null, null);
    verify(eventBus).post(any(GameDirectoryChooseEvent.class));
  }

  @Test
  public void iceCloseOnError() throws Exception {
    Game game = GameBuilder.create().defaultValues().get();

    game.setMapFolderName("map");

    GameLaunchMessage gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();

    mockGlobalStartGameProcess(gameLaunchMessage.getUid());
    when(mapService.isInstalled("map")).thenReturn(true);
    when(fafService.requestJoinGame(game.getId(), null)).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));
    when(replayService.start(anyInt(), any(Supplier.class))).thenReturn(completedFuture(new Throwable()));

    CompletableFuture<Void> future = instance.joinGame(game, null).toCompletableFuture();

    assertNull(future.get(TIMEOUT, TIME_UNIT));
    verify(mapService, never()).download(any());
    verify(replayService).start(eq(game.getId()), any());
    verify(iceAdapter).stop();
  }
}
