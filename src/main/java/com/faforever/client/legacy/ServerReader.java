package com.faforever.client.legacy;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class ServerReader extends Thread {

  private static final Logger logger = LoggerFactory.getLogger(ServerReader.class);

  interface OnServerMessageListener {

    void onServerMessage(ServerMessage serverMessage);

    void onServerPing();
  }

  public static final int NUMBER_OF_USELESS_BYTES = 27;

  private final Gson gson;
  private final OnServerMessageListener listener;
  private final Socket socket;
  private boolean stopped;

  public ServerReader(Gson gson, Socket socket, OnServerMessageListener listener) {
    this.gson = gson;
    this.listener = listener;
    this.socket = socket;

    setDaemon(true);
  }

  @Override
  public void run() {
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

    if (socket.isInputShutdown()) {
      logger.warn("Connection has been closed by remote host ({})", socket.getRemoteSocketAddress());
    }
  }

  private void dispatchServerCommand(QDataInputStream socketIn, MessageType messageType) throws IOException {
    switch (messageType) {
      case PING:
        listener.onServerPing();
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
      logger.warn("Object from server: {}", message);
      ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);
      listener.onServerMessage(serverMessage);
    } catch (JsonSyntaxException e) {
      logger.warn("Unhandled server message: " + message);
    }
  }
}
