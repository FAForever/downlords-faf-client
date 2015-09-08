package com.faforever.client.game;

import com.faforever.client.fa.ForgedAllianceService;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.OnGameInfoListener;
import com.faforever.client.legacy.OnGameTypeInfoListener;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.GameTypeInfo;
import com.faforever.client.legacy.proxy.Proxy;
import com.faforever.client.map.MapService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.util.Callback;
import javafx.collections.MapChangeListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsEmptyCollection.emptyCollectionOf;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameServiceImplTest extends AbstractPlainJavaFxTest {

  private static final int GAME_PORT = 1234;
  private GameServiceImpl instance;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private LobbyServerAccessor lobbyServerAccessor;
  @Mock
  private ForgedAllianceService forgedAllianceService;
  @Mock
  private MapService mapService;
  @Mock
  private Proxy proxy;
  @Mock
  private Preferences preferences;
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;

  @Before
  public void setUp() throws Exception {
    instance = new GameServiceImpl();
    instance.lobbyServerAccessor = lobbyServerAccessor;
    instance.mapService = mapService;
    instance.forgedAllianceService = forgedAllianceService;
    instance.proxy = proxy;
    instance.preferencesService = preferencesService;

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(forgedAlliancePrefs.getPort()).thenReturn(GAME_PORT);

    instance.postConstruct();
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

    when(gameInfoBean.getTechnicalName()).thenReturn("map");

    when(mapService.isAvailable("map")).thenReturn(true);

    Callback<Void> callback = mock(Callback.class);
    doAnswer(invocation -> {
      callback.success(null);
      return null;
    }).when(lobbyServerAccessor).requestJoinGame(eq(gameInfoBean), eq(null), any(Callback.class));

    instance.joinGame(gameInfoBean, null, callback);

    verify(callback).success(null);
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

    doAnswer((InvocationOnMock invocation) -> {
      Callback<GameLaunchInfo> callback = (Callback<GameLaunchInfo>) invocation.getArguments()[1];
      callback.success(gameLaunchInfo);
      return null;
    }).when(lobbyServerAccessor).requestNewGame(eq(newGameInfo), any(Callback.class));

    when(forgedAllianceService.startGame(
        eq(gameLaunchInfo.getUid()), eq(gameLaunchInfo.getMod()), eq(Arrays.asList("/foo", "bar", "/bar", "foo"))
    )).thenReturn(process);

    Callback<Void> callback = mock(Callback.class);

    instance.addOnGameStartedListener(listener);
    instance.hostGame(newGameInfo, callback);

    verify(callback).success(null);
    verify(listener).onGameStarted(gameLaunchInfo.getUid());
    verify(lobbyServerAccessor).notifyGameStarted();
    verify(forgedAllianceService).startGame(
        eq(gameLaunchInfo.getUid()), eq(gameLaunchInfo.getMod()), eq(Arrays.asList("/foo", "bar", "/bar", "foo"))
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

    instance.waitForProcessTerminationInBackground(process);

    serviceStateDoneFuture.get(500, TimeUnit.MILLISECONDS);

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
  public void accept1v1Match() {
    instance.accept1v1Match(Faction.SERAPHIM);

    verify(lobbyServerAccessor).accept1v1Match(Faction.SERAPHIM, GAME_PORT);
    verify(lobbyServerAccessor).stopSearchingRanked();
  }

  @Test
  public void testStartSearchRanked1v1() throws Exception {
    @SuppressWarnings("unchecked")
    Callback<GameLaunchInfo> callback = mock(Callback.class);

    instance.startSearchRanked1v1(Faction.CYBRAN, callback);

    verify(lobbyServerAccessor).stopSearchingRanked();
    verify(lobbyServerAccessor).startSearchRanked1v1(Faction.CYBRAN, GAME_PORT, callback);
  }
}
