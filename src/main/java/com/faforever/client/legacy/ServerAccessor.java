package com.faforever.client.legacy;

import com.faforever.client.legacy.gson.GameStateTypeAdapter;
import com.faforever.client.legacy.gson.GameTypeTypeAdapter;
import com.faforever.client.legacy.message.ClientMessage;
import com.faforever.client.legacy.message.PlayerInfo;
import com.faforever.client.legacy.message.ServerWritable;
import com.faforever.client.util.Callback;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

public class ServerAccessor implements OnSessionInitiatedListener, OnServerPingListener, OnPlayerInfoListener, OnGameInfoListener {

  private static final int VERSION = 123;

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final CountDownLatch WAIT_FOR_WELCOME_LATCH = new CountDownLatch(1);

  @Autowired
  Environment environment;

  private final Object writeMonitor = new Object();

  private String localIp;
  private String session;
  private String username;
  private Socket socket;
  private QStreamWriter socketOut;
  private Gson gson;
  private String uniqueId;
  private Collection<OnPlayerInfoListener> onPlayerInfoListeners;
  private Collection<OnGameInfoListener> onGameInfoListeners;

  public ServerAccessor() {
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(GameType.class, new GameTypeTypeAdapter())
        .registerTypeAdapter(GameState.class, new GameStateTypeAdapter())
        .create();
    onPlayerInfoListeners = new ArrayList<>();
    onGameInfoListeners = new ArrayList<>();
  }

  public void connect() throws IOException {
    socket = new Socket(
        environment.getProperty("lobby.host"),
        environment.getProperty("lobby.port", int.class)
    );
    socket.setKeepAlive(true);

    localIp = socket.getLocalAddress().getHostAddress();
    socketOut = new QStreamWriter(new DataOutputStream(new BufferedOutputStream(socket.getOutputStream())));

    writeToServer(ClientMessage.askSession(username));
    startServerReader(socket);
  }

  private void startServerReader(Socket socket) {
    ServerReader serverReader = new ServerReader(gson, socket);
    serverReader.setOnSessionInitiatedListener(this);
    serverReader.setOnGameInfoListener(this);
    serverReader.setOnServerPingListener(this);
    serverReader.setOnPlayerInfoListener(this);
    serverReader.start();
  }

  @Override
  public void onSessionInitiated(WelcomeMessage message) {
    this.session = message.session;
    this.uniqueId = com.faforever.client.util.UID.generate(session);

    WAIT_FOR_WELCOME_LATCH.countDown();
  }

  @Override
  public void onServerPing() {
    writeToServer(PongMessage.INSTANCE);
  }

  public void login(final String username, final String password, Callback<Void> callback) {
    this.username = username;

    executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        ensureConnected();
        waitForWelcome();

        if (session == null || uniqueId == null) {
          throw new IllegalStateException("session or uniqueId has not been set");
        }
        writeToServer(ClientMessage.login(username, password, session, uniqueId, localIp, VERSION));
        return null;
      }
    }, new Callback<Void>() {
      @Override
      public void success(Void result) {
        callback.success(result);
      }

      @Override
      public void error(Throwable e) {
        callback.error(e);
      }
    });
  }

  private void waitForWelcome() {
    try {
      WAIT_FOR_WELCOME_LATCH.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void ensureConnected() throws IOException {
    if (socket == null || socket.isClosed()) {
      connect();
    }
  }

  private void writeToServer(ServerWritable serverWritable) {
    synchronized (writeMonitor) {
      try {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Writer stringWriter = new StringWriter();
        serverWritable.write(gson, stringWriter);

        QStreamWriter qStreamWriter = new QStreamWriter(byteArrayOutputStream);
        qStreamWriter.appendQString(stringWriter.toString());
        qStreamWriter.appendQString(username);
        qStreamWriter.appendQString(session);

        byte[] byteArray = byteArrayOutputStream.toByteArray();

        logger.debug("Writing to server: {}", new String(byteArray, StandardCharsets.UTF_16BE));

        socketOut.append(byteArray);
        socketOut.flush();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void onPlayerInfo(PlayerInfo playerInfo) {
    for (OnPlayerInfoListener listener : onPlayerInfoListeners) {
      listener.onPlayerInfo(playerInfo);
    }
  }

  @Override
  public void onGameInfo(GameInfo gameInfo) {
    for (OnGameInfoListener listener : onGameInfoListeners) {
      listener.onGameInfo(gameInfo);
    }
  }

  public void addOnGameInfoListener(OnGameInfoListener listener) {
    onGameInfoListeners.add(listener);
  }

  public void addOnPlayerInfoListener(OnPlayerInfoListener listener) {
    onPlayerInfoListeners.add(listener);
  }
}
