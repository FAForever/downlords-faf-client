package com.faforever.client.relay;

import com.faforever.client.connectivity.DatagramGateway;
import com.faforever.client.connectivity.TurnServerAccessor;
import com.faforever.client.game.GameLaunchMessageBuilder;
import com.faforever.client.game.GameType;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.SocketAddressUtil;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.relay.event.GameFullEvent;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.user.UserService;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
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
  private ThreadPoolExecutor threadPoolExecutor;
  @Mock
  private DatagramGateway datagramGateway;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private EventBus eventBus;

  @Captor
  private ArgumentCaptor<Consumer<GameLaunchMessage>> gameLaunchMessageListenerCaptor;
  @Captor
  private ArgumentCaptor<Consumer<HostGameMessage>> hostGameMessageListenerCaptor;
  @Captor
  private ArgumentCaptor<Consumer<JoinGameMessage>> joinGameMessageListenerCaptor;
  @Captor
  private ArgumentCaptor<Consumer<ConnectToPeerMessage>> connectToPeerMessageListenerCaptor;
  @Captor
  private ArgumentCaptor<Consumer<DisconnectFromPeerMessage>> disconnectFromPeerMessageListenerCaptor;

  @Before
  public void setUp() throws Exception {
    messagesReceivedByFafServer = new ArrayBlockingQueue<>(10);
    messagesReceivedByGame = new ArrayBlockingQueue<>(10);
    IntegerProperty portProperty = new SimpleIntegerProperty(GAME_PORT);

    CountDownLatch gameConnectedLatch = new CountDownLatch(1);

    instance = new LocalRelayServerImpl();
    instance.userService = userService;
    instance.preferencesService = preferencesService;
    instance.fafService = fafService;
    instance.threadPoolExecutor = threadPoolExecutor;
    instance.notificationService = notificationService;
    instance.i18n = i18n;
    instance.eventBus = eventBus;

    ForgedAlliancePrefs forgedAlliancePrefs = mock(ForgedAlliancePrefs.class);
    Preferences preferences = mock(Preferences.class);

    instance.addOnGameConnectedListener(gameConnectedLatch::countDown);

    doAnswer(
        invocation -> WaitForAsyncUtils.async(invocation.getArgumentAt(0, Runnable.class))
    ).when(threadPoolExecutor).execute(any(Runnable.class));

    when(forgedAlliancePrefs.getPort()).thenReturn(GAME_PORT);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferencesService.getCacheDirectory()).thenReturn(cacheDirectory.getRoot().toPath());
    when(forgedAlliancePrefs.portProperty()).thenReturn(portProperty);
    when(userService.getUid()).thenReturn((int) USER_ID);
    when(userService.getUsername()).thenReturn("junit");
    when(fafService.getSessionId()).thenReturn(SESSION_ID);
    doAnswer(invocation -> {
      messagesReceivedByFafServer.put(invocation.getArgumentAt(0, GpgClientMessage.class));
      return null;
    }).when(fafService).sendGpgMessage(any());

    instance.postConstruct();

    verify(fafService).addOnMessageListener(eq(GameLaunchMessage.class), gameLaunchMessageListenerCaptor.capture());
    verify(fafService).addOnMessageListener(eq(HostGameMessage.class), hostGameMessageListenerCaptor.capture());
    verify(fafService).addOnMessageListener(eq(JoinGameMessage.class), joinGameMessageListenerCaptor.capture());
    verify(fafService).addOnMessageListener(eq(ConnectToPeerMessage.class), connectToPeerMessageListenerCaptor.capture());
    verify(fafService).addOnMessageListener(eq(DisconnectFromPeerMessage.class), disconnectFromPeerMessageListenerCaptor.capture());
    verify(fafService, never()).addOnMessageListener(eq(CreateLobbyServerMessage.class), any());

    GameLaunchMessage gameLaunchMessage = GameLaunchMessageBuilder.create().defaultValues().get();
    gameLaunchMessage.setMod(GameType.DEFAULT.getString());
    gameLaunchMessageListenerCaptor.getValue().accept(gameLaunchMessage);

    instance.start(datagramGateway).toCompletableFuture().get(2, TimeUnit.SECONDS);

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
    stopped = true;
    IOUtils.closeQuietly(gameToRelaySocket);
    instance.close();
    threadPoolExecutor.shutdownNow();
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
  public void testHostGame() throws Exception {
    HostGameMessage hostGameMessage = new HostGameMessage();
    hostGameMessage.setArgs(singletonList("3v3 sand box.v0001"));
    hostGameMessageListenerCaptor.getValue().accept(hostGameMessage);

    GpgServerMessage receivedMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage.getMessageType(), is(GpgServerMessageType.HOST_GAME));
    assertThat(receivedMessage.getArgs(), contains("3v3 sand box.v0001"));
  }

  @Test
  public void testJoinGame() throws Exception {
    enterIdleState();

    try (DatagramSocket fakePeer = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 0))) {
      JoinGameMessage joinGameMessage = new JoinGameMessage();
      joinGameMessage.setArgs(Arrays.asList(Arrays.asList(fakePeer.getLocalAddress().getHostAddress(), fakePeer.getLocalPort()), "TechMonkey", 81655));
      joinGameMessageListenerCaptor.getValue().accept(joinGameMessage);

      GpgServerMessage receivedMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
      assertThat(receivedMessage.getMessageType(), is(GpgServerMessageType.JOIN_GAME));

      List<Object> args = receivedMessage.getArgs();
      assertThat(args, hasSize(3));
      assertThat((String) args.get(0), startsWith("127.0.0.1:"));
      assertThat(args.get(1), is("TechMonkey"));
      assertThat(args.get(2), is(81655));

      // Imitate the game sending a UDP packet to the joined peer
      CompletableFuture<DatagramPacket> pingPacketFuture = new CompletableFuture<>();
      doAnswer(invocation -> {
        pingPacketFuture.complete(invocation.getArgumentAt(0, DatagramPacket.class));
        return null;
      }).when(datagramGateway).send(any());

      byte[] data = new byte[]{0x05, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
      DatagramPacket pingPacket = new DatagramPacket(data, data.length);
      pingPacket.setSocketAddress(SocketAddressUtil.fromString((String) args.get(0)));

      try (DatagramSocket fakeGameSocket = new DatagramSocket(instance.getGameSocketAddress())) {
        fakeGameSocket.send(pingPacket);
      }

      assertThat(pingPacketFuture.get(3, TimeUnit.SECONDS).getSocketAddress(), is(fakePeer.getLocalSocketAddress()));
    }
  }

  private void enterIdleState() throws IOException, InterruptedException {
    sendFromGame(new GpgClientMessage(GpgClientCommand.GAME_STATE, singletonList("Idle")));
    messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
  }

  @Test
  public void testConnectToPeer() throws Exception {
    enterIdleState();

    ConnectToPeerMessage connectToPeerMessage = new ConnectToPeerMessage();
    connectToPeerMessage.setArgs(Arrays.asList(Arrays.asList("86.128.102.173", GAME_PORT), "Cadet", 79359));
    connectToPeerMessageListenerCaptor.getValue().accept(connectToPeerMessage);

    GpgServerMessage receivedMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage.getMessageType(), is(GpgServerMessageType.CONNECT_TO_PEER));

    List<Object> args = receivedMessage.getArgs();
    assertThat(args, hasSize(3));
    assertThat((String) args.get(0), startsWith("127.0.0.1:"));
    assertThat(args.get(1), is("Cadet"));
    assertThat(args.get(2), is(79359));
  }

  @Test
  public void testDisconnectFromPeer() throws Exception {
    enterIdleState();

    DisconnectFromPeerMessage disconnectFromPeerMessage = new DisconnectFromPeerMessage();
    disconnectFromPeerMessage.setUid(79359);
    disconnectFromPeerMessageListenerCaptor.getValue().accept(disconnectFromPeerMessage);

    GpgServerMessage receivedMessage = messagesReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage.getMessageType(), is(GpgServerMessageType.DISCONNECT_FROM_PEER));
    assertThat(receivedMessage.getArgs(), contains(79359));
  }


  @Test
  public void testGameFull() throws Exception {
    when(i18n.get(anyString())).thenReturn("test");
    sendFromGame(new GpgClientMessage(GpgClientCommand.GAME_FULL, emptyList()));

    verify(eventBus, timeout(1000)).post(any(GameFullEvent.class));
    verify(fafService, never()).sendGpgMessage(any(GpgClientMessage.class));
  }
}
