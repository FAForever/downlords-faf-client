package com.faforever.client.replay;

import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.OnGameInfoListener;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.legacy.io.QtCompress;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.ConcurrentUtil;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Bytes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE_NEW;

public class ReplayServerImpl implements ReplayServer, OnGameInfoListener {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int REPLAY_BUFFER_SIZE = 0x200;

  /**
   * This is a prefix used in the FA live replay protocol that needs to be stripped away when storing to a file.
   */
  private static final byte[] LIVE_REPLAY_PREFIX = new byte[]{'P', '/'};

  @Autowired
  Environment environment;

  @Autowired
  NotificationService notificationService;

  @Autowired
  I18n i18n;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  GameService gameService;

  @Autowired
  UserService userService;

  private int uid;
  private final Gson gson;
  private GameInfo gameInfo;

  public ReplayServerImpl() {
    gson = new GsonBuilder().create();
  }

  @PostConstruct
  void postConstruct() {
    gameService.addOnGameInfoListener(this);
    startInBackground();
  }

  void startInBackground() {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        Integer localReplayServerPort = environment.getProperty("localReplayServer.port", Integer.class);
        String fafReplayServerHost = environment.getProperty("fafReplayServer.host");
        Integer fafReplayServerPort = environment.getProperty("fafReplayServer.port", Integer.class);

        logger.debug("Opening local replay server on port {}", localReplayServerPort);


        while (!isCancelled()) {
          try (ServerSocket serverSocket = new ServerSocket(localReplayServerPort);
               Socket fafReplayServerSocket = new Socket(fafReplayServerHost, fafReplayServerPort)) {
            while (!serverSocket.isClosed() && !fafReplayServerSocket.isClosed()) {
              recordAndRelay(serverSocket, new BufferedOutputStream(fafReplayServerSocket.getOutputStream()));
            }
          } catch (IOException e) {
            logger.warn("Error while recording replay", e);
            notificationService.addNotification(new PersistentNotification(
                i18n.get("replayServer.listeningFailed", localReplayServerPort),
                Severity.ERROR
            ));
          }
        }
        return null;
      }
    });
  }

  private void recordAndRelay(ServerSocket serverSocket, OutputStream fafRelayOutputStream) throws IOException {
    Socket socket = serverSocket.accept();
    logger.debug("Accepted connection from {}", socket.getRemoteSocketAddress());

    ByteArrayOutputStream replayData = new ByteArrayOutputStream();

    byte[] buffer = new byte[REPLAY_BUFFER_SIZE];
    try (InputStream inputStream = socket.getInputStream()) {
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        if (replayData.size() == 0 && Bytes.indexOf(buffer, LIVE_REPLAY_PREFIX) != -1) {
          int dataBeginIndex = Bytes.indexOf(buffer, (byte) 0x0000) + 1;
          replayData.write(buffer, dataBeginIndex, bytesRead - dataBeginIndex);
        } else {
          replayData.write(buffer, 0, bytesRead);
        }

        fafRelayOutputStream.write(buffer, 0, bytesRead);
      }
    } finally {
      fafRelayOutputStream.flush();
    }

    logger.debug("FAF has disconnected, writing replay data to file");
    writeReplayDataToFile(replayData);
  }

  private void writeReplayDataToFile(ByteArrayOutputStream replayData) throws IOException {
    ReplayInfo replayInfo = new ReplayInfo();

    replayInfo.setGameEnd(System.currentTimeMillis());

    String fileName = String.format(environment.getProperty("replayFileFormat"), uid, userService.getUsername());
    Path replayFile = preferencesService.getReplayDirectory().resolve(fileName);

    logger.info("Writing replay file to {} ({} bytes)", replayFile, replayData.size());

    Files.createDirectories(replayFile.getParent());

    try (BufferedWriter writer = Files.newBufferedWriter(replayFile, UTF_8, CREATE_NEW)) {
      byte[] compressedBytes = QtCompress.qCompress(replayData.toByteArray());
      String base64ReplayData = BaseEncoding.base64().encode(compressedBytes);

      gson.toJson(replayInfo, writer);
      writer.write('\n');
      writer.write(base64ReplayData);
    }
  }

  @Override
  public void onGameInfo(GameInfo gameInfo) {
    if (gameInfo.uid != uid) {
      return;
    }

    this.gameInfo = gameInfo;
  }
}
