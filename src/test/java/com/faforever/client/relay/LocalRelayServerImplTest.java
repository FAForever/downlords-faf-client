package com.faforever.client.relay;

import com.faforever.client.connectivity.ConnectivityService;
import com.faforever.client.connectivity.ConnectivityState;
import com.faforever.client.connectivity.TurnServerAccessor;
import com.faforever.client.game.GameLaunchMessageBuilder;
import com.faforever.client.game.GameType;
import com.faforever.client.legacy.domain.FafServerMessageType;
import com.faforever.client.legacy.domain.GameLaunchMessage;
import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.legacy.gson.GpgServerMessageTypeTypeAdapter;
import com.faforever.client.legacy.gson.MessageTargetTypeAdapter;
import com.faforever.client.legacy.gson.ServerMessageTypeTypeAdapter;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.user.UserService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.util.SocketUtils.PORT_RANGE_MAX;
import static org.springframework.util.SocketUtils.PORT_RANGE_MIN;

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
        .registerTypeAdapter(GpgServerMessageType.class, GpgServerMessageTypeTypeAdapter.INSTANCE)
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
  private TurnServerAccessor turnServerAccessor;
  @Mock
  private UserService userService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private FafService fafService;
  @Mock
  private ExecutorService executorService;
  @Mock
  private ConnectivityService connectivityService;

  @Captor
  private ArgumentCaptor<Consumer<GpgServerMessage>> onGpgServerMessageListenerCaptor;
  @Captor
  private ArgumentCaptor<Consumer<GameLaunchMessage>> onGameLaunchInfoListener;
  private Integer relayPort;
  private IntegerProperty portProperty;
  private ReadOnlyObjectProperty<ConnectivityState> connectivityStateProperty;

  @Before
  public void setUp() throws Exception {
    messagesReceivedByFafServer = new ArrayBlockingQueue<>(10);
    messagesReceivedByGame = new ArrayBlockingQueue<>(10);
    portProperty = new SimpleIntegerProperty(GAME_PORT);
    connectivityStateProperty = new SimpleObjectProperty<>();

    CountDownLatch gameConnectedLatch = new CountDownLatch(1);

    instance = new LocalRelayServerImpl();
    instance.userService = userService;
    instance.preferencesService = preferencesService;
    instance.fafService = fafService;
    instance.executorService = executorService;
    instance.connectivityService = connectivityService;

    ForgedAlliancePrefs forgedAlliancePrefs = mock(ForgedAlliancePrefs.class);
    Preferences preferences = mock(Preferences.class);

    instance.addOnConnectionAcceptedListener(gameConnectedLatch::countDown);

    doAnswer(invocation -> {
      WaitForAsyncUtils.async(invocation.getArgumentAt(0, Runnable.class));
      return null;
    }).when(executorService).execute(any(Runnable.class));

    when(forgedAlliancePrefs.getPort()).thenReturn(GAME_PORT);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferencesService.getCacheDirectory()).thenReturn(cacheDirectory.getRoot().toPath());
    when(forgedAlliancePrefs.portProperty()).thenReturn(portProperty);
    when(userService.getUid()).thenReturn((int) USER_ID);
    when(userService.getUsername()).thenReturn("junit");
    when(fafService.getSessionId()).thenReturn(SESSION_ID);
    when(connectivityService.getConnectivityState()).thenReturn(ConnectivityState.PUBLIC);
    when(connectivityService.connectivityStateProperty()).thenReturn(connectivityStateProperty);
    doAnswer(invocation -> {
      messagesReceivedByFafServer.put(invocation.getArgumentAt(0, GpgClientMessage.class));
      return null;
    }).when(fafService).sendGpgMessage(any());

    instance.postConstruct();

    verify(fafService).addOnMessageListener(eq(GpgServerMessage.class), onGpgServerMessageListenerCaptor.capture());
    verify(fafService).addOnMessageListener(eq(GameLaunchMessage.class), onGameLaunchInfoListener.capture());

    GameLaunchMessage gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();
    gameLaunchMessage.setMod(GameType.DEFAULT.getString());
    onGameLaunchInfoListener.getValue().accept(gameLaunchMessage);

    relayPort = instance.getGpgRelayPort();

    startFakeGameProcess();
    gameConnectedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertTrue("Fake game did not connect within timeout", gameConnectedLatch.getCount() == 0);
  }

  private void startFakeGameProcess() throws IOException {
    gameToRelaySocket = new Socket(LOOPBACK_ADDRESS, relayPort);
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

    List<Object> args = relayMessage.getArgs();
    assertThat(args.get(0), is(LobbyMode.DEFAULT_LOBBY.getMode()));
    assertThat((Integer) args.get(1), is(both(greaterThan(PORT_RANGE_MIN)).and(lessThan(PORT_RANGE_MAX))));
    assertThat(args.get(2), is("junit"));
    assertThat(args.get(3), is((int) USER_ID));
    assertThat(args.get(4), is(1));
  }

  @Test
  public void testSendNatPacket() throws Exception {
    String message = "/PLAYERID 21447 Downlord";
    DatagramSocket datagramSocket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 0));

    SendNatPacketMessage sendNatPacketMessage = new SendNatPacketMessage();
    sendNatPacketMessage.setPublicAddress((InetSocketAddress) datagramSocket.getLocalSocketAddress());
    sendNatPacketMessage.setMessage(message);
    sendFromServer(sendNatPacketMessage);

    byte[] bytes = new byte[128];
    DatagramPacket packet = new DatagramPacket(bytes, 0, bytes.length);
    datagramSocket.receive(packet);

    assertThat(new String(packet.getData(), 1, packet.getLength() - 1, StandardCharsets.US_ASCII), is(message));
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
    gpgServerMessage.setArgs(Arrays.asList(Arrays.asList("86.128.102.173", GAME_PORT), "TechMonkey", 81655));
    sendFromServer(gpgServerMessage);

    gpgServerMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgServerMessage.getMessageType(), is(GpgServerMessageType.JOIN_GAME));

    List<Object> args = gpgServerMessage.getArgs();
    assertThat(args, hasSize(3));
    assertThat((String) args.get(0), startsWith("127.0.0.1:"));
    assertThat(args.get(1), is("TechMonkey"));
    assertThat(args.get(2), is(81655));
  }

  @Test
  public void testConnectToPeer() throws Exception {
    GpgServerMessage gpgServerMessage = new ConnectToPeerMessage();
    gpgServerMessage.setArgs(Arrays.asList(Arrays.asList("86.128.102.173", GAME_PORT), "Cadet", 79359));
    sendFromServer(gpgServerMessage);

    gpgServerMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgServerMessage.getMessageType(), is(GpgServerMessageType.CONNECT_TO_PEER));

    List<Object> args = gpgServerMessage.getArgs();
    assertThat(args, hasSize(3));
    assertThat((String) args.get(0), startsWith("127.0.0.1:"));
    assertThat(args.get(1), is("Cadet"));
    assertThat(args.get(2), is(79359));
  }

  @Test
  public void testDisconnectFromPeer() throws Exception {
    DisconnectFromPeerMessage disconnectFromPeerMessage = new DisconnectFromPeerMessage();
    disconnectFromPeerMessage.setUid(79359);
    sendFromServer(disconnectFromPeerMessage);

    GpgServerMessage gpgServerMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(gpgServerMessage.getMessageType(), is(GpgServerMessageType.DISCONNECT_FROM_PEER));
    assertThat(gpgServerMessage.getArgs(), contains(79359));
  }

  @Test
  public void testCreateLobby() throws Exception {
    sendFromServer(new CreateLobbyServerMessage(
        LobbyMode.DEFAULT_LOBBY, GAME_PORT, "Downlord", 21447, 1
    ));

    // Message should be discarded
    assertThat(messagesReceivedByGame, empty());
  }
}
