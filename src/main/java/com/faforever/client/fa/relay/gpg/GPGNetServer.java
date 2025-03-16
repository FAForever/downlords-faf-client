package com.faforever.client.fa.relay.gpg;

import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fa.GameFullNotifier;
import com.faforever.client.fa.relay.gpg.GPGMessage.ConnectToPeer;
import com.faforever.client.fa.relay.gpg.GPGMessage.CreateLobby;
import com.faforever.client.fa.relay.gpg.GPGMessage.DisconnectFromPeer;
import com.faforever.client.fa.relay.gpg.GPGMessage.GameFull;
import com.faforever.client.fa.relay.gpg.GPGMessage.HostGame;
import com.faforever.client.fa.relay.gpg.GPGMessage.JoinGame;
import com.faforever.client.fa.relay.gpg.GameState.Known;
import com.faforever.client.fa.relay.ice.IceAdapter;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.commons.lobby.ConnectToPeerGpgCommand;
import com.faforever.commons.lobby.DisconnectFromPeerGpgCommand;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import com.faforever.commons.lobby.HostGameGpgCommand;
import com.faforever.commons.lobby.JoinGameGpgCommand;
import com.faforever.commons.lobby.MessageTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;


@Slf4j
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class GPGNetServer {

  private final PlayerService playerService;
  private final FafServerAccessor fafServerAccessor;
  @Lazy
  private final GameFullNotifier gameFullNotifier;
  private final IceAdapter iceAdapter;

  private ServerSocket serverSocket;
  private GPGNetClient activeClient;

  public CompletableFuture<Integer> start(int gameId, LobbyInitMode lobbyInitMode) {
    if (serverSocket == null || serverSocket.isClosed()) {
      try {
        serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
      } catch (IOException exception) {
        log.warn("Unable to start gpg net server");
        throw new RuntimeException(exception);
      }
    }

    if (activeClient != null) {
      activeClient.stop();
    }

    int gpgPort = iceAdapter.start(gameId, serverSocket.getLocalPort());

    return CompletableFuture.runAsync(() -> {
      try {
        activeClient = new GPGNetClient(serverSocket.accept(), lobbyInitMode);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }).thenApply(aVoid -> gpgPort);
  }

  public void stop() {
    if (serverSocket != null) {
      try {
        serverSocket.close();
      } catch (IOException exception) {
        log.warn("Unable to stop gpgnet server");
      }
    }

    if (activeClient != null) {
      activeClient.stop();
      activeClient = null;
    }
  }

  private class GPGNetClient {
    private static final Logger log = LoggerFactory.getLogger(GPGNetClient.class);

    private final Thread processorThread;
    private final Thread senderThread;

    private final GPGNetWriter GPGNetWriter;
    private final GPGNetReader GPGNetReader;
    private final Disposable messageDisposable;
    private final Queue<GPGMessage> messageQueue = new ConcurrentLinkedQueue<>();
    private final CountDownLatch lobbyLatch = new CountDownLatch(1);
    private final Socket socket;

    private final LobbyInitMode lobbyInitMode;

    private GPGNetClient(Socket socket, LobbyInitMode lobbyInitMode) throws IOException {
      this.socket = socket;
      this.lobbyInitMode = lobbyInitMode;
      GPGNetWriter = new GPGNetWriter(socket.getOutputStream());
      GPGNetReader = new GPGNetReader(socket.getInputStream());

      processorThread = Thread.startVirtualThread(this::processMessages);
      senderThread = Thread.startVirtualThread(this::sendMessages);

      Flux<JoinGame> joinMessages = fafServerAccessor.getEvents(JoinGameGpgCommand.class)
                                                     .map(message -> new JoinGame(message.getUsername(),
                                                                                  message.getPeerUid()));
      Flux<HostGame> hostMessages = fafServerAccessor.getEvents(HostGameGpgCommand.class)
                                                     .map(message -> new HostGame(message.getMap()));
      Flux<ConnectToPeer> connectMessages = fafServerAccessor.getEvents(ConnectToPeerGpgCommand.class)
                                                             .map(message -> new ConnectToPeer(message.getUsername(),
                                                                                               message.getPeerUid()));
      Flux<DisconnectFromPeer> disconnectMessages = fafServerAccessor.getEvents(DisconnectFromPeerGpgCommand.class)
                                                                     .map(message -> new DisconnectFromPeer(
                                                                         message.getUid()));

      messageDisposable = Flux.merge(joinMessages, hostMessages, connectMessages, disconnectMessages)
                              .doOnNext(messageQueue::offer)
                              .doOnError(throwable -> log.warn("Unable to queue message", throwable))
                              .retry()
                              .subscribe();
    }

    /**
     * Process an incoming message from FA
     */
    private void processGPGNetMessage(GPGMessage message) {
      log.trace("Received GPGNet message: {}", message);
      switch (message) {
        case GPGMessage.GameState(GameState state) when state == Known.IDLE -> {
          PlayerInfo currentPlayer = playerService.getCurrentPlayer();
          sendGPGNetMessage(new CreateLobby(lobbyInitMode, 0, currentPlayer.getUsername(), currentPlayer.getId()));
        }
        case GPGMessage.GameState(GameState state) when state == Known.LOBBY -> lobbyLatch.countDown();
        case GameFull() -> gameFullNotifier.onGameFull();
        default -> {}
      }

      fafServerAccessor.sendGpgMessage(
          new GpgGameOutboundMessage(message.command(), message.arguments(), MessageTarget.GAME));
    }

    /**
     * Send a message to this FA instance via GPGNet
     */
    public void sendGPGNetMessage(GPGMessage message) {
      if (!(message instanceof CreateLobby)) {
        try {
          lobbyLatch.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      try {
        log.trace("Sending GPGNet message: {}", message);
        GPGNetWriter.writeMessage(message);
      } catch (IOException e) {
        log.error("Error while communicating with FA (output), assuming shutdown", e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    /**
     * Listens for incoming messages from FA
     */
    private void processMessages() {
      log.debug("Listening for GPG messages");

      while (!Thread.interrupted()) {
        try {
          GPGMessage message = GPGNetReader.readMessage();
          processGPGNetMessage(message);
        } catch (EOFException e) {
          log.info("Lost connection to FA, shutting down");
          stop();
          break;
        } catch (IOException e) {
          log.error("Error while communicating with FA (input)", e);
        }
      }
      log.debug("No longer listening for GPGPNET from FA");
    }

    /**
     * Sends for outgoing messages to FA
     */
    private void sendMessages() {
      log.debug("Sending GPG messages");

      while (!Thread.interrupted()) {
        try {
          GPGMessage message = messageQueue.poll();
          if (message == null) {
            continue;
          }

          GPGNetWriter.writeMessage(message);
        } catch (IOException e) {
          log.error("Error while communicating with FA (output)", e);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      log.debug("No longer sending from GPGPNET to FA");
    }

    public void stop() {
      log.info("Closing GPGNetClient");
      processorThread.interrupt();
      senderThread.interrupt();
      messageDisposable.dispose();
      try {
        socket.close();
      } catch (IOException e) {
        log.warn("Unable to close socket", e);
      }
    }
  }
}
