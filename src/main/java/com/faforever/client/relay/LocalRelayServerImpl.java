package com.faforever.client.relay;

import com.faforever.client.connectivity.ConnectivityService;
import com.faforever.client.connectivity.TurnClient;
import com.faforever.client.game.GameType;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.domain.GameLaunchMessage;
import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.legacy.gson.GpgServerMessageTypeTypeAdapter;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.ConcurrentUtil;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.concurrent.Task;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class LocalRelayServerImpl implements LocalRelayServer {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Gson gson;
  private final Collection<Runnable> onConnectionAcceptedListeners;

  @Resource
  TurnClient turnClient;
  @Resource
  UserService userService;
  @Resource
  PreferencesService preferencesService;
  @Resource
  LobbyServerAccessor lobbyServerAccessor;
  @Resource
  ExecutorService executorService;
  @Resource
  ConnectivityService connectivityService;

  private FaDataOutputStream gameOutputStream;
  private FaDataInputStream gameInputStream;
  private LobbyMode lobbyMode;
  private ServerSocket serverSocket;
  private boolean stopped;
  private Socket gameSocket;
  private Map<Integer, DatagramSocket> peerRelaySockets;
  private DatagramSocket publicSocket;
  private Consumer<DatagramPacket> forwarder;
  private boolean useTurn;
  private CompletableFuture<Integer> portFuture;

  public LocalRelayServerImpl() {
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(GpgServerMessageType.class, GpgServerMessageTypeTypeAdapter.INSTANCE)
        .create();
    peerRelaySockets = new HashMap<>();
    onConnectionAcceptedListeners = new ArrayList<>();
    lobbyMode = LobbyMode.DEFAULT_LOBBY;
  }

  @Override
  public void addOnConnectionAcceptedListener(Runnable listener) {
    onConnectionAcceptedListeners.add(listener);
  }

  /**
   * Starts a local, GPG-like server in background that FA can connect to. Received data is forwarded to the FAF server
   * and vice-versa.
   */
  @Override
  public CompletableFuture<Integer> startInBackground() {
    portFuture = new CompletableFuture<>();
    CompletableFuture.runAsync(() -> {
      try {
        start();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }, executorService);
    return portFuture;
  }

  @Override
  @PreDestroy
  public void close() {
    logger.info("Closing relay server");
    stopped = true;
    IOUtils.closeQuietly(serverSocket);
    IOUtils.closeQuietly(gameSocket);
    IOUtils.closeQuietly(turnClient);
  }

  @PostConstruct
  void postConstruct() {
    startInBackground();
    lobbyServerAccessor.addOnMessageListener(GameLaunchMessage.class, this::updateLobbyModeFromGameInfo);
    lobbyServerAccessor.addOnMessageListener(GpgServerMessage.class, this::onGpgServerMessage);
  }

  private void start() throws IOException {
    switch (connectivityService.getConnectivityState()) {
      case BLOCKED:
        forwarder = turnForwarder();
        useTurn = true;
        break;

      default:
        forwarder = publicForwarder();
    }

    try (ServerSocket serverSocket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
      this.serverSocket = serverSocket;
      int localPort = serverSocket.getLocalPort();
      portFuture.complete(localPort);

      logger.info("Relay server listening on port {}, using turn: {}", localPort, useTurn);

      while (!stopped) {
        try (Socket faSocket = serverSocket.accept()) {
          this.gameSocket = faSocket;
          logger.debug("Forged Alliance connected to relay server from {}:{}", faSocket.getInetAddress(), faSocket.getPort());

          onConnectionAcceptedListeners.forEach(Runnable::run);

          this.gameInputStream = createFaInputStream(faSocket.getInputStream());
          this.gameOutputStream = createFaOutputStream(faSocket.getOutputStream());

          redirectGameToServer();
        }
      }
    }
  }

  /**
   * Creates a forwarder that forwards datagrams through a TURN server.
   */
  private Consumer<DatagramPacket> turnForwarder() {
    return datagramPacket -> turnClient.send(datagramPacket);
  }

  /**
   * Creates a forwarder that forwards datagrams directly from an UDP socket.
   */
  private Consumer<DatagramPacket> publicForwarder() {
    return datagramPacket -> {
      try {
        int port = preferencesService.getPreferences().getForgedAlliance().getPort();
        publicSocket = new DatagramSocket(port);
        publicSocket.send(datagramPacket);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  private FaDataInputStream createFaInputStream(InputStream inputStream) {
    return new FaDataInputStream(inputStream);
  }

  private FaDataOutputStream createFaOutputStream(OutputStream outputStream) {
    return new FaDataOutputStream(outputStream);
  }

  /**
   * Starts a background task that reads data from FA and redirects it to the given ServerWriter.
   */
  private void redirectGameToServer() {
    try {
      while (true) {
        String message = gameInputStream.readString();

        logger.debug("Received message from FA: {}", message);

        List<Object> chunks = gameInputStream.readChunks();
        GpgClientMessage gpgClientMessage = new GpgClientMessage(message, chunks);

        handleDataFromFa(gpgClientMessage);
      }
    } catch (IOException e) {
      logger.info("Forged Alliance disconnected from local relay server (" + e.getMessage() + ")");
    } finally {
      logger.debug("No longer redirecting from game to server");
      disconnect();
    }
  }

  private void handleDataFromFa(GpgClientMessage gpgClientMessage) throws IOException {
    if (isHostGameMessage(gpgClientMessage)) {
      String username = userService.getUsername();
      if (lobbyMode == null) {
        throw new IllegalStateException("lobbyMode has not been set");
      }

      int gamePort = preferencesService.getPreferences().getForgedAlliance().getPort();
      handleCreateLobby(new CreateLobbyServerMessage(lobbyMode, gamePort, username, userService.getUid(), 1));
    }

    lobbyServerAccessor.sendGpgMessage(gpgClientMessage);
  }

  private void disconnect() {
    IOUtils.closeQuietly(gameSocket);
  }

  private boolean isHostGameMessage(GpgClientMessage gpgClientMessage) {
    return gpgClientMessage.getCommand() == GpgClientCommand.GAME_STATE
        && gpgClientMessage.getArgs().get(0).equals("Idle");
  }

  private void handleCreateLobby(CreateLobbyServerMessage createLobbyServerMessage) throws IOException {

    writeToFa(createLobbyServerMessage);
  }

  private void writeToFa(GpgServerMessage gpgServerMessage) throws IOException {
    writeHeader(gpgServerMessage);
    gameOutputStream.writeArgs(gpgServerMessage.getArgs());
    gameOutputStream.flush();
  }

  private void writeHeader(GpgServerMessage gpgServerMessage) throws IOException {
    String commandString = gpgServerMessage.getMessageType().getString();

    int headerSize = commandString.length();
    String headerField = commandString.replace("\t", "/t").replace("\n", "/n");

    logger.debug("Writing data to FA, command: {}, args: {}", commandString, gpgServerMessage.getArgs());

    gameOutputStream.writeInt(headerSize);
    gameOutputStream.writeString(headerField);
  }

  private void updateLobbyModeFromGameInfo(GameLaunchMessage gameLaunchMessage) {
    if (GameType.LADDER_1V1.getString().equals(gameLaunchMessage.getMod())) {
      lobbyMode = LobbyMode.NO_LOBBY;
    } else {
      lobbyMode = LobbyMode.DEFAULT_LOBBY;
    }
  }

  private void onGpgServerMessage(GpgServerMessage message) {
    if (message.getTarget() != MessageTarget.GAME) {
      return;
    }
    try {
      dispatchServerCommand(message.getMessageType(), message.getJsonString());
    } catch (IOException e) {
      logger.warn("Error while handling message: " + message, e);
    }
  }

  private void dispatchServerCommand(GpgServerMessageType messageType, String jsonString) throws IOException {
    switch (messageType) {
      case HOST_GAME:
        HostGameMessage hostGameMessage = gson.fromJson(jsonString, HostGameMessage.class);
        handleHostGame(hostGameMessage);
        break;
      case SEND_NAT_PACKET:
        SendNatPacketMessage sendNatPacketMessage = gson.fromJson(jsonString, SendNatPacketMessage.class);
        handleSendNatPacket(sendNatPacketMessage);
        break;
      case P2P_RECONNECT:
        logger.warn("P2P Reconnect has not been implemented");
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
        // CreateLobby was already sent to FAF by this relayer, so the server message is discarded
        logger.debug("Discarding message from server: {}", jsonString);
        break;
      case DISCONNECT_FROM_PEER:
        DisconnectFromPeerMessage disconnectFromPeerMessage = gson.fromJson(jsonString, DisconnectFromPeerMessage.class);
        handleDisconnectFromPeer(disconnectFromPeerMessage);
        break;
      case JOIN_PROXY:
        logger.warn("Server unexpectedly asked to join proxy");
        break;

      default:
        throw new IllegalStateException("Unhandled server message type: " + messageType);
    }
  }

  private void handleDisconnectFromPeer(DisconnectFromPeerMessage disconnectFromPeerMessage) throws IOException {
    writeToFa(disconnectFromPeerMessage);
  }

  private void handleHostGame(HostGameMessage hostGameMessage) throws IOException {
    writeToFa(hostGameMessage);
  }

  private void handleSendNatPacket(SendNatPacketMessage sendNatPacketMessage) throws IOException {
    writeToFaUdp(sendNatPacketMessage);
  }

  private void handleConnectToPeer(ConnectToPeerMessage connectToPeerMessage) throws IOException {
    int peerUid = connectToPeerMessage.getPeerUid();
    SocketAddress peerSocketAddress = getSocketForPeer(peerUid).getLocalSocketAddress();

    connectToPeerMessage.setPeerAddress((InetSocketAddress) peerSocketAddress);

    writeToFa(connectToPeerMessage);
  }

  /**
   * Opens a local UDP socket that serves as a proxy for a peer to FA. In other words, instead of connecting to peer X
   * directly, a local port is opened that represents that peer. FA will then connect to that port, thinking it's peer
   * X.
   *
   * @return the UDP socket the peer has been bound to
   */
  private DatagramSocket getSocketForPeer(int peerUid) throws SocketException {
    if (!peerRelaySockets.containsKey(peerUid)) {
      peerRelaySockets.put(peerUid, new DatagramSocket(0));
    }
    DatagramSocket datagramSocket = peerRelaySockets.get(peerUid);
    forwardSocket(datagramSocket, forwarder);
    return datagramSocket;
  }

  private void handleJoinGame(JoinGameMessage joinGameMessage) throws IOException {
    int peerUid = joinGameMessage.getPeerUid();
    SocketAddress peerSocketAddress = getSocketForPeer(peerUid).getLocalSocketAddress();

    joinGameMessage.setPeerAddress((InetSocketAddress) peerSocketAddress);

    writeToFa(joinGameMessage);
  }

  private void writeToFaUdp(GpgServerMessage gpgServerMessage) throws IOException {
    writeHeader(gpgServerMessage);
    gameOutputStream.writeUdpArgs(gpgServerMessage.getArgs());
    gameOutputStream.flush();
  }

  private void forwardSocket(final DatagramSocket socket, Consumer<DatagramPacket> forwarder) {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        byte[] buffer = new byte[8092];
        DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

        while (!isCancelled()) {
          socket.receive(datagramPacket);
          forwarder.accept(datagramPacket);
        }

        return null;
      }
    });
  }
}
