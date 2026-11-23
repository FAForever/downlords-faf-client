package com.faforever.client.fa.relay.ice;

import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fa.GameFullNotifier;
import com.faforever.client.fa.RebuiltEngineExecutable;
import com.faforever.client.mapstruct.IceServerMapper;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.commons.api.dto.CoturnServer;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameType;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
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
import java.util.concurrent.CompletableFuture;

/**
 * The game's own network links: no adapter process, the client hosts GPGNet itself and passes messages between the game
 * and the lobby server.
 */
@Component
@Lazy
@Slf4j
@RequiredArgsConstructor
public class NativeIceAdapter implements IceAdapter {

  private static final byte ARG_INT = 0;
  private static final byte ARG_STRING = 1;
  // Far above any GPGNet message; a larger length means the stream is not GPGNet.
  private static final int MAX_FIELD_BYTES = 16 * 1024 * 1024;
  // CreateLobby's first argument, as the Java adapter's LobbyInitMode ids: the normal lobby screen, or none for a
  // matchmaker game.
  private static final int LOBBY_INIT_NORMAL = 0;
  private static final int LOBBY_INIT_AUTO = 1;
  // CreateLobby's lobby port: the game binds a free one itself.
  private static final int LOBBY_PORT_ANY = 0;
  // CreateLobby's last argument, which the Java adapter also sends as 1.
  private static final int LOBBY_NAT_TRAVERSAL = 1;
  // ConnectToPeer's last argument: this side offers the connection, or answers the other side's offer.
  private static final int CONNECT_OFFER = 1;
  private static final int CONNECT_ANSWER = 0;

  private final PlayerService playerService;
  private final FafServerAccessor fafServerAccessor;
  private final IceServerMapper iceServerMapper;
  private final ObjectMapper objectMapper;
  @Lazy
  private final GameFullNotifier gameFullNotifier;

  private final Object sendLock = new Object();
  private volatile boolean active;
  private volatile boolean forceRelay;
  private volatile String iceServersJson = "[]";
  private volatile DisposableServer server;
  private volatile Connection gameConnection;
  private GameType gameType;
  private Sinks.Many<byte[]> toGame;

  @Override
  public void hostGame(String mapName) {
    send("HostGame", mapName);
  }

  @Override
  public void joinGame(String remotePlayerLogin, int remotePlayerId) {
    send("JoinGame", remotePlayerLogin, remotePlayerId);
  }

  @Override
  public void connectToPeer(String remotePlayerLogin, int remotePlayerId, boolean offer) {
    send("ConnectToPeer", remotePlayerLogin, remotePlayerId, offer ? CONNECT_OFFER : CONNECT_ANSWER);
  }

  @Override
  public void disconnectFromPeer(int remotePlayerId) {
    send("DisconnectFromPeer", remotePlayerId);
  }

  @Override
  public void iceMsg(int remotePlayerId, Object message) {
    send("IceMsg", remotePlayerId, iceMessageText(message));
  }

  @Override
  public void prepare(GameLaunchResponse gameLaunchResponse) {
    gameType = gameLaunchResponse.getGameType();
  }

  @Override
  public List<String> gameArguments() {
    List<String> arguments = new ArrayList<>(forceRelay ? List.of("/nativenet", "/nativenetrelay") : List.of("/nativenet"));
    arguments.addAll(RebuiltEngineExecutable.TAKES_RETURNING_PLAYERS);
    return arguments;
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
    fafServerAccessor.sendGpgMessage(message);
  }

  @Override
  public CompletableFuture<Integer> start(int gameId, boolean forceRelay) {
    this.forceRelay = forceRelay;
    stop();
    active = true;
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

  /**
   * One game's connection. The newest one is the game: it closes the one before, and what the one before still sends is
   * dropped, so it can't get a lobby set up on the new connection.
   */
  private Publisher<Void> serveGame(NettyInbound inbound, NettyOutbound outbound) {
    Sinks.Many<byte[]> toThisGame = Sinks.many().unicast().onBackpressureBuffer();

    Mono<Void> fromGame = inbound.withConnection(connection -> {
                                   connection.addHandlerLast("gpgnet", new GpgNetDecoder());
                                   Connection replaced = becomeTheGame(connection, toThisGame);
                                   if (replaced != null && replaced != connection) {
                                     log.info("Native network: a new game connection replaces the previous one");
                                     replaced.dispose();
                                   }
                                 })
                                 .receiveObject()
                                 .cast(GpgGameOutboundMessage.class)
                                 .doOnNext(message -> onMessageFromGame(message, toThisGame))
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

  /** Makes this connection the game's; returns the connection it replaces, if any. */
  private Connection becomeTheGame(Connection connection, Sinks.Many<byte[]> toThisGame) {
    synchronized (sendLock) {
      Connection replaced = gameConnection;
      gameConnection = connection;
      toGame = toThisGame;
      return replaced;
    }
  }

  private boolean isTheGame(Sinks.Many<byte[]> toThisGame) {
    synchronized (sendLock) {
      return toGame == toThisGame;
    }
  }

  private void onMessageFromGame(GpgGameOutboundMessage message, Sinks.Many<byte[]> from) {
    if (!isTheGame(from)) {
      log.debug("Dropping a message from a replaced game connection: '{}' '{}'", message.getCommand(),
                message.getArgs());
      return;
    }
    log.debug("Message from game: '{}' '{}'", message.getCommand(), message.getArgs());
    if ("GameState".equals(message.getCommand()) && message.getArgs().contains("Idle")) {
      sendLobbySetup(from);
    }
    onGpgGameMessage(message);
  }

  /** To the connection that went Idle, and only while it is still the game's. */
  private void sendLobbySetup(Sinks.Many<byte[]> to) {
    PlayerInfo player = playerService.getCurrentPlayer();
    sendTo(to, "IceServers", iceServersJson);
    sendTo(to, "CreateLobby", gameType == GameType.MATCHMAKER ? LOBBY_INIT_AUTO : LOBBY_INIT_NORMAL, LOBBY_PORT_ANY,
           player.getUsername(), player.getId(), LOBBY_NAT_TRAVERSAL);
  }

  /** To the game's current connection. */
  private void send(String command, Object... args) {
    sendTo(null, command, args);
  }

  /** To the game's current connection; with {@code to}, only while that is still it. */
  private void sendTo(Sinks.Many<byte[]> to, String command, Object... args) {
    byte[] frame = encode(command, args);
    synchronized (sendLock) {
      if (!active || toGame == null || to != null && toGame != to) {
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
  static final class GpgNetDecoder extends ByteToMessageDecoder {

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
