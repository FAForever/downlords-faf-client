package com.faforever.client.game;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fa.ForgedAllianceService;
import com.faforever.client.fa.RatingMode;
import com.faforever.client.fa.relay.event.RehostRequestEvent;
import com.faforever.client.fa.relay.ice.IceAdapter;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.patch.GameUpdater;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.GameInfoMessage;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.event.Event;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.ReflectionUtils;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.faforever.client.fa.RatingMode.GLOBAL;
import static com.faforever.client.game.Faction.AEON;
import static com.faforever.client.game.Faction.CYBRAN;
import static com.faforever.client.game.KnownFeaturedMod.LADDER_1V1;
import static com.faforever.client.remote.domain.GameStatus.CLOSED;
import static com.faforever.client.remote.domain.GameStatus.OPEN;
import static com.faforever.client.remote.domain.GameStatus.PLAYING;
import static com.natpryce.hamcrest.reflection.HasAnnotationMatcher.hasAnnotation;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GameServiceTest extends AbstractPlainJavaFxTest {

  private static final long TIMEOUT = 5000;
  private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
  private static final int GAME_PORT = 1234;
  private static final Integer GPG_PORT = 1234;
  private static final int LOCAL_REPLAY_PORT = 15111;

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
  private Executor executor;
  @Mock
  private ReplayService replayService;
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
  @Captor
  private ArgumentCaptor<Consumer<GameInfoMessage>> gameInfoMessageListenerCaptor;
  @Captor
  private ArgumentCaptor<Set<String>> simModsCaptor;

  private Player junitPlayer;

  @Before
  public void setUp() throws Exception {
    junitPlayer = PlayerBuilder.create("JUnit").defaultValues().get();

    ClientProperties clientProperties = new ClientProperties();

    instance = new GameService(clientProperties, fafService, forgedAllianceService, mapService,
        preferencesService, gameUpdater, notificationService, i18n, executor, playerService,
        reportingService, eventBus, iceAdapter, modService, platformService);
    instance.replayService = replayService;

    Preferences preferences = new Preferences();
    preferences.getForgedAlliance().setPort(GAME_PORT);

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(fafService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>());
    when(replayService.startReplayServer(anyInt())).thenReturn(completedFuture(LOCAL_REPLAY_PORT));
    when(iceAdapter.start()).thenReturn(CompletableFuture.completedFuture(GPG_PORT));
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(junitPlayer));

    doAnswer(invocation -> {
      try {
        ((Runnable) invocation.getArgument(0)).run();
      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }).when(executor).execute(any());

    instance.postConstruct();

    verify(fafService).addOnMessageListener(eq(GameInfoMessage.class), gameInfoMessageListenerCaptor.capture());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void postConstruct() {
    verify(fafService).addOnMessageListener(eq(GameInfoMessage.class), any(Consumer.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testJoinGameMapIsAvailable() throws Exception {
    Game game = GameBuilder.create().defaultValues().get();

    ObservableMap<String, String> simMods = FXCollections.observableHashMap();
    simMods.put("123-456-789", "Fake mod name");

    game.setSimMods(simMods);
    game.setMapFolderName("map");

    GameLaunchMessage gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();

    when(mapService.isInstalled("map")).thenReturn(true);
    when(fafService.requestJoinGame(game.getId(), null)).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(CompletableFuture.completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));

    CompletableFuture<Void> future = instance.joinGame(game, null).toCompletableFuture();

    assertThat(future.get(TIMEOUT, TIME_UNIT), is(nullValue()));
    verify(mapService, never()).download(any());
    verify(replayService).startReplayServer(game.getId());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testModEnabling() throws Exception {
    Game game = GameBuilder.create().defaultValues().get();

    ObservableMap<String, String> simMods = FXCollections.observableHashMap();
    simMods.put("123-456-789", "Fake mod name");

    game.setSimMods(simMods);
    game.setMapFolderName("map");

    GameLaunchMessage gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();

    when(mapService.isInstalled("map")).thenReturn(true);
    when(fafService.requestJoinGame(game.getId(), null)).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(CompletableFuture.completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));

    instance.joinGame(game, null).toCompletableFuture().get();
    verify(modService).enableSimMods(simModsCaptor.capture());
    assertEquals(simModsCaptor.getValue().iterator().next(), "123-456-789");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAddOnGameStartedListener() throws Exception {
    Process process = mock(Process.class);

    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    GameLaunchMessage gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();
    gameLaunchMessage.setArgs(asList("/foo bar", "/bar foo"));

    when(forgedAllianceService.startGame(
        gameLaunchMessage.getUid(), null, asList("/foo", "bar", "/bar", "foo"), GLOBAL, GPG_PORT, LOCAL_REPLAY_PORT, false, junitPlayer)
    ).thenReturn(process);
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(fafService.requestHostGame(newGameInfo)).thenReturn(completedFuture(gameLaunchMessage));
    when(mapService.download(newGameInfo.getMap())).thenReturn(CompletableFuture.completedFuture(null));

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
        gameLaunchMessage.getUid(), null, asList("/foo", "bar", "/bar", "foo"), GLOBAL,
        GPG_PORT, LOCAL_REPLAY_PORT, false, junitPlayer);
    verify(replayService).startReplayServer(gameLaunchMessage.getUid());
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

    Game game1 = new Game(gameInfoMessage1);
    Game game2 = new Game(gameInfoMessage2);

    assertThat(instance.getGames(), containsInAnyOrder(game1, game2));
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
    GameLaunchMessage gameLaunchMessage = new GameLaunchMessage();
    gameLaunchMessage.setMod("ladder1v1");
    gameLaunchMessage.setUid(123);
    gameLaunchMessage.setArgs(emptyList());
    gameLaunchMessage.setMapname("scmp_037");

    FeaturedMod featuredMod = FeaturedModBeanBuilder.create().defaultValues().get();

    when(fafService.startSearchLadder1v1(CYBRAN, GAME_PORT)).thenReturn(CompletableFuture.completedFuture(gameLaunchMessage));
    when(gameUpdater.update(featuredMod, null, Collections.emptyMap(), Collections.emptySet())).thenReturn(CompletableFuture.completedFuture(null));
    when(mapService.isInstalled("scmp_037")).thenReturn(false);
    when(mapService.download("scmp_037")).thenReturn(CompletableFuture.completedFuture(null));
    when(modService.getFeaturedMod(LADDER_1V1.getTechnicalName())).thenReturn(CompletableFuture.completedFuture(featuredMod));

    CompletableFuture<Void> future = instance.startSearchLadder1v1(CYBRAN).toCompletableFuture();

    verify(fafService).startSearchLadder1v1(CYBRAN, GAME_PORT);
    verify(mapService).download("scmp_037");
    verify(replayService).startReplayServer(123);
    verify(forgedAllianceService, timeout(100))
        .startGame(eq(123), eq(CYBRAN), eq(asList("/team", "1", "/players", "2")), eq(RatingMode.LADDER_1V1), anyInt(), eq(LOCAL_REPLAY_PORT), eq(false), eq(junitPlayer));
    assertThat(future.get(TIMEOUT, TIME_UNIT), is(nullValue()));
  }

  @Test
  public void testStartSearchLadder1v1GameRunningDoesNothing() throws Exception {
    Process process = mock(Process.class);
    when(process.isAlive()).thenReturn(true);

    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    GameLaunchMessage gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();

    when(forgedAllianceService.startGame(anyInt(), any(), any(), any(), anyInt(), eq(LOCAL_REPLAY_PORT), eq(false), eq(junitPlayer))).thenReturn(process);
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(fafService.requestHostGame(newGameInfo)).thenReturn(completedFuture(gameLaunchMessage));
    when(mapService.download(newGameInfo.getMap())).thenReturn(CompletableFuture.completedFuture(null));

    CountDownLatch gameRunningLatch = new CountDownLatch(1);
    instance.gameRunningProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        gameRunningLatch.countDown();
      }
    });

    instance.hostGame(newGameInfo);
    gameRunningLatch.await(TIMEOUT, TIME_UNIT);

    instance.startSearchLadder1v1(AEON);

    assertThat(instance.searching1v1Property().get(), is(false));
  }

  @Test
  public void testStopSearchLadder1v1() {
    instance.searching1v1Property().set(true);
    instance.stopSearchLadder1v1();
    assertThat(instance.searching1v1Property().get(), is(false));
    verify(fafService).stopSearchingRanked();
  }

  @Test
  public void testStopSearchLadder1v1NotSearching() {
    instance.searching1v1Property().set(false);
    instance.stopSearchLadder1v1();
    assertThat(instance.searching1v1Property().get(), is(false));
    verify(fafService, never()).stopSearchingRanked();
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

    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(CompletableFuture.completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(fafService.requestHostGame(any())).thenReturn(completedFuture(GameLaunchMessageBuilder.create().defaultValues().get()));
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(CompletableFuture.completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));
    when(mapService.download(game.getMapFolderName())).thenReturn(CompletableFuture.completedFuture(null));

    instance.onRehostRequest(new RehostRequestEvent());

    verify(forgedAllianceService).startGame(anyInt(), eq(null), anyList(), eq(GLOBAL), anyInt(), eq(LOCAL_REPLAY_PORT), eq(true), eq(junitPlayer));
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

    game.setStatus(CLOSED);

    WaitForAsyncUtils.waitForFxEvents();
    verify(notificationService).addNotification(any(PersistentNotification.class));
  }

  @Test
  public void testCurrentGameEndedAndReplayNotPresent() {
    Game game = new Game();
    game.setId(123);
    game.setStatus(PLAYING);

    when(replayService.findById(123)).thenReturn(completedFuture(Optional.empty()));

    instance.currentGame.set(game);

    verify(notificationService, never()).addNotification(any(PersistentNotification.class));

    game.setStatus(CLOSED);

    WaitForAsyncUtils.waitForFxEvents();

    ArgumentCaptor<PersistentNotification> persistentNotificationArgumentCaptor = ArgumentCaptor.forClass(PersistentNotification.class);
    verify(notificationService).addNotification(persistentNotificationArgumentCaptor.capture());

    PersistentNotification value = persistentNotificationArgumentCaptor.getValue();
    value.getActions().get(0).call(new Event(Event.ANY));

    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }
}
