package com.faforever.client.legacy;

import com.faforever.client.games.NewGameInfo;
import com.faforever.client.legacy.gson.GameStateTypeAdapter;
import com.faforever.client.legacy.gson.GameTypeTypeAdapter;
import com.faforever.client.legacy.message.ClientMessage;
import com.faforever.client.legacy.message.GameAccess;
import com.faforever.client.legacy.message.GameInfoMessage;
import com.faforever.client.legacy.message.GameLaunchMessage;
import com.faforever.client.legacy.message.GameStatus;
import com.faforever.client.legacy.message.GameType;
import com.faforever.client.legacy.message.OnFafLoginSucceededListener;
import com.faforever.client.legacy.message.OnGameInfoMessageListener;
import com.faforever.client.legacy.message.OnGameLaunchMessageListener;
import com.faforever.client.legacy.message.OnPingMessageListener;
import com.faforever.client.legacy.message.OnPlayerInfoMessageListener;
import com.faforever.client.legacy.message.OnSessionInitiatedListener;
import com.faforever.client.legacy.message.PlayerInfoMessage;
import com.faforever.client.legacy.message.PongMessage;
import com.faforever.client.legacy.message.ServerWritable;
import com.faforever.client.legacy.message.WelcomeMessage;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.Callback;
import com.faforever.client.util.JavaFxUtil;
import com.faforever.client.util.UID;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;

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
import java.util.HashMap;

import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

public class ServerAccessor implements OnSessionInitiatedListener, OnPingMessageListener, OnPlayerInfoMessageListener, OnGameInfoMessageListener, OnFafLoginSucceededListener, OnModInfoMessageListener, OnGameLaunchMessageListener {

  private static final int VERSION = 123;
  private static final long RECONNECT_DELAY = 3000;

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  Environment environment;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  TaskScheduler taskScheduler;

  private String uniqueId;
  private String session;
  private String username;
  private String password;
  private String localIp;
  private Socket socket;
  private Gson gson;
  private QStreamWriter socketOut;
  private Callback<Void> loginCallback;
  private Callback<GameLaunchMessage> gameLaunchCallback;
  private Collection<OnPlayerInfoMessageListener> onPlayerInfoMessageListeners;
  private Collection<OnGameInfoMessageListener> onGameInfoMessageListeners;
  private Collection<OnModInfoMessageListener> onModInfoMessageListeners;

  public ServerAccessor() {
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(GameType.class, new GameTypeTypeAdapter())
        .registerTypeAdapter(GameStatus.class, new GameStateTypeAdapter())
        .create();
    onPlayerInfoMessageListeners = new ArrayList<>();
    onGameInfoMessageListeners = new ArrayList<>();
    onModInfoMessageListeners = new ArrayList<>();
  }

  /**
   * Connects to the FAF server and logs in using the credentials from {@link PreferencesService}. This method runs in
   * background, the callback however is called on the FX application thread.
   */
  public void connectAndLogInInBackground(Callback<Void> callback) {
    loginCallback = callback;

    LoginPrefs login = preferencesService.getPreferences().getLogin();
    username = login.getUsername();
    password = login.getPassword();

    if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
      throw new IllegalStateException("Username or password has not been set");
    }

    executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        while (!isCancelled()) {
          String lobbyHost = environment.getProperty("lobby.host");
          Integer lobbyPort = environment.getProperty("lobby.port", int.class);

          logger.info("Trying to connect to FAF server at {}:{}", lobbyHost, lobbyPort);

          try {
            socket = new Socket(lobbyHost, lobbyPort);
            socket.setKeepAlive(true);

            logger.info("FAF server connection established");

            localIp = socket.getLocalAddress().getHostAddress();
            socketOut = new QStreamWriter(new DataOutputStream(new BufferedOutputStream(socket.getOutputStream())));

            writeToServer(ClientMessage.askSession(username));

            blockingReadServer(socket);
          } catch (IOException e) {
            logger.warn("Lost connection to FAF server, trying to reconnect in " + RECONNECT_DELAY / 1000 + "s", e);
            Thread.sleep(RECONNECT_DELAY);
          }
        }
        return null;
      }
    });
  }

  private void blockingReadServer(Socket socket) throws IOException {
    JavaFxUtil.assertNotApplicationThread();

    ServerReader serverReader = new ServerReader(gson, socket);
    serverReader.setOnSessionInitiatedListener(this);
    serverReader.setOnGameInfoMessageListener(this);
    serverReader.setOnPingMessageListener(this);
    serverReader.setOnPlayerInfoMessageListener(this);
    serverReader.setOnFafLoginSucceededListener(this);
    serverReader.setOnModInfoMessageListener(this);
    serverReader.setOnGameLaunchMessageListenerListener(this);
    serverReader.blockingRead();
  }

  @Override
  public void onSessionInitiated(WelcomeMessage message) {
    this.session = message.session;
    this.uniqueId = UID.generate(session);

    logger.info("FAF session initiated, session ID: {}", session);

    executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        logger.info("Sending login information to FAF server");
        writeToServer(ClientMessage.login(username, password, session, uniqueId, localIp, VERSION));
        return null;
      }
    });
  }

  @Override
  public void onServerPing() {
    writeToServer(PongMessage.INSTANCE);
  }

  private void writeToServer(ServerWritable serverWritable) {
    try {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

      Writer stringWriter = new StringWriter();
      serverWritable.write(gson, stringWriter);

      QStreamWriter qStreamWriter = new QStreamWriter(byteArrayOutputStream);
      qStreamWriter.appendQString(stringWriter.toString());
      qStreamWriter.appendQString(username);
      qStreamWriter.appendQString(session);

      byte[] byteArray = byteArrayOutputStream.toByteArray();

      if (serverWritable.isConfidential()) {
        logger.debug("Writing confidential information to server");
      } else {
        logger.debug("Writing to server: {}", new String(byteArray, StandardCharsets.UTF_16BE));
      }

      socketOut.append(byteArray);
      socketOut.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onFafLoginSucceeded() {
    logger.info("FAF login succeeded");

    Platform.runLater(() -> {
      if (loginCallback != null) {
        loginCallback.success(null);
        loginCallback = null;
      }
    });
  }

  public void addOnModInfoMessageListener(OnModInfoMessageListener listener) {
    onModInfoMessageListeners.add(listener);
  }

  @Override
  public void onModInfoMessage(ModInfoMessage modInfoMessage) {
    for (OnModInfoMessageListener listener : onModInfoMessageListeners) {
      listener.onModInfoMessage(modInfoMessage);
    }
  }

  @Override
  public void onPlayerInfoMessage(PlayerInfoMessage playerInfoMessage) {
    for (OnPlayerInfoMessageListener listener : onPlayerInfoMessageListeners) {
      listener.onPlayerInfoMessage(playerInfoMessage);
    }
  }

  @Override
  public void onGameInfoMessage(GameInfoMessage gameInfoMessage) {
    for (OnGameInfoMessageListener listener : onGameInfoMessageListeners) {
      listener.onGameInfoMessage(gameInfoMessage);
    }
  }

  public void addOnGameInfoMessageListener(OnGameInfoMessageListener listener) {
    onGameInfoMessageListeners.add(listener);
  }

  public void addOnPlayerInfoMessageListener(OnPlayerInfoMessageListener listener) {
    onPlayerInfoMessageListeners.add(listener);
  }


  public void requestNewGame(NewGameInfo newGameInfo, Callback<GameLaunchMessage> callback) {
    ClientMessage clientMessage = ClientMessage.hostGame(
        StringUtils.isEmpty(newGameInfo.getPassword()) ? GameAccess.PUBLIC : GameAccess.PRIVATE,
        newGameInfo.getMap(),
        newGameInfo.getTitle(),
        preferencesService.getPreferences().getSupCom().getPort(),
        new HashMap<>(),
        newGameInfo.getMod()
    );

    gameLaunchCallback = callback;
    executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        writeToServer(clientMessage);
        return null;
      }
    });
  }

  @Override
  public void onGameLaunchMessage(GameLaunchMessage gameLaunchMessage) {
    gameLaunchCallback.success(gameLaunchMessage);
  }
}
