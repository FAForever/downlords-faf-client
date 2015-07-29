package com.faforever.client.legacy.relay;

import com.faforever.client.game.FeaturedMod;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.OnGameLaunchInfoListener;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.io.QDataReader;
import com.faforever.client.legacy.proxy.Proxy;
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
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LocalRelayServerImplTest extends AbstractPlainJavaFxTest {

  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();
  private static final String SESSION_ID = "1234";
  private static final int GAME_PORT = 6112;
  public static final int RECEIVE_TIMEOUT = 200000;
  public static final TimeUnit RECEIVE_TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

  private int relayServerPort;
  private BlockingQueue<LobbyMessage> messagesReceivedByFafServer;
  private BlockingQueue<RelayServerMessage> messagesReceivedByGame;
  private boolean stopped;

  private LocalRelayServerImpl instance;
  private FaDataOutputStream gameToRelayOutputStream;
  private FaDataInputStream gameFromRelayInputStream;
  private Socket gameToRelaySocket;
  private ServerWriter serverToRelayWriter;
  private ServerSocket fafRelayServerSocket;

  @Before
  public void setUp() throws Exception {
    messagesReceivedByFafServer = new ArrayBlockingQueue<>(100);
    messagesReceivedByGame = new ArrayBlockingQueue<>(100);

    startFakeFafRelayServer();

    CountDownLatch relayLocalServerReadyLatch = new CountDownLatch(1);
    CountDownLatch gameConnectedLatch = new CountDownLatch(1);

    instance = new LocalRelayServerImpl();
    instance.proxy = mock(Proxy.class);
    instance.environment = mock(Environment.class);
    instance.userService = mock(UserService.class);
    instance.preferencesService = mock(PreferencesService.class);
    instance.lobbyServerAccessor = mock(LobbyServerAccessor.class);

    ForgedAlliancePrefs forgedAlliancePrefs = mock(ForgedAlliancePrefs.class);
    Preferences preferences = mock(Preferences.class);

    instance.addOnReadyListener(relayLocalServerReadyLatch::countDown);
    instance.addOnConnectionAcceptedListener(gameConnectedLatch::countDown);

    when(instance.environment.getProperty("relay.host")).thenReturn(LOOPBACK_ADDRESS.getHostAddress());
    when(instance.environment.getProperty("relay.port", int.class)).thenReturn(relayServerPort);

    when(forgedAlliancePrefs.getPort()).thenReturn(GAME_PORT);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(instance.preferencesService.getPreferences()).thenReturn(preferences);

    when(instance.userService.getSessionId()).thenReturn(SESSION_ID);
    when(instance.userService.getUsername()).thenReturn("junit");

    instance.postConstruct();
    ArgumentCaptor<OnGameLaunchInfoListener> captor = ArgumentCaptor.forClass(OnGameLaunchInfoListener.class);
    verify(instance.lobbyServerAccessor).addOnGameLaunchListener(captor.capture());

    GameLaunchInfo gameLaunchInfo = new GameLaunchInfo();
    gameLaunchInfo.mod = FeaturedMod.DEFAULT_MOD.getString();
    captor.getValue().onGameLaunchInfo(gameLaunchInfo);

    relayLocalServerReadyLatch.await();

    startFakeGameProcess();
    gameConnectedLatch.await();
  }

  @After
  public void after() {
    IOUtils.closeQuietly(gameToRelaySocket);
    IOUtils.closeQuietly(fafRelayServerSocket);
  }

  @Test
  public void testAuthenticateSentToRelayServer() throws Exception {
    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);

    assertThat(lobbyMessage.getAction(), is(LobbyAction.AUTHENTICATE));
    assertThat(lobbyMessage.getChunks().get(0), is(SESSION_ID));
  }

  @Test
  public void testIdle() throws Exception {
    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.AUTHENTICATE));

    sendFromGame(new LobbyMessage(LobbyAction.GAME_STATE, Collections.singletonList("Idle")));

    lobbyMessage = messagesReceivedByFafServer.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.GAME_STATE));
    assertThat(lobbyMessage.getChunks().get(0), is("Idle"));
  }

  @Test
  public void testCreateLobbyUponIdle() throws Exception {
    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.AUTHENTICATE));

    sendFromGame(new LobbyMessage(LobbyAction.GAME_STATE, Collections.singletonList("Idle")));

    RelayServerMessage relayMessage = messagesReceivedByGame.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(relayMessage.getCommand(), is(RelayServerCommand.CREATE_LOBBY));
    assertThat(relayMessage.getArgs(), contains(0, GAME_PORT, "junit", LobbyMode.DEFAULT_LOBBY.getMode(), 1));
  }

  @Test
  public void testSendNatPacket() throws Exception {
    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.AUTHENTICATE));

    RelayServerMessage relayServerMessage = new RelayServerMessage(RelayServerCommand.SEND_NAT_PACKET);
    relayServerMessage.setArgs(Arrays.asList("37.58.123.2:30351", "/PLAYERID 21447 Downlord"));
    sendFromServer(relayServerMessage);

    RelayServerMessage relayMessage = messagesReceivedByGame.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(relayMessage.getCommand(), is(RelayServerCommand.SEND_NAT_PACKET));
    assertThat(relayMessage.getArgs(), contains("37.58.123.2:30351", "\b/PLAYERID 21447 Downlord"));
  }

  @Test
  public void testPing() throws Exception {
    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.AUTHENTICATE));

    sendFromServer(new RelayServerMessage(RelayServerCommand.PING));

    lobbyMessage = messagesReceivedByFafServer.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.PONG));
    assertThat(lobbyMessage.getChunks(), empty());
  }

  @Test
  public void testHostGame() throws Exception {
    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.AUTHENTICATE));

    RelayServerMessage relayServerMessage = new RelayServerMessage(RelayServerCommand.HOST_GAME);
    relayServerMessage.setArgs(Collections.singletonList("3v3 sand box.v0001"));
    sendFromServer(relayServerMessage);

    relayServerMessage = messagesReceivedByGame.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(relayServerMessage.getCommand(), is(RelayServerCommand.HOST_GAME));
    assertThat(relayServerMessage.getArgs(), contains("3v3 sand box.v0001"));
  }

  @Test
  public void testJoinGame() throws Exception {
    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.AUTHENTICATE));

    RelayServerMessage relayServerMessage = new RelayServerMessage(RelayServerCommand.JOIN_GAME);
    relayServerMessage.setArgs(Arrays.asList("86.128.102.173:6112", "TechMonkey", 81655));
    sendFromServer(relayServerMessage);

    relayServerMessage = messagesReceivedByGame.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(relayServerMessage.getCommand(), is(RelayServerCommand.JOIN_GAME));
    assertThat(relayServerMessage.getArgs(), contains("86.128.102.173:6112", "TechMonkey", 81655));
  }

  @Test
  public void testConnectToPeer() throws Exception {
    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.AUTHENTICATE));

    RelayServerMessage relayServerMessage = new RelayServerMessage(RelayServerCommand.CONNECT_TO_PEER);
    relayServerMessage.setArgs(Arrays.asList("80.2.69.214:6112", "Cadet", 79359));
    sendFromServer(relayServerMessage);

    relayServerMessage = messagesReceivedByGame.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(relayServerMessage.getCommand(), is(RelayServerCommand.CONNECT_TO_PEER));
    assertThat(relayServerMessage.getArgs(), contains("80.2.69.214:6112", "Cadet", 79359));
  }

  @Test
  public void testDisconnectFromPeer() throws Exception {
    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.AUTHENTICATE));

    RelayServerMessage relayServerMessage = new RelayServerMessage(RelayServerCommand.DISCONNECT_FROM_PEER);
    // TODO fill with actual arguments
    relayServerMessage.setArgs(Collections.singletonList(79359));
    sendFromServer(relayServerMessage);

    relayServerMessage = messagesReceivedByGame.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(relayServerMessage.getCommand(), is(RelayServerCommand.DISCONNECT_FROM_PEER));
    assertThat(relayServerMessage.getArgs(), contains(79359));
  }

  @Test
  public void testConnectToProxy() throws Exception {
    int playerNumber = 0;
    int peerUid = 1234;
    InetSocketAddress inetSocketAddress = new InetSocketAddress(0);

    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.AUTHENTICATE));

    when(instance.proxy.bindAndGetProxySocketAddress(playerNumber, peerUid)).thenReturn(
        inetSocketAddress
    );

    RelayServerMessage relayServerMessage = new RelayServerMessage(RelayServerCommand.CONNECT_TO_PROXY);
    relayServerMessage.setArgs(Arrays.asList(playerNumber, 0, "junit", peerUid));
    sendFromServer(relayServerMessage);

    relayServerMessage = messagesReceivedByGame.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(relayServerMessage.getCommand(), is(RelayServerCommand.CONNECT_TO_PEER));
    assertThat(relayServerMessage.getArgs(),
        contains(SocketAddressUtil.toString(inetSocketAddress), "junit", peerUid));
  }

  @Test
  public void testJoinProxy() throws Exception {
    int playerNumber = 0;
    int peerUid = 1234;
    InetSocketAddress inetSocketAddress = new InetSocketAddress(0);

    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.AUTHENTICATE));

    when(instance.proxy.bindAndGetProxySocketAddress(playerNumber, peerUid)).thenReturn(
        inetSocketAddress
    );

    RelayServerMessage relayServerMessage = new RelayServerMessage(RelayServerCommand.JOIN_PROXY);
    relayServerMessage.setArgs(Arrays.asList(playerNumber, 0, "junit", peerUid));
    sendFromServer(relayServerMessage);

    relayServerMessage = messagesReceivedByGame.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(relayServerMessage.getCommand(), is(RelayServerCommand.JOIN_GAME));
    assertThat(relayServerMessage.getArgs(),
        contains(SocketAddressUtil.toString(inetSocketAddress), "junit", peerUid));
  }

  @Test
  public void testCreateLobby() throws Exception {
    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.AUTHENTICATE));

    RelayServerMessage relayServerMessage = new RelayServerMessage(RelayServerCommand.CREATE_LOBBY);
    relayServerMessage.setArgs(Arrays.asList(0, 6112, "Downlord", 21447, 1));
    sendFromServer(relayServerMessage);

    assertThat(messagesReceivedByGame, empty());
  }

  @Test
  public void testP2pReconnect() throws Exception {
    assertThat(instance.isP2pProxyEnabled(), is(false));

    LobbyMessage lobbyMessage = messagesReceivedByFafServer.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(lobbyMessage.getAction(), is(LobbyAction.AUTHENTICATE));

    CountDownLatch latch = new CountDownLatch(1);
    instance.addOnP2pProxyEnabledChangeListener((observable, oldValue, newValue) -> {
      latch.countDown();
    });

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

  /**
   * Writes the specified message to the local relay server as if it was sent by the FAF server.
   */
  private void sendFromServer(RelayServerMessage relayServerMessage) throws IOException {
    serverToRelayWriter.write(relayServerMessage);
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

  private void startFakeFafRelayServer() throws IOException {
    fafRelayServerSocket = new ServerSocket(0);
    relayServerPort = fafRelayServerSocket.getLocalPort();

    WaitForAsyncUtils.async(() -> {
      Gson gson = new GsonBuilder()
          .registerTypeAdapter(LobbyAction.class, new RelayServerActionDeserializer())
          .create();

      try (Socket socket = fafRelayServerSocket.accept()) {
        QDataReader qDataReader = new QDataReader(new DataInputStream(socket.getInputStream()));
        serverToRelayWriter = new ServerWriter(socket.getOutputStream());
        serverToRelayWriter.registerMessageSerializer(new RelayServerMessageSerializer(), RelayServerMessage.class);

        while (!stopped) {
          qDataReader.skipBlockSize();
          String json = qDataReader.readQString();

          LobbyMessage lobbyMessage = gson.fromJson(json, LobbyMessage.class);

          messagesReceivedByFafServer.add(lobbyMessage);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

  }
}
