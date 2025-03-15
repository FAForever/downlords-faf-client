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
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.commons.lobby.ConnectToPeerGpgCommand;
import com.faforever.commons.lobby.DisconnectFromPeerGpgCommand;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameType;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import com.faforever.commons.lobby.HostGameGpgCommand;
import com.faforever.commons.lobby.JoinGameGpgCommand;
import com.faforever.commons.lobby.MessageTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;


@Slf4j
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class GPGNetServer {

  private final PlayerService playerService;
  private final FafServerAccessor fafServerAccessor;
  private final GameFullNotifier gameFullNotifier;

  private boolean stoppedGracefully = false;
  private ServerSocket serverSocket;

  public int start() {
    if (serverSocket != null && !serverSocket.isClosed()) {
      return serverSocket.getLocalPort();
    }

    try {
      serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    } catch (IOException exception) {
      log.warn("Unable to start gpg net server");
      throw new RuntimeException(exception);
    }
    return serverSocket.getLocalPort();
  }

  public void stop() {
    if (serverSocket == null) {
      return;
    }
    try {
      serverSocket.close();
      stoppedGracefully = true;
    } catch (IOException exception) {
      log.warn("Unable to stop replay server");
      stoppedGracefully = false;
    }
  }


  @Slf4j
  private class GPGNetClient implements AutoCloseable {
    private final Thread listenerThread;

    private final FaDataWriter faDataWriter;
    private final FaDataReader faDataReader;
    private final CountDownLatch lobbyLatch = new CountDownLatch(1);
    private final ReentrantLock sendLock = new ReentrantLock(true);
    private final Socket socket;

    private LobbyInitMode lobbyInitMode = LobbyInitMode.NORMAL;

    private GPGNetClient(Socket socket) throws IOException {
      this.socket = socket;
      faDataWriter = new FaDataWriter(socket.getOutputStream());
      faDataReader = new FaDataReader(socket.getInputStream());

      listenerThread = Thread.startVirtualThread(this::processMessages);
      listenerThread.start();

      fafServerAccessor.getEvents(JoinGameGpgCommand.class)
                       .map(message -> new JoinGame(message.getUsername(), message.getPeerUid()))
                       .doOnNext(this::sendGPGNetMessage)
                       .doOnError(throwable -> log.warn("Unable to join game", throwable))
                       .retry()
                       .subscribe();
      fafServerAccessor.getEvents(HostGameGpgCommand.class)
                       .map(message -> new HostGame(message.getMap()))
                       .doOnNext(this::sendGPGNetMessage)
                       .doOnError(throwable -> log.warn("Unable to host game", throwable))
                       .retry()
                       .subscribe();
      fafServerAccessor.getEvents(ConnectToPeerGpgCommand.class)
                       .map(message -> new ConnectToPeer(message.getUsername(), message.getPeerUid()))
                       .doOnNext(this::sendGPGNetMessage)
                       .doOnError(throwable -> log.warn("Unable to connect to peer", throwable))
                       .retry()
                       .subscribe();
      fafServerAccessor.getEvents(DisconnectFromPeerGpgCommand.class)
                       .map(message -> new DisconnectFromPeer(message.getUid()))
                       .doOnNext(this::sendGPGNetMessage)
                       .doOnError(throwable -> log.warn("Unable to disconnect from peer", throwable))
                       .retry()
                       .subscribe();
      fafServerAccessor.getEvents(GameLaunchResponse.class)
                       .map(GameLaunchResponse::getGameType)
                       .map(gameType -> gameType == GameType.MATCHMAKER ? LobbyInitMode.AUTO : LobbyInitMode.NORMAL)
                       .doOnNext(initMode -> lobbyInitMode = initMode)
                       .doOnError(throwable -> log.warn("Unable to update game type", throwable))
                       .retry()
                       .subscribe();
    }

    /**
     * Process an incoming message from FA
     */
    private void processGpgnetMessage(GPGMessage message) {
      log.debug("Processing GPGNet message: {}", message);
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
        sendLock.lockInterruptibly();
        try {
          log.debug("Sending GPGNet message: {}", message);
          faDataWriter.writeMessage(message);
        } catch (IOException e) {
          log.error("Error while communicating with FA (output), assuming shutdown", e);
        } finally {
          sendLock.unlock();
        }
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
          GPGMessage message = faDataReader.readMessage();
          processGpgnetMessage(message);
        } catch (IOException e) {
          log.error("Error while communicating with FA (input), assuming shutdown", e);
          break;
        }
      }
      log.debug("No longer listening for GPGPNET from FA");
    }

    public void close() throws IOException {
      this.listenerThread.interrupt();
      log.info("Closing GPGNetClient");
      socket.close();
    }
  }


}
