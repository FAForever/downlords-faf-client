package com.faforever.client.game;

import com.faforever.client.fa.ForgedAllianceService;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.OnGameInfoListener;
import com.faforever.client.legacy.OnGameTypeInfoListener;
import com.faforever.client.legacy.domain.GameInfoMessage;
import com.faforever.client.legacy.domain.GameLaunchMessageLobby;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.GameTypeMessage;
import com.faforever.client.legacy.domain.VictoryCondition;
import com.faforever.client.legacy.proxy.Proxy;
import com.faforever.client.map.MapService;
import com.faforever.client.patch.GameUpdateService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.relay.LocalRelayServer;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
  private PlayerService playerService;
  @Mock
  private ScheduledExecutorService scheduledExecutorService;
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
    when(gameInfoBean.getMapTechnicalName()).thenReturn("map");

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
    GameTypeMessage gameTypeMessage = GameTypeInfoBuilder.create().defaultValues().get();
    instance.onGameTypeInfo(gameTypeMessage);
    instance.onGameTypeInfo(gameTypeMessage);

    List<GameTypeBean> gameTypes = instance.getGameTypes();

    assertThat(gameTypes, hasSize(1));
  }

  @Test
  public void testDifferentGameTypes() throws Exception {
    GameTypeMessage gameTypeMessage1 = GameTypeInfoBuilder.create().defaultValues().get();
    GameTypeMessage gameTypeMessage2 = GameTypeInfoBuilder.create().defaultValues().get();

    gameTypeMessage1.setName("number1");
    gameTypeMessage2.setName("number2");

    instance.onGameTypeInfo(gameTypeMessage1);
    instance.onGameTypeInfo(gameTypeMessage2);

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
    GameLaunchMessageLobby gameLaunchMessage = GameLaunchInfoBuilder.create().defaultValues().get();
    gameLaunchMessage.setArgs(Arrays.asList("/foo bar", "/bar foo"));

    when(forgedAllianceService.startGame(
        gameLaunchMessage.getUid(), gameLaunchMessage.getMod(), null, Arrays.asList("/foo", "bar", "/bar", "foo"), GLOBAL)
    ).thenReturn(process);
    when(gameUpdateService.updateInBackground(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(lobbyServerAccessor.requestNewGame(newGameInfo)).thenReturn(completedFuture(gameLaunchMessage));

    instance.addOnGameStartedListener(listener);
    instance.hostGame(newGameInfo);

    verify(listener).onGameStarted(gameLaunchMessage.getUid());
    verify(forgedAllianceService).startGame(
        gameLaunchMessage.getUid(), gameLaunchMessage.getMod(), null, Arrays.asList("/foo", "bar", "/bar", "foo"), GLOBAL
    );
  }

  @Test
  public void testWaitForProcessTerminationInBackground() throws Exception {
    CompletableFuture<Void> serviceStateDoneFuture = new CompletableFuture<>();

    doAnswer(invocation -> {
      serviceStateDoneFuture.complete(null);
      return null;
    }).when(proxy).close();

    Process process = mock(Process.class);

    instance.spawnTerminationListener(process);

    serviceStateDoneFuture.get(5000, TimeUnit.MILLISECONDS);

    verify(process).waitFor();
    verify(proxy).close();
  }

  @Test
  public void testOnGameInfoAdd() {
    assertThat(instance.getGameInfoBeans(), empty());

    GameInfoMessage gameInfoMessage1 = new GameInfoMessage();
    gameInfoMessage1.setUid(1);
    gameInfoMessage1.setTitle("Game 1");
    gameInfoMessage1.setState(GameState.OPEN);
    instance.onGameInfo(gameInfoMessage1);

    GameInfoMessage gameInfoMessage2 = new GameInfoMessage();
    gameInfoMessage2.setUid(2);
    gameInfoMessage2.setTitle("Game 2");
    gameInfoMessage2.setState(GameState.OPEN);
    instance.onGameInfo(gameInfoMessage2);

    GameInfoBean gameInfoBean1 = new GameInfoBean(gameInfoMessage1);
    GameInfoBean gameInfoBean2 = new GameInfoBean(gameInfoMessage2);

    assertThat(instance.getGameInfoBeans(), containsInAnyOrder(gameInfoBean1, gameInfoBean2));
  }

  @Test
  public void testOnGameInfoModify() {
    assertThat(instance.getGameInfoBeans(), empty());

    GameInfoMessage gameInfoMessage = new GameInfoMessage();
    gameInfoMessage.setUid(1);
    gameInfoMessage.setTitle("Game 1");
    gameInfoMessage.setState(GameState.OPEN);
    instance.onGameInfo(gameInfoMessage);

    gameInfoMessage = new GameInfoMessage();
    gameInfoMessage.setUid(1);
    gameInfoMessage.setTitle("Game 1 modified");
    gameInfoMessage.setState(GameState.OPEN);
    instance.onGameInfo(gameInfoMessage);

    assertEquals(gameInfoMessage.getTitle(), instance.getGameInfoBeans().iterator().next().getTitle());
  }

  @Test
  public void testOnGameInfoRemove() {
    assertThat(instance.getGameInfoBeans(), empty());

    GameInfoMessage gameInfoMessage = new GameInfoMessage();
    gameInfoMessage.setUid(1);
    gameInfoMessage.setTitle("Game 1");
    gameInfoMessage.setState(GameState.OPEN);
    instance.onGameInfo(gameInfoMessage);

    gameInfoMessage = new GameInfoMessage();
    gameInfoMessage.setUid(1);
    gameInfoMessage.setTitle("Game 1 modified");
    gameInfoMessage.setState(GameState.CLOSED);
    instance.onGameInfo(gameInfoMessage);

    assertThat(instance.getGameInfoBeans(), empty());
  }

  @Test
  public void testStartSearchRanked1v1() throws Exception {
    GameLaunchMessageLobby gameLaunchMessage = new GameLaunchMessageLobby();
    gameLaunchMessage.setUid(123);
    gameLaunchMessage.setArgs(Collections.<String>emptyList());
    when(lobbyServerAccessor.startSearchRanked1v1(Faction.CYBRAN, GAME_PORT)).thenReturn(CompletableFuture.completedFuture(gameLaunchMessage));
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
    GameLaunchMessageLobby gameLaunchMessage = GameLaunchInfoBuilder.create().defaultValues().get();
    when(forgedAllianceService.startGame(anyInt(), any(), any(), any(), any())).thenReturn(process);
    when(gameUpdateService.updateInBackground(any(), any(), any(), any())).thenReturn(completedFuture(null));
    when(lobbyServerAccessor.requestNewGame(newGameInfo)).thenReturn(completedFuture(gameLaunchMessage));

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
  @SuppressWarnings("unchecked")
  public void testAddOnGameInfoBeanListener() throws Exception {
    ListChangeListener<GameInfoBean> listener = mock(ListChangeListener.class);
    instance.addOnGameInfoBeanListener(listener);

    GameInfoMessage gameInfoMessage = new GameInfoMessage();
    gameInfoMessage.setUid(1);
    gameInfoMessage.setHost("host");
    gameInfoMessage.setTitle("title");
    gameInfoMessage.setMapname("mapName");
    gameInfoMessage.setFeaturedMod("mod");
    gameInfoMessage.setNumPlayers(2);
    gameInfoMessage.setMaxPlayers(4);
    gameInfoMessage.setGameType(VictoryCondition.DOMINATION);
    gameInfoMessage.setState(GameState.PLAYING);

    instance.onGameInfo(gameInfoMessage);

    verify(listener).onChanged(gameInfoBeanChangeListenerCaptor.capture());

    ListChangeListener.Change<? extends GameInfoBean> change = gameInfoBeanChangeListenerCaptor.getValue();
    assertThat(change.next(), is(true));
    List<? extends GameInfoBean> addedSubList = change.getAddedSubList();
    assertThat(addedSubList, hasSize(1));

    GameInfoBean gameInfoBean = addedSubList.get(0);
    assertThat(gameInfoBean.getUid(), is(1));
    assertThat(gameInfoBean.getHost(), is("host"));
    assertThat(gameInfoBean.getTitle(), is("title"));
    assertThat(gameInfoBean.getNumPlayers(), is(2));
    assertThat(gameInfoBean.getMaxPlayers(), is(4));
    assertThat(gameInfoBean.getFeaturedMod(), is("mod"));
    assertThat(gameInfoBean.getVictoryCondition(), is(VictoryCondition.DOMINATION));
    assertThat(gameInfoBean.getStatus(), is(GameState.PLAYING));
  }
}
