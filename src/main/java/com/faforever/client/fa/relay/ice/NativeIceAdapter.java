package com.faforever.client.fa.relay.ice;

import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fa.GameFullNotifier;
import com.faforever.client.mapstruct.IceServerMapper;
import com.faforever.client.player.PlayerService;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * The game's own network links: no adapter process, the client hosts GPGNet itself and passes messages between the game
 * and the lobby server. The contract is docs/LAUNCHER_NATIVE_NETWORK.md in faf-re.
 */
@Component
@Lazy
@Slf4j
@RequiredArgsConstructor
public class NativeIceAdapter implements IceAdapter, InitializingBean {

  private static final byte ARG_INT = 0;
  private static final byte ARG_STRING = 1;

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
  private GameType gameType;
  private ServerSocket listener;
  private Socket gameSocket;
  private OutputStream toGame;

  @Override
  public void afterPropertiesSet() {
    fafServerAccessor.getEvents(JoinGameGpgCommand.class)
                     .doOnNext(message -> send("JoinGame", message.getUsername(), message.getPeerUid()))
                     .doOnError(throwable -> log.warn("Unable to join game", throwable))
                     .retry()
                     .subscribe();
    fafServerAccessor.getEvents(HostGameGpgCommand.class)
                     .doOnNext(message -> send("HostGame", message.getMap()))
                     .doOnError(throwable -> log.warn("Unable to host game", throwable))
                     .retry()
                     .subscribe();
    fafServerAccessor.getEvents(ConnectToPeerGpgCommand.class)
                     .doOnNext(message -> send("ConnectToPeer", message.getUsername(), message.getPeerUid(),
                                               message.isOffer() ? 1 : 0))
                     .doOnError(throwable -> log.warn("Unable to connect to peer", throwable))
                     .retry()
                     .subscribe();
    fafServerAccessor.getEvents(DisconnectFromPeerGpgCommand.class)
                     .doOnNext(message -> send("DisconnectFromPeer", message.getUid()))
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
    return forceRelay ? List.of("/nativenet", "/nativenetrelay") : List.of("/nativenet");
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
    return CompletableFuture.supplyAsync(() -> {
      stop();
      try {
        listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
      } catch (IOException e) {
        throw new CompletionException("Unable to open the GPGNet port", e);
      }
      active = true;
      Thread reader = new Thread(this::serveGame, "NativeGpgNet");
      reader.setDaemon(true);
      reader.start();
      log.info("Native network: GPGNet on port {} for game {}", listener.getLocalPort(), gameId);
      return listener.getLocalPort();
    });
  }

  @Override
  public void stop() {
    active = false;
    closeQuietly(gameSocket);
    closeQuietly(listener);
    synchronized (sendLock) {
      toGame = null;
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

  private void serveGame() {
    ServerSocket openListener = listener;
    try (Socket socket = openListener.accept()) {
      socket.setTcpNoDelay(true);
      gameSocket = socket;
      synchronized (sendLock) {
        toGame = new BufferedOutputStream(socket.getOutputStream());
      }
      DataInputStream fromGame = new DataInputStream(socket.getInputStream());
      while (active) {
        String command = readString(fromGame);
        List<Object> args = new ArrayList<>();
        int argCount = readInt(fromGame);
        for (int i = 0; i < argCount; i++) {
          args.add(fromGame.readByte() == ARG_INT ? (Object) readInt(fromGame) : readString(fromGame));
        }
        log.debug("Message from game: '{}' '{}'", command, args);
        if ("GameState".equals(command) && args.contains("Idle")) {
          sendLobbySetup();
        }
        onGpgGameMessage(new GpgGameOutboundMessage(command, args, MessageTarget.GAME));
      }
    } catch (EOFException e) {
      log.info("Native network: the game closed GPGNet");
    } catch (IOException e) {
      if (active) {
        log.warn("Native network: GPGNet failed", e);
      }
    }
  }

  private void sendLobbySetup() {
    PlayerInfo player = playerService.getCurrentPlayer();
    send("IceServers", iceServersJson);
    send("CreateLobby", gameType == GameType.MATCHMAKER ? 1 : 0, 0, player.getUsername(), player.getId(), 1);
  }

  private void send(String command, Object... args) {
    synchronized (sendLock) {
      if (!active || toGame == null) {
        return;
      }
      try {
        writeString(toGame, command);
        writeInt(toGame, args.length);
        for (Object arg : args) {
          if (arg instanceof Number number) {
            toGame.write(ARG_INT);
            writeInt(toGame, number.intValue());
          } else {
            toGame.write(ARG_STRING);
            writeString(toGame, String.valueOf(arg));
          }
        }
        toGame.flush();
      } catch (IOException e) {
        log.warn("Native network: unable to send {} to the game", command, e);
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

  private static int readInt(DataInputStream in) throws IOException {
    return Integer.reverseBytes(in.readInt());
  }

  private static String readString(DataInputStream in) throws IOException {
    byte[] bytes = new byte[readInt(in)];
    in.readFully(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private static void writeInt(OutputStream out, int value) throws IOException {
    out.write(ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array());
  }

  private static void writeString(OutputStream out, String value) throws IOException {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    writeInt(out, bytes.length);
    out.write(bytes);
  }

  private static void closeQuietly(AutoCloseable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (Exception e) {
        log.debug("Native network: close failed", e);
      }
    }
  }
}
