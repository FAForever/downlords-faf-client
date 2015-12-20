package com.faforever.client.relay;

import com.faforever.client.connectivity.ConnectivityService;
import com.faforever.client.connectivity.ConnectivityState;
import com.faforever.client.connectivity.TurnClient;
import com.faforever.client.game.GameType;
import com.faforever.client.legacy.domain.GameLaunchMessage;
import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.legacy.gson.GpgServerMessageTypeTypeAdapter;
import com.faforever.client.legacy.gson.MessageTargetTypeAdapter;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.SocketAddressUtil;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SocketUtils;

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
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Acts as a layer between the "outside world" and the game, like a NAT.
 */
public class LocalRelayServerImpl implements LocalRelayServer {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Gson gson;
  private final Collection<Runnable> onConnectionAcceptedListeners;
  private final Collection<Consumer<DatagramPacket>> onPacketFromOutsideListeners;
  @Resource
  UserService userService;
  @Resource
  PreferencesService preferencesService;
  @Resource
  FafService fafService;
  @Resource
  ExecutorService executorService;
  @Resource
  ConnectivityService connectivityService;
  @Resource
  TurnClient turnClient;
  private FaDataOutputStream gameOutputStream;
  private FaDataInputStream gameInputStream;
  private LobbyMode lobbyMode;
  private ServerSocket serverSocket;
  private boolean stopped;
  private Socket gameSocket;
  /**
   * Maps peer UIDs to relay sockets.
   */
  private Map<Integer, DatagramSocket> peerSocketsByUid;
  private CompletableFuture<Integer> gpgPortFuture;
  private int faGamePort;
  /**
   * A consumer that forwards game packets to the "outside world".
   */
  private Consumer<DatagramPacket> gameDataForwarder;
  /**
   * The socket to the "outside", receives and sends game data.
   */
  private DatagramSocket publicSocket;

  public LocalRelayServerImpl() {
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(GpgServerMessageType.class, GpgServerMessageTypeTypeAdapter.INSTANCE)
        .registerTypeAdapter(MessageTarget.class, MessageTargetTypeAdapter.INSTANCE)
        .create();
    peerSocketsByUid = new HashMap<>();
    onConnectionAcceptedListeners = new ArrayList<>();
    onPacketFromOutsideListeners = new ArrayList<>();
    lobbyMode = LobbyMode.DEFAULT_LOBBY;
  }

  @Override
  public void addOnPacketFromOutsideListener(Consumer<DatagramPacket> listener) {
    synchronized (onPacketFromOutsideListeners) {
      onPacketFromOutsideListeners.add(listener);
    }
  }

  @Override
  public void addOnConnectionAcceptedListener(Runnable listener) {
    onConnectionAcceptedListeners.add(listener);
  }

  @Override
  public Integer getGpgRelayPort() {
    try {
      return gpgPortFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Integer getPublicPort() {
    return publicSocket.getLocalPort();
  }

  @Override
  public InetSocketAddress getRelayAddress() {
    return turnClient.getRelayAddress();
  }

  @Override
  public void removeOnPackedFromOutsideListener(Consumer<DatagramPacket> listener) {
    synchronized (onPacketFromOutsideListeners) {
      onPacketFromOutsideListeners.remove(listener);
    }
  }

  private void start() {
    try (ServerSocket serverSocket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
      this.serverSocket = serverSocket;

      int localPort = serverSocket.getLocalPort();
      gpgPortFuture.complete(localPort);

      logger.info("GPG relay server listening on port {}", localPort);

      while (!stopped) {
        try (Socket faSocket = serverSocket.accept()) {
          this.gameSocket = faSocket;
          logger.debug("Forged Alliance connected to relay server from {}:{}", faSocket.getInetAddress(), faSocket.getPort());

          onConnectionAcceptedListeners.forEach(Runnable::run);

          this.gameInputStream = createFaInputStream(faSocket.getInputStream());
          this.gameOutputStream = createFaOutputStream(faSocket.getOutputStream());

          redirectGpgConnection();
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
  private void redirectGpgConnection() {
    try {
      while (true) {
        String message = gameInputStream.readString();

        List<Object> chunks = gameInputStream.readChunks();
        GpgClientMessage gpgClientMessage = new GpgClientMessage(message, chunks);

        handleDataFromFa(gpgClientMessage);
      }
    } catch (IOException e) {
      logger.info("Forged Alliance disconnected from local relay server (" + e.getMessage() + ")");
    }
  }

  private void handleDataFromFa(GpgClientMessage gpgClientMessage) throws IOException {
    if (isHostGameMessage(gpgClientMessage)) {
      String username = userService.getUsername();
      if (lobbyMode == null) {
        throw new IllegalStateException("lobbyMode has not been set");
      }

      faGamePort = SocketUtils.findAvailableUdpPort();
      handleCreateLobby(new CreateLobbyServerMessage(lobbyMode, faGamePort, username, userService.getUid(), 1));
    }

    fafService.sendGpgMessage(gpgClientMessage);
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

  @PreDestroy
  void close() {
    logger.info("Closing relay server");
    stopped = true;
    IOUtils.closeQuietly(publicSocket);
    IOUtils.closeQuietly(serverSocket);
    IOUtils.closeQuietly(gameSocket);
  }

  private void onSendNatPacket(SendNatPacketMessage sendNatPacketMessage) {
    InetSocketAddress received = sendNatPacketMessage.getPublicAddress();
    String message = sendNatPacketMessage.getMessage();

    logger.debug("Sending NAT packet to {}: {}", received, message);

    byte[] bytes = (message).getBytes(US_ASCII);
    DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
    datagramPacket.setSocketAddress(received);
    try {
      publicSocket.send(datagramPacket);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @PostConstruct
  void postConstruct() {
    connectivityService.connectivityStateProperty().addListener((observable, oldValue, newValue) -> {
      onConnectivityStateChanged(newValue);
    });
    onConnectivityStateChanged(connectivityService.getConnectivityState());

    fafService.addOnMessageListener(GameLaunchMessage.class, this::updateLobbyModeFromGameInfo);
    fafService.addOnMessageListener(SendNatPacketMessage.class, this::onSendNatPacket);
    fafService.addOnMessageListener(GpgServerMessage.class, this::onGpgServerMessage);

    preferencesService.getPreferences().getForgedAlliance().portProperty().addListener((observable, oldValue, newValue) -> {
      initPublicSocket(newValue.intValue());
    });
    initPublicSocket(preferencesService.getPreferences().getForgedAlliance().getPort());

    gpgPortFuture = new CompletableFuture<>();
    CompletableFuture.runAsync(this::start, executorService);
  }

  private void onConnectivityStateChanged(ConnectivityState newValue) {
    switch (newValue) {
      case UNKNOWN:
      case PUBLIC:
        gameDataForwarder = publicForwarder();
        break;
      case STUN:
        gameDataForwarder = turnForwarder();
        turnClient.connect();
        break;
      case BLOCKED:
        throw new IllegalStateException("Can't connect");
      default:
        throw new AssertionError("Uncovered connectivity state: " + newValue);
    }
  }

  /**
   * Opens the "public" UDP socket. This is a proxy for the game socket; all data send from the game is sent through
   * this socket, and all data received on this socket is forwarded to the game.
   */
  private void initPublicSocket(int port) {
    IOUtils.closeQuietly(publicSocket);

    try {
      publicSocket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), port));
      listenForPublicGameData();
      logger.info("Opened public UDP socket: {}", SocketAddressUtil.toString((InetSocketAddress) publicSocket.getLocalSocketAddress()));
    } catch (SocketException | UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a forwarder that sends datagrams directly from a public UDP socket.
   */
  private Consumer<DatagramPacket> publicForwarder() {
    logger.info("Using direct connection");
    return datagramPacket -> {
      try {
        publicSocket.send(datagramPacket);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  /**
   * Creates a forwarder that forwards datagrams through to a TURN server.
   */
  private Consumer<DatagramPacket> turnForwarder() {
    logger.info("Using TURN server");
    return datagramPacket -> turnClient.send(datagramPacket);
  }

  private void listenForPublicGameData() {
    forwardSocket(publicSocket, datagramPacket -> {
      synchronized (onPacketFromOutsideListeners) {
        onPacketFromOutsideListeners.forEach(datagramPacketConsumer -> datagramPacketConsumer.accept(datagramPacket));
      }
    });
  }

  private void forwardSocket(final DatagramSocket socket, Consumer<DatagramPacket> forwarder) {
    CompletableFuture.runAsync(() -> {
      byte[] buffer = new byte[8092];
      DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

      try {
        while (!socket.isClosed()) {
          socket.receive(datagramPacket);
          logger.trace("Received {} bytes on {} for {}", datagramPacket.getLength(), socket.getLocalSocketAddress(), datagramPacket.getAddress());
          forwarder.accept(datagramPacket);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      } finally {
        logger.info("Closing socket: {}", socket.getLocalSocketAddress());
        IOUtils.closeQuietly(socket);
      }
    }, executorService).whenComplete((aVoid, throwable) -> {
      if (throwable != null) {
        logger.warn("Exception while forwarding socket " + socket.getLocalSocketAddress(), throwable);
      }
    });
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
        onSendNatPacket(sendNatPacketMessage);
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
    IOUtils.closeQuietly(peerSocketsByUid.remove(disconnectFromPeerMessage.getUid()));
    writeToFa(disconnectFromPeerMessage);
  }

  private void handleHostGame(HostGameMessage hostGameMessage) throws IOException {
    writeToFa(hostGameMessage);
  }

  private void handleConnectToPeer(ConnectToPeerMessage connectToPeerMessage) throws IOException {
    InetSocketAddress peerAddress = connectToPeerMessage.getPeerAddress();

    DatagramSocket peerSocket = createOrGetPeerSocket(peerAddress, connectToPeerMessage.getPeerUid());
    SocketAddress peerSocketAddress = peerSocket.getLocalSocketAddress();
    connectToPeerMessage.setPeerAddress((InetSocketAddress) peerSocketAddress);

    writeToFa(connectToPeerMessage);
  }

  /**
   * Opens a local UDP socket that serves as a proxy for a peer to FA. In other words, instead of connecting to peer X
   * directly, a local port is opened which represents that peer. FA will then data to this port, thinking it's peer X.
   * All data received on that port is then forwarded to the original peer and any data received on the public UDP
   * socket is forwarded through its peer socket.
   *
   * @return the UDP socket the peer has been bound to
   */
  private DatagramSocket createOrGetPeerSocket(SocketAddress originalSocketAddress, int peerUid) throws SocketException {
    if (!peerSocketsByUid.containsKey(peerUid)) {
      InetSocketAddress gameSocketAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), faGamePort);

      DatagramSocket peerSocket = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
      peerSocket.connect(gameSocketAddress);
      peerSocketsByUid.put(peerUid, peerSocket);

      logger.debug("Mapped peer {} to {}", peerUid, peerSocket.getLocalSocketAddress());
      forwardSocket(peerSocket, datagramPacket -> {
        datagramPacket.setSocketAddress(originalSocketAddress);
        gameDataForwarder.accept(datagramPacket);
      });

      synchronized (onPacketFromOutsideListeners) {
        onPacketFromOutsideListeners.add(datagramPacket -> {
          try {
            logger.trace("Forwarding {} bytes from {} to {}", datagramPacket.getLength(), datagramPacket.getSocketAddress(), gameSocketAddress);
            datagramPacket.setSocketAddress(gameSocketAddress);
            peerSocket.send(datagramPacket);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
      }
    }

    return peerSocketsByUid.get(peerUid);
  }

  private void handleJoinGame(JoinGameMessage joinGameMessage) throws IOException {
    InetSocketAddress socketAddress = joinGameMessage.getPeerAddress();

    DatagramSocket peerSocket = createOrGetPeerSocket(socketAddress, joinGameMessage.getPeerUid());
    SocketAddress peerSocketAddress = peerSocket.getLocalSocketAddress();
    joinGameMessage.setPeerAddress((InetSocketAddress) peerSocketAddress);

    writeToFa(joinGameMessage);
  }
}
