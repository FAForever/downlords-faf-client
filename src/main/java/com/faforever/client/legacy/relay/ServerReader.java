package com.faforever.client.legacy.relay;

import com.faforever.client.legacy.QDataInputStream;
import com.faforever.client.legacy.ServerWriter;
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
import java.util.Arrays;
import java.util.List;

import static com.faforever.client.legacy.relay.RelayServerCommand.CONNECT_TO_PEER;
import static com.faforever.client.legacy.relay.RelayServerCommand.JOIN_GAME;

class ServerReader implements Closeable {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final InputStream inputStream;
  private final Proxy proxyServer;
  private final FaDataOutputStream faOutputStream;
  private final ServerWriter serverWriter;
  private final Gson gson;

  private boolean stopped;
  private boolean p2pProxyEnabled;

  public ServerReader(InputStream inputStream, Proxy proxyServer, FaDataOutputStream faOutputStream, ServerWriter serverWriter) {
    this.inputStream = inputStream;
    this.proxyServer = proxyServer;
    this.faOutputStream = faOutputStream;
    this.serverWriter = serverWriter;

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
    } catch (EOFException e) {
      logger.info("Disconnected from FAF relay server (EOF)");
    }
  }

  private void dispatchServerCommand(String command, List<Object> args) throws IOException {
    RelayServerCommand relayServerCommand = RelayServerCommand.fromString(command);

    if (relayServerCommand == null) {
      logger.debug("Command '{}' is unknown, putting it straight trough", command, args);

      // No special server command, put it straight through
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
        handleJoinGame(command, args);
        break;
      case CONNECT_TO_PEER:
        handleConnectToPeer(command, args);
        break;
      case CREATE_LOBBY:
        handleCreateLobby(command, args);
        break;
      case CONNECT_TO_PROXY:
        handleConnectToProxy(command, args);
        break;
      case JOIN_PROXY:
        handleJoinProxy(command, args);
        break;
      default:
        throw new IllegalStateException("Command is know but unhandled: " + relayServerCommand);
    }
  }

  private void handlePing() {
    logger.debug("Received ping from server, answering pong");
    serverWriter.write(RelayClientMessage.pong());
  }

  private void handleSendNatPacket(String command, List<Object> args) throws IOException {
    if (p2pProxyEnabled) {
      String publicAddress = (String) args.get(0);

      proxyServer.registerPeerIfNecessary(publicAddress);

      args.set(0, proxyServer.translateToLocal(publicAddress));
    }

    writeUdp(command, args);
  }

  private void handleP2pReconnect() throws SocketException {
    proxyServer.initialize();
    p2pProxyEnabled = true;
  }

  private void handleJoinGame(String command, List<Object> args) throws IOException {
    if (p2pProxyEnabled) {
      String peerAddress = (String) args.get(0);
      int peerUid = extractInt(args.get(2));

      proxyServer.registerPeerIfNecessary(peerAddress);

      args.set(0, proxyServer.translateToLocal(peerAddress));
      proxyServer.setUidForPeer(peerAddress, peerUid);
    }

    write(command, args);
  }

  private static int extractInt(Object object) {
    // JSON doesn't know integers, but double
    return ((Double) object).intValue();
  }

  private void handleConnectToPeer(String command, List<Object> args) throws IOException {
    handleJoinGame(command, args);
  }

  private void handleCreateLobby(String command, List<Object> args) throws IOException {
    int peerUid = extractInt(args.get(3));
    proxyServer.setUid(peerUid);

    if (p2pProxyEnabled) {
      args.set(1, ProxyUtils.translateToProxyPort(proxyServer.getPort()));
    }

    write(command, args);
  }

  private void handleConnectToProxy(String command, List<Object> args) throws IOException {
    int port = extractInt(args.get(0));
    String login = (String) args.get(2);
    int uid = extractInt(args.get(3));

    InetSocketAddress socketAddress = proxyServer.bindSocket(port, uid);

    List<Object> newArgs = Arrays.asList(
        SocketAddressUtil.toString(socketAddress),
        login,
        uid
    );

    write(CONNECT_TO_PEER.getString(), newArgs);
  }

  private void handleJoinProxy(String command, List<Object> args) throws IOException {
    int port = extractInt(args.get(0));
    String login = (String) args.get(2);
    int uid = extractInt(args.get(3));

    InetSocketAddress socketAddress = proxyServer.bindSocket(port, uid);

    List<Object> newArgs = Arrays.asList(
        SocketAddressUtil.toString(socketAddress),
        login,
        uid
    );

    write(JOIN_GAME.getString(), newArgs);

  }

  private void writeUdp(String command, List<Object> chunks) throws IOException {
    int headerSize = command.length();
    String headerField = command.replace("\t", "/t").replace("\n", "/n");

    logger.debug("Writing data to FA, command: {}, chunks: {}", command, chunks);

    faOutputStream.writeInt(headerSize);
    faOutputStream.writeString(headerField);
    faOutputStream.writeUdpChunks(chunks);
    faOutputStream.flush();
  }

  private void write(String command, List<Object> chunks) throws IOException {
    int headerSize = command.length();
    String headerField = command.replace("\t", "/t").replace("\n", "/n");

    logger.debug("Writing data to FA, command: {}, chunks: {}", command, chunks);

    faOutputStream.writeInt(headerSize);
    faOutputStream.writeString(headerField);
    faOutputStream.writeChunks(chunks);
    faOutputStream.flush();
  }

  @Override
  public void close() throws IOException {
    inputStream.close();
  }
}
