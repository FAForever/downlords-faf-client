package com.faforever.client.legacy;

import com.faforever.client.legacy.gson.GameStateTypeAdapter;
import com.faforever.client.legacy.gson.GameTypeTypeAdapter;
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
import com.faforever.client.legacy.message.ServerMessageType;
import com.faforever.client.legacy.message.ServerMessage;
import com.faforever.client.legacy.message.WelcomeMessage;
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

class ServerReader {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Socket socket;
  private final Gson gson;
  private boolean stopped;
  private OnSessionInitiatedListener onSessionInitiatedListener;
  private OnGameInfoMessageListener onGameInfoMessageListener;
  private OnPlayerInfoMessageListener onPlayerInfoMessageListener;
  private OnPingMessageListener onPingMessageListener;
  private OnGameLaunchMessageListener onGameLaunchMessageListenerListener;
  private OnFafLoginSucceededListener onFafLoginSucceededListener;
  private OnModInfoMessageListener onModInfoMessageListener;

  public ServerReader(Socket socket) {
    this.socket = socket;
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(GameType.class, new GameTypeTypeAdapter())
        .registerTypeAdapter(GameStatus.class, new GameStateTypeAdapter())
        .create();
  }

  public void blockingRead() throws IOException {
    try (QDataInputStream dataInput = new QDataInputStream(new DataInputStream(new BufferedInputStream(socket.getInputStream())))) {

      while (!stopped && !socket.isInputShutdown()) {
        dataInput.skipBlockSize();
        String message = dataInput.readQString();

        ServerMessageType serverMessageType = ServerMessageType.fromString(message);
        if (serverMessageType != null) {
          dispatchServerMessage(dataInput, serverMessageType);
        } else {
          parseServerMessage(message);
        }
      }
    }

    logger.info("Connection to server {} has been closed", socket.getRemoteSocketAddress());
  }

  private void dispatchServerMessage(QDataInputStream socketIn, ServerMessageType serverMessageType) throws IOException {
    switch (serverMessageType) {
      case PING:
        onPingMessageListener.onServerPing();
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
        logger.warn("Error from server: {}", socketIn.readQString());
        break;

      case MESSAGE:
        logger.warn("Message from server: {}", socketIn.readQString());
        break;

      default:
        logger.warn("Unknown server response: {}", serverMessageType);
    }
  }

  private void parseServerMessage(String message) {
    try {
      logger.debug("Object from server: {}", message);
      ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);

      ServerCommand serverCommand = ServerCommand.fromString(serverMessage.command);

      if (serverCommand == null) {
        logger.warn("Unknown server message: " + message);
        return;
      }

      switch (serverCommand) {
        case WELCOME:
          WelcomeMessage welcomeMessage = gson.fromJson(message, WelcomeMessage.class);
          if (welcomeMessage.session != null) {
            onSessionInitiatedListener.onSessionInitiated(welcomeMessage);
          } else if (welcomeMessage.email != null) {
            onFafLoginSucceededListener.onFafLoginSucceeded();
          }
          break;

        case GAME_INFO:
          GameInfoMessage gameInfoMessage = gson.fromJson(message, GameInfoMessage.class);
          onGameInfoMessageListener.onGameInfoMessage(gameInfoMessage);
          break;

        case PLAYER_INFO:
          PlayerInfoMessage playerInfoMessage = gson.fromJson(message, PlayerInfoMessage.class);
          onPlayerInfoMessageListener.onPlayerInfoMessage(playerInfoMessage);
          break;

        case GAME_LAUNCH:
          GameLaunchMessage gameLaunchMessage = gson.fromJson(message, GameLaunchMessage.class);
          onGameLaunchMessageListenerListener.onGameLaunchMessage(gameLaunchMessage);
          break;

        case MOD_INFO:
          ModInfoMessage modInfoMessage = gson.fromJson(message, ModInfoMessage.class);
          onModInfoMessageListener.onModInfoMessage(modInfoMessage);
          break;

        case TUTORIALS_INFO:
          break;

        case MATCHMAKER_INFO:
          break;

        default:
          logger.warn("Missing case for server command:: " + serverCommand);
      }
    } catch (JsonSyntaxException e) {
      logger.warn("Could not deserialize message: " + message);
    }
  }

  public void setOnSessionInitiatedListener(OnSessionInitiatedListener onSessionInitiatedListener) {
    this.onSessionInitiatedListener = onSessionInitiatedListener;
  }

  public void setOnGameInfoMessageListener(OnGameInfoMessageListener onGameInfoMessageListener) {
    this.onGameInfoMessageListener = onGameInfoMessageListener;
  }

  public void setOnPlayerInfoMessageListener(OnPlayerInfoMessageListener onPlayerInfoMessageListener) {
    this.onPlayerInfoMessageListener = onPlayerInfoMessageListener;
  }

  public void setOnPingMessageListener(OnPingMessageListener onPingMessageListener) {
    this.onPingMessageListener = onPingMessageListener;
  }

  public void setOnFafLoginSucceededListener(OnFafLoginSucceededListener onFafLoginSucceededListener) {
    this.onFafLoginSucceededListener = onFafLoginSucceededListener;
  }

  public void setOnGameLaunchMessageListenerListener(OnGameLaunchMessageListener onGameLaunchMessageListenerListener) {
    this.onGameLaunchMessageListenerListener = onGameLaunchMessageListenerListener;
  }

  public void setOnModInfoMessageListener(OnModInfoMessageListener onModInfoMessageListener) {
    this.onModInfoMessageListener = onModInfoMessageListener;
  }
}
