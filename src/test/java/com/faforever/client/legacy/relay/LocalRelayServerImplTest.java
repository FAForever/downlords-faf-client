package com.faforever.client.legacy.relay;

import com.faforever.client.game.FeaturedMod;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.OnGameLaunchInfoListener;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.io.QDataInputStream;
import com.faforever.client.legacy.proxy.Proxy;
import com.faforever.client.legacy.proxy.ProxyUtils;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.user.UserService;
import com.faforever.client.util.SocketAddressUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;
import org.testfx.util.WaitForAsyncUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LocalRelayServerImplTest extends AbstractPlainJavaFxTest {

  public static final int TIMEOUT = 10000;
  public static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();
  private static final String SESSION_ID = "1234";
  private static final int GAME_PORT = 6112;
  private BlockingQueue<LobbyMessage> messagesReceivedByFafServer;
  private BlockingQueue<RelayServerMessage> messagesReceivedByGame;
  private boolean stopped;

  private LocalRelayServerImpl instance;
  private FaDataOutputStream gameToRelayOutputStream;
  private FaDataInputStream gameFromRelayInputStream;
  private Socket gameToRelaySocket;
  private ServerWriter serverToRelayWriter;
  private ServerSocket fafRelayServerSocket;
  private Socket localToServerSocket;

  @Before
  public void setUp() throws Exception {
    messagesReceivedByFafServer = new ArrayBlockingQueue<>(10);
    messagesReceivedByGame = new ArrayBlockingQueue<>(10);

    startFakeFafRelayServer();

    CountDownLatch localRelayServerReadyLatch = new CountDownLatch(1);
    CountDownLatch gameConnectedLatch = new CountDownLatch(1);

    instance = new LocalRelayServerImpl();
    instance.proxy = mock(Proxy.class);
    instance.environment = mock(Environment.class);
    instance.userService = mock(UserService.class);
    instance.preferencesService = mock(PreferencesService.class);
    instance.lobbyServerAccessor = mock(LobbyServerAccessor.class);

    ForgedAlliancePrefs forgedAlliancePrefs = mock(ForgedAlliancePrefs.class);
    Preferences preferences = mock(Preferences.class);

    instance.addOnReadyListener(localRelayServerReadyLatch::countDown);
    instance.addOnConnectionAcceptedListener(gameConnectedLatch::countDown);

    when(instance.environment.getProperty("relay.host")).thenReturn(LOOPBACK_ADDRESS.getHostAddress());
    when(instance.environment.getProperty("relay.port", int.class)).thenReturn(fafRelayServerSocket.getLocalPort());
    when(forgedAlliancePrefs.getPort()).thenReturn(GAME_PORT);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(instance.preferencesService.getPreferences()).thenReturn(preferences);
    when(instance.userService.getSessionId()).thenReturn(SESSION_ID);
    when(instance.userService.getUsername()).thenReturn("junit");
    when(instance.proxy.getPort()).thenReturn(GAME_PORT);

    instance.postConstruct();
    ArgumentCaptor<OnGameLaunchInfoListener> captor = ArgumentCaptor.forClass(OnGameLaunchInfoListener.class);
    verify(instance.lobbyServerAccessor).addOnGameLaunchListener(captor.capture());

    GameLaunchInfo gameLaunchInfo = new GameLaunchInfo();
    gameLaunchInfo.setMod(FeaturedMod.DEFAULT_MOD.getString());
    captor.getValue().onGameLaunchInfo(gameLaunchInfo);

    localRelayServerReadyLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertTrue("Local relay server did not get ready within timeout", localRelayServerReadyLatch.getCount() == 0);

    startFakeGameProcess();
    gameConnectedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertTrue("Fake game did not connect within timeout", gameConnectedLatch.getCount() == 0);
  }

  private void startFakeFafRelayServer() throws IOException {
    fafRelayServerSocket = new ServerSocket(0);
    System.out.println("Fake server listening on " + fafRelayServerSocket.getLocalPort());

    WaitForAsyncUtils.async(() -> {
      Gson gson = new GsonBuilder()
          .registerTypeAdapter(LobbyAction.class, new RelayServerActionDeserializer())
          .create();

      try (Socket socket = fafRelayServerSocket.accept()) {
        localToServerSocket = socket;
        QDataInputStream qDataInputStream = new QDataInputStream(new DataInputStream(socket.getInputStream()));
        serverToRelayWriter = new ServerWriter(socket.getOutputStream());
        serverToRelayWriter.registerMessageSerializer(new RelayServerMessageSerializer(), RelayServerMessage.class);

        while (!stopped) {
          qDataInputStream.skipBlockSize();
          String json = qDataInputStream.readQString();

          LobbyMessage lobbyMessage = gson.fromJson(json, LobbyMessage.class);

          messagesReceivedByFafServer.add(lobbyMessage);
        }
      } catch (IOException e) {
        System.out.println("Closing fake FAF relay server: " + e.getMessage());
        throw new RuntimeException(e);
      }
    });
  }

  private void startFakeGameProcess() throws IOException {
    gameToRelaySocket = new Socket(LOOPBACK_ADDRESS, instance.getPort());
    this.gameToRelayOutputStream = new FaDataOutputStream(gameToRelaySocket.getOutputStream());
    this.gameFromRelayInputStream = new FaDataInputStream(gameToRelaySocket.getInputStream());

    WaitForAsyncUtils.async(() -> {
      while (!stopped) {
        try {
          RelayServerCommand command = RelayServerCommand.fromString(gameFromRelayInputStream.readString());
          RelayServerMessage message = new RelayServerMessage(command);
          message.setArgs(gameFromRelayInputStream.readChunks());

          messagesReceivedByGame.add(message);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  @After
  public void tearDown() {
    IOUtils.closeQuietly(gameToRelaySocket);
    IOUtils.closeQuietly(fafRelayServerSocket);
    IOUtils.closeQuietly(localToServerSocket);
    instance.close();
  }

  @Test
  public void testAuthenticateSentToRelayServer() throws Exception {
    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);

    assertThat(lobbyMessage.getAction(), is(LobbyAction.AUTHENTICATE));
    assertThat(lobbyMessage.getChunks().get(0), is(SESSION_ID));
  }

  @Test
  public void testAuthenticateSentToRelayServer4() throws Exception {
    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);

    assertThat(lobbyMessage.getAction(), is(LobbyAction.AUTHENTICATE));
    assertThat(lobbyMessage.getChunks().get(0), is(SESSION_ID));
  }

  @Test
  public void testAuthenticateSentToRelayServer1() throws Exception {
    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);

    assertThat(lobbyMessage.getAction(), is(LobbyAction.AUTHENTICATE));
    assertThat(lobbyMessage.getChunks().get(0), is(SESSION_ID));
  }

  @Test
  public void testAuthenticateSentToRelayServer3() throws Exception {
    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);

    assertThat(lobbyMessage.getAction(), is(LobbyAction.AUTHENTICATE));
    assertThat(lobbyMessage.getChunks().get(0), is(SESSION_ID));
  }

  @Test
  public void testAuthenticateSentToRelayServer2() throws Exception {
    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);

    assertThat(lobbyMessage.getAction(), is(LobbyAction.AUTHENTICATE));
    assertThat(lobbyMessage.getChunks().get(0), is(SESSION_ID));
  }

  @Test
  public void testIdle() throws Exception {
    verifyAuthenticateMessage();

    sendFromGame(new LobbyMessage(LobbyAction.GAME_STATE, singletonList("Idle")));

    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.GAME_STATE));
    assertThat(lobbyMessage.getChunks().get(0), is("Idle"));
  }

  private void verifyAuthenticateMessage() throws InterruptedException {
    LobbyMessage authenticateMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(authenticateMessage.getAction(), is(LobbyAction.AUTHENTICATE));
  }

  /**
   * Writes the specified message to the local relay server as if it was sent by the game.
   */
  private void sendFromGame(LobbyMessage message) throws IOException {
    String action = message.getAction().getString();

    int headerSize = action.length();
    String headerField = action.replace("\t", "/t").replace("\n", "/n");

    gameToRelayOutputStream.writeInt(headerSize);
    gameToRelayOutputStream.writeString(headerField);
    gameToRelayOutputStream.writeArgs(message.getChunks());
    gameToRelayOutputStream.flush();
  }

  @Test
  public void testCreateLobbyUponIdle() throws Exception {
    verifyAuthenticateMessage();

    sendFromGame(new LobbyMessage(LobbyAction.GAME_STATE, singletonList("Idle")));

    RelayServerMessage relayMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(relayMessage.getCommand(), is(RelayServerCommand.CREATE_LOBBY));
    assertThat(relayMessage.getArgs(), contains(0, GAME_PORT, "junit", LobbyMode.DEFAULT_LOBBY.getMode(), 1));
  }

  @Test
  public void testCreateLobbyUponIdleP2pProxyEnabled() throws Exception {
    verifyAuthenticateMessage();
    enableP2pProxy();

    sendFromGame(new LobbyMessage(LobbyAction.GAME_STATE, singletonList("Idle")));

    RelayServerMessage relayMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(relayMessage.getCommand(), is(RelayServerCommand.CREATE_LOBBY));
    assertThat(relayMessage.getArgs(), contains(0, ProxyUtils.translateToProxyPort(GAME_PORT), "junit", LobbyMode.DEFAULT_LOBBY.getMode(), 1));
  }

  private void enableP2pProxy() throws IOException, InterruptedException {
    CountDownLatch p2pProxyEnabledLatch = new CountDownLatch(1);
    instance.addOnP2pProxyEnabledChangeListener((observable, oldValue, newValue) -> p2pProxyEnabledLatch.countDown());
    sendFromServer(new RelayServerMessage(RelayServerCommand.P2P_RECONNECT));
    p2pProxyEnabledLatch.await();
  }

  /**
   * Writes the specified message to the local relay server as if it was sent by the FAF server.
   */
  private void sendFromServer(RelayServerMessage relayServerMessage) {
    serverToRelayWriter.write(relayServerMessage);
  }

  @Test
  public void testSendNatPacket() throws Exception {
    verifyAuthenticateMessage();

    RelayServerMessage relayServerMessage = new RelayServerMessage(RelayServerCommand.SEND_NAT_PACKET);
    relayServerMessage.setArgs(Arrays.asList("37.58.123.2:30351", "/PLAYERID 21447 Downlord"));
    sendFromServer(relayServerMessage);

    RelayServerMessage relayMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(relayMessage.getCommand(), is(RelayServerCommand.SEND_NAT_PACKET));
    assertThat(relayMessage.getArgs(), contains("37.58.123.2:30351", "\b/PLAYERID 21447 Downlord"));
  }

  @Test
  public void testSendNatPacketP2pProxyEnabled() throws Exception {
    when(instance.proxy.translateToLocal("37.58.123.2:6112")).thenReturn("127.0.0.1:53214");

    verifyAuthenticateMessage();
    enableP2pProxy();

    SendNatPacketMessage sendNatPacketMessage = new SendNatPacketMessage();
    sendNatPacketMessage.setPublicAddress("37.58.123.2:6112");
    sendFromServer(sendNatPacketMessage);

    RelayServerMessage relayMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);

    verify(instance.proxy).registerP2pPeerIfNecessary("37.58.123.2:6112");
    assertThat(relayMessage.getCommand(), is(RelayServerCommand.SEND_NAT_PACKET));
    assertThat(relayMessage.getArgs(), contains("127.0.0.1:53214"));
  }

  @Test
  public void testHandleConnectToPeerP2pProxyEnabled() throws Exception {
    when(instance.proxy.translateToLocal("37.58.123.2:6112")).thenReturn("127.0.0.1:53214");

    verifyAuthenticateMessage();
    enableP2pProxy();

    ConnectToPeerMessage connectToPeerMessage = new ConnectToPeerMessage();
    connectToPeerMessage.setPeerAddress("37.58.123.2:6112");
    connectToPeerMessage.setUsername("junit");
    connectToPeerMessage.setPeerUid(4);
    sendFromServer(connectToPeerMessage);

    RelayServerMessage relayMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);

    verify(instance.proxy).registerP2pPeerIfNecessary("37.58.123.2:6112");
    verify(instance.proxy).setUidForPeer("37.58.123.2:6112", 4);
    assertThat(relayMessage.getCommand(), is(RelayServerCommand.CONNECT_TO_PEER));
    assertThat(relayMessage.getArgs(), contains("127.0.0.1:53214", "junit", 4));
  }

  @Test
  public void testPing() throws Exception {
    verifyAuthenticateMessage();

    sendFromServer(new RelayServerMessage(RelayServerCommand.PING));

    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.PONG));
    assertThat(lobbyMessage.getChunks(), empty());
  }

  @Test
  public void testHostGame() throws Exception {
    verifyAuthenticateMessage();

    RelayServerMessage relayServerMessage = new RelayServerMessage(RelayServerCommand.HOST_GAME);
    relayServerMessage.setArgs(singletonList("3v3 sand box.v0001"));
    sendFromServer(relayServerMessage);

    relayServerMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(relayServerMessage.getCommand(), is(RelayServerCommand.HOST_GAME));
    assertThat(relayServerMessage.getArgs(), contains("3v3 sand box.v0001"));
  }

  @Test
  public void testJoinGame() throws Exception {
    verifyAuthenticateMessage();

    RelayServerMessage relayServerMessage = new RelayServerMessage(RelayServerCommand.JOIN_GAME);
    relayServerMessage.setArgs(Arrays.asList("86.128.102.173:6112", "TechMonkey", 81655));
    sendFromServer(relayServerMessage);

    relayServerMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(relayServerMessage.getCommand(), is(RelayServerCommand.JOIN_GAME));
    assertThat(relayServerMessage.getArgs(), contains("86.128.102.173:6112", "TechMonkey", 81655));
  }

  @Test
  public void testJoinGameP2pProxyEnabled() throws Exception {
    when(instance.proxy.translateToLocal("37.58.123.2:6112")).thenReturn("127.0.0.1:53214");

    verifyAuthenticateMessage();
    enableP2pProxy();

    JoinGameMessage joinGameMessage = new JoinGameMessage();
    joinGameMessage.setPeerAddress("37.58.123.2:6112");
    joinGameMessage.setUsername("junit");
    joinGameMessage.setPeerUid(4);
    sendFromServer(joinGameMessage);

    RelayServerMessage relayMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);

    verify(instance.proxy).registerP2pPeerIfNecessary("37.58.123.2:6112");
    verify(instance.proxy).setUidForPeer("37.58.123.2:6112", 4);
    assertThat(relayMessage.getCommand(), is(RelayServerCommand.JOIN_GAME));
    assertThat(relayMessage.getArgs(), contains("127.0.0.1:53214", "junit", 4));
  }

  @Test
  public void testConnectToPeer() throws Exception {
    verifyAuthenticateMessage();

    RelayServerMessage relayServerMessage = new RelayServerMessage(RelayServerCommand.CONNECT_TO_PEER);
    relayServerMessage.setArgs(Arrays.asList("80.2.69.214:6112", "Cadet", 79359));
    sendFromServer(relayServerMessage);

    relayServerMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(relayServerMessage.getCommand(), is(RelayServerCommand.CONNECT_TO_PEER));
    assertThat(relayServerMessage.getArgs(), contains("80.2.69.214:6112", "Cadet", 79359));
  }

  @Test
  public void testDisconnectFromPeer() throws Exception {
    verifyAuthenticateMessage();

    RelayServerMessage relayServerMessage = new RelayServerMessage(RelayServerCommand.DISCONNECT_FROM_PEER);
    // TODO fill with actual arguments
    relayServerMessage.setArgs(singletonList(79359));
    sendFromServer(relayServerMessage);

    relayServerMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(relayServerMessage.getCommand(), is(RelayServerCommand.DISCONNECT_FROM_PEER));
    assertThat(relayServerMessage.getArgs(), contains(79359));
  }

  @Test
  public void testConnectToProxy() throws Exception {
    int playerNumber = 0;
    int peerUid = 1234;
    InetSocketAddress inetSocketAddress = new InetSocketAddress(0);

    verifyAuthenticateMessage();

    when(instance.proxy.bindAndGetProxySocketAddress(playerNumber, peerUid)).thenReturn(
        inetSocketAddress
    );

    RelayServerMessage relayServerMessage = new RelayServerMessage(RelayServerCommand.CONNECT_TO_PROXY);
    relayServerMessage.setArgs(Arrays.asList(playerNumber, 0, "junit", peerUid));
    sendFromServer(relayServerMessage);

    relayServerMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(relayServerMessage.getCommand(), is(RelayServerCommand.CONNECT_TO_PEER));
    assertThat(relayServerMessage.getArgs(),
        contains(SocketAddressUtil.toString(inetSocketAddress), "junit", peerUid));
  }

  @Test
  public void testJoinProxy() throws Exception {
    int playerNumber = 0;
    int peerUid = 1234;
    InetSocketAddress inetSocketAddress = new InetSocketAddress(0);

    verifyAuthenticateMessage();

    when(instance.proxy.bindAndGetProxySocketAddress(playerNumber, peerUid)).thenReturn(
        inetSocketAddress
    );

    RelayServerMessage relayServerMessage = new RelayServerMessage(RelayServerCommand.JOIN_PROXY);
    relayServerMessage.setArgs(Arrays.asList(playerNumber, 0, "junit", peerUid));
    sendFromServer(relayServerMessage);

    relayServerMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(relayServerMessage.getCommand(), is(RelayServerCommand.JOIN_GAME));
    assertThat(relayServerMessage.getArgs(),
        contains(SocketAddressUtil.toString(inetSocketAddress), "junit", peerUid));
  }

  @Test
  public void testCreateLobby() throws Exception {
    verifyAuthenticateMessage();

    sendFromServer(new CreateLobbyServerMessage(
        LobbyMode.DEFAULT_LOBBY, 6112, "Downlord", 21447, 1
    ));

    // Message should be discarded
    assertThat(messagesReceivedByGame, empty());
  }

  @Test
  public void testCreateLobbyP2pProxyEnabled() throws Exception {
    verifyAuthenticateMessage();
    enableP2pProxy();

    sendFromServer(new CreateLobbyServerMessage(
        LobbyMode.DEFAULT_LOBBY, 6112, "Downlord", 21447, 1
    ));

    // Message should be discarded
    assertThat(messagesReceivedByGame, empty());
  }

  @Test
  public void testP2pReconnect() throws Exception {
    assertThat(instance.isP2pProxyEnabled(), is(false));

    verifyAuthenticateMessage();

    CountDownLatch latch = new CountDownLatch(1);
    instance.addOnP2pProxyEnabledChangeListener((observable, oldValue, newValue) -> latch.countDown());

    RelayServerMessage relayServerMessage = new RelayServerMessage(RelayServerCommand.P2P_RECONNECT);
    sendFromServer(relayServerMessage);

    latch.await();

    assertThat(instance.isP2pProxyEnabled(), is(true));
  }

  @Test
  public void testOnProxyInitialized() {
    assertThat(instance.isP2pProxyEnabled(), is(false));
    instance.onP2pProxyInitialized();
    assertThat(instance.isP2pProxyEnabled(), is(true));
  }

  @Test
  public void testUpdateProxyStateProcessNatPacket() throws Exception {
    when(instance.proxy.translateToPublic("127.0.0.1:53214")).thenReturn("37.58.123.2:6112");

    verifyAuthenticateMessage();
    enableP2pProxy();

    sendFromGame(new LobbyMessage(LobbyAction.PROCESS_NAT_PACKET, singletonList("127.0.0.1:53214")));

    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.PROCESS_NAT_PACKET));
    assertThat(lobbyMessage.getChunks(), contains("37.58.123.2:6112"));
  }

  @Test
  public void testUpdateProxyStateDisconnected() throws Exception {
    verifyAuthenticateMessage();
    enableP2pProxy();

    sendFromGame(new LobbyMessage(LobbyAction.DISCONNECTED, singletonList(4)));

    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.DISCONNECTED));
    assertThat(lobbyMessage.getChunks(), contains(4.0));
    verify(instance.proxy).updateConnectedState(4, false);
  }

  @Test
  public void testUpdateProxyStateConnected() throws Exception {
    verifyAuthenticateMessage();
    enableP2pProxy();

    sendFromGame(new LobbyMessage(LobbyAction.CONNECTED, singletonList(4)));

    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.CONNECTED));
    assertThat(lobbyMessage.getChunks(), contains(4.0));
    verify(instance.proxy).updateConnectedState(4, true);
  }

  @Test
  public void testUpdateProxyStateGameStateLaunching() throws Exception {
    verifyAuthenticateMessage();
    enableP2pProxy();

    sendFromGame(new LobbyMessage(LobbyAction.GAME_STATE, singletonList(LocalRelayServerImpl.GAME_STATE_LAUNCHING)));

    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.GAME_STATE));
    assertThat(lobbyMessage.getChunks(), contains(LocalRelayServerImpl.GAME_STATE_LAUNCHING));
    verify(instance.proxy).setGameLaunched(true);
  }

  @Test
  public void testUpdateProxyStateGameStateLobby() throws Exception {
    verifyAuthenticateMessage();
    enableP2pProxy();

    sendFromGame(new LobbyMessage(LobbyAction.GAME_STATE, singletonList(LocalRelayServerImpl.GAME_STATE_LOBBY)));

    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.GAME_STATE));
    assertThat(lobbyMessage.getChunks(), contains(LocalRelayServerImpl.GAME_STATE_LOBBY));
    verify(instance.proxy).setGameLaunched(false);
  }

  @Test
  public void testUpdateProxyStateBottleneck() throws Exception {
    verifyAuthenticateMessage();
    enableP2pProxy();

    sendFromGame(new LobbyMessage(LobbyAction.BOTTLENECK, emptyList()));

    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.BOTTLENECK));
    verify(instance.proxy).setBottleneck(true);
  }

  @Test
  public void testUpdateProxyStateBottleneckCleared() throws Exception {
    verifyAuthenticateMessage();
    enableP2pProxy();

    sendFromGame(new LobbyMessage(LobbyAction.BOTTLENECK_CLEARED, emptyList()));

    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.BOTTLENECK_CLEARED));
    verify(instance.proxy).setBottleneck(false);
  }
}
