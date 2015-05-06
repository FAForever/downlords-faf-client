package com.faforever.client.legacy;

import com.faforever.client.games.GameInfoBean;
import com.faforever.client.games.NewGameInfo;
import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.FriendAndFoeLists;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.domain.ModInfo;
import com.faforever.client.legacy.domain.OnFafLoginSucceededListener;
import com.faforever.client.legacy.domain.PlayerInfo;
import com.faforever.client.legacy.domain.PongMessage;
import com.faforever.client.legacy.domain.ServerWritable;
import com.faforever.client.legacy.domain.SessionInfo;
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

import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

/**
 * Entry class for all communication with the FAF lobby server, be it reading or writing.
 */
public class ServerAccessor implements OnSessionInfoListener, OnPingListener, OnPlayerInfoListener, OnGameInfoListener, OnFafLoginSucceededListener, OnModInfoListener, OnGameLaunchInfoListener, OnFriendAndFoeListListener {

  public interface OnLobbyConnectingListener {

    void onFaConnecting();
  }

  public interface OnLobbyDisconnectedListener {

    void onFaDisconnected();
  }

  public interface OnLobbyConnectedListener {

    void onFaConnected();
  }

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int VERSION = 123;
  private static final long RECONNECT_DELAY = 3000;

  @Autowired
  Environment environment;

  @Autowired
  PreferencesService preferencesService;

  private String uniqueId;
  private String username;
  private String password;
  private String localIp;
  private ServerWriter serverWriter;
  private Callback<Void> loginCallback;
  private Callback<GameLaunchInfo> gameLaunchCallback;
  private Collection<OnPlayerInfoListener> onPlayerInfoListeners;
  private Collection<OnGameInfoListener> onGameInfoListeners;
  private Collection<OnModInfoListener> onModInfoListeners;
  private Collection<OnFriendAndFoeListListener> onFriendAndFoeListListeners;

  // Yes I know, those aren't lists. They will become if it's necessary
  private OnLobbyConnectingListener onLobbyConnectingListener;
  private OnLobbyDisconnectedListener onLobbyDisconnectedListener;
  private OnLobbyConnectedListener onFafConnectedListener;

  public ServerAccessor() {
    onPlayerInfoListeners = new ArrayList<>();
    onGameInfoListeners = new ArrayList<>();
    onModInfoListeners = new ArrayList<>();
    onFriendAndFoeListListeners = new ArrayList<>();
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
          if (onLobbyConnectingListener != null) {
            Platform.runLater(onLobbyConnectingListener::onFaConnecting);
          }

          try (Socket fafServerSocket = new Socket(lobbyHost, lobbyPort);
               OutputStream outputStream = fafServerSocket.getOutputStream()) {

            fafServerSocket.setKeepAlive(true);

            logger.info("FAF server connection established");
            if (onFafConnectedListener != null) {
              Platform.runLater(onFafConnectedListener::onFaConnected);
            }

            localIp = fafServerSocket.getLocalAddress().getHostAddress();

            serverWriter = new ServerWriter(outputStream);
            serverWriter.setAppendSession(true);
            serverWriter.setAppendUsername(true);

            writeToServer(ClientMessage.askSession(username));

            blockingReadServer(fafServerSocket);
          } catch (IOException e) {
            logger.warn("Lost connection to FAF server, trying to reconnect in " + RECONNECT_DELAY / 1000 + "s", e);
            if (onLobbyDisconnectedListener != null) {
              Platform.runLater(onLobbyDisconnectedListener::onFaDisconnected);
            }
            Thread.sleep(RECONNECT_DELAY);
          }

        }
        return null;
      }
    });
  }

  private void writeToServer(ServerWritable serverWritable) {
    serverWriter.write(serverWritable);
  }

  private void blockingReadServer(Socket socket) throws IOException {
    JavaFxUtil.assertNotApplicationThread();

    ServerReader serverReader = new ServerReader(socket);
    serverReader.setOnSessionInfoListener(this);
    serverReader.setOnGameInfoListener(this);
    serverReader.setOnPingListener(this);
    serverReader.setOnPlayerInfoListener(this);
    serverReader.setOnFafLoginSucceededListener(this);
    serverReader.setOnModInfoListener(this);
    serverReader.setOnGameLaunchInfoListenerListener(this);
    serverReader.setOnFriendAndFoeListListener(this);
    serverReader.blockingRead();
  }

  @Override
  public void onSessionInitiated(SessionInfo message) {
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

  public void addOnModInfoMessageListener(OnModInfoListener listener) {
    onModInfoListeners.add(listener);
  }

  @Override
  public void onModInfo(ModInfo modInfo) {
    for (OnModInfoListener listener : onModInfoListeners) {
      Platform.runLater(() -> listener.onModInfo(modInfo));
    }
  }

  @Override
  public void onPlayerInfo(PlayerInfo playerInfo) {
    for (OnPlayerInfoListener listener : onPlayerInfoListeners) {
      Platform.runLater(() -> listener.onPlayerInfo(playerInfo));
    }
  }

  @Override
  public void onGameInfo(GameInfo gameInfo) {
    for (OnGameInfoListener listener : onGameInfoListeners) {
      Platform.runLater(() -> listener.onGameInfo(gameInfo));
    }
  }

  @Override
  public void onFriendAndFoeList(FriendAndFoeLists friendAndFoeLists) {
    for (OnFriendAndFoeListListener listener : onFriendAndFoeListListeners) {
      Platform.runLater(() -> listener.onFriendAndFoeList(friendAndFoeLists));
    }
  }

  public void addOnGameInfoMessageListener(OnGameInfoListener listener) {
    onGameInfoListeners.add(listener);
  }

  public void addOnFriendAndFoeListListener(OnFriendAndFoeListListener listener) {
    onFriendAndFoeListListeners.add(listener);
  }

  public void addOnPlayerInfoMessageListener(OnPlayerInfoListener listener) {
    onPlayerInfoListeners.add(listener);
  }


  public void requestNewGame(NewGameInfo newGameInfo, Callback<GameLaunchInfo> callback) {
    ClientMessage clientMessage = ClientMessage.hostGame(
        StringUtils.isEmpty(newGameInfo.getPassword()) ? GameAccess.PUBLIC : GameAccess.PASSWORD,
        newGameInfo.getMap(),
        newGameInfo.getTitle(),
        preferencesService.getPreferences().getForgedAlliance().getPort(),
        new boolean[0],
        newGameInfo.getMod(),
        newGameInfo.getPassword()
    );

    gameLaunchCallback = callback;
    writeToServerInBackground(clientMessage);
  }

  private void writeToServerInBackground(final ClientMessage clientMessage) {
    executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        writeToServer(clientMessage);
        return null;
      }
    });
  }

  public void requestJoinGame(GameInfoBean gameInfoBean, String password, Callback<GameLaunchInfo> callback) {
    ClientMessage clientMessage = ClientMessage.joinGame(
        gameInfoBean.getUid(),
        preferencesService.getPreferences().getForgedAlliance().getPort(),
        password);

    gameLaunchCallback = callback;
    writeToServerInBackground(clientMessage);
  }

  @Override
  public void onGameLaunchInfo(GameLaunchInfo gameLaunchInfo) {
    gameLaunchCallback.success(gameLaunchInfo);
  }

  public void notifyGameStarted() {
    writeToServer(ClientMessage.gameStarted());
  }

  public void notifyGameTerminated() {
    writeToServer(ClientMessage.gameTerminated());
  }

  public void setOnLobbyConnectingListener(OnLobbyConnectingListener onLobbyConnectingListener) {
    this.onLobbyConnectingListener = onLobbyConnectingListener;
  }

  public void setOnLobbyDisconnectedListener(OnLobbyDisconnectedListener onLobbyDisconnectedListener) {
    this.onLobbyDisconnectedListener = onLobbyDisconnectedListener;
  }

  public void setOnFafConnectedListener(OnLobbyConnectedListener onFafConnectedListener) {
    this.onFafConnectedListener = onFafConnectedListener;
  }
}
