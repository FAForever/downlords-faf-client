package com.faforever.client.legacy.relay;

import com.faforever.client.legacy.writer.QDataReader;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.legacy.proxy.Proxy;
import com.faforever.client.legacy.proxy.ProxyUtils;
import com.faforever.client.util.SocketAddressUtil;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * Reads data from FA and relays it to the FAF server (and vice-versa).
 */
class Relayer implements Closeable {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final InputStream inputStream;
  private final Proxy proxyServer;
  private final FaDataOutputStream faOutputStream;
  private final ServerWriter serverWriter;
  private final Gson gson;

  private boolean stopped;
  private boolean p2pProxyEnabled;

  public Relayer(InputStream inputStream, Proxy proxyServer, FaDataOutputStream faOutputStream, ServerWriter serverWriter) {
    this.inputStream = inputStream;
    this.proxyServer = proxyServer;
    this.faOutputStream = faOutputStream;
    this.serverWriter = serverWriter;

    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(RelayServerCommand.class, new RelayServerCommandTypeAdapter())
        .create();
  }

  public void blockingRead() throws IOException {
    try (QDataReader dataInput = new QDataReader(new DataInputStream(new BufferedInputStream(inputStream)))) {
      while (!stopped) {
        dataInput.skipBlockSize();
        String message = dataInput.readQString();

        logger.debug("Object from FAF relay server: {}", message);

        RelayServerMessage relayServerMessage = gson.fromJson(message, RelayServerMessage.class);

        dispatchServerCommand(relayServerMessage.getCommand(), message);
      }
    } catch (EOFException e) {
      logger.info("Disconnected from FAF relay server (EOF)");
    }
  }

  private void dispatchServerCommand(RelayServerCommand command, String jsonString) throws IOException {
    switch (command) {
      case PING:
        handlePing();
        break;
      case HOST_GAME:
        HostGameMessage hostGameMessage = gson.fromJson(jsonString, HostGameMessage.class);
        handleHostGame(hostGameMessage);
        break;
      case SEND_NAT_PACKET:
        SendNatPacketMessage sendNatPacketMessage = gson.fromJson(jsonString, SendNatPacketMessage.class);
        handleSendNatPacket(sendNatPacketMessage);
        break;
      case P2P_RECONNECT:
        handleP2pReconnect();
        break;
      case JOIN_GAME:
        JoinGameMessage joinGameMessage = gson.fromJson(jsonString, JoinGameMessage.class);
        handleJoinGame(joinGameMessage);
        break;
      case CONNECT_TO_PEER:
        ConnectToPeerMessage connectToPeerMessage = gson.fromJson(jsonString, ConnectToPeerMessage.class);
        handleConnectToPeer(connectToPeerMessage);
        break;
      case CREATE_LOBBY:
        CreateLobbyMessage createLobbyMessage = gson.fromJson(jsonString, CreateLobbyMessage.class);
        handleCreateLobby(createLobbyMessage);
        break;
      case DISCONNECT_FROM_PEER:
        DisconnectFromPeerMessage disconnectFromPeerMessage = gson.fromJson(jsonString, DisconnectFromPeerMessage.class);
        handleDisconnectFromPeer(disconnectFromPeerMessage);
        break;
      case CONNECT_TO_PROXY:
        ConnectToProxyMessage connectToProxyMessage = gson.fromJson(jsonString, ConnectToProxyMessage.class);
        handleConnectToProxy(connectToProxyMessage);
        break;
      case JOIN_PROXY:
        JoinProxyMessage joinProxyMessage = gson.fromJson(jsonString, JoinProxyMessage.class);
        handleJoinProxy(joinProxyMessage);
        break;

      default:
        throw new IllegalStateException("Unhandled relay server command: " + command);
    }
  }

  private void handleDisconnectFromPeer(DisconnectFromPeerMessage disconnectFromPeerMessage) throws IOException {
    writeToFa(disconnectFromPeerMessage);
  }

  private void handleHostGame(HostGameMessage hostGameMessage) throws IOException {
    writeToFa(hostGameMessage);
  }

  private void handlePing() {
    serverWriter.write(RelayClientMessage.pong());
  }

  private void handleSendNatPacket(SendNatPacketMessage sendNatPacketMessage) throws IOException {
    if (p2pProxyEnabled) {
      String publicAddress = sendNatPacketMessage.getPublicAddress();

      proxyServer.registerPeerIfNecessary(publicAddress);

      sendNatPacketMessage.setPublicAddress(proxyServer.translateToLocal(publicAddress));
    }

    writeToFaUdp(sendNatPacketMessage);
  }

  private void handleP2pReconnect() throws SocketException {
    proxyServer.initializeP2pProxy();
    p2pProxyEnabled = true;
  }

  private void handleConnectToPeer(ConnectToPeerMessage connectToPeerMessage) throws IOException {
    if (p2pProxyEnabled) {
      String peerAddress = connectToPeerMessage.getPeerAddress();
      int peerUid = connectToPeerMessage.getPeerUid();

      proxyServer.registerPeerIfNecessary(peerAddress);

      connectToPeerMessage.setPeerAddress(proxyServer.translateToLocal(peerAddress));
      proxyServer.setUidForPeer(peerAddress, peerUid);
    }

    writeToFa(connectToPeerMessage);
  }

  private void handleJoinGame(JoinGameMessage joinGameMessage) throws IOException {
    if (p2pProxyEnabled) {
      String peerAddress = joinGameMessage.getPeerAddress();
      int peerUid = joinGameMessage.getPeerUid();

      proxyServer.registerPeerIfNecessary(peerAddress);

      joinGameMessage.setPeerAddress(proxyServer.translateToLocal(peerAddress));
      proxyServer.setUidForPeer(peerAddress, peerUid);
    }

    writeToFa(joinGameMessage);
  }

  private void handleCreateLobby(CreateLobbyMessage createLobbyMessage) throws IOException {
    int peerUid = createLobbyMessage.getPeerUid();
    proxyServer.setUid(peerUid);

    if (p2pProxyEnabled) {
      createLobbyMessage.setPort(ProxyUtils.translateToProxyPort(proxyServer.getPort()));
    }

    writeToFa(createLobbyMessage);
  }

  private void handleConnectToProxy(ConnectToProxyMessage connectToProxyMessage) throws IOException {
    int playerNumber = connectToProxyMessage.getPlayerNumber();
    int peerUid = connectToProxyMessage.getPeerUid();

    InetSocketAddress proxySocket = proxyServer.bindAndGetProxySocketAddress(playerNumber, peerUid);

    // Ask FA to connect to the other player via the local proxy port
    ConnectToPeerMessage connectToPeerMessage = new ConnectToPeerMessage();
    connectToPeerMessage.setPlayerNumber(playerNumber);
    connectToPeerMessage.setPeerAddress(SocketAddressUtil.toString(proxySocket));
    connectToPeerMessage.setUsername(connectToProxyMessage.getUsername());
    connectToPeerMessage.setPeerUid(connectToProxyMessage.getPeerUid());

    writeToFa(connectToPeerMessage);
  }

  private void handleJoinProxy(JoinProxyMessage joinProxyMessage) throws IOException {
    int playerId = joinProxyMessage.getPlayerNumber();
    int peerUid = joinProxyMessage.getPeerUid();

    InetSocketAddress proxySocket = proxyServer.bindAndGetProxySocketAddress(playerId, peerUid);

    // Ask FA to join the game via the local proxy port
    JoinGameMessage joinGameMessage = new JoinGameMessage();
    joinGameMessage.setPeerAddress(SocketAddressUtil.toString(proxySocket));
    joinGameMessage.setUsername(joinProxyMessage.getUsername());
    joinGameMessage.setPeerUid(joinProxyMessage.getPeerUid());

    writeToFa(joinGameMessage);
  }

  private void writeToFaUdp(RelayServerMessage relayServerMessage) throws IOException {
    String commandString = relayServerMessage.getCommand().getString();

    int headerSize = commandString.length();
    String headerField = commandString.replace("\t", "/t").replace("\n", "/n");

    logger.debug("Writing data to FA, command: {}, args: {}", commandString, relayServerMessage.getArgs());

    faOutputStream.writeInt(headerSize);
    faOutputStream.writeString(headerField);
    faOutputStream.writeUdpArgs(relayServerMessage.getArgs());
    faOutputStream.flush();
  }

  private void writeToFa(RelayServerMessage relayServerMessage) throws IOException {
    String commandString = relayServerMessage.getCommand().getString();

    int headerSize = commandString.length();
    String headerField = commandString.replace("\t", "/t").replace("\n", "/n");

    logger.debug("Writing data to FA, command: {}, args: {}", commandString, relayServerMessage.getArgs());

    faOutputStream.writeInt(headerSize);
    faOutputStream.writeString(headerField);
    faOutputStream.writeArgs(relayServerMessage.getArgs());
    faOutputStream.flush();
  }

  @Override
  public void close() throws IOException {
    inputStream.close();
  }
}
