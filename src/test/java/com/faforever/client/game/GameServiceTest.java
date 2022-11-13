package com.faforever.client.game;

import com.faforever.client.builders.FeaturedModBeanBuilder;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.GameInfoMessageBuilder;
import com.faforever.client.builders.GameLaunchMessageBuilder;
import com.faforever.client.builders.LeagueEntryBeanBuilder;
import com.faforever.client.builders.NewGameInfoBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.discord.DiscordRichPresenceService;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fa.ForgedAllianceService;
import com.faforever.client.fa.GameParameters;
import com.faforever.client.fa.relay.event.RehostRequestEvent;
import com.faforever.client.fa.relay.ice.CoturnService;
import com.faforever.client.fa.relay.ice.IceAdapter;
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
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.replay.ReplayServer;
import com.faforever.client.teammatchmaking.event.PartyOwnerChangedEvent;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.faforever.commons.lobby.GameInfo;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameType;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
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
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
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
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.faforever.client.game.KnownFeaturedMod.FAF;
import static com.faforever.commons.lobby.GameStatus.CLOSED;
import static com.faforever.commons.lobby.GameStatus.OPEN;
import static com.faforever.commons.lobby.GameStatus.PLAYING;
import static com.natpryce.hamcrest.reflection.HasAnnotationMatcher.hasAnnotation;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameServiceTest extends ServiceTest {

  private static final long TIMEOUT = 5000;
  private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
  private static final Integer GPG_PORT = 1234;
  private static final int LOCAL_REPLAY_PORT = 15111;
  private static final String GLOBAL_RATING_TYPE = "global";
  private static final String LADDER_1v1_RATING_TYPE = "ladder_1v1";

  @InjectMocks
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
  private EventBus eventBus;
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

  @Captor
  private ArgumentCaptor<Consumer<GameInfo>> GameInfoListenerCaptor;
  @Captor
  private ArgumentCaptor<Set<String>> simModsCaptor;

  private PlayerBean junitPlayer;
  private Preferences preferences;
  @Spy
  private GameMapper gameMapper = Mappers.getMapper(GameMapper.class);
  @Spy
  private ClientProperties clientProperties = new ClientProperties();

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(gameMapper);
    junitPlayer = PlayerBeanBuilder.create().defaultValues().get();
    preferences = PreferencesBuilder.create().defaultValues().get();

    when(coturnService.getSelectedCoturns()).thenReturn(completedFuture(List.of()));
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferencesService.isGamePathValid()).thenReturn(true);
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

    verify(fafServerAccessor).addEventListener(eq(GameInfo.class), GameInfoListenerCaptor.capture());
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

  private void mockMatchmakerChain() {
    when(modService.getFeaturedMod(FAF.getTechnicalName()))
        .thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(fafServerAccessor.startSearchMatchmaker()).thenReturn(new CompletableFuture<>());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void postConstruct() {
    verify(fafServerAccessor).addEventListener(eq(GameInfo.class), any(Consumer.class));
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
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));

    CompletableFuture<Void> future = instance.joinGame(game, null).toCompletableFuture();

    assertNull(future.get(TIMEOUT, TIME_UNIT));
    verify(mapService, never()).download(any());
    verify(replayServer).start(eq(game.getId()), any());

    verify(forgedAllianceService).startGameOnline(gameParameters);
  }

  @Test
  public void testStartReplayWhileInGameAllowed() throws Exception {
    preferences.getForgedAlliance().setAllowReplaysWhileInGame(true);
    GameBean game = GameBeanBuilder.create().defaultValues().get();

    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();
    Path replayPath = Path.of("temp.scfareplay");
    int replayId = 1234;
    GameParameters gameParameters = gameMapper.map(gameLaunchMessage);

    mockStartGameProcess(gameParameters);
    when(mapService.isInstalled(anyString())).thenReturn(true);
    when(fafServerAccessor.requestJoinGame(anyInt(), isNull())).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(anyString())).thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));
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
  public void testStartReplayWhileInGameNotAllowed() throws Exception {
    preferences.getForgedAlliance().setAllowReplaysWhileInGame(false);
    GameBean game = GameBeanBuilder.create().defaultValues().get();

    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();
    Path replayPath = Path.of("temp.scfareplay");
    int replayId = 1234;
    GameParameters gameParameters = gameMapper.map(gameLaunchMessage);

    mockStartGameProcess(gameParameters);
    when(mapService.isInstalled(anyString())).thenReturn(true);
    when(fafServerAccessor.requestJoinGame(anyInt(), isNull())).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(anyString())).thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));
    when(process.isAlive()).thenReturn(false);

    CompletableFuture<Void> future = instance.joinGame(game, null).toCompletableFuture();
    future.join();
    mockStartReplayProcess(replayPath, replayId);
    when(process.isAlive()).thenReturn(true);
    future = instance.runWithReplay(replayPath, replayId, "", null, null, null, "map");
    future.join();

    verify(replayServer).start(eq(game.getId()), any());
    verify(forgedAllianceService).startGameOnline(gameParameters);
    verify(forgedAllianceService, never()).startReplay(replayPath, replayId);
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
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));

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
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
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

    instance.spawnTerminationListener(process);

    disconnectedFuture.get(5000, TimeUnit.MILLISECONDS);

    verify(process).onExit();
    verify(process).exitValue();
  }

  @Test
  public void testOnGames() {
    assertThat(instance.getGames(), empty());

    GameInfo multiGameInfo = GameInfoMessageBuilder.create(1)
        .games(
            List.of(GameInfoMessageBuilder.create(1).defaultValues().get(),
                GameInfoMessageBuilder.create(2).defaultValues().get())
        ).get();


    GameInfoListenerCaptor.getValue().accept(multiGameInfo);


    assertThat(instance.getGames(), hasSize(2));
  }

  @Test
  public void testOnGameInfoAdd() {
    assertThat(instance.getGames(), empty());

    GameInfo GameInfo1 = GameInfoMessageBuilder.create(1).defaultValues().title("Game 1").get();
    GameInfoListenerCaptor.getValue().accept(GameInfo1);

    GameInfo GameInfo2 = GameInfoMessageBuilder.create(2).defaultValues().title("Game 2").get();
    GameInfoListenerCaptor.getValue().accept(GameInfo2);


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
    preferences.getLastGame().setLastGamePassword("banana");
    instance.currentGame.set(null);

    when(playerService.isCurrentPlayerInGame(any())).thenReturn(true);

    GameInfo GameInfo1 = GameInfoMessageBuilder.create(1)
        .defaultValues()
        .host("me")
        .addTeamMember("1", "me")
        .passwordProtected(true)
        .title("Game 1")
        .get();
    GameInfoListenerCaptor.getValue().accept(GameInfo1);


    assertThat(instance.currentGame.get().getPassword(), is("banana"));

  }

  @Test
  public void testOnGameInfoSetsCurrentGameIfUserIsInAndStatusOpen() {
    assertThat(instance.getCurrentGame(), nullValue());

    when(playerService.isCurrentPlayerInGame(any())).thenReturn(true);

    GameInfo GameInfo = GameInfoMessageBuilder.create(1234).defaultValues()
        .state(OPEN)
        .addTeamMember("1", "PlayerName").get();
    GameInfoListenerCaptor.getValue().accept(GameInfo);


    assertThat(instance.getCurrentGame(), notNullValue());
    assertThat(instance.getCurrentGame().getId(), is(1234));
  }

  @Test
  public void testOnGameInfoDoesntSetCurrentGameIfUserIsInAndStatusNotOpen() {
    assertThat(instance.getCurrentGame(), nullValue());

    when(playerService.isCurrentPlayerInGame(any())).thenReturn(true);

    GameInfo GameInfo = GameInfoMessageBuilder.create(1234).defaultValues()
        .state(PLAYING)
        .addTeamMember("1", "PlayerName").get();
    GameInfoListenerCaptor.getValue().accept(GameInfo);

    assertThat(instance.getCurrentGame(), nullValue());
  }

  @Test
  public void testOnGameInfoDoesntSetCurrentGameIfUserDoesntMatch() {
    assertThat(instance.getCurrentGame(), nullValue());

    when(playerService.isCurrentPlayerInGame(any())).thenReturn(false);

    GameInfo GameInfo = GameInfoMessageBuilder.create(1234).defaultValues().addTeamMember("1", "Other").get();
    GameInfoListenerCaptor.getValue().accept(GameInfo);

    assertThat(instance.getCurrentGame(), nullValue());
  }

  @Test
  public void testOnGameInfoModify() throws InterruptedException {
    assertThat(instance.getGames(), empty());

    GameInfo GameInfo = GameInfoMessageBuilder.create(1).defaultValues().title("Game 1").state(PLAYING).get();
    GameInfoListenerCaptor.getValue().accept(GameInfo);


    CountDownLatch changeLatch = new CountDownLatch(1);
    GameBean game = instance.getGames().iterator().next();
    game.titleProperty().addListener((observable, oldValue, newValue) -> {
      changeLatch.countDown();
    });

    GameInfo = GameInfoMessageBuilder.create(1).defaultValues().title("Game 1 modified").state(PLAYING).get();
    GameInfoListenerCaptor.getValue().accept(GameInfo);

    changeLatch.await();
    assertEquals(GameInfo.getTitle(), game.getTitle());
  }

  @Test
  public void testOnGameInfoRemove() {
    assertThat(instance.getGames(), empty());

    GameInfo GameInfo = GameInfoMessageBuilder.create(1).defaultValues().title("Game 1").get();
    GameInfoListenerCaptor.getValue().accept(GameInfo);

    GameInfo = GameInfoMessageBuilder.create(1).title("Game 1").defaultValues().state(CLOSED).get();
    GameInfoListenerCaptor.getValue().accept(GameInfo);

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
    when(gameUpdater.update(featuredMod, Set.of(),null, null)).thenReturn(completedFuture(null));
    when(mapService.isInstalled(map)).thenReturn(false);
    when(mapService.download(map)).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(FAF.getTechnicalName())).thenReturn(completedFuture(featuredMod));
    when(process.onExit()).thenReturn(CompletableFuture.completedFuture(process));

    instance.startSearchMatchmaker().join();

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
    when(gameUpdater.update(featuredMod, Set.of(),null, null)).thenReturn(completedFuture(null));
    when(mapService.isInstalled(map)).thenReturn(false);
    when(mapService.download(map)).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(FAF.getTechnicalName())).thenReturn(completedFuture(featuredMod));
    when(process.onExit()).thenReturn(CompletableFuture.completedFuture(process));

    instance.startSearchMatchmaker().join();

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
    when(gameUpdater.update(featuredMod, Set.of(), null, null)).thenReturn(completedFuture(null));
    when(mapService.isInstalled(map)).thenReturn(false);
    when(mapService.download(map)).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(FAF.getTechnicalName())).thenReturn(completedFuture(featuredMod));
    when(process.onExit()).thenReturn(CompletableFuture.completedFuture(process).newIncompleteFuture());

    CompletableFuture<Void> matchmakerFuture = instance.startSearchMatchmaker();

    assertSame(matchmakerFuture, instance.startSearchMatchmaker());
  }

  @Test
  public void testStartSearchMatchmakerGameRunningDoesNothing() throws Exception {
    Process process = mock(Process.class);
    when(process.isAlive()).thenReturn(true);

    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();

    when(forgedAllianceService.startGameOnline(any())).thenReturn(process);
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
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

    instance.startSearchMatchmaker().join();

    verify(notificationService).addImmediateWarnNotification("game.gameRunning");
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
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    instance.currentGame.set(game);
    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().uid(game.getId()).get();
    GameParameters gameParameters = gameMapper.map(gameLaunchMessage);
    gameParameters.setRehost(true);

    mockStartGameProcess(gameParameters);
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(fafServerAccessor.requestHostGame(any())).thenReturn(completedFuture(GameLaunchMessageBuilder.create().defaultValues().get()));
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));
    when(mapService.download(game.getMapFolderName())).thenReturn(completedFuture(null));

    instance.onRehostRequest(new RehostRequestEvent());

    verify(forgedAllianceService).startGameOnline(eq(gameParameters));
  }

  @Test
  public void testRehostIfGameIsRunning() throws Exception {
    instance.gameRunning.set(true);

    GameBean game = GameBeanBuilder.create().defaultValues().get();
    instance.currentGame.set(game);

    instance.onRehostRequest(new RehostRequestEvent());

    verify(forgedAllianceService, never()).startGameOnline(any());
  }

  @Test
  public void testCurrentGameEndedBehaviour() {
    when(playerService.isCurrentPlayerInGame(any())).thenReturn(true);
    GameBean game = GameBeanBuilder.create().defaultValues().id(123).status(PLAYING).teams(Map.of(1, List.of(junitPlayer))).get();
    junitPlayer.setGame(game);

    instance.currentGame.set(game);

    verify(notificationService, never()).addNotification(any(PersistentNotification.class));


    game.setStatus(PLAYING);
    game.setStatus(CLOSED);


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
    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();
    when(gameUpdater.update(newGameInfo.getFeaturedMod(), newGameInfo.getSimMods(), null, null)).thenReturn(completedFuture(null));
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
    when(preferencesService.isGamePathValid()).thenReturn(true);
    when(forgedAllianceService.startGameOffline(any())).thenReturn(process);
    when(process.onExit()).thenReturn(CompletableFuture.completedFuture(process));
    instance.startGameOffline();
    verify(forgedAllianceService).startGameOffline(any());
  }

  @Test
  public void testOfflineGameInvalidPath() throws IOException {
    when(preferencesService.isGamePathValid()).thenReturn(false);
    instance.startGameOffline();
    verify(eventBus).post(any(GameDirectoryChooseEvent.class));
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
  public void startSearchMatchmakerWithGameOptions() throws IOException {
    when(preferencesService.isGamePathValid()).thenReturn(true);
    when(modService.getFeaturedMod(FAF.getTechnicalName()))
        .thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));
    when(process.onExit()).thenReturn(completedFuture(process));
    when(process.exitValue()).thenReturn(0);
    Map<String, String> gameOptions = new LinkedHashMap<>();
    gameOptions.put("Share", "ShareUntilDeath");
    gameOptions.put("UnitCap", "500");
    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().team(1).expectedPlayers(4).mapPosition(3).gameOptions(gameOptions).get();
    GameParameters gameParameters = gameMapper.map(gameLaunchMessage);
    gameParameters.setDivision("unlisted");
    mockStartGameProcess(gameParameters);
    when(leaderboardService.getActiveLeagueEntryForPlayer(junitPlayer, "global")).thenReturn(completedFuture(Optional.empty()));
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(mapService.download(gameLaunchMessage.getMapName())).thenReturn(completedFuture(null));
    when(fafServerAccessor.startSearchMatchmaker()).thenReturn(completedFuture(gameLaunchMessage));
    instance.startSearchMatchmaker().join();
    verify(forgedAllianceService).startGameOnline(gameParameters);
  }

  @Test
  public void startSearchMatchmakerThenCancelledWithGame() throws IOException {
    when(preferencesService.isGamePathValid()).thenReturn(true);
    when(modService.getFeaturedMod(FAF.getTechnicalName()))
        .thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));
    when(process.isAlive()).thenReturn(false);
    when(process.onExit()).thenReturn(new CompletableFuture<>());
    when(process.exitValue()).thenReturn(1);
    Map<String, String> gameOptions = new LinkedHashMap<>();
    gameOptions.put("Share", "ShareUntilDeath");
    gameOptions.put("UnitCap", "500");
    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().team(1).expectedPlayers(4).mapPosition(3).gameOptions(gameOptions).get();
    GameParameters gameParameters = gameMapper.map(gameLaunchMessage);
    gameParameters.setDivision("unlisted");
    mockStartGameProcess(gameParameters);
    when(leaderboardService.getActiveLeagueEntryForPlayer(junitPlayer, "global")).thenReturn(completedFuture(Optional.empty()));
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(mapService.download(gameLaunchMessage.getMapName())).thenReturn(completedFuture(null));
    when(fafServerAccessor.startSearchMatchmaker()).thenReturn(CompletableFuture.completedFuture(gameLaunchMessage));
    CompletableFuture<Void> future = instance.startSearchMatchmaker();
    when(process.isAlive()).thenReturn(true);
    future.cancel(false);
    verify(notificationService).addServerNotification(any());
  }

  @Test
  public void startSearchMatchmakerThenCancelledNoGame() throws IOException {
    when(preferencesService.isGamePathValid()).thenReturn(true);
    when(modService.getFeaturedMod(FAF.getTechnicalName()))
        .thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));
    when(process.isAlive()).thenReturn(false);
    when(process.onExit()).thenReturn(new CompletableFuture<>());
    when(process.exitValue()).thenReturn(1);
    Map<String, String> gameOptions = new LinkedHashMap<>();
    gameOptions.put("Share", "ShareUntilDeath");
    gameOptions.put("UnitCap", "500");
    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().team(1).expectedPlayers(4).mapPosition(3).gameOptions(gameOptions).get();
    mockStartGameProcess(gameMapper.map(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(mapService.download(gameLaunchMessage.getMapName())).thenReturn(completedFuture(null));
    when(fafServerAccessor.startSearchMatchmaker()).thenReturn(CompletableFuture.completedFuture(gameLaunchMessage));
    instance.startSearchMatchmaker().cancel(false);
    verify(notificationService, never()).addServerNotification(any());
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
    mockMatchmakerChain();
    instance.startSearchMatchmaker();
    instance.runWithReplay(null, null, null, null, null, null, null);

    verify(notificationService).addImmediateWarnNotification("replay.inQueue");
  }

  @Test
  public void runWithLiveReplayInMatchmakerQueue() {
    mockMatchmakerChain();
    instance.startSearchMatchmaker();
    instance.runWithLiveReplay(null, null, null, null);

    verify(notificationService).addImmediateWarnNotification("replay.inQueue");
  }

  @Test
  public void runWithReplayInParty() {
    instance.onPartyOwnerChangedEvent(new PartyOwnerChangedEvent(PlayerBeanBuilder.create().defaultValues().id(100).get()));
    instance.runWithReplay(null, null, null, null, null, null, null);

    verify(notificationService).addImmediateWarnNotification("replay.inParty");
  }

  @Test
  public void runWithLiveReplayInParty() {
    instance.onPartyOwnerChangedEvent(new PartyOwnerChangedEvent(PlayerBeanBuilder.create().defaultValues().id(100).get()));
    instance.runWithLiveReplay(null, null, null, null);

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
    GameBean game = GameBeanBuilder.create().defaultValues().get();

    game.setMapFolderName("map");

    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();

    mockStartGameProcess(gameMapper.map(gameLaunchMessage));
    when(mapService.isInstalled("map")).thenReturn(true);
    when(fafServerAccessor.requestJoinGame(game.getId(), null)).thenReturn(completedFuture(gameLaunchMessage));
    when(gameUpdater.update(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));
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
}
