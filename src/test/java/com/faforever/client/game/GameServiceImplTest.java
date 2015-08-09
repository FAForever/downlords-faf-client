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
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.util.Callback;
import javafx.collections.MapChangeListener;
import org.junit.Before;
import org.junit.Test;
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

  private GameServiceImpl instance;

  @Before
  public void setUp() throws Exception {
    instance = new GameServiceImpl();
    instance.lobbyServerAccessor = mock(LobbyServerAccessor.class);
    instance.mapService = mock(MapService.class);
    instance.forgedAllianceService = mock(ForgedAllianceService.class);
    instance.proxy = mock(Proxy.class);

    instance.postConstruct();
  }

  @Test
  public void postConstruct() {
    verify(instance.lobbyServerAccessor).addOnGameTypeInfoListener(any(OnGameTypeInfoListener.class));
    verify(instance.lobbyServerAccessor).addOnGameInfoListener(any(OnGameInfoListener.class));
  }

  @Test
  public void testJoinGameMapIsAvailable() throws Exception {
    GameInfoBean gameInfoBean = mock(GameInfoBean.class);
    Callback<Void> callback = mock(Callback.class);

    when(gameInfoBean.getTechnicalName()).thenReturn("map");

    when(instance.mapService.isAvailable("map")).thenReturn(true);

    doAnswer(invocation -> {
      callback.success(null);
      return null;
    }).when(instance.lobbyServerAccessor).requestJoinGame(eq(gameInfoBean), eq(null), any(Callback.class));

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

    gameTypeInfo1.name = "number1";
    gameTypeInfo2.name = "number2";

    instance.onGameTypeInfo(gameTypeInfo1);
    instance.onGameTypeInfo(gameTypeInfo2);

    List<GameTypeBean> gameTypes = instance.getGameTypes();

    assertThat(gameTypes, hasSize(2));
  }

  @Test
  public void testAddOnGameTypeInfoListener() throws Exception {
    MapChangeListener listener = mock(MapChangeListener.class);
    instance.addOnGameTypeInfoListener(listener);

    instance.onGameTypeInfo(GameTypeInfoBuilder.create().defaultValues().get());
  }

  @Test
  public void testAddOnGameStartedListener() throws Exception {
    OnGameStartedListener listener = mock(OnGameStartedListener.class);
    Callback<Void> callback = mock(Callback.class);
    Process process = mock(Process.class);

    NewGameInfo newGameInfo = NewGameInfoBuilder.create().defaultValues().get();
    GameLaunchInfo gameLaunchInfo = GameLaunchInfoBuilder.create().defaultValues().get();
    gameLaunchInfo.args = Arrays.asList("/foo bar", "/bar foo");

    doAnswer((InvocationOnMock invocation) -> {
      Callback<GameLaunchInfo> callback1 = (Callback<GameLaunchInfo>) invocation.getArguments()[1];
      callback1.success(gameLaunchInfo);
      return null;
    }).when(instance.lobbyServerAccessor).requestNewGame(eq(newGameInfo), any(Callback.class));

    when(instance.forgedAllianceService.startGame(
        eq(gameLaunchInfo.uid), eq(gameLaunchInfo.mod), eq(Arrays.asList("/foo", "bar", "/bar", "foo"))
    )).thenReturn(process);

    instance.addOnGameStartedListener(listener);
    instance.hostGame(newGameInfo, callback);

    verify(callback).success(null);
    verify(listener).onGameStarted(gameLaunchInfo.uid);
    verify(instance.lobbyServerAccessor).notifyGameStarted();
    verify(instance.forgedAllianceService).startGame(
        eq(gameLaunchInfo.uid), eq(gameLaunchInfo.mod), eq(Arrays.asList("/foo", "bar", "/bar", "foo"))
    );
  }

  @Test
  public void testWaitForProcessTerminationInBackground() throws Exception {
    CompletableFuture<Void> serviceStateDoneFuture = new CompletableFuture<>();

    doAnswer(invocation -> {
      serviceStateDoneFuture.complete(null);
      return null;
    }).when(instance.lobbyServerAccessor).notifyGameTerminated();

    Process process = mock(Process.class);

    instance.waitForProcessTerminationInBackground(process);

    serviceStateDoneFuture.get(500, TimeUnit.MILLISECONDS);

    verify(process).waitFor();
    verify(instance.proxy).close();
    verify(instance.lobbyServerAccessor).notifyGameTerminated();
  }

  @Test
  public void testOnGameInfoAdd() {
    assertThat(instance.getGameInfoBeans(), empty());

    GameInfo gameInfo1 = new GameInfo();
    gameInfo1.uid = 1;
    gameInfo1.title = "Game 1";
    gameInfo1.state = GameState.OPEN;
    instance.onGameInfo(gameInfo1);

    GameInfo gameInfo2 = new GameInfo();
    gameInfo2.uid = 2;
    gameInfo2.title = "Game 2";
    gameInfo2.state = GameState.OPEN;
    instance.onGameInfo(gameInfo2);

    GameInfoBean gameInfoBean1 = new GameInfoBean(gameInfo1);
    GameInfoBean gameInfoBean2 = new GameInfoBean(gameInfo2);

    assertThat(instance.getGameInfoBeans(), containsInAnyOrder(gameInfoBean1, gameInfoBean2));
  }

  @Test
  public void testOnGameInfoModify() {
    assertThat(instance.getGameInfoBeans(), empty());

    GameInfo gameInfo = new GameInfo();
    gameInfo.uid = 1;
    gameInfo.title = "Game 1";
    gameInfo.state = GameState.OPEN;
    instance.onGameInfo(gameInfo);

    gameInfo = new GameInfo();
    gameInfo.uid = 1;
    gameInfo.title = "Game 1 modified";
    gameInfo.state = GameState.OPEN;
    instance.onGameInfo(gameInfo);

    assertEquals(gameInfo.title, instance.getGameInfoBeans().iterator().next().getTitle());
  }

  @Test
  public void testOnGameInfoRemove() {
    assertThat(instance.getGameInfoBeans(), empty());

    GameInfo gameInfo = new GameInfo();
    gameInfo.uid = 1;
    gameInfo.title = "Game 1";
    gameInfo.state = GameState.OPEN;
    instance.onGameInfo(gameInfo);

    gameInfo = new GameInfo();
    gameInfo.uid = 1;
    gameInfo.title = "Game 1 modified";
    gameInfo.state = GameState.CLOSED;
    instance.onGameInfo(gameInfo);

    assertThat(instance.getGameInfoBeans(), empty());
  }
}
