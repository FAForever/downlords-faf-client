package com.faforever.client.legacy;

import com.faforever.client.games.NewGameInfo;
import com.faforever.client.legacy.message.ClientMessage;
import com.faforever.client.legacy.message.GameAccess;
import com.faforever.client.legacy.message.GameInfoMessage;
import com.faforever.client.legacy.message.GameLaunchMessage;
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
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

public class ServerAccessor implements OnSessionInitiatedListener, OnPingMessageListener, OnPlayerInfoMessageListener, OnGameInfoMessageListener, OnFafLoginSucceededListener, OnModInfoMessageListener, OnGameLaunchMessageListener {

  public interface OnFaConnectingListener {

    void onFaConnecting();
  }

  public interface OnFaDisconnectedListener {

    void onFaDisconnected();
  }

  public interface OnFaConnectedListener {

    void onFaConnected();
  }

  private static final int VERSION = 123;
  private static final long RECONNECT_DELAY = 3000;

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  Environment environment;

  @Autowired
  PreferencesService preferencesService;

  private String uniqueId;
  private String username;
  private String password;
  private String localIp;
  private Callback<Void> loginCallback;
  private Callback<GameLaunchMessage> gameLaunchCallback;
  private Collection<OnPlayerInfoMessageListener> onPlayerInfoMessageListeners;
  private Collection<OnGameInfoMessageListener> onGameInfoMessageListeners;
  private Collection<OnModInfoMessageListener> onModInfoMessageListeners;
  private ServerWriter serverWriter;
  private OnFaConnectingListener onFaConnectingListener;
  private OnFaDisconnectedListener onFaDisconnectedListener;
  private OnFaConnectedListener onFaConnectedListener;

  public ServerAccessor() {
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
          if (onFaConnectingListener != null) {
            Platform.runLater(onFaConnectingListener::onFaConnecting);
          }

          try (Socket fafServerSocket = new Socket(lobbyHost, lobbyPort);
               OutputStream outputStream = fafServerSocket.getOutputStream()) {

            fafServerSocket.setKeepAlive(true);

            logger.info("FAF server connection established");
            if (onFaConnectedListener != null) {
              Platform.runLater(onFaConnectedListener::onFaConnected);
            }

            localIp = fafServerSocket.getLocalAddress().getHostAddress();

            serverWriter = new ServerWriter(outputStream);
            serverWriter.setAppendSession(true);
            serverWriter.setAppendUsername(true);

            writeToServer(ClientMessage.askSession(username));

            blockingReadServer(fafServerSocket);
          } catch (IOException e) {
            logger.warn("Lost connection to FAF server, trying to reconnect in " + RECONNECT_DELAY / 1000 + "s", e);
            if (onFaDisconnectedListener != null) {
              Platform.runLater(onFaDisconnectedListener::onFaDisconnected);
            }
            Thread.sleep(RECONNECT_DELAY);
          }

        }
        return null;
      }
    });
  }

  private void blockingReadServer(Socket socket) throws IOException {
    JavaFxUtil.assertNotApplicationThread();

    ServerReader serverReader = new ServerReader(socket);
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
    String session = message.session;
    this.uniqueId = UID.generate(session);

    serverWriter.setSession(session);

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
    serverWriter.write(serverWritable);
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
      Platform.runLater(() -> listener.onModInfoMessage(modInfoMessage));
    }
  }

  @Override
  public void onPlayerInfoMessage(PlayerInfoMessage playerInfoMessage) {
    for (OnPlayerInfoMessageListener listener : onPlayerInfoMessageListeners) {
      Platform.runLater(() -> listener.onPlayerInfoMessage(playerInfoMessage));
    }
  }

  @Override
  public void onGameInfoMessage(GameInfoMessage gameInfoMessage) {
    for (OnGameInfoMessageListener listener : onGameInfoMessageListeners) {
      Platform.runLater(() -> listener.onGameInfoMessage(gameInfoMessage));
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

  public void notifyGameStarted() {
    writeToServer(ClientMessage.gameStarted());
  }

  public void notifyGameTerminated() {
    writeToServer(ClientMessage.gameTerminated());
  }

  public void setOnFaConnectingListener(OnFaConnectingListener onFaConnectingListener) {
    this.onFaConnectingListener = onFaConnectingListener;
  }

  public void setOnFaDisconnectedListener(OnFaDisconnectedListener onFaDisconnectedListener) {
    this.onFaDisconnectedListener = onFaDisconnectedListener;
  }

  public void setOnFaConnectedListener(OnFaConnectedListener onFaConnectedListener) {
    this.onFaConnectedListener = onFaConnectedListener;
  }
}
