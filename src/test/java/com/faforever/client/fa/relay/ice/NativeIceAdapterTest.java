package com.faforever.client.fa.relay.ice;

import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fa.GameFullNotifier;
import com.faforever.client.mapstruct.IceServerMapper;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.lobby.ConnectToPeerGpgCommand;
import com.faforever.commons.lobby.DisconnectFromPeerGpgCommand;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameType;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import com.faforever.commons.lobby.IceMsgGpgCommand;
import com.faforever.commons.lobby.MessageTarget;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
  @Spy
  private ForgedAlliancePrefs forgedAlliancePrefs;

  private final Sinks.Many<ConnectToPeerGpgCommand> connectToPeer = Sinks.many().multicast().onBackpressureBuffer();
  private final Sinks.Many<IceMsgGpgCommand> iceMsg = Sinks.many().multicast().onBackpressureBuffer();
  private final Sinks.Many<DisconnectFromPeerGpgCommand> disconnectFromPeer = Sinks.many().multicast().onBackpressureBuffer();
  private NativeIceAdapter instance;
  private Socket game;
  private int gamePort;
  private DataInputStream fromClient;

  @BeforeEach
  public void setUp() throws Exception {
    lenient().when(fafServerAccessor.getEvents(any())).thenReturn(Flux.never());
    lenient().when(fafServerAccessor.getEvents(ConnectToPeerGpgCommand.class)).thenReturn(connectToPeer.asFlux());
    lenient().when(fafServerAccessor.getEvents(IceMsgGpgCommand.class)).thenReturn(iceMsg.asFlux());
    lenient().when(fafServerAccessor.getEvents(DisconnectFromPeerGpgCommand.class)).thenReturn(disconnectFromPeer.asFlux());
    PlayerInfo player = mock(PlayerInfo.class);
    lenient().when(player.getUsername()).thenReturn("junit");
    lenient().when(player.getId()).thenReturn(5);
    lenient().when(playerService.getCurrentPlayer()).thenReturn(player);

    instance = new NativeIceAdapter(playerService, fafServerAccessor, Mappers.getMapper(IceServerMapper.class),
                                    new ObjectMapper(), gameFullNotifier, forgedAlliancePrefs);
    instance.afterPropertiesSet();

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

  private int readInt() throws IOException {
    return Integer.reverseBytes(fromClient.readInt());
  }

  private String readString() throws IOException {
    byte[] bytes = new byte[readInt()];
    fromClient.readFully(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private List<Object> readMessage() throws IOException {
    game.setSoTimeout(WAIT_MILLIS);
    List<Object> message = new ArrayList<>();
    message.add(readString());
    int argCount = readInt();
    for (int i = 0; i < argCount; i++) {
      message.add(fromClient.readByte() == 0 ? (Object) readInt() : readString());
    }
    return message;
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
    sendFromGame("GameState", "Lobby");
    verify(fafServerAccessor, timeout(WAIT_MILLIS)).sendGpgMessage(any());

    ConnectToPeerGpgCommand command = mock(ConnectToPeerGpgCommand.class);
    when(command.getUsername()).thenReturn("other");
    when(command.getPeerUid()).thenReturn(9);
    when(command.isOffer()).thenReturn(true);
    connectToPeer.tryEmitNext(command);

    assertEquals(List.of("ConnectToPeer", "other", 9, 1), readMessage());
  }

  @Test
  public void testIceMessageObjectIsSentAsJsonText() throws Exception {
    sendFromGame("GameState", "Lobby");
    verify(fafServerAccessor, timeout(WAIT_MILLIS)).sendGpgMessage(any());

    IceMsgGpgCommand command = mock(IceMsgGpgCommand.class);
    when(command.getSender()).thenReturn(9);
    when(command.getRecord()).thenReturn(java.util.Map.of("a", 1));
    iceMsg.tryEmitNext(command);

    assertEquals(List.of("IceMsg", 9, "{\"a\":1}"), readMessage());
  }

  private void connectToPeer(String login, int id, boolean offer) {
    ConnectToPeerGpgCommand command = mock(ConnectToPeerGpgCommand.class);
    when(command.getUsername()).thenReturn(login);
    when(command.getPeerUid()).thenReturn(id);
    when(command.isOffer()).thenReturn(offer);
    connectToPeer.tryEmitNext(command);
  }

  /** A game with two peers crashes, and the client gets ready to start it again. */
  private void crashWithTwoPeers() throws Exception {
    forgedAlliancePrefs.setAllowRejoin(true);
    sendFromGame("GameState", "Lobby");
    verify(fafServerAccessor, timeout(WAIT_MILLIS)).sendGpgMessage(any());
    connectToPeer("first", 9, true);
    assertEquals(List.of("ConnectToPeer", "first", 9, 1), readMessage());
    connectToPeer("second", 3, false);
    assertEquals(List.of("ConnectToPeer", "second", 3, 0), readMessage());
    sendFromGame("RejoinOffer", "1", "5=1,9=0,3=2");
    for (int i = 0; i < 500 && !instance.canRejoin(); i++) {
      Thread.sleep(10);
    }
    assertTrue(instance.canRejoin());
    gamePort = game.getPort();
    game.close();
  }

  private void gameConnectsAgain() throws IOException {
    game = new Socket(InetAddress.getLoopbackAddress(), gamePort);
    fromClient = new DataInputStream(game.getInputStream());
  }

  @Test
  public void testAGameComingBackGetsItsPeersAgainWithoutAskingForIdle() throws Exception {
    crashWithTwoPeers();

    assertEquals(List.of("/nativenet", "/rejoin", "9=0,3=2", "1", "0"), instance.rejoinArguments());
    gameConnectsAgain();

    assertEquals(List.of("IceServers", "[]"), readMessage());
    assertEquals(List.of("CreateLobby", 1, 0, "junit", 5, 1), readMessage());
    assertEquals(List.of("ConnectToPeer", "first", 9, 1), readMessage());
    assertEquals(List.of("ConnectToPeer", "second", 3, 0), readMessage());
  }

  @Test
  public void testAPeerWhoLeftIsNotAskedForAgain() throws Exception {
    DisconnectFromPeerGpgCommand left = mock(DisconnectFromPeerGpgCommand.class);
    when(left.getUid()).thenReturn(9);
    crashWithTwoPeers();
    disconnectFromPeer.tryEmitNext(left);

    assertEquals(List.of("/nativenet", "/rejoin", "3=2", "1", "0"), instance.rejoinArguments());
    gameConnectsAgain();

    assertEquals(List.of("IceServers", "[]"), readMessage());
    assertEquals(List.of("CreateLobby", 1, 0, "junit", 5, 1), readMessage());
    assertEquals(List.of("ConnectToPeer", "second", 3, 0), readMessage());
  }

  @Test
  public void testAGameNotAskedBackGetsNoPeers() throws Exception {
    crashWithTwoPeers();
    gameConnectsAgain();

    sendFromGame("GameState", "Idle");

    assertEquals(List.of("IceServers", "[]"), readMessage());
    assertEquals(List.of("CreateLobby", 1, 0, "junit", 5, 1), readMessage());
    assertThrows(SocketTimeoutException.class, this::shortRead);
  }

  private void shortRead() throws IOException {
    game.setSoTimeout(200);
    fromClient.readInt();
  }

  @Test
  public void testAnOfferIsNotForTheServer() throws Exception {
    sendFromGame("RejoinOffer", "1", "5=1,9=0");

    verify(fafServerAccessor, after(200).never()).sendGpgMessage(any());
  }

  @Test
  public void testNoOfferMeansNoReturn() {
    forgedAlliancePrefs.setAllowRejoin(true);

    assertFalse(instance.canRejoin());
    assertEquals(List.of(), instance.rejoinArguments());
  }

  @Test
  public void testNothingIsSentAfterStop() throws Exception {
    instance.stop();

    connectToPeer.tryEmitNext(mock(ConnectToPeerGpgCommand.class));

    verify(fafServerAccessor, never()).sendGpgMessage(any());
  }
}
