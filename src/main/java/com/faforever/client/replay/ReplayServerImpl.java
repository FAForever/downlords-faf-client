package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.GameBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.user.UserService;
import com.faforever.commons.replay.ReplayMetadata;
import com.google.common.primitives.Bytes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Lazy
@Component
@Slf4j
@RequiredArgsConstructor
public class ReplayServerImpl implements ReplayServer {

  /**
   * Size for buffer used to send data to the live replay server. The buffer needs to be large enough to not flush too
   * many times (after all, it's a TCP stream so we don't want to send single bytes) but small enough to not delay data
   * for too long. I don't have any data right now but it can be expected that the replay stream produces about 70 bytes
   * per second (See #973).
   */
  private static final int REPLAY_BUFFER_SIZE = 128;

  /**
   * This is a prefix used in the FA live replay protocol that needs to be stripped away when storing to a file.
   */
  private static final byte[] LIVE_REPLAY_PREFIX = new byte[]{'P', '/'};

  private final ClientProperties clientProperties;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final UserService userService;
  private final ReplayFileWriter replayFileWriter;
  private final ClientUpdateService clientUpdateService;

  private ReplayMetadata replayInfo;
  private ServerSocket serverSocket;
  private boolean stoppedGracefully;

  /**
   * Returns the current millis the same way as python does since this is what's stored in the replay files *yay*.
   */
  private static double pythonTime() {
    return System.currentTimeMillis() / 1000;
  }

  @Override
  public void stop() throws IOException {
    if (serverSocket == null) {
      return;
    }
    stoppedGracefully = true;
    serverSocket.close();
  }

  @Override
  public CompletableFuture<Integer> start(int gameId, Supplier<GameBean> gameSupplier) {
    stoppedGracefully = false;
    CompletableFuture<Integer> future = new CompletableFuture<>();
    new Thread(() -> {
      String remoteReplayServerHost = clientProperties.getReplay().getRemoteHost();
      Integer remoteReplayServerPort = clientProperties.getReplay().getRemotePort();

      log.debug("Connecting to replay server at '{}:{}'", remoteReplayServerHost, remoteReplayServerPort);

      try (ServerSocket localSocket = new ServerSocket(0)) {
        log.debug("Opening local replay server on port {}", localSocket.getLocalPort());
        this.serverSocket = localSocket;
        future.complete(serverSocket.getLocalPort());

        try (Socket remoteReplayServerSocket = new Socket(remoteReplayServerHost, remoteReplayServerPort);
             DataOutputStream fafReplayOutputStream = new DataOutputStream(remoteReplayServerSocket.getOutputStream())) {
          recordAndRelay(gameId, localSocket, fafReplayOutputStream, gameSupplier);
        } catch (ConnectException e) {
          log.warn("Could not connect to remote replay server", e);
          notificationService.addNotification(new PersistentNotification(i18n.get("replayServer.unreachable"), Severity.WARN));
          recordAndRelay(gameId, localSocket, null, gameSupplier);
        }
      } catch (IOException e) {
        if (stoppedGracefully) {
          return;
        }
        future.completeExceptionally(e);
        log.warn("Error in replay server", e);
        notificationService.addNotification(new PersistentNotification(
            i18n.get("replayServer.listeningFailed"),
            Severity.WARN, Collections.singletonList(new Action(i18n.get("replayServer.retry"), event -> start(gameId, gameSupplier)))
        ));
      }
    }).start();
    return future;
  }

  private void initReplayInfo(int uid) {
    replayInfo = new ReplayMetadata();
    replayInfo.setUid(uid);
    replayInfo.setLaunchedAt(pythonTime());
    replayInfo.setVersionInfo(new HashMap<>());
    replayInfo.getVersionInfo().put("lobby",
        String.format("dfaf-%s", clientUpdateService.getCurrentVersion())
    );
  }

  /**
   * @param fafReplayOutputStream if {@code null}, the replay won't be relayed
   */
  private void recordAndRelay(int uid, ServerSocket serverSocket, @Nullable OutputStream fafReplayOutputStream, Supplier<GameBean> onGameInfoFinished) throws IOException {
    Socket socket = serverSocket.accept();
    GameBean game = onGameInfoFinished.get();
    log.debug("Accepted connection from {}", socket.getRemoteSocketAddress());

    initReplayInfo(uid);

    ByteArrayOutputStream replayData = new ByteArrayOutputStream();

    boolean connectionToServerLost = false;
    byte[] buffer = new byte[REPLAY_BUFFER_SIZE];
    try (InputStream inputStream = socket.getInputStream()) {
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        if (replayData.size() == 0 && Bytes.indexOf(buffer, LIVE_REPLAY_PREFIX) != -1) {
          int dataBeginIndex = Bytes.indexOf(buffer, (byte) 0x00) + 1;
          replayData.write(buffer, dataBeginIndex, bytesRead - dataBeginIndex);
        } else {
          replayData.write(buffer, 0, bytesRead);
        }

        if (!connectionToServerLost && fafReplayOutputStream != null) {
          try {
            fafReplayOutputStream.write(buffer, 0, bytesRead);
          } catch (SocketException e) {
            // In case we lose connection to the replay server, just stop writing to it
            log.warn("Connection to replay server lost ({})", e.getMessage());
            connectionToServerLost = true;
          }
        }
      }
    } catch (Exception e) {
      log.warn("Error while recording replay", e);
      throw e;
    }

    log.debug("FAF has disconnected, writing replay data to file");
    finishReplayInfo(game);
    replayFileWriter.writeReplayDataToFile(replayData, replayInfo);
  }

  private void finishReplayInfo(GameBean game) {
    replayInfo.setHost(game.getHost());
    replayInfo.setUid(game.getId());
    replayInfo.setTitle(game.getTitle());
    replayInfo.setMapname(game.getMapFolderName());
    replayInfo.setVictoryCondition(game.getVictoryCondition());
    replayInfo.setFeaturedMod(game.getFeaturedMod());
    replayInfo.setMaxPlayers(game.getMaxPlayers());
    replayInfo.setNumPlayers(game.getNumPlayers());
    replayInfo.setSimMods(game.getSimMods());
    replayInfo.setTeams(game.getTeams());
    replayInfo.setFeaturedModVersions(Map.of());
    replayInfo.setGameEnd(pythonTime());
    replayInfo.setRecorder(userService.getUsername());
    // TODO: Use enum when setter is fixed in java commons
    replayInfo.setState("closed");
    replayInfo.setComplete(true);
  }
}
