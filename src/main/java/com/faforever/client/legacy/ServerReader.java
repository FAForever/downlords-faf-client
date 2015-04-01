package com.faforever.client.legacy;

import com.faforever.client.legacy.message.PlayerInfo;
import com.faforever.client.legacy.message.ServerMessage;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.Socket;

public class ServerReader extends Thread {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Gson gson;
  private final Socket socket;
  private OnSessionInitiatedListener onSessionInitiatedListener;
  private OnGameInfoListener onGameInfoListener;
  private OnPlayerInfoListener onPlayerInfoListener;
  private OnServerPingListener onServerPingListener;
  private boolean stopped;

  public ServerReader(Gson gson, Socket socket) {
    this.gson = gson;
    this.socket = socket;

    setDaemon(true);
  }

  @Override
  public void run() {
    while (!socket.isInputShutdown()) {
      try {
        QDataInputStream socketIn = new QDataInputStream(new DataInputStream(new BufferedInputStream(socket.getInputStream())));

        while (!stopped && !socket.isInputShutdown()) {
          socketIn.skipBlockSize();
          String message = socketIn.readQString();

          MessageType messageType = MessageType.fromString(message);
          if (messageType != null) {
            dispatchServerCommand(socketIn, messageType);
          } else {
            parseServerMessage(message);
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    logger.warn("Connection has been closed by remote host ({})", socket.getRemoteSocketAddress());
  }

  private void dispatchServerCommand(QDataInputStream socketIn, MessageType messageType) throws IOException {
    switch (messageType) {
      case PING:
        onServerPingListener.onServerPing();
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
        logger.warn("Unknown server response: {}", messageType);
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
          // Logged in
        }
      } else if ("game_info".equals(serverMessage.command)) {
        GameInfo gameInfo = gson.fromJson(message, GameInfo.class);
        onGameInfoListener.onGameInfo(gameInfo);
      } else if ("player_info".equals(serverMessage.command)) {
        PlayerInfo playerInfo = gson.fromJson(message, PlayerInfo.class);
        onPlayerInfoListener.onPlayerInfo(playerInfo);
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

  public void setOnGameInfoListener(OnGameInfoListener onGameInfoListener) {
    this.onGameInfoListener = onGameInfoListener;
  }

  public void setOnPlayerInfoListener(OnPlayerInfoListener onPlayerInfoListener) {
    this.onPlayerInfoListener = onPlayerInfoListener;
  }

  public void setOnServerPingListener(OnServerPingListener onServerPingListener) {
    this.onServerPingListener = onServerPingListener;
  }
}
