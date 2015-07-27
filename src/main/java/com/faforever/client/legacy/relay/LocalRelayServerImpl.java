package com.faforever.client.legacy.relay;

import com.faforever.client.legacy.io.QDataReader;
import com.faforever.client.legacy.proxy.Proxy;
import com.faforever.client.legacy.proxy.ProxyUtils;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.user.UserService;
import com.faforever.client.util.SocketAddressUtil;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

public class LocalRelayServerImpl implements LocalRelayServer, Proxy.OnProxyInitializedListener {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  Proxy proxyServer;

  @Autowired
  Environment environment;

  @Autowired
  UserService userService;

  private boolean p2pProxyEnabled;
  private int port;
  private final Gson gson;
  private FaDataOutputStream faOutputStream;
  private FaDataInputStream faInputStream;
  private ServerWriter serverWriter;
  private InputStream fafInputStream;

  public LocalRelayServerImpl() {
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(RelayServerCommand.class, new RelayServerCommandTypeAdapter())
        .create();
  }

  @Override
  public int getPort() {
    return port;
  }

  @PostConstruct
  void postConstruct() {
    startInBackground();
  }

  /**
   * Starts a local, GPG-like server in background that FA can connect to. Received data is forwarded to the FAF server
   * and vice-versa.
   */
  @Override
  public void startInBackground() {
    proxyServer.addOnProxyInitializedListener(this);

    executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        start();
        return null;
      }
    });
  }

  private void start() throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {

      port = serverSocket.getLocalPort();

      logger.info("Relay server listening on port {}", port);

      while (!isCancelled()) {
        try (Socket faSocket = serverSocket.accept()) {
          logger.debug("Forged Alliance connected to relay server from {}:{}", faSocket.getInetAddress(), faSocket.getPort());

          try (Socket fafSocket = new Socket(environment.getProperty("relay.host"), environment.getProperty("relay.port", int.class));
               FaDataInputStream faInputStream = createFaInputStream(faSocket.getInputStream());
               FaDataOutputStream faOutputStream = createFaOutputStream(faSocket.getOutputStream());
               ServerWriter serverWriter = createServerWriter(fafSocket)) {
            this.faInputStream = faInputStream;
            this.faOutputStream = faOutputStream;
            this.serverWriter = serverWriter;
            this.fafInputStream = fafSocket.getInputStream();

            serverWriter.write(new RelayClientMessage(RelayServerAction.AUTHENTICATE, Collections.singletonList(userService.getSessionId())));

            startFaReader();
            redirectFafToFa();
          }
        } catch (SocketException | EOFException e) {
          logger.debug("Forged Alliance disconnected from relay server");
        }
      }
    }
  }

  private ServerWriter createServerWriter(Socket fafSocket) throws IOException {
    ServerWriter serverWriter = new ServerWriter(fafSocket.getOutputStream());
    serverWriter.registerMessageSerializer(new RelayClientMessageSerializer(), RelayClientMessage.class);
    return serverWriter;
  }

  private boolean isCancelled() {
    return false;
  }

  private FaDataInputStream createFaInputStream(InputStream inputStream) throws IOException {
    return new FaDataInputStream(inputStream);
  }

  private FaDataOutputStream createFaOutputStream(OutputStream outputStream) {
    return new FaDataOutputStream(outputStream);
  }

  /**
   * Starts a background task that reads data from FA and redirects it to the given ServerWriter.
   */
  private void startFaReader() {
    executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        try {
          redirectFaToFaf(faInputStream, serverWriter);
        } catch (EOFException | SocketException e) {
          logger.info("Forged Alliance disconnected from local relay server (EOF)");
        }
        return null;
      }
    });
  }

  /**
   * Reads data from the FAF server and redirects it to FA.
   */
  private void redirectFafToFa() throws IOException {
    try (QDataReader dataInput = new QDataReader(new DataInputStream(new BufferedInputStream(fafInputStream)))) {
      dataInput.skipBlockSize();
      String message = dataInput.readQString();

      logger.debug("Message from FAF relay server: {}", message);

      RelayServerMessage relayServerMessage = gson.fromJson(message, RelayServerMessage.class);

      dispatchServerCommand(relayServerMessage.getCommand(), message);
    } catch (EOFException e) {
      logger.info("Disconnected from FAF relay server (EOF)");
    }
  }

  /**
   * Redirects any data read from the #faInputStream to the specified #serverWriter.
   *
   * @param faInputStream game input stream
   * @param serverWriter FAF relay server writer
   *
   * @throws IOException
   */
  private void redirectFaToFaf(FaDataInputStream faInputStream, ServerWriter serverWriter) throws IOException {

    while (!isCancelled()) {
      RelayServerAction action = RelayServerAction.fromString(faInputStream.readString());
      List<Object> chunks = faInputStream.readChunks();
      RelayClientMessage relayClientMessage = new RelayClientMessage(action, chunks);

      if (p2pProxyEnabled) {
        updateProxyState(relayClientMessage);
      }

      serverWriter.write(relayClientMessage);
    }
  }

  private void updateProxyState(RelayClientMessage relayClientMessage) {
    RelayServerAction action = relayClientMessage.getAction();
    List<Object> chunks = relayClientMessage.getChunks();

    logger.debug("Received '{}' with chunks: {}", action.getString(), chunks);

    switch (action) {
      case PROCESS_NAT_PACKET:
        chunks.set(0, proxyServer.translateToPublic((String) chunks.get(0)));
        break;
      case DISCONNECTED:
        proxyServer.updateConnectedState((Integer) chunks.get(0), false);
        break;
      case CONNECTED:
        proxyServer.updateConnectedState((Integer) chunks.get(0), true);
        break;
      case GAME_STATE:
        switch ((String) chunks.get(0)) {
          case "Launching":
            proxyServer.setGameLaunched(true);
            break;
          case "Lobby":
            proxyServer.setGameLaunched(false);
            break;
        }
        break;
      case BOTTLENECK:
        proxyServer.setBottleneck(true);
        break;
      case BOTTLENECK_CLEARED:
        proxyServer.setBottleneck(false);
        break;

      default:
        // Do nothing
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
    int peerUid = createLobbyMessage.getUid();
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
    connectToPeerMessage.setPeerAddress(SocketAddressUtil.toString(proxySocket));
    connectToPeerMessage.setUsername(connectToProxyMessage.getUsername());
    connectToPeerMessage.setPeerUid(connectToProxyMessage.getPeerUid());

    writeToFa(connectToPeerMessage);
  }

  private void handleJoinProxy(JoinProxyMessage joinProxyMessage) throws IOException {
    int playerNumber = joinProxyMessage.getPlayerNumber();
    int peerUid = joinProxyMessage.getPeerUid();

    InetSocketAddress proxySocket = proxyServer.bindAndGetProxySocketAddress(playerNumber, peerUid);

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
  public void onProxyInitialized() {
    p2pProxyEnabled = true;
  }
}
