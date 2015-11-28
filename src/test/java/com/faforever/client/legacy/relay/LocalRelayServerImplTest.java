package com.faforever.client.legacy.relay;

import com.faforever.client.game.GameType;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.OnGameLaunchInfoListener;
import com.faforever.client.legacy.domain.FafServerMessageType;
import com.faforever.client.legacy.domain.GameLaunchMessageLobby;
import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.legacy.gson.MessageTargetTypeAdapter;
import com.faforever.client.legacy.gson.ServerMessageTypeTypeAdapter;
import com.faforever.client.legacy.proxy.Proxy;
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
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.core.env.Environment;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LocalRelayServerImplTest extends AbstractPlainJavaFxTest {

  private static final int TIMEOUT = 5000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();
  private static final long SESSION_ID = 1234;
  private static final double USER_ID = 872348.0;
  private static final int GAME_PORT = 6112;
  private static final Gson gson;

  static {
    gson = new GsonBuilder()
        .registerTypeHierarchyAdapter(FafServerMessageType.class, ServerMessageTypeTypeAdapter.INSTANCE)
        .registerTypeHierarchyAdapter(MessageTarget.class, MessageTargetTypeAdapter.INSTANCE)
        .registerTypeAdapter(GpgServerMessageType.class, GpgServerCommandTypeAdapter.INSTANCE)
        .create();
  }

  @Rule
  public TemporaryFolder cacheDirectory = new TemporaryFolder();
  private BlockingQueue<GpgClientMessage> messagesReceivedByFafServer;
  private BlockingQueue<GpgServerMessage> messagesReceivedByGame;
  private LocalRelayServerImpl instance;
  private FaDataOutputStream gameToRelayOutputStream;
  private FaDataInputStream gameFromRelayInputStream;
  private Socket gameToRelaySocket;
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
  @Mock
  private ExecutorService executorService;

  @Captor
  private ArgumentCaptor<Consumer<GpgServerMessage>> onGpgServerMessageListenerCaptor;
  @Captor
  private ArgumentCaptor<OnGameLaunchInfoListener> onGameLaunchInfoListener;

  @Before
  public void setUp() throws Exception {
    messagesReceivedByFafServer = new ArrayBlockingQueue<>(10);
    messagesReceivedByGame = new ArrayBlockingQueue<>(10);

    CountDownLatch localRelayServerReadyLatch = new CountDownLatch(1);
    CountDownLatch gameConnectedLatch = new CountDownLatch(1);

    instance = new LocalRelayServerImpl();
    instance.proxy = proxy;
    instance.environment = environment;
    instance.userService = userService;
    instance.preferencesService = preferencesService;
    instance.lobbyServerAccessor = lobbyServerAccessor;
    instance.executorService = executorService;

    ForgedAlliancePrefs forgedAlliancePrefs = mock(ForgedAlliancePrefs.class);
    Preferences preferences = mock(Preferences.class);

    instance.addOnReadyListener(localRelayServerReadyLatch::countDown);
    instance.addOnConnectionAcceptedListener(gameConnectedLatch::countDown);

    doAnswer(invocation -> {
      WaitForAsyncUtils.async(invocation.getArgumentAt(0, Runnable.class));
      return null;
    }).when(executorService).execute(any(Runnable.class));

    when(forgedAlliancePrefs.getPort()).thenReturn(GAME_PORT);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferencesService.getCacheDirectory()).thenReturn(cacheDirectory.getRoot().toPath());
    when(userService.getUid()).thenReturn((int) USER_ID);
    when(userService.getUsername()).thenReturn("junit");
    when(proxy.getPort()).thenReturn(GAME_PORT);
    when(lobbyServerAccessor.getSessionId()).thenReturn(SESSION_ID);
    doAnswer(invocation -> {
      messagesReceivedByFafServer.put(invocation.getArgumentAt(0, GpgClientMessage.class));
      return null;
    }).when(lobbyServerAccessor).sendGpgMessage(any());

    instance.postConstruct();

    verify(lobbyServerAccessor).addOnGpgServerMessageListener(onGpgServerMessageListenerCaptor.capture());
    verify(lobbyServerAccessor).addOnGameLaunchListener(onGameLaunchInfoListener.capture());

    GameLaunchMessageLobby gameLaunchMessage = new GameLaunchMessageLobby();
    gameLaunchMessage.setMod(GameType.DEFAULT.getString());
    onGameLaunchInfoListener.getValue().onGameLaunchInfo(gameLaunchMessage);

    localRelayServerReadyLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertTrue("Local relay server did not get ready within timeout", localRelayServerReadyLatch.getCount() == 0);

    startFakeGameProcess();
    gameConnectedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertTrue("Fake game did not connect within timeout", gameConnectedLatch.getCount() == 0);
  }

  private void startFakeGameProcess() throws IOException {
    gameToRelaySocket = new Socket(LOOPBACK_ADDRESS, instance.getPort());
    this.gameToRelayOutputStream = new FaDataOutputStream(gameToRelaySocket.getOutputStream());
    this.gameFromRelayInputStream = new FaDataInputStream(gameToRelaySocket.getInputStream());

    WaitForAsyncUtils.async(() -> {
      while (!stopped) {
        try {
          GpgServerMessageType command = GpgServerMessageType.fromString(gameFromRelayInputStream.readString());
          List<Object> args = gameFromRelayInputStream.readChunks();

          GpgServerMessage message = new GpgServerMessage(command, args);

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
    instance.close();
  }

  @Test
  public void testIdle() throws Exception {
    sendFromGame(new GpgClientMessage(GpgClientCommand.GAME_STATE, singletonList("Idle")));

    GpgClientMessage gpgClientMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgClientMessage.getCommand(), is(GpgClientCommand.GAME_STATE));
    assertThat(gpgClientMessage.getArgs().get(0), is("Idle"));
  }

  /**
   * Writes the specified message to the local relay server as if it was sent by the game.
   */
  private void sendFromGame(GpgClientMessage message) throws IOException {
    String action = message.getCommand().getString();

    int headerSize = action.length();
    String headerField = action.replace("\t", "/t").replace("\n", "/n");

    gameToRelayOutputStream.writeInt(headerSize);
    gameToRelayOutputStream.writeString(headerField);
    gameToRelayOutputStream.writeArgs(message.getArgs());
    gameToRelayOutputStream.flush();
  }

  @Test
  public void testCreateLobbyUponIdle() throws Exception {
    sendFromGame(new GpgClientMessage(GpgClientCommand.GAME_STATE, singletonList("Idle")));

    GpgServerMessage relayMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(relayMessage.getMessageType(), is(GpgServerMessageType.CREATE_LOBBY));
    assertThat(relayMessage.getArgs(), contains(LobbyMode.DEFAULT_LOBBY.getMode(), GAME_PORT, "junit", (int) USER_ID, 1));
  }

  @Test
  public void testSendNatPacket() throws Exception {
    GpgServerMessage gpgServerMessage = new SendNatPacketMessage();
    gpgServerMessage.setArgs(Arrays.asList("37.58.123.2:30351", "/PLAYERID 21447 Downlord"));
    sendFromServer(gpgServerMessage);

    GpgServerMessage relayMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(relayMessage.getMessageType(), is(GpgServerMessageType.SEND_NAT_PACKET));
    assertThat(relayMessage.getArgs(), contains("37.58.123.2:30351", "\b/PLAYERID 21447 Downlord"));
  }

  private void sendFromServer(GpgServerMessage gpgServerMessage) {
    String json = gson.toJson(gpgServerMessage);
    gpgServerMessage.setJsonString(json);
    onGpgServerMessageListenerCaptor.getValue().accept(gpgServerMessage);
  }

  @Test
  public void testHostGame() throws Exception {
    GpgServerMessage gpgServerMessage = new HostGameMessage();
    gpgServerMessage.setArgs(singletonList("3v3 sand box.v0001"));
    sendFromServer(gpgServerMessage);

    gpgServerMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgServerMessage.getMessageType(), is(GpgServerMessageType.HOST_GAME));
    assertThat(gpgServerMessage.getArgs(), contains("3v3 sand box.v0001"));
  }

  @Test
  public void testJoinGame() throws Exception {
    GpgServerMessage gpgServerMessage = new JoinGameMessage();
    gpgServerMessage.setArgs(Arrays.asList("86.128.102.173:6112", "TechMonkey", 81655));
    sendFromServer(gpgServerMessage);

    gpgServerMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgServerMessage.getMessageType(), is(GpgServerMessageType.JOIN_GAME));
    assertThat(gpgServerMessage.getArgs(), contains("86.128.102.173:6112", "TechMonkey", 81655));
  }

  @Test
  public void testConnectToPeer() throws Exception {
    GpgServerMessage gpgServerMessage = new ConnectToPeerMessage();
    gpgServerMessage.setArgs(Arrays.asList("80.2.69.214:6112", "Cadet", 79359));
    sendFromServer(gpgServerMessage);

    gpgServerMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgServerMessage.getMessageType(), is(GpgServerMessageType.CONNECT_TO_PEER));
    assertThat(gpgServerMessage.getArgs(), contains("80.2.69.214:6112", "Cadet", 79359));
  }

  @Test
  public void testDisconnectFromPeer() throws Exception {
    GpgServerMessage gpgServerMessage = new DisconnectFromPeerMessage();
    // TODO fill with actual arguments
    gpgServerMessage.setArgs(singletonList(79359));
    sendFromServer(gpgServerMessage);

    gpgServerMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgServerMessage.getMessageType(), is(GpgServerMessageType.DISCONNECT_FROM_PEER));
    assertThat(gpgServerMessage.getArgs(), contains(79359));
  }

  @Test
  public void testJoinProxy() throws Exception {
    int playerNumber = 0;
    int peerUid = 1234;
    InetSocketAddress inetSocketAddress = new InetSocketAddress(0);

    when(proxy.bindAndGetProxySocketAddress(playerNumber, peerUid)).thenReturn(
        inetSocketAddress
    );

    JoinProxyMessage joinProxyMessage = new JoinProxyMessage();
    joinProxyMessage.setArgs(Arrays.asList(playerNumber, 0, "junit", peerUid));
    sendFromServer(joinProxyMessage);

    GpgServerMessage gpgServerMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgServerMessage.getMessageType(), is(GpgServerMessageType.JOIN_GAME));
    assertThat(gpgServerMessage.getArgs(), contains(SocketAddressUtil.toString(inetSocketAddress), "junit", peerUid));
  }

  @Test
  public void testCreateLobby() throws Exception {
    sendFromServer(new CreateLobbyServerMessage(
        LobbyMode.DEFAULT_LOBBY, 6112, "Downlord", 21447, 1
    ));

    // Message should be discarded
    assertThat(messagesReceivedByGame, empty());
  }
}
