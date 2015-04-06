package com.faforever.client.legacy;

import com.faforever.client.legacy.message.GameInfoMessage;
import com.faforever.client.legacy.message.GameLaunchMessage;
import com.faforever.client.legacy.message.OnFafLoginSucceededListener;
import com.faforever.client.legacy.message.OnGameInfoMessageListener;
import com.faforever.client.legacy.message.OnGameLaunchMessageListener;
import com.faforever.client.legacy.message.OnPlayerInfoMessageListener;
import com.faforever.client.legacy.message.OnPingMessageListener;
import com.faforever.client.legacy.message.OnSessionInitiatedListener;
import com.faforever.client.legacy.message.PlayerInfoMessage;
import com.faforever.client.legacy.message.ServerCommand;
import com.faforever.client.legacy.message.ServerMessage;
import com.faforever.client.legacy.message.WelcomeMessage;
import com.google.gson.Gson;
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

  private final Gson gson;
  private final Socket socket;
  private OnSessionInitiatedListener onSessionInitiatedListener;
  private OnGameInfoMessageListener onGameInfoMessageListener;
  private OnPlayerInfoMessageListener onPlayerInfoMessageListener;
  private OnPingMessageListener onPingMessageListener;
  private boolean stopped;
  private OnGameLaunchMessageListener onGameLaunchMessageListenerListener;
  private OnFafLoginSucceededListener onFafLoginSucceededListener;
  private OnModInfoMessageListener onModInfoMessageListener;

  public ServerReader(Gson gson, Socket socket) {
    this.gson = gson;
    this.socket = socket;
  }

  public void blockingRead() throws IOException {
    QDataInputStream socketIn = new QDataInputStream(new DataInputStream(new BufferedInputStream(socket.getInputStream())));

    while (!stopped && !socket.isInputShutdown()) {
      socketIn.skipBlockSize();
      String message = socketIn.readQString();

      ServerCommand serverCommand = ServerCommand.fromString(message);
      if (serverCommand != null) {
        dispatchServerCommand(socketIn, serverCommand);
      } else {
        parseServerMessage(message);
      }
    }

    logger.info("Connection to server {} has been closed", socket.getRemoteSocketAddress());
  }

  private void dispatchServerCommand(QDataInputStream socketIn, ServerCommand serverCommand) throws IOException {
    switch (serverCommand) {
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
        logger.warn("Unknown server response: {}", serverCommand);
    }
  }

  private void parseServerMessage(String message) {
    try {
      logger.debug("Object from server: {}", message);
      ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);

      if ("welcome".equals(serverMessage.command)) {
        WelcomeMessage welcomeMessage = gson.fromJson(message, WelcomeMessage.class);
        if (welcomeMessage.session != null) {
          onSessionInitiatedListener.onSessionInitiated(welcomeMessage);
        } else if (welcomeMessage.email != null) {
          onFafLoginSucceededListener.onFafLoginSucceeded();
        }
      } else if ("game_info".equals(serverMessage.command)) {
        GameInfoMessage gameInfoMessage = gson.fromJson(message, GameInfoMessage.class);
        onGameInfoMessageListener.onGameInfoMessage(gameInfoMessage);
      } else if ("player_info".equals(serverMessage.command)) {
        PlayerInfoMessage playerInfoMessage = gson.fromJson(message, PlayerInfoMessage.class);
        onPlayerInfoMessageListener.onPlayerInfoMessage(playerInfoMessage);
      } else if ("game_launch".equals(serverMessage.command)) {
        GameLaunchMessage gameLaunchMessage = gson.fromJson(message, GameLaunchMessage.class);
        onGameLaunchMessageListenerListener.onGameLaunchMessage(gameLaunchMessage);
      } else if("mod_info".equals(serverMessage.command)) {
        ModInfoMessage modInfoMessage = gson.fromJson(message, ModInfoMessage.class);
        onModInfoMessageListener.onModInfoMessage(modInfoMessage);
      } else {
        logger.warn("Unknown server message: " + message);
      }
    } catch (JsonSyntaxException e) {
      logger.warn("Unhandled server message: " + message);
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
