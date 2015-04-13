package com.faforever.client.legacy.relay;

import com.faforever.client.legacy.QDataInputStream;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.List;

class ServerReader implements Closeable {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final InputStream inputStream;
  private final Gson gson;
  private boolean stopped;
  private FaDataOutputStream faOutputStream;

  public ServerReader(InputStream inputStream) {
    this.inputStream = inputStream;
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();
  }

  public void blockingRead() throws IOException {
    try (QDataInputStream dataInput = new QDataInputStream(new DataInputStream(new BufferedInputStream(inputStream)))) {
      while (!stopped) {
        dataInput.skipBlockSize();
        String message = dataInput.readQString();

        logger.debug("Object from server: {}", message);

        RelayServerMessage relayServerMessage = gson.fromJson(message, RelayServerMessage.class);

        dispatchServerCommand(relayServerMessage.key, relayServerMessage.commands);
      }
    }
  }

  private void dispatchServerCommand(String command, List<Object> args) throws IOException {
    RelayServerCommand relayServerCommand = RelayServerCommand.fromString(command);

    if (relayServerCommand == null) {
      write(command, args);
      return;
    }

    switch (relayServerCommand) {
      case PING:
        handlePing();
        break;
      case SEND_NAT_PACKET:
        handleSendNatPacket(command, args);
        break;
      case P2P_RECONNECT:
        handleP2pReconnect();
        break;
      case JOIN_GAME:
        handleJoinGame();
        break;
      case CONNECT_TO_PEER:
        handleConnectToPeer();
        break;
      case CREATE_LOBBY:
        handleCreateLobby(command, args);
        break;
      case CONNECT_TO_PROXY:
        handleConnectToProxy();
        break;
      case JOIN_PROXY:
        handleJoinProxy();
        break;
      default:
        throw new IllegalStateException("Command has been defined as enum but is unhandled: " + relayServerCommand);
    }
  }

  private void handleJoinProxy() {

  }

  private void handleConnectToProxy() {

  }

  private void handleConnectToPeer() {

  }

  private void handleJoinGame() {

  }

  private void handleP2pReconnect() {

  }

  private void handleSendNatPacket(String command, List<Object> args) throws IOException {
    writeUdp(command, args);
    faOutputStream.flush();
  }

  private void handlePing() {

  }

  private void handleCreateLobby(String command, List<Object> args) throws IOException {
    // JSON doesn't know integers, but double
    int uid = ((Double) args.get(3)).intValue();

    write(command, args);
  }

  private void write(String command, List<Object> args) throws IOException {
    int headerSize = command.length();
    String headerField = command.replace("\t", "/t").replace("\n", "/n");

    faOutputStream.writeInt(headerSize);
    faOutputStream.writeString(headerField);
    faOutputStream.writeChunks(args);
  }

  private void writeUdp(String command, List<Object> args) throws IOException {
    int headerSize = command.length();
    String headerField = command.replace("\t", "/t").replace("\n", "/n");

    faOutputStream.writeInt(headerSize);
    faOutputStream.writeString(headerField);
    faOutputStream.writeUdpChunks(args);
  }

  @Override
  public void close() throws IOException {
    inputStream.close();
  }

  public void setFaOutputStream(FaDataOutputStream faOutputStream) {
    this.faOutputStream = faOutputStream;
  }
}
