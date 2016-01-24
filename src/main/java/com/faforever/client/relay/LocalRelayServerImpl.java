package com.faforever.client.relay;

import com.faforever.client.connectivity.DatagramGateway;
import com.faforever.client.game.GameType;
import com.faforever.client.legacy.domain.GameLaunchMessage;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.user.UserService;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static com.faforever.client.net.NetUtil.readSocket;
import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Acts as a layer between the "outside world" and the game, like a NAT.
 */
public class LocalRelayServerImpl implements LocalRelayServer {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Collection<Runnable> onConnectionAcceptedListeners;
  private final Consumer<DatagramPacket> datagramPacketConsumer;
  private final Map<SocketAddress, DatagramSocket> proxySocketsByOriginalAddress;
  private final Map<Integer, SocketAddress> originalAddressByUid;
  @Resource
  UserService userService;
  @Resource
  PreferencesService preferencesService;
  @Resource
  FafService fafService;
  @Resource
  ExecutorService executorService;
  private FaDataOutputStream gameOutputStream;
  private FaDataInputStream gameInputStream;
  private LobbyMode lobbyMode;
  private ServerSocket serverSocket;
  private Socket gameSocket;
  private CompletableFuture<Integer> gpgPortFuture;
  private int faGamePort;
  /**
   * A consumer that forwards game packets to the "outside world".
   */
  private DatagramGateway gateway;
  private CompletableFuture<InetSocketAddress> gameUdpSocketFuture;

  public LocalRelayServerImpl() {
    proxySocketsByOriginalAddress = new HashMap<>();
    originalAddressByUid = new HashMap<>();
    onConnectionAcceptedListeners = new ArrayList<>();
    lobbyMode = LobbyMode.DEFAULT_LOBBY;
    datagramPacketConsumer = this::onPacketFromOutside;
  }

  @Override
  public void addOnConnectionAcceptedListener(Runnable listener) {
    onConnectionAcceptedListeners.add(listener);
  }

  @Override
  public Integer getPort() {
    try {
      return gpgPortFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public CompletableFuture<Integer> start(DatagramGateway gateway) {
    this.gateway = gateway;

    gateway.addOnPacketListener(datagramPacketConsumer);

    gameUdpSocketFuture = new CompletableFuture<>();
    gpgPortFuture = new CompletableFuture<>();
    CompletableFuture.runAsync(this::innerStart, executorService);
    return gpgPortFuture;
  }

  @Override
  public InetSocketAddress getGameSocketAddress() {
    try {
      return gameUdpSocketFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  @PreDestroy
  public void close() {
    logger.info("Closing relay server");

    proxySocketsByOriginalAddress.values().forEach(IOUtils::closeQuietly);
    proxySocketsByOriginalAddress.clear();
    originalAddressByUid.clear();

    if (gateway != null) {
      gateway.removeOnPacketListener(datagramPacketConsumer);
    }
    IOUtils.closeQuietly(serverSocket);
    IOUtils.closeQuietly(gameSocket);
  }

  private void onPacketFromOutside(DatagramPacket packet) {
    DatagramSocket proxySocket = createOrGetProxySocket(packet.getSocketAddress());
    try {
      if (logger.isTraceEnabled()) {
        logger.trace("Forwarding {} bytes from peer {}' to FA: {}", packet.getLength(),
            proxySocket.getLocalSocketAddress(), new String(packet.getData(), 0, packet.getLength(), US_ASCII));
      }
      packet.setSocketAddress(getGameSocketAddress());
      proxySocket.send(packet);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Opens a local UDP socket that serves as a proxy for a peer to FA. In other words, instead of connecting to peer X
   * directly, a local port is opened which represents that peer. FA will then data to this port, thinking it's peer X.
   * All data received on that port is then forwarded to the original peer and any data received on the public UDP
   * socket is forwarded through its peer socket.
   *
   * @param originalSocketAddress the original address of the peer, to receive data from and send data to
   *
   * @return the UDP socket the peer has been bound to
   */
  private DatagramSocket createOrGetProxySocket(SocketAddress originalSocketAddress) {
    if (!proxySocketsByOriginalAddress.containsKey(originalSocketAddress)) {
      try {
        DatagramSocket proxySocket = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        logger.debug("Mapping peer {} to proxy socket {}", originalSocketAddress);

        proxySocket.connect(getGameSocketAddress());
        proxySocketsByOriginalAddress.put(originalSocketAddress, proxySocket);

        readSocket(executorService, proxySocket, packet -> {
          packet.setSocketAddress(originalSocketAddress);
          if (logger.isTraceEnabled()) {
            logger.trace("Forwarding {} bytes from FA to peer {}: {}", packet.getLength(),
                originalSocketAddress, new String(packet.getData(), 0, packet.getLength(), US_ASCII));
          }

          gateway.send(packet);
        });
      } catch (SocketException e) {
        throw new RuntimeException(e);
      }
    }

    return proxySocketsByOriginalAddress.get(originalSocketAddress);
  }

  private void innerStart() {
    try (ServerSocket serverSocket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
      LocalRelayServerImpl.this.serverSocket = serverSocket;

      int localPort = serverSocket.getLocalPort();
      gpgPortFuture.complete(localPort);

      logger.info("GPG relay server listening on port {}", localPort);

      try (Socket faSocket = serverSocket.accept()) {
        LocalRelayServerImpl.this.gameSocket = faSocket;
        logger.debug("Forged Alliance connected to relay server from {}:{}", faSocket.getInetAddress(), faSocket.getPort());

        onConnectionAcceptedListeners.forEach(Runnable::run);

        LocalRelayServerImpl.this.gameInputStream = createFaInputStream(faSocket.getInputStream());
        LocalRelayServerImpl.this.gameOutputStream = createFaOutputStream(faSocket.getOutputStream());
        redirectGpgConnection();
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
      close();
    }
  }

  private void handleDataFromFa(GpgClientMessage gpgClientMessage) throws IOException {
    if (isIdleLobbyMessage(gpgClientMessage)) {
      String username = userService.getUsername();
      if (lobbyMode == null) {
        throw new IllegalStateException("lobbyMode has not been set");
      }

      faGamePort = SocketUtils.findAvailableUdpPort();
      logger.debug("Picked port for FA to listen: {}", faGamePort);

      handleCreateLobby(new CreateLobbyServerMessage(lobbyMode, faGamePort, username, userService.getUid(), 1));
      gameUdpSocketFuture.complete(new InetSocketAddress(InetAddress.getLoopbackAddress(), faGamePort));
    }

    fafService.sendGpgMessage(gpgClientMessage);
  }

  private boolean isIdleLobbyMessage(GpgClientMessage gpgClientMessage) {
    return gpgClientMessage.getCommand() == GpgClientCommand.GAME_STATE
        && gpgClientMessage.getArgs().get(0).equals("Idle");
  }

  private void handleCreateLobby(CreateLobbyServerMessage createLobbyServerMessage) throws IOException {
    writeToFa(createLobbyServerMessage);
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

  @PostConstruct
  void postConstruct() {
    fafService.addOnMessageListener(GameLaunchMessage.class, this::updateLobbyModeFromGameInfo);
    fafService.addOnMessageListener(HostGameMessage.class, this::handleHostGame);
    fafService.addOnMessageListener(JoinGameMessage.class, this::handleJoinGame);
    fafService.addOnMessageListener(ConnectToPeerMessage.class, this::handleConnectToPeer);
    fafService.addOnMessageListener(DisconnectFromPeerMessage.class, this::handleDisconnectFromPeer);
  }

  private void updateLobbyModeFromGameInfo(GameLaunchMessage gameLaunchMessage) {
    if (GameType.LADDER_1V1.getString().equals(gameLaunchMessage.getMod())) {
      lobbyMode = LobbyMode.NO_LOBBY;
    } else {
      lobbyMode = LobbyMode.DEFAULT_LOBBY;
    }
  }

  private void handleDisconnectFromPeer(DisconnectFromPeerMessage disconnectFromPeerMessage) {
    SocketAddress originalAddress = originalAddressByUid.remove(disconnectFromPeerMessage.getUid());
    DatagramSocket proxySocket = proxySocketsByOriginalAddress.remove(originalAddress);
    IOUtils.closeQuietly(proxySocket);
    writeToFa(disconnectFromPeerMessage);
  }

  private void handleHostGame(HostGameMessage hostGameMessage) {
    writeToFa(hostGameMessage);
  }

  private void handleConnectToPeer(ConnectToPeerMessage connectToPeerMessage) {
    InetSocketAddress peerAddress = connectToPeerMessage.getPeerAddress();

    DatagramSocket peerSocket = createOrGetProxySocket(peerAddress);
    SocketAddress peerSocketAddress = peerSocket.getLocalSocketAddress();
    connectToPeerMessage.setPeerAddress((InetSocketAddress) peerSocketAddress);

    writeToFa(connectToPeerMessage);
  }

  private void handleJoinGame(JoinGameMessage joinGameMessage) {
    InetSocketAddress originalAddress = joinGameMessage.getPeerAddress();
    originalAddressByUid.put(joinGameMessage.getPeerUid(), originalAddress);

    DatagramSocket proxySocket = createOrGetProxySocket(originalAddress);
    SocketAddress peerSocketAddress = proxySocket.getLocalSocketAddress();
    joinGameMessage.setPeerAddress((InetSocketAddress) peerSocketAddress);

    writeToFa(joinGameMessage);
  }
}
