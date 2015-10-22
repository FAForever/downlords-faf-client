package com.faforever.client.game;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.fa.ForgedAllianceService;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.OnGameInfoListener;
import com.faforever.client.legacy.OnGameTypeInfoListener;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.GameTypeInfo;
import com.faforever.client.legacy.domain.VictoryCondition;
import com.faforever.client.legacy.proxy.Proxy;
import com.faforever.client.map.MapService;
import com.faforever.client.patch.GameUpdateService;
import com.faforever.client.play.PlayServices;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.relay.LocalRelayServer;
import com.faforever.client.stats.domain.ArmyBuilder;
import com.faforever.client.stats.domain.EconomyStatBuilder;
import com.faforever.client.stats.domain.GameStats;
import com.faforever.client.stats.domain.GameStatsBuilder;
import com.faforever.client.stats.domain.SummaryStatBuilder;
import com.faforever.client.stats.domain.Unit;
import com.faforever.client.stats.domain.UnitStatBuilder;
import com.faforever.client.stats.domain.UnitType;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.faforever.client.fa.RatingMode.GLOBAL;
import static com.faforever.client.stats.domain.UnitCategory.AIR;
import static com.faforever.client.stats.domain.UnitCategory.ENGINEER;
import static com.faforever.client.stats.domain.UnitCategory.LAND;
import static com.faforever.client.stats.domain.UnitCategory.NAVAL;
import static com.faforever.client.stats.domain.UnitCategory.TECH1;
import static com.faforever.client.stats.domain.UnitCategory.TECH2;
import static com.faforever.client.stats.domain.UnitCategory.TECH3;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsEmptyCollection.emptyCollectionOf;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class GameServiceImplTest extends AbstractPlainJavaFxTest {

  private static final long TIMEOUT = 5000;
  private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
  private static final int GAME_PORT = 1234;
  private static final int SEARCH_EXPANSION_DELAY = 3000;
  private static final float SEARCH_MAX_RADIUS = .10f;
  private static final float SEARCH_RADIUS_INCREMENT = .01f;

  private GameServiceImpl instance;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private LobbyServerAccessor lobbyServerAccessor;
  @Mock
  private MapService mapService;
  @Mock
  private ForgedAllianceService forgedAllianceService;
  @Mock
  private Proxy proxy;
  @Mock
  private GameUpdateService gameUpdateService;
  @Mock
  private Preferences preferences;
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private SearchExpansionTask searchExpansionTask;
  @Mock
  private Environment environment;
  @Mock
  private LocalRelayServer localRelayServer;
  @Mock
  private PlayServices playServices;
  @Mock
  private PlayerService playerService;
  @Mock
  private ScheduledExecutorService scheduledExecutorService;
  @Captor
  private ArgumentCaptor<Consumer<GameStats>> gameStatsListenerCaptor;
  @Captor
  private ArgumentCaptor<Consumer<Void>> gameLaunchedListenerCaptor;
  @Captor
  private ArgumentCaptor<ListChangeListener.Change<? extends GameInfoBean>> gameInfoBeanChangeListenerCaptor;

  @Before
  public void setUp() throws Exception {
    instance = new GameServiceImpl();
    instance.lobbyServerAccessor = lobbyServerAccessor;
    instance.mapService = mapService;
    instance.forgedAllianceService = forgedAllianceService;
    instance.proxy = proxy;
    instance.gameUpdateService = gameUpdateService;
    instance.preferencesService = preferencesService;
    instance.applicationContext = applicationContext;
    instance.environment = environment;
    instance.localRelayServer = localRelayServer;
    instance.playServices = playServices;
    instance.playerService = playerService;
    instance.scheduledExecutorService = scheduledExecutorService;

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(forgedAlliancePrefs.getPort()).thenReturn(GAME_PORT);
    when(environment.getProperty("ranked1v1.search.expansionDelay", int.class)).thenReturn(SEARCH_EXPANSION_DELAY);
    when(environment.getProperty("ranked1v1.search.maxRadius", float.class)).thenReturn(SEARCH_MAX_RADIUS);
    when(environment.getProperty("ranked1v1.search.radiusIncrement", float.class)).thenReturn(SEARCH_RADIUS_INCREMENT);
    when(environment.getProperty("playServices.minGameTime", int.class)).thenReturn(0);

    doAnswer(invocation -> {
      try {
        invocation.getArgumentAt(0, Runnable.class).run();
      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }).when(scheduledExecutorService).execute(any());

    instance.postConstruct();

    verify(localRelayServer).setGameStatsListener(gameStatsListenerCaptor.capture());
    verify(localRelayServer).setGameLaunchedListener(gameLaunchedListenerCaptor.capture());
  }

  @Test
  public void postConstruct() {
    verify(lobbyServerAccessor).addOnGameTypeInfoListener(any(OnGameTypeInfoListener.class));
    verify(lobbyServerAccessor).addOnGameInfoListener(any(OnGameInfoListener.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testJoinGameMapIsAvailable() throws Exception {
    GameInfoBean gameInfoBean = mock(GameInfoBean.class);

    ObservableMap<String, String> simMods = FXCollections.observableHashMap();
    simMods.put("123-456-789", "Fake mod name");

    when(gameInfoBean.getSimMods()).thenReturn(simMods);
    when(gameInfoBean.getTechnicalName()).thenReturn("map");

    when(mapService.isAvailable("map")).thenReturn(true);
    when(lobbyServerAccessor.requestJoinGame(gameInfoBean, null)).thenReturn(completedFuture(null));
    when(gameUpdateService.updateInBackground(any(), any(), any(), any())).thenReturn(completedFuture(null));

    CompletableFuture<Void> future = instance.joinGame(gameInfoBean, null);

    assertThat(future.get(TIMEOUT, TIME_UNIT), is(nullValue()));
  }

  @Test
  public void testNoGameTypes() throws Exception {
    List<GameTypeBean> gameTypes = instance.getGameTypes();

    assertThat(gameTypes, emptyCollectionOf(GameTypeBean.class));
    assertThat(gameTypes, hasSize(0));
  }

  @Test
  public void testGameTypeIsOnlyAddedOnce() throws Exception {
    GameTypeInfo gameTypeInfo = GameTypeInfoBuilder.create().defaultValues().get();
    instance.onGameTypeInfo(gameTypeInfo);
    instance.onGameTypeInfo(gameTypeInfo);

    List<GameTypeBean> gameTypes = instance.getGameTypes();

    assertThat(gameTypes, hasSize(1));
  }

  @Test
  public void testDifferentGameTypes() throws Exception {
    GameTypeInfo gameTypeInfo1 = GameTypeInfoBuilder.create().defaultValues().get();
    GameTypeInfo gameTypeInfo2 = GameTypeInfoBuilder.create().defaultValues().get();

    gameTypeInfo1.setName("number1");
    gameTypeInfo2.setName("number2");

    instance.onGameTypeInfo(gameTypeInfo1);
    instance.onGameTypeInfo(gameTypeInfo2);

    List<GameTypeBean> gameTypes = instance.getGameTypes();

    assertThat(gameTypes, hasSize(2));
  }

  @Test
  public void testAddOnGameTypeInfoListener() throws Exception {
    @SuppressWarnings("unchecked")
    MapChangeListener<String, GameTypeBean> listener = mock(MapChangeListener.class);
    instance.addOnGameTypeInfoListener(listener);

    instance.onGameTypeInfo(GameTypeInfoBuilder.create().defaultValues().get());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAddOnGameStartedListener() throws Exception {
    OnGameStartedListener listener = mock(OnGameStartedListener.class);
    Process process = mock(Process.class);

    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    GameLaunchInfo gameLaunchInfo = GameLaunchInfoBuilder.create().defaultValues().get();
    gameLaunchInfo.setArgs(Arrays.asList("/foo bar", "/bar foo"));

    when(forgedAllianceService.startGame(
        gameLaunchInfo.getUid(), gameLaunchInfo.getMod(), null, Arrays.asList("/foo", "bar", "/bar", "foo"), GLOBAL)
    ).thenReturn(process);
    when(gameUpdateService.updateInBackground(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(lobbyServerAccessor.requestNewGame(newGameInfo)).thenReturn(completedFuture(gameLaunchInfo));

    instance.addOnGameStartedListener(listener);
    instance.hostGame(newGameInfo);

    verify(listener).onGameStarted(gameLaunchInfo.getUid());
    verify(lobbyServerAccessor).notifyGameStarted();
    verify(forgedAllianceService).startGame(
        gameLaunchInfo.getUid(), gameLaunchInfo.getMod(), null, Arrays.asList("/foo", "bar", "/bar", "foo"), GLOBAL
    );
  }

  @Test
  public void testWaitForProcessTerminationInBackground() throws Exception {
    CompletableFuture<Void> serviceStateDoneFuture = new CompletableFuture<>();

    doAnswer(invocation -> {
      serviceStateDoneFuture.complete(null);
      return null;
    }).when(lobbyServerAccessor).notifyGameTerminated();

    Process process = mock(Process.class);

    instance.spawnTerminationListener(process);

    serviceStateDoneFuture.get(5000, TimeUnit.MILLISECONDS);

    verify(process).waitFor();
    verify(proxy).close();
    verify(lobbyServerAccessor).notifyGameTerminated();
  }

  @Test
  public void testOnGameInfoAdd() {
    assertThat(instance.getGameInfoBeans(), empty());

    GameInfo gameInfo1 = new GameInfo();
    gameInfo1.setUid(1);
    gameInfo1.setTitle("Game 1");
    gameInfo1.setState(GameState.OPEN);
    instance.onGameInfo(gameInfo1);

    GameInfo gameInfo2 = new GameInfo();
    gameInfo2.setUid(2);
    gameInfo2.setTitle("Game 2");
    gameInfo2.setState(GameState.OPEN);
    instance.onGameInfo(gameInfo2);

    GameInfoBean gameInfoBean1 = new GameInfoBean(gameInfo1);
    GameInfoBean gameInfoBean2 = new GameInfoBean(gameInfo2);

    assertThat(instance.getGameInfoBeans(), containsInAnyOrder(gameInfoBean1, gameInfoBean2));
  }

  @Test
  public void testOnGameInfoModify() {
    assertThat(instance.getGameInfoBeans(), empty());

    GameInfo gameInfo = new GameInfo();
    gameInfo.setUid(1);
    gameInfo.setTitle("Game 1");
    gameInfo.setState(GameState.OPEN);
    instance.onGameInfo(gameInfo);

    gameInfo = new GameInfo();
    gameInfo.setUid(1);
    gameInfo.setTitle("Game 1 modified");
    gameInfo.setState(GameState.OPEN);
    instance.onGameInfo(gameInfo);

    assertEquals(gameInfo.getTitle(), instance.getGameInfoBeans().iterator().next().getTitle());
  }

  @Test
  public void testOnGameInfoRemove() {
    assertThat(instance.getGameInfoBeans(), empty());

    GameInfo gameInfo = new GameInfo();
    gameInfo.setUid(1);
    gameInfo.setTitle("Game 1");
    gameInfo.setState(GameState.OPEN);
    instance.onGameInfo(gameInfo);

    gameInfo = new GameInfo();
    gameInfo.setUid(1);
    gameInfo.setTitle("Game 1 modified");
    gameInfo.setState(GameState.CLOSED);
    instance.onGameInfo(gameInfo);

    assertThat(instance.getGameInfoBeans(), empty());
  }

  @Test
  public void testStartSearchRanked1v1() throws Exception {
    GameLaunchInfo gameLaunchInfo = new GameLaunchInfo();
    gameLaunchInfo.setUid(123);
    gameLaunchInfo.setArgs(Collections.<String>emptyList());
    when(lobbyServerAccessor.startSearchRanked1v1(Faction.CYBRAN, GAME_PORT)).thenReturn(CompletableFuture.completedFuture(gameLaunchInfo));
    when(gameUpdateService.updateInBackground(GameType.LADDER_1V1.getString(), null, Collections.emptyMap(), Collections.emptySet())).thenReturn(CompletableFuture.completedFuture(null));
    when(applicationContext.getBean(SearchExpansionTask.class)).thenReturn(searchExpansionTask);
    when(scheduledExecutorService.scheduleWithFixedDelay(any(), anyLong(), anyLong(), any())).thenReturn(mock(ScheduledFuture.class));

    CompletableFuture<Void> future = instance.startSearchRanked1v1(Faction.CYBRAN);

    verify(searchExpansionTask).setMaxRadius(SEARCH_MAX_RADIUS);
    verify(searchExpansionTask).setRadiusIncrement(SEARCH_RADIUS_INCREMENT);
    verify(scheduledExecutorService).scheduleWithFixedDelay(searchExpansionTask, SEARCH_EXPANSION_DELAY, SEARCH_EXPANSION_DELAY, TimeUnit.MILLISECONDS);
    verify(lobbyServerAccessor).startSearchRanked1v1(Faction.CYBRAN, GAME_PORT);
    assertThat(future.get(TIMEOUT, TIME_UNIT), is(nullValue()));
  }

  @Test
  public void testStartSearchRanked1v1GameRunningDoesNothing() throws Exception {
    Process process = mock(Process.class);
    when(process.isAlive()).thenReturn(true);

    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    GameLaunchInfo gameLaunchInfo = GameLaunchInfoBuilder.create().defaultValues().get();
    when(forgedAllianceService.startGame(anyInt(), any(), any(), any(), any())).thenReturn(process);
    when(gameUpdateService.updateInBackground(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(lobbyServerAccessor.requestNewGame(newGameInfo)).thenReturn(completedFuture(gameLaunchInfo));

    instance.hostGame(newGameInfo);

    instance.startSearchRanked1v1(Faction.AEON);

    assertThat(instance.searching1v1Property().get(), is(false));
  }

  @Test
  public void testStopSearchRanked1v1() throws Exception {
    instance.searching1v1Property().set(true);
    instance.stopSearchRanked1v1();
    assertThat(instance.searching1v1Property().get(), is(false));
    verify(lobbyServerAccessor).stopSearchingRanked();
  }

  @Test
  public void testStopSearchRanked1v1NotSearching() throws Exception {
    instance.searching1v1Property().set(false);
    instance.stopSearchRanked1v1();
    assertThat(instance.searching1v1Property().get(), is(false));
    verify(lobbyServerAccessor, never()).stopSearchingRanked();
  }

  @Test
  public void testWonWithFaction() throws Exception {
    instance.ratingMode = GLOBAL;
    for (Faction faction : Faction.values()) {
      verifyWonFaction(faction);
    }
  }

  private void verifyWonFaction(Faction faction) throws Exception {
    Unit acu;
    switch (faction) {
      case UEF:
        acu = Unit.UEF_ACU;
        break;
      case AEON:
        acu = Unit.AEON_ACU;
        break;
      case CYBRAN:
        acu = Unit.CYBRAN_ACU;
        break;
      case SERAPHIM:
        acu = Unit.SERAPHIM_ACU;
        break;
      default:
        throw new IllegalArgumentException("Unmapped faction: " + faction);
    }
    when(playerService.getCurrentPlayer()).thenReturn(new PlayerInfoBean("junit"));
    GameStats gameStats = GameStatsBuilder.create()
        .army(ArmyBuilder.create("junit").defaultValues()
            .unitStat(UnitStatBuilder.create().defaultValues().id(acu).built(0).buildTime(1).get())
            .get())
        .army(ArmyBuilder.create("junit2").defaultValues().get())
        .get();

    sendGameStats(gameStats);

    verify(playServices).factionPlayed(faction, true);
  }

  private void sendGameStats(GameStats gameStats) throws Exception {
    instance.gameStats = null;
    Consumer<GameStats> gameStatsListener = gameStatsListenerCaptor.getValue();

    gameLaunchedListenerCaptor.getValue().accept(null);
    // Allow the game time to be different from the game end time
    Thread.sleep(1);

    // Processing is done after game has terminated, so simulate this
    CompletableFuture<Void> statsSubmittedFuture = new CompletableFuture<>();
    doAnswer(invocation -> {
      statsSubmittedFuture.complete(null);
      return null;
    }).when(playServices).resetBatchUpdate();

    // Stats are sent twice by the game
    gameStatsListener.accept(gameStats);
    gameStatsListener.accept(gameStats);

    instance.spawnTerminationListener(mock(Process.class));

    statsSubmittedFuture.get(5000, TimeUnit.MILLISECONDS);
  }

  @Test
  public void testOnGameStatsTopPlayerAndSurvived() throws Exception {
    instance.ratingMode = GLOBAL;
    when(playerService.getCurrentPlayer()).thenReturn(new PlayerInfoBean("junit"));

    GameStats gameStats = GameStatsBuilder.create()
        .army(ArmyBuilder.create("junit")
            .unitStat(UnitStatBuilder.create().unitType(UnitType.ACU).id(Unit.AEON_ACU).killed(2).lost(0).built(0).buildTime(1).damageReceived(200).damageDealt(12000).get())
            .unitStat(UnitStatBuilder.create().unitType(UnitType.ENGINEER).built(20).get())
            .unitStat(UnitStatBuilder.create().unitType(UnitType.MEDIUM_TANK).damageReceived(100).damageDealt(500).lost(0).killed(1).get())
                // Experimentals
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.ATLANTIS).damageReceived(100).damageDealt(500).built(1).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.AHWASSA).damageReceived(100).damageDealt(500).built(1).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.YTHOTHA).damageReceived(100).damageDealt(500).built(1).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.CZAR).built(3).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.FATBOY).built(5).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.GALACTIC_COLOSSUS).built(9).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.MAVOR).built(11).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.YOLONA_OSS).built(79).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.MEGALITH).built(13).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.MONKEYLORD).built(19).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.PARAGON).built(23).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.SCATHIS).built(31).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.SOUL_RIPPER).built(33).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.TEMPEST).built(47).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.NOVAX_CENTER).built(49).killed(1).get())
                // Other game enders
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.SALVATION).built(29).killed(1).get())
                // ASFs
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.CORONA).built(1).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.GEMINI).built(2).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.WASP).built(4).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.IAZYNE).built(8).killed(1).get())
                // Other units
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.FIRE_BEETLE).built(7).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.MERCY).built(17).killed(1).get())
                // SACUs
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.AEON_SACU).built(37).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.CYBRAN_SACU).built(39).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.UEF_SACU).built(41).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.SERAPHIM_SACU).built(43).killed(1).get())
                // Transporters
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.CHARIOT).built(3).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.ALUMINAR).built(7).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.SKYHOOK).built(9).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.DRAGON_FLY).built(11).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.C6_COURIER).built(13).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.C14_STAR_LIFTER).built(17).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.CONTINENTAL).built(19).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.VISH).built(23).killed(1).get())
            .unitStat(UnitStatBuilder.create().defaultValues().id(Unit.VISHALA).built(29).killed(1).get())
            .summaryStat(SummaryStatBuilder.create().type(AIR).built(1).killed(2).get())
            .summaryStat(SummaryStatBuilder.create().type(LAND).built(3).killed(4).get())
            .summaryStat(SummaryStatBuilder.create().type(NAVAL).built(5).killed(6).get())
            .summaryStat(SummaryStatBuilder.create().type(ENGINEER).built(7).killed(8).get())
            .summaryStat(SummaryStatBuilder.create().type(TECH1).built(9).killed(1).get())
            .summaryStat(SummaryStatBuilder.create().type(TECH2).built(2).killed(3).get())
            .summaryStat(SummaryStatBuilder.create().type(TECH3).built(4).killed(5).get())
            .massStat(EconomyStatBuilder.create().produced(100).consumed(100).storage(10).get())
            .energyStat(EconomyStatBuilder.create().produced(300).consumed(300).storage(30).get())
            .get())
        .army(ArmyBuilder.create("another1").defaultValues().get())
        .army(ArmyBuilder.create("another2").defaultValues().get())
        .army(ArmyBuilder.create("civilian").defaultValues().get())
        .get();

    sendGameStats(gameStats);

    verify(playServices).startBatchUpdate();
    verify(playServices).customGamePlayed();
    verify(playServices).timePlayed(any(), eq(true));
    verify(playServices).factionPlayed(Faction.AEON, true);
    verify(playServices).killedCommanders(2, true);
    verify(playServices).acuDamageReceived(200, true);
    verify(playServices).topScoringPlayer(3);
    verify(playServices).asfBuilt(15);
    verify(playServices).builtAhwasshas(1);
    verify(playServices).builtAtlantis(1);
    verify(playServices).builtCzars(3);
    verify(playServices).builtFatboys(5);
    verify(playServices).builtFireBeetles(7);
    verify(playServices).builtGalacticColossus(9);
    verify(playServices).builtMavors(11, true);
    verify(playServices).builtMegaliths(13);
    verify(playServices).builtMercies(17);
    verify(playServices).builtMonkeylords(19);
    verify(playServices).builtParagons(23, true);
    verify(playServices).builtSalvations(29, true);
    verify(playServices).builtScathis(31, true);
    verify(playServices).builtSoulRippers(33);
    verify(playServices).builtSupportCommanders(160);
    verify(playServices).builtTempests(47);
    verify(playServices).builtTransports(131);
    verify(playServices).builtYolonaOss(79, true);
    verify(playServices).builtYthothas(1);
    verify(playServices).unitStats(1, 2, 3, 4, 5, 6, 9, 1, 2, 3, 4, 5, 325, 15, 7, 8, true);
    verify(playServices).executeBatchUpdate();
    verify(playServices).resetBatchUpdate();

    verifyNoMoreInteractions(playServices);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAddOnGameInfoBeanListener() throws Exception {
    ListChangeListener<GameInfoBean> listener = mock(ListChangeListener.class);
    instance.addOnGameInfoBeanListener(listener);

    GameInfo gameInfo = new GameInfo();
    gameInfo.setUid(1);
    gameInfo.setHost("host");
    gameInfo.setTitle("title");
    gameInfo.setAccess(GameAccess.PUBLIC);
    gameInfo.setMapname("mapName");
    gameInfo.setFeaturedMod("mod");
    gameInfo.setNumPlayers(2);
    gameInfo.setMaxPlayers(4);
    gameInfo.setGameType(VictoryCondition.DOMINATION);
    gameInfo.setState(GameState.PLAYING);

    instance.onGameInfo(gameInfo);

    verify(listener).onChanged(gameInfoBeanChangeListenerCaptor.capture());

    ListChangeListener.Change<? extends GameInfoBean> change = gameInfoBeanChangeListenerCaptor.getValue();
    assertThat(change.next(), is(true));
    List<? extends GameInfoBean> addedSubList = change.getAddedSubList();
    assertThat(addedSubList, hasSize(1));

    GameInfoBean gameInfoBean = addedSubList.get(0);
    assertThat(gameInfoBean.getUid(), is(1));
    assertThat(gameInfoBean.getHost(), is("host"));
    assertThat(gameInfoBean.getTitle(), is("title"));
    assertThat(gameInfoBean.getAccess(), is(GameAccess.PUBLIC));
    assertThat(gameInfoBean.getNumPlayers(), is(2));
    assertThat(gameInfoBean.getMaxPlayers(), is(4));
    assertThat(gameInfoBean.getFeaturedMod(), is("mod"));
    assertThat(gameInfoBean.getVictoryCondition(), is(VictoryCondition.DOMINATION));
    assertThat(gameInfoBean.getStatus(), is(GameState.PLAYING));
  }
}
