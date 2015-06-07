package com.faforever.client.legacy;

import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.domain.ModInfo;
import com.faforever.client.legacy.domain.OnFafLoginSucceededListener;
import com.faforever.client.legacy.domain.PlayerInfo;
import com.faforever.client.legacy.domain.SessionInfo;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.Callback;
import com.faforever.client.util.JavaFxUtil;
import com.faforever.client.util.UID;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
import java.util.List;

import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

public class ServerAccessorImpl implements ServerAccessor,
    OnSessionInfoListener,
    OnPingListener,
    OnPlayerInfoListener,
    OnFafLoginSucceededListener,
    OnModInfoListener,
    OnGameLaunchInfoListener,
    OnFriendListListener,
    OnFoeListListener,
    OnGameInfoListener {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int VERSION = 123;
  private static final long RECONNECT_DELAY = 3000;
  private static final String PONG = "PONG";

  @Autowired
  Environment environment;

  @Autowired
  PreferencesService preferencesService;

  private Task<Void> fafConnectionTask;
  private String uniqueId;
  private String username;
  private String password;
  private String localIp;
  private StringProperty sessionId;
  private ServerWriter serverWriter;
  private Callback<Void> loginCallback;
  private Callback<GameLaunchInfo> gameLaunchCallback;
  private Collection<OnGameInfoListener> onGameInfoListeners;
  private Collection<OnModInfoListener> onModInfoListeners;
  private Collection<OnFoeListListener> onFoeListListeners;

  // Yes I know, those aren't lists. They will become if it's necessary
  private OnLobbyConnectingListener onLobbyConnectingListener;
  private OnLobbyDisconnectedListener onLobbyDisconnectedListener;
  private OnLobbyConnectedListener onLobbyConnectedListener;
  private OnPlayerInfoListener onPlayerInfoListener;
  private OnFoeListListener onFoeListListener;
  private OnFriendListListener onFriendListListener;

  public ServerAccessorImpl() {
    onGameInfoListeners = new ArrayList<>();
    onModInfoListeners = new ArrayList<>();
    sessionId = new SimpleStringProperty();
  }

  @Override
  public void connectAndLogInInBackground(Callback<Void> callback) {
    loginCallback = callback;

    LoginPrefs login = preferencesService.getPreferences().getLogin();
    username = login.getUsername();
    password = login.getPassword();

    if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
      throw new IllegalStateException("Username or password has not been set");
    }

    fafConnectionTask = new Task<Void>() {
      Socket fafServerSocket;

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
            this.fafServerSocket = fafServerSocket;

            fafServerSocket.setKeepAlive(true);

            logger.info("FAF server connection established");
            if (onLobbyConnectedListener != null) {
              Platform.runLater(onLobbyConnectedListener::onFaConnected);
            }

            localIp = fafServerSocket.getLocalAddress().getHostAddress();

            serverWriter = createServerWriter(outputStream);

            writeToServer(ClientMessage.askSession(username));

            blockingReadServer(fafServerSocket);
          } catch (IOException e) {
            if (isCancelled()) {
              logger.debug("Login has been cancelled");
            } else {
              logger.warn("Lost connection to FAF server, trying to reconnect in " + RECONNECT_DELAY / 1000 + "s", e);
              if (onLobbyDisconnectedListener != null) {
                Platform.runLater(onLobbyDisconnectedListener::onFaDisconnected);
              }
              Thread.sleep(RECONNECT_DELAY);
            }
          }
        }
        return null;
      }

      @Override
      protected void cancelled() {
        try {
          if (fafServerSocket != null) {
            serverWriter.close();
            fafServerSocket.close();
          }
          logger.debug("Closed connection to FAF lobby server");
        } catch (IOException e) {
          logger.warn("Could not close fafServerSocket", e);
        }
      }
    };
    executeInBackground(fafConnectionTask);
  }

  private ServerWriter createServerWriter(OutputStream outputStream) throws IOException {
    ServerWriter serverWriter = new ServerWriter(outputStream);
    serverWriter.registerObjectWriter(new ClientMessageSerializer(username, sessionId), ClientMessage.class);
    serverWriter.registerObjectWriter(new StringSerializer(), String.class);
    return serverWriter;
  }

  private void writeToServer(Object object) {
    serverWriter.write(object);
  }

  private void blockingReadServer(Socket socket) throws IOException {
    JavaFxUtil.assertBackgroundThread();

    ServerReader serverReader = new ServerReader(socket);
    serverReader.setOnSessionInfoListener(this);
    serverReader.setOnGameInfoListener(this);
    serverReader.setOnPingListener(this);
    serverReader.setOnPlayerInfoListener(this);
    serverReader.setOnFafLoginSucceededListener(this);
    serverReader.setOnModInfoListener(this);
    serverReader.setOnGameLaunchInfoListenerListener(this);
    serverReader.setOnFriendListListener(this);
    serverReader.setOnFoeListListener(this);
    serverReader.blockingRead();
  }

  @Override
  public void onSessionInitiated(SessionInfo message) {
    this.sessionId.set(message.session);
    this.uniqueId = UID.generate(sessionId.get(), preferencesService.getFafDataDirectory().resolve("uid.log"));

    logger.info("FAF session initiated, session ID: {}", sessionId);

    executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        writeToServer(ClientMessage.login(username, password, sessionId.get(), uniqueId, localIp, VERSION));
        return null;
      }
    });
  }

  @Override
  public void onServerPing() {
    writeToServer(PONG);
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

  @Override
  public void addOnModInfoMessageListener(OnModInfoListener listener) {
    onModInfoListeners.add(listener);
  }

  @Override
  public void onGameInfo(GameInfo gameInfo) {
    for (OnGameInfoListener listener : onGameInfoListeners) {
      Platform.runLater(() -> listener.onGameInfo(gameInfo));
    }
  }

  @Override
  public void onModInfo(ModInfo modInfo) {
    for (OnModInfoListener listener : onModInfoListeners) {
      Platform.runLater(() -> listener.onModInfo(modInfo));
    }
  }

  @Override
  public void onPlayerInfo(PlayerInfo playerInfo) {
    if (onPlayerInfoListener != null) {
      onPlayerInfoListener.onPlayerInfo(playerInfo);
    }
  }


  @Override
  public void onFriendList(List<String> friends) {
    if (onFriendListListener != null) {
      onFriendListListener.onFriendList(friends);
    }
  }

  @Override
  public void onFoeList(List<String> foes) {
    if (onFoeListListener != null) {
      onFoeListListener.onFoeList(foes);
    }
  }

  @Override
  public void addOnGameInfoListener(OnGameInfoListener listener) {
    onGameInfoListeners.add(listener);
  }

  @Override
  public void setOnPlayerInfoMessageListener(OnPlayerInfoListener listener) {
    onPlayerInfoListener = listener;
  }


  @Override
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

  @Override
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

  @Override
  public void notifyGameStarted() {
    writeToServer(ClientMessage.gameStarted());
  }

  @Override
  public void notifyGameTerminated() {
    writeToServer(ClientMessage.gameTerminated());
  }

  @Override
  public void setOnLobbyConnectingListener(OnLobbyConnectingListener onLobbyConnectingListener) {
    this.onLobbyConnectingListener = onLobbyConnectingListener;
  }

  @Override
  public void setOnLobbyDisconnectedListener(OnLobbyDisconnectedListener onLobbyDisconnectedListener) {
    this.onLobbyDisconnectedListener = onLobbyDisconnectedListener;
  }

  @Override
  public void setOnFriendListListener(OnFriendListListener onFriendListListener) {
    this.onFriendListListener = onFriendListListener;
  }

  @Override
  public void setOnFoeListListener(OnFoeListListener onFoeListListener) {
    this.onFoeListListener = onFoeListListener;
  }

  @Override
  public void disconnect() {
    fafConnectionTask.cancel();
  }

  @Override
  public void setOnLobbyConnectedListener(OnLobbyConnectedListener onLobbyConnectedListener) {
    this.onLobbyConnectedListener = onLobbyConnectedListener;
  }
}
