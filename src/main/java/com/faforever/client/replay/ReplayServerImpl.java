package com.faforever.client.replay;

import com.faforever.client.game.Game;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.remote.domain.GameState;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.user.UserService;
import com.google.common.primitives.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static com.github.nocatch.NoCatch.noCatch;

public class ReplayServerImpl implements ReplayServer {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int REPLAY_BUFFER_SIZE = 0x200;

  /**
   * This is a prefix used in the FA live replay protocol that needs to be stripped away when storing to a file.
   */
  private static final byte[] LIVE_REPLAY_PREFIX = new byte[]{'P', '/'};

  @Resource
  Environment environment;
  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;
  @Resource
  GameService gameService;
  @Resource
  UserService userService;
  @Resource
  ReplayFileWriter replayFileWriter;
  @Resource
  ClientUpdateService clientUpdateService;

  private LocalReplayInfo replayInfo;
  private ServerSocket serverSocket;
  private boolean stoppedGracefully;

  /**
   * Returns the current millis the same way as python does since this is what's stored in the replay files *yay*.
   */
  private static double pythonTime() {
    return System.currentTimeMillis() / 1000;
  }

  @Override
  public void stop() {
    if (serverSocket == null) {
      throw new IllegalStateException("Server has never been started");
    }
    stoppedGracefully = true;
    noCatch(() -> serverSocket.close());
  }

  @Override
  public CompletableFuture<Void> start(int uid) {
    stoppedGracefully = false;
    CompletableFuture<Void> future = new CompletableFuture<>();
    new Thread(() -> {
      Integer localReplayServerPort = environment.getProperty("localReplayServer.port", Integer.class);
      String fafReplayServerHost = environment.getProperty("fafReplayServer.host");
      Integer fafReplayServerPort = environment.getProperty("fafReplayServer.port", Integer.class);

      logger.debug("Opening local replay server on port {}", localReplayServerPort);

      try (ServerSocket serverSocket = new ServerSocket(localReplayServerPort);
           Socket fafReplayServerSocket = new Socket(fafReplayServerHost, fafReplayServerPort)) {
        this.serverSocket = serverSocket;
        future.complete(null);
        recordAndRelay(uid, serverSocket, new BufferedOutputStream(fafReplayServerSocket.getOutputStream()));
      } catch (IOException e) {
        if (stoppedGracefully) {
          return;
        }
        future.completeExceptionally(e);
        logger.warn("Error in replay server", e);
        notificationService.addNotification(new PersistentNotification(
                i18n.get("replayServer.listeningFailed", localReplayServerPort),
            Severity.WARN, Collections.singletonList(new Action(i18n.get("replayServer.retry"), event -> start(uid)))
            )
        );
      }
    }).start();
    return future;
  }

  private void initReplayInfo(int uid) {
    replayInfo = new LocalReplayInfo();
    replayInfo.setUid(uid);
    replayInfo.setLaunchedAt(pythonTime());
    replayInfo.setVersionInfo(new HashMap<>());
    replayInfo.getVersionInfo().put("lobby",
        String.format("dfaf-%s", clientUpdateService.getCurrentVersion().getCanonical())
    );
  }

  private void recordAndRelay(int uid, ServerSocket serverSocket, OutputStream fafReplayOutputStream) throws IOException {
    Socket socket = serverSocket.accept();
    logger.debug("Accepted connection from {}", socket.getRemoteSocketAddress());

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

        if (!connectionToServerLost) {
          try {
            fafReplayOutputStream.write(buffer, 0, bytesRead);
          } catch (SocketException e) {
            // In case we lose connection to the replay server, just stop writing to it
            logger.warn("Connection to replay server lost ({})", e.getMessage());
            connectionToServerLost = true;
          }
        }
      }
    } catch (Exception e) {
      logger.warn("Error while recording replay", e);
      throw e;
    } finally {
      try {
        fafReplayOutputStream.flush();
      } catch (IOException e) {
        logger.warn("Could not flush FAF replay output stream", e);
      }
    }

    logger.debug("FAF has disconnected, writing replay data to file");
    finishReplayInfo();
    replayFileWriter.writeReplayDataToFile(replayData, replayInfo);
  }

  private void finishReplayInfo() {
    Game game = gameService.getByUid(replayInfo.getUid());

    replayInfo.setGameEnd(pythonTime());
    replayInfo.setRecorder(userService.getUsername());
    replayInfo.setComplete(true);
    replayInfo.setState(GameState.CLOSED);
    replayInfo.updateFromGameInfoBean(game);
  }
}
