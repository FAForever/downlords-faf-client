package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.SocialInfo;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.GameType;
import com.faforever.client.legacy.domain.ModInfo;
import com.faforever.client.legacy.domain.OnFafLoginSucceededListener;
import com.faforever.client.legacy.domain.PlayerInfo;
import com.faforever.client.legacy.domain.ServerMessageType;
import com.faforever.client.legacy.domain.ServerObject;
import com.faforever.client.legacy.domain.ServerObjectType;
import com.faforever.client.legacy.domain.SessionInfo;
import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.legacy.gson.GameStateTypeAdapter;
import com.faforever.client.legacy.gson.GameTypeTypeAdapter;
import com.faforever.client.legacy.gson.StatisticsTypeTypeAdapter;
import com.faforever.client.legacy.writer.QDataReader;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.Socket;

/**
 * Reads data from the FAF lobby server, like player information, open games, friends, foes and so on. Classes should
 * not use the server reader directly, but {@link ServerAccessor} instead.
 */
class ServerReader {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Socket socket;
  private final Gson gson;
  private boolean stopped;
  private OnSessionInfoListener onSessionInfoListener;
  private OnGameInfoListener onGameInfoListener;
  private OnPlayerInfoListener onPlayerInfoListener;
  private OnPingListener onPingListener;
  private OnGameLaunchInfoListener onGameLaunchInfoListener;
  private OnFafLoginSucceededListener onFafLoginSucceededListener;
  private OnModInfoListener onModInfoListener;
  private OnFriendListListener onFriendListListener;
  private OnFoeListListener onFoeListListener;

  public ServerReader(Socket socket) {
    this.socket = socket;
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(GameType.class, new GameTypeTypeAdapter())
        .registerTypeAdapter(GameState.class, new GameStateTypeAdapter())
        .registerTypeAdapter(StatisticsType.class, new StatisticsTypeTypeAdapter())
        .create();
  }

  /**
   * Reads data received from the server and dispatches it. So far, there are two types of data sent by the server: <ol>
   * <li><strong>Server messages</strong> are simple words like ACK or PING, followed by some bytes..</li>
   * <li><strong>Objects</strong> are JSON-encoded objects like game or player information. Those are converted into a
   * {@link ServerObject}</li> </ol> I'm not yet happy with those terms, so any suggestions are welcome.
   */
  public void blockingRead() throws IOException {
    try (QDataReader dataInput = new QDataReader(new DataInputStream(new BufferedInputStream(socket.getInputStream())))) {

      while (!stopped && !socket.isInputShutdown()) {
        dataInput.skipBlockSize();
        String message = dataInput.readQString();

        ServerMessageType serverMessageType = ServerMessageType.fromString(message);
        if (serverMessageType != null) {
          dispatchServerMessage(dataInput, serverMessageType);
        } else {
          parseServerObject(message);
        }
      }
    }

    logger.info("Connection to server {} has been closed", socket.getRemoteSocketAddress());
  }

  private void dispatchServerMessage(QDataReader socketIn, ServerMessageType serverMessageType) throws IOException {
    switch (serverMessageType) {
      case PING:
        onPingListener.onServerPing();
        logger.debug("Server PINGed");
        break;

      case LOGIN_AVAILABLE:
        logger.warn("Login available: {}", socketIn.readQString());
        break;

      case ACK:
        // Number of bytes acknowledged... as a string... I mean, why not.
        int acknowledgedBytes = Integer.parseInt(socketIn.readQString());
        // I really don't care. This is TCP with keepalive!
        logger.debug("Server acknowledged {} bytes", acknowledgedBytes);
        break;

      case ERROR:
        logger.warn("Unhandled error from server: {}", socketIn.readQString());
        break;

      case MESSAGE:
        logger.warn("Unhandled message from server: {}", socketIn.readQString());
        break;

      default:
        logger.warn("Unknown server response: {}", serverMessageType);
    }
  }

  private void parseServerObject(String jsonString) {
    try {
      logger.debug("Object from server: {}", jsonString);
      ServerObject serverObject = gson.fromJson(jsonString, ServerObject.class);

      ServerObjectType serverObjectType = ServerObjectType.fromString(serverObject.command);

      if (serverObjectType == null) {
        logger.warn("Unknown server object: " + jsonString);
        return;
      }

      switch (serverObjectType) {
        case WELCOME:
          SessionInfo sessionInfo = gson.fromJson(jsonString, SessionInfo.class);
          if (sessionInfo.session != null) {
            onSessionInfoListener.onSessionInitiated(sessionInfo);
          } else if (sessionInfo.email != null) {
            onFafLoginSucceededListener.onFafLoginSucceeded();
          }
          break;

        case GAME_INFO:
          GameInfo gameInfo = gson.fromJson(jsonString, GameInfo.class);
          onGameInfoListener.onGameInfo(gameInfo);
          break;

        case PLAYER_INFO:
          PlayerInfo playerInfo = gson.fromJson(jsonString, PlayerInfo.class);
          onPlayerInfoListener.onPlayerInfo(playerInfo);
          break;

        case GAME_LAUNCH:
          GameLaunchInfo gameLaunchInfo = gson.fromJson(jsonString, GameLaunchInfo.class);
          onGameLaunchInfoListener.onGameLaunchInfo(gameLaunchInfo);
          break;

        case MOD_INFO:
          ModInfo modInfo = gson.fromJson(jsonString, ModInfo.class);
          onModInfoListener.onModInfo(modInfo);
          break;

        case TUTORIALS_INFO:
          logger.warn("Tutorials info still unhandled: " + jsonString);
          break;

        case MATCHMAKER_INFO:
          logger.warn("Matchmaker info still unhandled: " + jsonString);
          break;

        case SOCIAL:
          SocialInfo socialInfo = gson.fromJson(jsonString, SocialInfo.class);
          dispatchSocialInfo(socialInfo);
          break;

        default:
          logger.warn("Missing case for server object type:: " + serverObjectType);
      }
    } catch (JsonSyntaxException e) {
      logger.warn("Could not deserialize message: " + jsonString);
    }
  }

  private void dispatchSocialInfo(SocialInfo socialInfo) {
    if (socialInfo.friends != null) {
      onFriendListListener.onFriendList(socialInfo.friends);
    } else if(socialInfo.foes != null) {
      onFoeListListener.onFoeList(socialInfo.foes);
    }
  }

  public void setOnSessionInfoListener(OnSessionInfoListener onSessionInfoListener) {
    this.onSessionInfoListener = onSessionInfoListener;
  }

  public void setOnGameInfoListener(OnGameInfoListener onGameInfoListener) {
    this.onGameInfoListener = onGameInfoListener;
  }

  public void setOnPlayerInfoListener(OnPlayerInfoListener onPlayerInfoListener) {
    this.onPlayerInfoListener = onPlayerInfoListener;
  }

  public void setOnPingListener(OnPingListener onPingListener) {
    this.onPingListener = onPingListener;
  }

  public void setOnFafLoginSucceededListener(OnFafLoginSucceededListener onFafLoginSucceededListener) {
    this.onFafLoginSucceededListener = onFafLoginSucceededListener;
  }

  public void setOnGameLaunchInfoListener(OnGameLaunchInfoListener onGameLaunchInfoListener) {
    this.onGameLaunchInfoListener = onGameLaunchInfoListener;
  }

  public void setOnModInfoListener(OnModInfoListener onModInfoListener) {
    this.onModInfoListener = onModInfoListener;
  }

  public void setOnFriendListListener(OnFriendListListener onFriendListListener) {
    this.onFriendListListener = onFriendListListener;
  }

  public void setOnFoeListListener(OnFoeListListener onFoeListListener) {
    this.onFoeListListener = onFoeListListener;
  }
}
