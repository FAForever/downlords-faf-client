package com.faforever.client.legacy.relay;

import com.faforever.client.game.GameType;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.OnGameLaunchInfoListener;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.gson.GpgClientCommandTypeAdapter;
import com.faforever.client.legacy.io.QDataInputStream;
import com.faforever.client.legacy.proxy.Proxy;
import com.faforever.client.legacy.proxy.ProxyUtils;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.relay.FaDataInputStream;
import com.faforever.client.relay.FaDataOutputStream;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.user.UserService;
import com.faforever.client.util.SocketAddressUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.testfx.util.WaitForAsyncUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LocalRelayServerImplTest extends AbstractPlainJavaFxTest {

  private static final int TIMEOUT = 10000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();
  private static final long SESSION_ID = 1234;
  private static final double USER_ID = 872348.0;
  private static final int GAME_PORT = 6112;
  @Rule
  public TemporaryFolder cacheDirectory = new TemporaryFolder();
  private BlockingQueue<GpgClientMessage> messagesReceivedByFafServer;
  private BlockingQueue<GpgpServerMessage> messagesReceivedByGame;
  private LocalRelayServerImpl instance;
  private FaDataOutputStream gameToRelayOutputStream;
  private FaDataInputStream gameFromRelayInputStream;
  private Socket gameToRelaySocket;
  private ServerWriter serverToRelayWriter;
  private ServerSocket fafRelayServerSocket;
  private Socket localToServerSocket;
  private boolean stopped;
  @Mock
  private Proxy proxy;
  @Mock
  private Environment environment;
  @Mock
  private UserService userService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private LobbyServerAccessor lobbyServerAccessor;

  @Before
  public void setUp() throws Exception {
    messagesReceivedByFafServer = new ArrayBlockingQueue<>(10);
    messagesReceivedByGame = new ArrayBlockingQueue<>(10);

    startFakeFafRelayServer();

    CountDownLatch localRelayServerReadyLatch = new CountDownLatch(1);
    CountDownLatch gameConnectedLatch = new CountDownLatch(1);

    instance = new LocalRelayServerImpl();
    instance.proxy = proxy;
    instance.environment = environment;
    instance.userService = userService;
    instance.preferencesService = preferencesService;
    instance.lobbyServerAccessor = lobbyServerAccessor;

    ForgedAlliancePrefs forgedAlliancePrefs = mock(ForgedAlliancePrefs.class);
    Preferences preferences = mock(Preferences.class);

    instance.addOnReadyListener(localRelayServerReadyLatch::countDown);
    instance.addOnConnectionAcceptedListener(gameConnectedLatch::countDown);

    when(environment.getProperty("relay.host")).thenReturn(LOOPBACK_ADDRESS.getHostAddress());
    when(environment.getProperty("relay.port", int.class)).thenReturn(fafRelayServerSocket.getLocalPort());
    when(forgedAlliancePrefs.getPort()).thenReturn(GAME_PORT);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferencesService.getCacheDirectory()).thenReturn(cacheDirectory.getRoot().toPath());
    when(userService.getUid()).thenReturn((int) USER_ID);
    when(userService.getUsername()).thenReturn("junit");
    when(lobbyServerAccessor.getSessionId()).thenReturn(SESSION_ID);
    when(proxy.getPort()).thenReturn(GAME_PORT);

    instance.postConstruct();
    ArgumentCaptor<OnGameLaunchInfoListener> captor = ArgumentCaptor.forClass(OnGameLaunchInfoListener.class);
    verify(lobbyServerAccessor).addOnGameLaunchListener(captor.capture());

    GameLaunchInfo gameLaunchInfo = new GameLaunchInfo();
    gameLaunchInfo.setMod(GameType.DEFAULT.getString());
    captor.getValue().onGameLaunchInfo(gameLaunchInfo);

    localRelayServerReadyLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertTrue("Local relay server did not get ready within timeout", localRelayServerReadyLatch.getCount() == 0);

    startFakeGameProcess();
    gameConnectedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertTrue("Fake game did not connect within timeout", gameConnectedLatch.getCount() == 0);
  }

  private void startFakeFafRelayServer() throws IOException {
    fafRelayServerSocket = new ServerSocket(0);
    logger.info("Fake server listening on " + fafRelayServerSocket.getLocalPort());

    WaitForAsyncUtils.async(() -> {
      Gson gson = new GsonBuilder()
          .registerTypeAdapter(GpgClientCommand.class, GpgClientCommandTypeAdapter.class)
          .create();

      try (Socket socket = fafRelayServerSocket.accept()) {
        localToServerSocket = socket;
        QDataInputStream qDataInputStream = new QDataInputStream(new DataInputStream(socket.getInputStream()));
        serverToRelayWriter = new ServerWriter(socket.getOutputStream());
        serverToRelayWriter.registerMessageSerializer(new GpgServerCommandMessageSerializer(), GpgpServerMessage.class);

        while (!stopped) {
          int blockSize = qDataInputStream.readInt();
          String json = qDataInputStream.readQString();

          GpgClientMessage gpgClientMessage = gson.fromJson(json, GpgClientMessage.class);

          messagesReceivedByFafServer.add(gpgClientMessage);
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
          GpgServerCommandServerCommand command = GpgServerCommandServerCommand.fromString(gameFromRelayInputStream.readString());
          GpgpServerMessage message = new GpgpServerMessage(command);
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
    verifyAuthenticateMessage();
  }

  private void verifyAuthenticateMessage() throws InterruptedException {
    GpgClientMessage authenticateMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(authenticateMessage.getAction(), is(GpgClientCommand.AUTHENTICATE));
    assertThat(authenticateMessage.getChunks(), hasSize(2));
    assertThat(((Double) authenticateMessage.getChunks().get(0)).longValue(), is(SESSION_ID));
    assertThat(authenticateMessage.getChunks().get(1), is(USER_ID));
  }

  @Test
  public void testIdle() throws Exception {
    verifyAuthenticateMessage();

    sendFromGame(new GpgClientMessage(GpgClientCommand.GAME_STATE, singletonList("Idle")));

    GpgClientMessage gpgClientMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgClientMessage.getAction(), is(GpgClientCommand.GAME_STATE));
    assertThat(gpgClientMessage.getChunks().get(0), is("Idle"));
  }

  /**
   * Writes the specified message to the local relay server as if it was sent by the game.
   */
  private void sendFromGame(GpgClientMessage message) throws IOException {
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

    sendFromGame(new GpgClientMessage(GpgClientCommand.GAME_STATE, singletonList("Idle")));

    GpgpServerMessage relayMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(relayMessage.getCommand(), is(GpgServerCommandServerCommand.CREATE_LOBBY));
    assertThat(relayMessage.getArgs(), contains(LobbyMode.DEFAULT_LOBBY.getMode(), GAME_PORT, "junit", (int) USER_ID, 1));
  }

  @Test
  public void testCreateLobbyUponIdleP2pProxyEnabled() throws Exception {
    verifyAuthenticateMessage();
    enableP2pProxy();

    sendFromGame(new GpgClientMessage(GpgClientCommand.GAME_STATE, singletonList("Idle")));

    GpgpServerMessage relayMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(relayMessage.getCommand(), is(GpgServerCommandServerCommand.CREATE_LOBBY));
    assertThat(relayMessage.getArgs(), contains(LobbyMode.DEFAULT_LOBBY.getMode(), ProxyUtils.translateToProxyPort(GAME_PORT), "junit", (int) USER_ID, 1));
  }

  private void enableP2pProxy() throws IOException, InterruptedException {
    CountDownLatch p2pProxyEnabledLatch = new CountDownLatch(1);
    instance.addOnP2pProxyEnabledChangeListener((observable, oldValue, newValue) -> p2pProxyEnabledLatch.countDown());
    sendFromServer(new GpgpServerMessage(GpgServerCommandServerCommand.P2P_RECONNECT));
    p2pProxyEnabledLatch.await();
  }

  /**
   * Writes the specified message to the local relay server as if it was sent by the FAF server.
   */
  private void sendFromServer(GpgpServerMessage gpgpServerMessage) {
    serverToRelayWriter.write(gpgpServerMessage);
  }

  @Test
  public void testSendNatPacket() throws Exception {
    verifyAuthenticateMessage();

    GpgpServerMessage gpgpServerMessage = new GpgpServerMessage(GpgServerCommandServerCommand.SEND_NAT_PACKET);
    gpgpServerMessage.setArgs(Arrays.asList("37.58.123.2:30351", "/PLAYERID 21447 Downlord"));
    sendFromServer(gpgpServerMessage);

    GpgpServerMessage relayMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(relayMessage.getCommand(), is(GpgServerCommandServerCommand.SEND_NAT_PACKET));
    assertThat(relayMessage.getArgs(), contains("37.58.123.2:30351", "\b/PLAYERID 21447 Downlord"));
  }

  @Test
  public void testSendNatPacketP2pProxyEnabled() throws Exception {
    when(proxy.translateToLocal("37.58.123.2:6112")).thenReturn("127.0.0.1:53214");

    verifyAuthenticateMessage();
    enableP2pProxy();

    SendNatPacketMessage sendNatPacketMessage = new SendNatPacketMessage();
    sendNatPacketMessage.setPublicAddress("37.58.123.2:6112");
    sendFromServer(sendNatPacketMessage);

    GpgpServerMessage relayMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);

    verify(proxy).registerP2pPeerIfNecessary("37.58.123.2:6112");
    assertThat(relayMessage.getCommand(), is(GpgServerCommandServerCommand.SEND_NAT_PACKET));
    assertThat(relayMessage.getArgs(), contains("127.0.0.1:53214"));
  }

  @Test
  public void testHandleConnectToPeerP2pProxyEnabled() throws Exception {
    when(proxy.translateToLocal("37.58.123.2:6112")).thenReturn("127.0.0.1:53214");

    verifyAuthenticateMessage();
    enableP2pProxy();

    ConnectToPeerMessage connectToPeerMessage = new ConnectToPeerMessage();
    connectToPeerMessage.setPeerAddress("37.58.123.2:6112");
    connectToPeerMessage.setUsername("junit");
    connectToPeerMessage.setPeerUid(4);
    sendFromServer(connectToPeerMessage);

    GpgpServerMessage relayMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);

    verify(proxy).registerP2pPeerIfNecessary("37.58.123.2:6112");
    verify(proxy).setUidForPeer("37.58.123.2:6112", 4);
    assertThat(relayMessage.getCommand(), is(GpgServerCommandServerCommand.CONNECT_TO_PEER));
    assertThat(relayMessage.getArgs(), contains("127.0.0.1:53214", "junit", 4));
  }

  @Test
  public void testPing() throws Exception {
    verifyAuthenticateMessage();

    sendFromServer(new GpgpServerMessage(GpgServerCommandServerCommand.PING));

    GpgClientMessage gpgClientMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgClientMessage.getAction(), is(GpgClientCommand.PONG));
    assertThat(gpgClientMessage.getChunks(), empty());
  }

  @Test
  public void testHostGame() throws Exception {
    verifyAuthenticateMessage();

    GpgpServerMessage gpgpServerMessage = new GpgpServerMessage(GpgServerCommandServerCommand.HOST_GAME);
    gpgpServerMessage.setArgs(singletonList("3v3 sand box.v0001"));
    sendFromServer(gpgpServerMessage);

    gpgpServerMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgpServerMessage.getCommand(), is(GpgServerCommandServerCommand.HOST_GAME));
    assertThat(gpgpServerMessage.getArgs(), contains("3v3 sand box.v0001"));
  }

  @Test
  public void testJoinGame() throws Exception {
    verifyAuthenticateMessage();

    GpgpServerMessage gpgpServerMessage = new GpgpServerMessage(GpgServerCommandServerCommand.JOIN_GAME);
    gpgpServerMessage.setArgs(Arrays.asList("86.128.102.173:6112", "TechMonkey", 81655));
    sendFromServer(gpgpServerMessage);

    gpgpServerMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgpServerMessage.getCommand(), is(GpgServerCommandServerCommand.JOIN_GAME));
    assertThat(gpgpServerMessage.getArgs(), contains("86.128.102.173:6112", "TechMonkey", 81655));
  }

  @Test
  public void testJoinGameP2pProxyEnabled() throws Exception {
    when(proxy.translateToLocal("37.58.123.2:6112")).thenReturn("127.0.0.1:53214");

    verifyAuthenticateMessage();
    enableP2pProxy();

    JoinGameMessage joinGameMessage = new JoinGameMessage();
    joinGameMessage.setPeerAddress("37.58.123.2:6112");
    joinGameMessage.setUsername("junit");
    joinGameMessage.setPeerUid(4);
    sendFromServer(joinGameMessage);

    GpgpServerMessage relayMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);

    verify(proxy).registerP2pPeerIfNecessary("37.58.123.2:6112");
    verify(proxy).setUidForPeer("37.58.123.2:6112", 4);
    assertThat(relayMessage.getCommand(), is(GpgServerCommandServerCommand.JOIN_GAME));
    assertThat(relayMessage.getArgs(), contains("127.0.0.1:53214", "junit", 4));
  }

  @Test
  public void testConnectToPeer() throws Exception {
    verifyAuthenticateMessage();

    GpgpServerMessage gpgpServerMessage = new GpgpServerMessage(GpgServerCommandServerCommand.CONNECT_TO_PEER);
    gpgpServerMessage.setArgs(Arrays.asList("80.2.69.214:6112", "Cadet", 79359));
    sendFromServer(gpgpServerMessage);

    gpgpServerMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgpServerMessage.getCommand(), is(GpgServerCommandServerCommand.CONNECT_TO_PEER));
    assertThat(gpgpServerMessage.getArgs(), contains("80.2.69.214:6112", "Cadet", 79359));
  }

  @Test
  public void testDisconnectFromPeer() throws Exception {
    verifyAuthenticateMessage();

    GpgpServerMessage gpgpServerMessage = new GpgpServerMessage(GpgServerCommandServerCommand.DISCONNECT_FROM_PEER);
    // TODO fill with actual arguments
    gpgpServerMessage.setArgs(singletonList(79359));
    sendFromServer(gpgpServerMessage);

    gpgpServerMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgpServerMessage.getCommand(), is(GpgServerCommandServerCommand.DISCONNECT_FROM_PEER));
    assertThat(gpgpServerMessage.getArgs(), contains(79359));
  }

  @Test
  public void testConnectToProxy() throws Exception {
    int playerNumber = 0;
    int peerUid = 1234;
    InetSocketAddress inetSocketAddress = new InetSocketAddress(0);

    verifyAuthenticateMessage();

    when(proxy.bindAndGetProxySocketAddress(playerNumber, peerUid)).thenReturn(
        inetSocketAddress
    );

    GpgpServerMessage gpgpServerMessage = new GpgpServerMessage(GpgServerCommandServerCommand.CONNECT_TO_PROXY);
    gpgpServerMessage.setArgs(Arrays.asList(playerNumber, 0, "junit", peerUid));
    sendFromServer(gpgpServerMessage);

    gpgpServerMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgpServerMessage.getCommand(), is(GpgServerCommandServerCommand.CONNECT_TO_PEER));
    assertThat(gpgpServerMessage.getArgs(),
        contains(SocketAddressUtil.toString(inetSocketAddress), "junit", peerUid));
  }

  @Test
  public void testJoinProxy() throws Exception {
    int playerNumber = 0;
    int peerUid = 1234;
    InetSocketAddress inetSocketAddress = new InetSocketAddress(0);

    verifyAuthenticateMessage();

    when(proxy.bindAndGetProxySocketAddress(playerNumber, peerUid)).thenReturn(
        inetSocketAddress
    );

    GpgpServerMessage gpgpServerMessage = new GpgpServerMessage(GpgServerCommandServerCommand.JOIN_PROXY);
    gpgpServerMessage.setArgs(Arrays.asList(playerNumber, 0, "junit", peerUid));
    sendFromServer(gpgpServerMessage);

    gpgpServerMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgpServerMessage.getCommand(), is(GpgServerCommandServerCommand.JOIN_GAME));
    assertThat(gpgpServerMessage.getArgs(),
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

    GpgpServerMessage gpgpServerMessage = new GpgpServerMessage(GpgServerCommandServerCommand.P2P_RECONNECT);
    sendFromServer(gpgpServerMessage);

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
    when(proxy.translateToPublic("127.0.0.1:53214")).thenReturn("37.58.123.2:6112");

    verifyAuthenticateMessage();
    enableP2pProxy();

    sendFromGame(new GpgClientMessage(GpgClientCommand.PROCESS_NAT_PACKET, singletonList("127.0.0.1:53214")));

    GpgClientMessage gpgClientMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgClientMessage.getAction(), is(GpgClientCommand.PROCESS_NAT_PACKET));
    assertThat(gpgClientMessage.getChunks(), contains("37.58.123.2:6112"));
  }

  @Test
  public void testUpdateProxyStateDisconnected() throws Exception {
    verifyAuthenticateMessage();
    enableP2pProxy();

    sendFromGame(new GpgClientMessage(GpgClientCommand.DISCONNECTED, singletonList(4)));

    GpgClientMessage gpgClientMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgClientMessage.getAction(), is(GpgClientCommand.DISCONNECTED));
    assertThat(gpgClientMessage.getChunks(), contains(4.0));
    verify(proxy).updateConnectedState(4, false);
  }

  @Test
  public void testUpdateProxyStateConnected() throws Exception {
    verifyAuthenticateMessage();
    enableP2pProxy();

    sendFromGame(new GpgClientMessage(GpgClientCommand.CONNECTED, singletonList(4)));

    GpgClientMessage gpgClientMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgClientMessage.getAction(), is(GpgClientCommand.CONNECTED));
    assertThat(gpgClientMessage.getChunks(), contains(4.0));
    verify(proxy).updateConnectedState(4, true);
  }

  @Test
  public void testUpdateProxyStateGameStateLaunching() throws Exception {
    verifyAuthenticateMessage();
    enableP2pProxy();

    sendFromGame(new GpgClientMessage(GpgClientCommand.GAME_STATE, singletonList(LocalRelayServerImpl.GAME_STATE_LAUNCHING)));

    GpgClientMessage gpgClientMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgClientMessage.getAction(), is(GpgClientCommand.GAME_STATE));
    assertThat(gpgClientMessage.getChunks(), contains(LocalRelayServerImpl.GAME_STATE_LAUNCHING));
    verify(proxy).setGameLaunched(true);
  }

  @Test
  public void testUpdateProxyStateGameStateLobby() throws Exception {
    verifyAuthenticateMessage();
    enableP2pProxy();

    sendFromGame(new GpgClientMessage(GpgClientCommand.GAME_STATE, singletonList(LocalRelayServerImpl.GAME_STATE_LOBBY)));

    GpgClientMessage gpgClientMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgClientMessage.getAction(), is(GpgClientCommand.GAME_STATE));
    assertThat(gpgClientMessage.getChunks(), contains(LocalRelayServerImpl.GAME_STATE_LOBBY));
    verify(proxy).setGameLaunched(false);
  }

  @Test
  public void testUpdateProxyStateBottleneck() throws Exception {
    verifyAuthenticateMessage();
    enableP2pProxy();

    sendFromGame(new GpgClientMessage(GpgClientCommand.BOTTLENECK, emptyList()));

    GpgClientMessage gpgClientMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgClientMessage.getAction(), is(GpgClientCommand.BOTTLENECK));
    verify(proxy).setBottleneck(true);
  }

  @Test
  public void testUpdateProxyStateBottleneckCleared() throws Exception {
    verifyAuthenticateMessage();
    enableP2pProxy();

    sendFromGame(new GpgClientMessage(GpgClientCommand.BOTTLENECK_CLEARED, emptyList()));

    GpgClientMessage gpgClientMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgClientMessage.getAction(), is(GpgClientCommand.BOTTLENECK_CLEARED));
    verify(proxy).setBottleneck(false);
  }
}
