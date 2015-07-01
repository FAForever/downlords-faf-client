package com.faforever.client.game;

import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.OnGameInfoListener;
import com.faforever.client.map.MapService;
import com.faforever.client.util.Callback;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameServiceImplTest {

  private GameServiceImpl instance;

  @Before
  public void setUp() throws Exception {
    instance = new GameServiceImpl();
    instance.lobbyServerAccessor = mock(LobbyServerAccessor.class);
    instance.mapService = mock(MapService.class);
  }

  @Test
  public void testAddOnGameInfoListener() throws Exception {
    OnGameInfoListener listener = mock(OnGameInfoListener.class);
    instance.addOnGameInfoListener(listener);

    verify(instance.lobbyServerAccessor).addOnGameInfoListener(listener);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testHostGame() throws Exception {
    Callback<Void> callback = Mockito.mock(Callback.class);
    NewGameInfo newGameInfo = mock(NewGameInfo.class);

    doAnswer(invocation -> {
      callback.success(null);
      return null;
    }).when(instance.lobbyServerAccessor).requestNewGame(eq(newGameInfo), any(Callback.class));

    instance.hostGame(newGameInfo, callback);

    verify(callback).success(null);
  }


  @Test
  public void testJoinGameMapIsAvailable() throws Exception {
    GameInfoBean gameInfoBean = mock(GameInfoBean.class);
    Callback<Void> callback = mock(Callback.class);

    when(gameInfoBean.getMapName()).thenReturn("map");

    when(instance.mapService.isAvailable("map")).thenReturn(true);

    doAnswer(invocation -> {
      callback.success(null);
      return null;
    }).when(instance.lobbyServerAccessor).requestJoinGame(eq(gameInfoBean), eq(null), any(Callback.class));

    instance.joinGame(gameInfoBean, null, callback);

    verify(callback).success(null);
  }

  @Test
  public void testGetGameTypes() throws Exception {

  }

  @Test
  public void testAddOnGameTypeInfoListener() throws Exception {

  }

  @Test
  public void testOnGameTypeInfo() throws Exception {

  }

  @Test
  public void testAddOnGameStartedListener() throws Exception {

  }
}
