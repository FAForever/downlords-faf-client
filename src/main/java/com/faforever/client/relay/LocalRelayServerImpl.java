package com.faforever.client.relay;

import com.faforever.client.connectivity.ConnectivityService;
import com.faforever.client.connectivity.ConnectivityState;
import com.faforever.client.connectivity.TurnServerAccessor;
import com.faforever.client.game.GameType;
import com.faforever.client.legacy.domain.GameLaunchMessage;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.SocketAddressUtil;
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
  TurnServerAccessor turnServerAccessor;
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
  private Consumer<DatagramPacket> fromGameToOutsideForwarder;
  /**
   * The socket to the "outside", receives and sends game data.
   */
  private DatagramSocket publicSocket;

  public LocalRelayServerImpl() {
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
    return turnServerAccessor.getRelayAddress();
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
      turnServerAccessor.unbind();
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

    writeToFa(new GameOptionMessage("ScenarioFile", "/maps/SCMP_019/SCMP_019_scenario.lua"));
  }

  private boolean isHostGameMessage(GpgClientMessage gpgClientMessage) {
    return gpgClientMessage.getCommand() == GpgClientCommand.GAME_STATE
        && gpgClientMessage.getArgs().get(0).equals("Idle");
  }

  private void handleCreateLobby(CreateLobbyServerMessage createLobbyServerMessage) throws IOException {
    writeToFa(createLobbyServerMessage);
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
    InetSocketAddress receiver = sendNatPacketMessage.getPublicAddress();
    String message = sendNatPacketMessage.getMessage();

    byte[] bytes = ("\b" + message).getBytes(US_ASCII);
    DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
    datagramPacket.setSocketAddress(receiver);
    try {
      logger.debug("Sending NAT packet to {}: {}", datagramPacket.getSocketAddress(), new String(datagramPacket.getData(), US_ASCII));
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
    fafService.addOnMessageListener(HostGameMessage.class, this::handleHostGame);
    fafService.addOnMessageListener(SendNatPacketMessage.class, this::onSendNatPacket);
    fafService.addOnMessageListener(JoinGameMessage.class, this::handleJoinGame);
    fafService.addOnMessageListener(ConnectToPeerMessage.class, this::handleConnectToPeer);
    fafService.addOnMessageListener(DisconnectFromPeerMessage.class, this::handleDisconnectFromPeer);

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
        fromGameToOutsideForwarder = publicForwarder();
        break;
      case STUN:
        fromGameToOutsideForwarder = turnForwarder();
        turnServerAccessor.connect();
        turnServerAccessor.setOnDataListener(this::dispatchPacketFromOutside);
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
    return datagramPacket -> turnServerAccessor.send(datagramPacket);
  }

  private void listenForPublicGameData() {
    forwardSocket("incoming", publicSocket, this::dispatchPacketFromOutside);
  }

  /**
   * Reads the specified socket and forwards all received packets using the specified forwarder.
   */
  private void forwardSocket(String tag, final DatagramSocket socket, Consumer<DatagramPacket> forwarder) {
    CompletableFuture.runAsync(() -> {
      byte[] buffer = new byte[8092];
      DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

      try {
        while (!socket.isClosed()) {
          socket.receive(datagramPacket);
          forwarder.accept(datagramPacket);
        }
      } catch (IOException e) {
        throw new RuntimeException(tag, e);
      } finally {
        logger.info("Closing socket: {}", socket.getLocalSocketAddress());
        IOUtils.closeQuietly(socket);
      }
    }, executorService).whenComplete((aVoid, throwable) -> {
      if (throwable != null) {
        logger.warn("Exception while forwarding socket: " + socket.getLocalSocketAddress(), throwable);
      }
    });
  }

  private void dispatchPacketFromOutside(DatagramPacket packet) {
    if (isNatPackage(packet.getData())) {
      logger.trace("Received NAT packet from outside, forwarding it to FAF server");
      String message = new String(packet.getData(), 1, packet.getLength() - 1);
      ProcessNatPacketMessage processNatPacketMessage = new ProcessNatPacketMessage((InetSocketAddress) packet.getSocketAddress(), message);
      fafService.sendGpgMessage(processNatPacketMessage);
    } else {
      synchronized (onPacketFromOutsideListeners) {
        logger.trace("Dispatching game data from outside ({} bytes)", packet.getLength());
        onPacketFromOutsideListeners.forEach(datagramPacketConsumer -> datagramPacketConsumer.accept(packet));
      }
    }
  }

  private boolean isNatPackage(byte[] data) {
    return data.length > 0 && data[0] == '\b';
  }

  private void updateLobbyModeFromGameInfo(GameLaunchMessage gameLaunchMessage) {
    if (GameType.LADDER_1V1.getString().equals(gameLaunchMessage.getMod())) {
      lobbyMode = LobbyMode.NO_LOBBY;
    } else {
      lobbyMode = LobbyMode.DEFAULT_LOBBY;
    }
  }

  private void handleDisconnectFromPeer(DisconnectFromPeerMessage disconnectFromPeerMessage) {
    IOUtils.closeQuietly(peerSocketsByUid.remove(disconnectFromPeerMessage.getUid()));
    writeToFa(disconnectFromPeerMessage);
  }

  private void writeToFa(GpgServerMessage gpgServerMessage) {
    try {
      writeHeader(gpgServerMessage);
      gameOutputStream.writeArgs(gpgServerMessage.getArgs());
      gameOutputStream.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeHeader(GpgServerMessage gpgServerMessage) throws IOException {
    String commandString = gpgServerMessage.getMessageType().getString();

    int headerSize = commandString.length();
    String headerField = commandString.replace("\t", "/t").replace("\n", "/n");

    logger.debug("Writing data to FA, command: {}, args: {}", commandString, gpgServerMessage.getArgs());

    gameOutputStream.writeInt(headerSize);
    gameOutputStream.writeString(headerField);
  }

  private void handleHostGame(HostGameMessage hostGameMessage) {
    writeToFa(hostGameMessage);
  }

  private void handleConnectToPeer(ConnectToPeerMessage connectToPeerMessage) {
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
   * @param originalPeerAddress the original address of the peer, to receive data from and send data to
   *
   * @return the UDP socket the peer has been bound to
   */
  private DatagramSocket createOrGetPeerSocket(SocketAddress originalPeerAddress, int peerUid) {
    if (!peerSocketsByUid.containsKey(peerUid)) {

      try {
        DatagramSocket peerRelaySocket = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        peerRelaySocket.connect(gameSocket.getRemoteSocketAddress());
        peerSocketsByUid.put(peerUid, peerRelaySocket);

        logger.debug("Mapped peer {} to socket {}", peerUid, peerRelaySocket.getLocalSocketAddress());
        forwardSocket("outgoing", peerRelaySocket, packet -> {
          packet.setSocketAddress(originalPeerAddress);
          logger.trace("Forwarding {} bytes from FA to peer {} ({})", packet.getLength(), peerUid, originalPeerAddress);
          fromGameToOutsideForwarder.accept(packet);
        });

        synchronized (onPacketFromOutsideListeners) {
          onPacketFromOutsideListeners.add(packet -> {
            try {
              logger.trace("Forwarding {} bytes from peer {} ({}) to FA", packet.getLength(), peerUid, packet.getSocketAddress());
              packet.setSocketAddress(gameSocket.getRemoteSocketAddress());
              peerRelaySocket.send(packet);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
        }
      } catch (SocketException e) {
        throw new RuntimeException(e);
      }
    }

    return peerSocketsByUid.get(peerUid);
  }

  private void handleJoinGame(JoinGameMessage joinGameMessage) {
    InetSocketAddress socketAddress = joinGameMessage.getPeerAddress();

    DatagramSocket peerSocket = createOrGetPeerSocket(socketAddress, joinGameMessage.getPeerUid());
    SocketAddress peerSocketAddress = peerSocket.getLocalSocketAddress();
    joinGameMessage.setPeerAddress((InetSocketAddress) peerSocketAddress);

    writeToFa(joinGameMessage);
  }
}
