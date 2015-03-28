package com.faforever.client.legacy;

import com.faforever.client.legacy.message.ClientMessage;
import com.faforever.client.legacy.message.PlayerInfo;
import com.faforever.client.legacy.message.Serializable;
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
import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

public class ServerAccessor implements OnGameInfoListener, OnPlayerInfoListener, OnSessionInitiatedListener, OnServerPingListener {

  private static final int VERSION = 122;

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  Environment environment;


  private final Object writeMonitor = new Object();

  private String localIp;
  private String session;
  private String username;
  private Callback<Void> loginCallback;
  private Socket socket;
  private QStreamWriter socketOut;
  private Gson gson;

  public ServerAccessor() {
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
//        .registerTypeAdapter(FeaturedModVersions, new FeatureModVersionsTypeAdapter())
        .create();
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
    serverReader.setOnPlayerInfoListener(this);
    serverReader.setOnServerPingListener(this);
    serverReader.start();
  }

  @Override
  public void onSessionInitiated(SessionInitiatedMessage message) {
    this.session = message.session;
  }

  @Override
  public void onGameInfo(GameInfo gameInfo) {

  }

  @Override
  public void onPlayerInfo(PlayerInfo playerInfo) {

  }

  @Override
  public void onServerPing() {
    writeToServer(PongMessage.INSTANCE);
  }

  public void login(final String username, final String password, Callback<Void> callback) {
    this.username = username;
    this.loginCallback = callback;

    executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        ensureConnected();
        writeToServer(ClientMessage.login(username, password, session));
        return null;
      }
    }, callback);
  }

  private void ensureConnected() throws IOException {
    if (socket == null || socket.isClosed()) {
      connect();
    }
  }

  private void writeToServer(Serializable serializable) {
    synchronized (writeMonitor) {
      try {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        serializable.serialize(new QStreamWriter(byteArrayOutputStream), username, session);

        byte[] byteArray = byteArrayOutputStream.toByteArray();

        logger.debug("Writing to server: {}", new String(byteArray, StandardCharsets.UTF_16BE));

        socketOut.append(byteArray);
        socketOut.flush();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
