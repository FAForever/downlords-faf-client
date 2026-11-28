package com.faforever.client.fa.relay.ice;

import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fa.GameFullNotifier;
import com.faforever.client.mapstruct.IceServerMapper;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameType;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import com.faforever.commons.api.dto.CoturnServer;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import com.faforever.commons.lobby.MessageTarget;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import reactor.core.publisher.Sinks;
import reactor.netty.Connection;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NativeIceAdapterTest extends ServiceTest {

  private static final int WAIT_MILLIS = 5000;

  @Mock
  private PlayerService playerService;
  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private GameFullNotifier gameFullNotifier;

  private NativeIceAdapter instance;
  private Socket game;
  private DataInputStream fromClient;

  @BeforeEach
  public void setUp() throws Exception {
    PlayerInfo player = mock(PlayerInfo.class);
    lenient().when(player.getUsername()).thenReturn("junit");
    lenient().when(player.getId()).thenReturn(5);
    lenient().when(playerService.getCurrentPlayer()).thenReturn(player);

    instance = new NativeIceAdapter(playerService, fafServerAccessor, Mappers.getMapper(IceServerMapper.class),
                                    new ObjectMapper(), gameFullNotifier);

    GameLaunchResponse launch = mock(GameLaunchResponse.class);
    when(launch.getGameType()).thenReturn(GameType.MATCHMAKER);
    instance.prepare(launch);

    int port = instance.start(2000, false).get();
    game = new Socket(InetAddress.getLoopbackAddress(), port);
    fromClient = new DataInputStream(game.getInputStream());
  }

  @AfterEach
  public void tearDown() throws IOException {
    instance.stop();
    game.close();
  }

  private void sendFromGame(String command, Object... args) throws IOException {
    game.getOutputStream().write(frame(command, args));
    game.getOutputStream().flush();
  }

  private static byte[] frame(String command, Object... args) {
    ByteBuffer buffer = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN);
    byte[] name = command.getBytes(StandardCharsets.UTF_8);
    buffer.putInt(name.length).put(name).putInt(args.length);
    for (Object arg : args) {
      if (arg instanceof Integer number) {
        buffer.put((byte) 0).putInt(number);
      } else {
        byte[] text = String.valueOf(arg).getBytes(StandardCharsets.UTF_8);
        buffer.put((byte) 1).putInt(text.length).put(text);
      }
    }
    byte[] bytes = new byte[buffer.position()];
    buffer.flip().get(bytes);
    return bytes;
  }

  private static int readInt(DataInputStream in) throws IOException {
    return Integer.reverseBytes(in.readInt());
  }

  private static String readString(DataInputStream in) throws IOException {
    byte[] bytes = new byte[readInt(in)];
    in.readFully(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private static List<Object> readMessage(Socket socket, DataInputStream in) throws IOException {
    socket.setSoTimeout(WAIT_MILLIS);
    List<Object> message = new ArrayList<>();
    message.add(readString(in));
    int argCount = readInt(in);
    for (int i = 0; i < argCount; i++) {
      message.add(in.readByte() == 0 ? (Object) readInt(in) : readString(in));
    }
    return message;
  }

  private List<Object> readMessage() throws IOException {
    return readMessage(game, fromClient);
  }

  @Test
  public void testIdleGetsIceServersThenCreateLobby() throws Exception {
    sendFromGame("GameState", "Idle");

    assertEquals(List.of("IceServers", "[]"), readMessage());
    assertEquals(List.of("CreateLobby", 1, 0, "junit", 5, 1), readMessage());
  }

  @Test
  public void testGameMessagesGoToTheServerUnchanged() throws Exception {
    sendFromGame("IceMsg", 7, "{\"a\":1}");

    ArgumentCaptor<GpgGameOutboundMessage> captor = ArgumentCaptor.forClass(GpgGameOutboundMessage.class);
    verify(fafServerAccessor, timeout(WAIT_MILLIS)).sendGpgMessage(captor.capture());
    assertEquals("IceMsg", captor.getValue().getCommand());
    assertEquals(List.of(7, "{\"a\":1}"), captor.getValue().getArgs());
    assertEquals(MessageTarget.GAME, captor.getValue().getTarget());
  }

  @Test
  public void testMessagesCutAnywhereInTheStreamArriveWhole() throws Exception {
    byte[] first = frame("IceMsg", 7, "{\"a\":1}");
    byte[] second = frame("GameState", "Lobby");
    byte[] stream = new byte[first.length + second.length];
    System.arraycopy(first, 0, stream, 0, first.length);
    System.arraycopy(second, 0, stream, first.length, second.length);
    int cut = first.length + 6;
    game.getOutputStream().write(stream, 0, cut);
    game.getOutputStream().flush();
    Thread.sleep(200);
    game.getOutputStream().write(stream, cut, stream.length - cut);
    game.getOutputStream().flush();

    ArgumentCaptor<GpgGameOutboundMessage> captor = ArgumentCaptor.forClass(GpgGameOutboundMessage.class);
    verify(fafServerAccessor, timeout(WAIT_MILLIS).times(2)).sendGpgMessage(captor.capture());
    assertEquals(List.of(7, "{\"a\":1}"), captor.getAllValues().get(0).getArgs());
    assertEquals("GameState", captor.getAllValues().get(1).getCommand());
    assertEquals(List.of("Lobby"), captor.getAllValues().get(1).getArgs());
  }

  @Test
  public void testGameFullStaysInTheClient() throws Exception {
    sendFromGame("GameFull");

    verify(gameFullNotifier, timeout(WAIT_MILLIS)).onGameFull();
    verify(fafServerAccessor, after(200).never()).sendGpgMessage(any());
  }

  @Test
  public void testConnectToPeerSendsOfferAsInt() throws Exception {
    gameConnected();

    instance.connectToPeer("other", 9, true);
    instance.connectToPeer("third", 10, false);

    assertEquals(List.of("ConnectToPeer", "other", 9, 1), readMessage());
    assertEquals(List.of("ConnectToPeer", "third", 10, 0), readMessage());
  }

  @Test
  public void testIceMessageObjectIsSentAsJsonText() throws Exception {
    gameConnected();

    instance.iceMsg(9, java.util.Map.of("a", 1));

    assertEquals(List.of("IceMsg", 9, "{\"a\":1}"), readMessage());
  }

  @Test
  public void testHostGameGoesToTheGameWithTheMap() throws Exception {
    gameConnected();

    instance.hostGame("scmp_009");

    assertEquals(List.of("HostGame", "scmp_009"), readMessage());
  }

  @Test
  public void testTheGameRunsItsOwnNetwork() {
    assertEquals(List.of("/nativenet", "/con", "net_PeerFeatures=1;net_RejoinAccept=1"), instance.gameArguments());
  }

  @Test
  public void testDisconnectFromPeerGoesToTheGame() throws Exception {
    gameConnected();

    instance.disconnectFromPeer(9);

    assertEquals(List.of("DisconnectFromPeer", 9), readMessage());
  }

  /** The game is connected once the client has passed one of its messages on. */
  private void gameConnected() throws Exception {
    sendFromGame("GameState", "Lobby");
    verify(fafServerAccessor, timeout(WAIT_MILLIS)).sendGpgMessage(any());
  }

  @Test
  public void testJoinGameGoesToTheGameWithThePeersId() throws Exception {
    gameConnected();

    instance.joinGame("host", 3);

    assertEquals(List.of("JoinGame", "host", 3), readMessage());
  }

  @Test
  public void testIceServersGoToTheGameAsJsonText() throws Exception {
    gameConnected();
    CoturnServer server = new CoturnServer();
    server.setUsername("0:1234");
    server.setCredential("secret");
    server.setUrls(java.util.Set.of(java.net.URI.create("turn://relay.example:3478?transport=udp")));

    instance.setIceServers(List.of(server));

    List<Object> message = readMessage();
    assertEquals("IceServers", message.get(0));
    String json = (String) message.get(1);
    assertTrue(json.contains("turn://relay.example:3478?transport=udp"), json);
    assertTrue(json.contains("0:1234"), json);
  }

  @Test
  public void testTheAdapterStateChangesNothing() throws Exception {
    instance.onIceAdapterStateChanged("Disconnected");

    verify(fafServerAccessor, after(200).never()).sendGpgMessage(any());
  }

  @Test
  public void testAnIceMessageGivenAsTextIsPassedAsIs() throws Exception {
    gameConnected();

    instance.iceMsg(9, "{\"b\":2}");

    assertEquals(List.of("IceMsg", 9, "{\"b\":2}"), readMessage());
  }

  @Test
  public void testAnIceMessageThatCantBeWrittenIsRefusedAndTheNextOneArrives() throws Exception {
    gameConnected();

    assertThrows(IllegalArgumentException.class, () -> instance.iceMsg(9, new Object()));
    instance.iceMsg(9, "{}");

    assertEquals(List.of("IceMsg", 9, "{}"), readMessage());
  }

  @Test
  public void testAStreamThatIsntGpgNetClosesTheConnection() throws Exception {
    game.getOutputStream().write(new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
    game.getOutputStream().flush();
    game.setSoTimeout(WAIT_MILLIS);

    assertEquals(-1, fromClient.read());
    verify(fafServerAccessor, never()).sendGpgMessage(any());
  }

  @Test
  public void testTheDecoderWaitsForEveryPartOfAMessage() {
    byte[] whole = frame("Cmd", 7, "text", 8);
    EmbeddedChannel channel = new EmbeddedChannel(new NativeIceAdapter.GpgNetDecoder());
    for (int i = 0; i < whole.length - 1; i++) {
      channel.writeInbound(Unpooled.wrappedBuffer(whole, i, 1));
      assertNull(channel.readInbound(), "a message out after byte " + i);
    }
    channel.writeInbound(Unpooled.wrappedBuffer(whole, whole.length - 1, 1));

    GpgGameOutboundMessage message = channel.readInbound();
    assertEquals("Cmd", message.getCommand());
    assertEquals(List.of(7, "text", 8), message.getArgs());
  }

  @Test
  public void testTheDecoderRefusesANegativeArgumentCount() {
    byte[] bad = ByteBuffer.allocate(11).order(ByteOrder.LITTLE_ENDIAN).putInt(3).put("Cmd".getBytes(StandardCharsets.UTF_8))
        .putInt(-1).array();
    EmbeddedChannel channel = new EmbeddedChannel(new NativeIceAdapter.GpgNetDecoder());

    assertThrows(DecoderException.class, () -> channel.writeInbound(Unpooled.wrappedBuffer(bad)));
  }

  @Test
  public void testRelayOnlyIsPassedToTheGame() throws Exception {
    instance.start(2001, true).get();

    assertTrue(instance.gameArguments().contains("/nativenetrelay"), String.valueOf(instance.gameArguments()));
  }

  @Test
  public void testACustomGamesLobbyIsNotInMatchmakerMode() throws Exception {
    GameLaunchResponse custom = mock(GameLaunchResponse.class);
    when(custom.getGameType()).thenReturn(GameType.CUSTOM);
    instance.prepare(custom);

    sendFromGame("GameState", "Idle");

    assertEquals(List.of("IceServers", "[]"), readMessage());
    assertEquals(List.of("CreateLobby", 0, 0, "junit", 5, 1), readMessage());
  }

  @Test
  public void testTheDecoderRefusesAnOversizedField() {
    byte[] bad = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(Integer.MAX_VALUE).array();
    EmbeddedChannel channel = new EmbeddedChannel(new NativeIceAdapter.GpgNetDecoder());

    assertThrows(DecoderException.class, () -> channel.writeInbound(Unpooled.wrappedBuffer(bad)));
  }

  @Test
  public void testAnIceServerListThatCantBeWrittenIsNotSent() throws Exception {
    ObjectMapper failing = mock(ObjectMapper.class);
    when(failing.writeValueAsString(any())).thenThrow(new com.fasterxml.jackson.core.JsonGenerationException("broken", (com.fasterxml.jackson.core.JsonGenerator) null));
    NativeIceAdapter adapter = new NativeIceAdapter(playerService, fafServerAccessor, Mappers.getMapper(IceServerMapper.class),
                                                    failing, gameFullNotifier);
    GameLaunchResponse launch = mock(GameLaunchResponse.class);
    when(launch.getGameType()).thenReturn(GameType.CUSTOM);
    adapter.prepare(launch);
    int port = adapter.start(2002, false).get();
    try (Socket other = new Socket(InetAddress.getLoopbackAddress(), port)) {
      DataInputStream in = new DataInputStream(other.getInputStream());
      // The connection to the game works first: Idle gets the lobby setup back, so a frame from the list below
      // would arrive.
      other.getOutputStream().write(frame("GameState", "Idle"));
      other.getOutputStream().flush();
      assertEquals(List.of("IceServers", "[]"), readMessage(other, in));
      assertEquals("CreateLobby", readMessage(other, in).get(0));

      CoturnServer server = new CoturnServer();
      server.setUsername("0:1234");
      server.setCredential("secret");
      server.setUrls(java.util.Set.of(java.net.URI.create("turn://relay.example:3478")));
      adapter.setIceServers(List.of(server));

      other.setSoTimeout(500);
      assertThrows(SocketTimeoutException.class, in::readInt);
    } finally {
      adapter.stop();
    }
  }

  @Test
  public void testANewConnectionClosesTheOldOneAndTheOldOneCantSetUpItsLobby() throws Exception {
    gameConnected();
    try (Socket replacement = new Socket(InetAddress.getLoopbackAddress(), game.getPort())) {
      DataInputStream toReplacement = new DataInputStream(replacement.getInputStream());

      // The old connection is closed once the new one is the game's.
      game.setSoTimeout(WAIT_MILLIS);
      assertEquals(-1, fromClient.read());
      // What it sent last is not passed on, and gets nothing sent to the new connection.
      try {
        sendFromGame("GameState", "Idle");
      } catch (IOException closedAlready) {
        // The client may have reset it already: nothing arrived either way.
      }
      verify(fafServerAccessor, after(300).times(1)).sendGpgMessage(any());

      replacement.getOutputStream().write(frame("GameState", "Idle"));
      replacement.getOutputStream().flush();
      assertEquals(List.of("IceServers", "[]"), readMessage(replacement, toReplacement));
      assertEquals("CreateLobby", readMessage(replacement, toReplacement).get(0));
      verify(fafServerAccessor, timeout(WAIT_MILLIS).times(2)).sendGpgMessage(any());
      // One lobby setup, the new connection's own: none for the old connection's Idle.
      replacement.setSoTimeout(500);
      assertThrows(SocketTimeoutException.class, toReplacement::readInt);
    }
  }

  @Test
  public void testAMessageFromAConnectionThatIsNoLongerTheGamesIsDropped() throws Exception {
    gameConnected();
    Sinks.Many<byte[]> replaced = Sinks.many().unicast().onBackpressureBuffer();

    instance.onMessageFromGame(new GpgGameOutboundMessage("GameState", List.of("Idle"), MessageTarget.GAME), replaced);

    // Neither passed on to the server nor a lobby set up on the game's connection.
    verify(fafServerAccessor, after(300).times(1)).sendGpgMessage(any());
    game.setSoTimeout(500);
    assertThrows(SocketTimeoutException.class, fromClient::readInt);
  }

  @Test
  public void testAConnectionOfAStoppedOrEarlierStartIsClosedAndNotTheGames() throws Exception {
    gameConnected();
    Connection late = mock(Connection.class);
    Sinks.Many<byte[]> toLate = Sinks.many().unicast().onBackpressureBuffer();

    // A connection accepted for an earlier start (or after stop, which also ends a start) is closed by its caller,
    // and the game's connection stays the game's.
    assertEquals(late, instance.becomeTheGame(late, toLate, Integer.MIN_VALUE));
    instance.hostGame("scmp_009");
    assertEquals(List.of("HostGame", "scmp_009"), readMessage());
  }

  @Test
  public void testStopClosesAConnectionThatArrivedJustBefore() throws Exception {
    gameConnected();

    instance.stop();

    game.setSoTimeout(WAIT_MILLIS);
    assertEquals(-1, fromClient.read());
  }

  @Test
  public void testNothingIsSentAfterStop() throws Exception {
    gameConnected();
    instance.stop();

    instance.connectToPeer("other", 9, true);

    game.setSoTimeout(WAIT_MILLIS);
    assertEquals(-1, fromClient.read());
  }
}
