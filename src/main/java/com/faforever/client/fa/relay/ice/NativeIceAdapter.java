package com.faforever.client.fa.relay.ice;

import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fa.GameFullNotifier;
import com.faforever.client.mapstruct.IceServerMapper;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.commons.api.dto.CoturnServer;
import com.faforever.commons.lobby.ConnectToPeerGpgCommand;
import com.faforever.commons.lobby.DisconnectFromPeerGpgCommand;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameType;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import com.faforever.commons.lobby.HostGameGpgCommand;
import com.faforever.commons.lobby.IceMsgGpgCommand;
import com.faforever.commons.lobby.JoinGameGpgCommand;
import com.faforever.commons.lobby.MessageTarget;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;
import reactor.netty.tcp.TcpServer;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The game's own network links: no adapter process, the client hosts GPGNet itself and passes messages between the game
 * and the lobby server.
 */
@Component
@Lazy
@Slf4j
@RequiredArgsConstructor
public class NativeIceAdapter implements IceAdapter, InitializingBean {

  private static final byte ARG_INT = 0;
  private static final byte ARG_STRING = 1;
  // Far above any GPGNet message; a larger length means the stream is not GPGNet.
  private static final int MAX_FIELD_BYTES = 16 * 1024 * 1024;

  private final PlayerService playerService;
  private final FafServerAccessor fafServerAccessor;
  private final IceServerMapper iceServerMapper;
  private final ObjectMapper objectMapper;
  @Lazy
  private final GameFullNotifier gameFullNotifier;
  private final DataPrefs dataPrefs;

  private final Object sendLock = new Object();
  // What the server said about this game's peers. It says it once, and a game started again needs it once more.
  private final List<PeerMessage> peerMessages = new CopyOnWriteArrayList<>();
  private volatile RejoinOffer rejoinOffer;
  private volatile boolean comingBack;
  private volatile boolean active;
  private volatile boolean forceRelay;
  private volatile String iceServersJson = "[]";
  private volatile DisposableServer server;
  private volatile Connection gameConnection;
  private GameType gameType;
  private Sinks.Many<byte[]> toGame;

  @Override
  public void afterPropertiesSet() {
    fafServerAccessor.getEvents(JoinGameGpgCommand.class)
                     .doOnNext(message -> rememberAndSend("JoinGame", message.getPeerUid(), message.getUsername(),
                                                          message.getPeerUid()))
                     .doOnError(throwable -> log.warn("Unable to join game", throwable))
                     .retry()
                     .subscribe();
    fafServerAccessor.getEvents(HostGameGpgCommand.class)
                     .doOnNext(message -> send("HostGame", message.getMap()))
                     .doOnError(throwable -> log.warn("Unable to host game", throwable))
                     .retry()
                     .subscribe();
    fafServerAccessor.getEvents(ConnectToPeerGpgCommand.class)
                     .doOnNext(message -> rememberAndSend("ConnectToPeer", message.getPeerUid(),
                                                          message.getUsername(), message.getPeerUid(),
                                                          message.isOffer() ? 1 : 0))
                     .doOnError(throwable -> log.warn("Unable to connect to peer", throwable))
                     .retry()
                     .subscribe();
    fafServerAccessor.getEvents(DisconnectFromPeerGpgCommand.class)
                     .doOnNext(message -> {
                       peerMessages.removeIf(known -> known.peerId() == message.getUid());
                       send("DisconnectFromPeer", message.getUid());
                     })
                     .doOnError(throwable -> log.warn("Unable to disconnect from peer", throwable))
                     .retry()
                     .subscribe();
    fafServerAccessor.getEvents(IceMsgGpgCommand.class)
                     .doOnNext(message -> send("IceMsg", message.getSender(), iceMessageText(message.getRecord())))
                     .doOnError(throwable -> log.warn("Unable to ice msg", throwable))
                     .retry()
                     .subscribe();
  }

  @Override
  public void prepare(GameLaunchResponse gameLaunchResponse) {
    gameType = gameLaunchResponse.getGameType();
  }

  @Override
  public List<String> gameArguments() {
    List<String> arguments = new ArrayList<>(forceRelay ? List.of("/nativenet", "/nativenetrelay") : List.of("/nativenet"));
    arguments.addAll(rejoinOfferArguments());
    return arguments;
  }

  /**
   * What the game needs to come back to its match after a crash: to say it can, and where its key for its seat is. The
   * key only ever signs a challenge from the other players, and it has nothing to do with the player's account. The game
   * offers a return only when every player's engine can take one.
   */
  private List<String> rejoinOfferArguments() {
    return List.of("/rejoinoffer", "/rejoinkeyfile", dataPrefs.getBaseDataDirectory().resolve("rejoin_seat.key").toString(),
                   "/con", "net_PeerFeatures=1;net_RejoinAccept=1");
  }

  @Override
  public void onIceAdapterStateChanged(String newState) {
    // The game has no adapter state to report; its link states are in the game log.
  }

  @Override
  public void onGpgGameMessage(GpgGameOutboundMessage message) {
    if ("GameFull".equals(message.getCommand())) {
      gameFullNotifier.onGameFull();
      return;
    }
    if ("RejoinOffer".equals(message.getCommand())) {
      List<Object> args = message.getArgs();
      rejoinOffer = RejoinOffer.parse(String.valueOf(args.get(0)), String.valueOf(args.get(1)));
      return;
    }
    fafServerAccessor.sendGpgMessage(message);
  }

  @Override
  public boolean canRejoin() {
    return rejoinOffer != null;
  }

  /**
   * The peers by player id: the game's links died with it, and it builds new ones from what it gets when it connects.
   */
  @Override
  public List<String> rejoinArguments() {
    RejoinOffer offer = rejoinOffer;
    if (offer == null) {
      return List.of();
    }
    StringJoiner peers = new StringJoiner(",");
    for (PeerMessage known : peerMessages) {
      Integer source = offer.sourceByPlayerId().get(known.peerId());
      if (source != null) {
        peers.add(known.peerId() + "=" + source);
      }
    }
    if (peers.length() == 0) {
      return List.of();
    }
    comingBack = true;
    List<String> arguments = new ArrayList<>(gameArguments());
    arguments.addAll(List.of("/rejoin", peers.toString(), String.valueOf(offer.localSource()), "0"));
    return arguments;
  }

  @Override
  public CompletableFuture<Integer> start(int gameId, boolean forceRelay) {
    this.forceRelay = forceRelay;
    stop();
    peerMessages.clear();
    rejoinOffer = null;
    comingBack = false;
    active = true;
    // The server stays up for the whole match, so a game started again after a crash connects to the same port.
    return TcpServer.create()
                    .bindAddress(() -> new InetSocketAddress(InetAddress.getLoopbackAddress(), 0))
                    .doOnBound(bound -> {
                      server = bound;
                      log.info("Native network: GPGNet on port {} for game {}", bound.port(), gameId);
                    })
                    .doOnUnbound(bound -> log.debug("Native network: GPGNet port {} closed", bound.port()))
                    .handle(this::serveGame)
                    .bind()
                    .map(DisposableServer::port)
                    .toFuture();
  }

  @Override
  public void stop() {
    active = false;
    synchronized (sendLock) {
      if (toGame != null) {
        toGame.tryEmitComplete();
        toGame = null;
      }
    }
    Connection connection = gameConnection;
    if (connection != null) {
      connection.dispose();
    }
    DisposableServer openServer = server;
    if (openServer != null) {
      openServer.dispose();
    }
  }

  @Override
  public void setIceServers(Collection<CoturnServer> coturnServers) {
    try {
      iceServersJson = objectMapper.writeValueAsString(iceServerMapper.map(coturnServers));
    } catch (JsonProcessingException e) {
      log.warn("Unable to write the ICE server list", e);
      return;
    }
    send("IceServers", iceServersJson);
  }

  /** One game's connection. The newest one is the game: a game started again after a crash takes over. */
  private Publisher<Void> serveGame(NettyInbound inbound, NettyOutbound outbound) {
    Sinks.Many<byte[]> toThisGame = Sinks.many().unicast().onBackpressureBuffer();
    synchronized (sendLock) {
      toGame = toThisGame;
    }
    if (comingBack) {
      comingBack = false;
      // It reports no lobby states, as the server would take them for a new lobby, so this can't wait for Idle.
      sendLobbySetup();
      peerMessages.forEach(known -> send(known.command(), known.args()));
    }

    Mono<Void> fromGame = inbound.withConnection(connection -> {
                                   gameConnection = connection;
                                   connection.addHandlerLast("gpgnet", new GpgNetDecoder());
                                 })
                                 .receiveObject()
                                 .cast(GpgGameOutboundMessage.class)
                                 .doOnNext(this::onMessageFromGame)
                                 .doOnComplete(() -> log.info("Native network: the game closed GPGNet"))
                                 .doOnError(throwable -> {
                                   if (active) {
                                     log.warn("Native network: GPGNet failed", throwable);
                                   }
                                 })
                                 .onErrorComplete()
                                 .doFinally(_ -> {
                                   synchronized (sendLock) {
                                     if (Objects.equals(toGame, toThisGame)) {
                                       toGame = null;
                                     }
                                   }
                                   toThisGame.tryEmitComplete();
                                 })
                                 .then();

    return Mono.when(fromGame, outbound.sendByteArray(toThisGame.asFlux()).then());
  }

  private void onMessageFromGame(GpgGameOutboundMessage message) {
    log.debug("Message from game: '{}' '{}'", message.getCommand(), message.getArgs());
    if ("GameState".equals(message.getCommand()) && message.getArgs().contains("Idle")) {
      sendLobbySetup();
    }
    onGpgGameMessage(message);
  }

  private void rememberAndSend(String command, int peerId, Object... args) {
    if (active) {
      peerMessages.removeIf(known -> known.peerId() == peerId);
      peerMessages.add(new PeerMessage(command, peerId, args));
    }
    send(command, args);
  }

  private void sendLobbySetup() {
    PlayerInfo player = playerService.getCurrentPlayer();
    send("IceServers", iceServersJson);
    send("CreateLobby", gameType == GameType.MATCHMAKER ? 1 : 0, 0, player.getUsername(), player.getId(), 1);
  }

  private void send(String command, Object... args) {
    byte[] frame = encode(command, args);
    synchronized (sendLock) {
      if (!active || toGame == null) {
        return;
      }
      Sinks.EmitResult result = toGame.tryEmitNext(frame);
      if (result.isFailure()) {
        log.warn("Native network: unable to send {} to the game: {}", command, result);
      }
    }
  }

  private String iceMessageText(Object record) {
    if (record instanceof String text) {
      return text;
    }
    try {
      return objectMapper.writeValueAsString(record);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Unable to write an ICE message", e);
    }
  }

  private record PeerMessage(String command, int peerId, Object[] args) {}

  /** A GPGNet message: little-endian lengths and ints, each argument tagged as an int or a string. */
  private static byte[] encode(String command, Object... args) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    writeString(out, command);
    writeInt(out, args.length);
    for (Object arg : args) {
      if (arg instanceof Number number) {
        out.write(ARG_INT);
        writeInt(out, number.intValue());
      } else {
        out.write(ARG_STRING);
        writeString(out, String.valueOf(arg));
      }
    }
    return out.toByteArray();
  }

  private static void writeInt(ByteArrayOutputStream out, int value) {
    out.writeBytes(ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array());
  }

  private static void writeString(ByteArrayOutputStream out, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    writeInt(out, bytes.length);
    out.writeBytes(bytes);
  }

  /** Cuts the game's byte stream into GPGNet messages; a message split across reads waits for the rest. */
  private static final class GpgNetDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf in, List<Object> out) {
      if (frameEnd(in) < 0) {
        return;
      }
      String command = readString(in);
      int argCount = in.readIntLE();
      List<Object> args = new ArrayList<>(argCount);
      for (int i = 0; i < argCount; i++) {
        args.add(in.readByte() == ARG_INT ? (Object) in.readIntLE() : readString(in));
      }
      out.add(new GpgGameOutboundMessage(command, args, MessageTarget.GAME));
    }

    /** Where the next whole message ends, or -1 while part of it has yet to arrive. */
    private static int frameEnd(ByteBuf in) {
      int end = in.writerIndex();
      int at = stringEnd(in, in.readerIndex(), end);
      if (at < 0 || end - at < Integer.BYTES) {
        return -1;
      }
      int argCount = checkedLength(in.getIntLE(at));
      at += Integer.BYTES;
      for (int i = 0; i < argCount && at >= 0; i++) {
        if (at >= end) {
          return -1;
        }
        byte type = in.getByte(at);
        at = type == ARG_INT ? fixedEnd(at + 1, end) : stringEnd(in, at + 1, end);
      }
      return at;
    }

    private static int fixedEnd(int at, int end) {
      return end - at >= Integer.BYTES ? at + Integer.BYTES : -1;
    }

    private static int stringEnd(ByteBuf in, int at, int end) {
      if (end - at < Integer.BYTES) {
        return -1;
      }
      int length = checkedLength(in.getIntLE(at));
      long next = (long) at + Integer.BYTES + length;
      return next <= end ? (int) next : -1;
    }

    private static int checkedLength(int length) {
      if (length < 0 || length > MAX_FIELD_BYTES) {
        throw new CorruptedFrameException("Not a GPGNet message: length " + length);
      }
      return length;
    }

    private static String readString(ByteBuf in) {
      int length = in.readIntLE();
      String text = in.toString(in.readerIndex(), length, StandardCharsets.UTF_8);
      in.skipBytes(length);
      return text;
    }
  }
}
