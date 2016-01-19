package com.faforever.client.relay;

import com.faforever.client.connectivity.ConnectivityService;
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

import static com.faforever.client.net.NetUtil.forwardSocket;
import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Acts as a layer between the "outside world" and the game, like a NAT.
 */
public class LocalRelayServerImpl implements LocalRelayServer {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Collection<Runnable> onConnectionAcceptedListeners;
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
  private Consumer<DatagramPacket> outgoingPackageForwarder;

  public LocalRelayServerImpl() {
    peerSocketsByUid = new HashMap<>();
    onConnectionAcceptedListeners = new ArrayList<>();
    lobbyMode = LobbyMode.DEFAULT_LOBBY;
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
  public void removeOnPackedFromOutsideListener(Consumer<DatagramPacket> listener) {
    synchronized (onPacketFromOutsideListeners) {
      onPacketFromOutsideListeners.remove(listener);
    }
  }

  @Override
  public CompletableFuture<Integer> start(Consumer<DatagramPacket> outgoingPackageConsumer, PackageReceiver packageReceiver) {
    packageReceiver.setOnPackageListener(this::onPacketFromOutside);
    this.outgoingPackageForwarder = outgoingPackageConsumer;

    gpgPortFuture = new CompletableFuture<>();
    CompletableFuture.runAsync(this::innerStart, executorService);
    return gpgPortFuture;
  }

  private void innerStart() {
    try (ServerSocket serverSocket1 = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
      LocalRelayServerImpl.this.serverSocket = serverSocket1;

      int localPort = serverSocket1.getLocalPort();
      gpgPortFuture.complete(localPort);

      logger.info("GPG relay server listening on port {}", localPort);

      while (!stopped) {
        try (Socket faSocket = serverSocket1.accept()) {
          LocalRelayServerImpl.this.gameSocket = faSocket;
          logger.debug("Forged Alliance connected to relay server from {}:{}", faSocket.getInetAddress(), faSocket.getPort());

          onConnectionAcceptedListeners.forEach(Runnable::run);

          LocalRelayServerImpl.this.gameInputStream = createFaInputStream(faSocket.getInputStream());
          LocalRelayServerImpl.this.gameOutputStream = createFaOutputStream(faSocket.getOutputStream());
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
      logger.debug("Picked port for FA to listen: {}", faGamePort);
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

  @PreDestroy
  void close() {
    logger.info("Closing relay server");
    stopped = true;
    IOUtils.closeQuietly(serverSocket);
    IOUtils.closeQuietly(gameSocket);
  }

  @PostConstruct
  void postConstruct() {
    fafService.addOnMessageListener(GameLaunchMessage.class, this::updateLobbyModeFromGameInfo);
    fafService.addOnMessageListener(HostGameMessage.class, this::handleHostGame);
    fafService.addOnMessageListener(JoinGameMessage.class, this::handleJoinGame);
    fafService.addOnMessageListener(ConnectToPeerMessage.class, this::handleConnectToPeer);
    fafService.addOnMessageListener(DisconnectFromPeerMessage.class, this::handleDisconnectFromPeer);
  }

  private void onPacketFromOutside(DatagramPacket packet) {
    synchronized (onPacketFromOutsideListeners) {
      if (logger.isTraceEnabled()) {
        logger.trace("Game data from outside: {}", new String(packet.getData(), 0, packet.getLength(), US_ASCII));
      }
      onPacketFromOutsideListeners.forEach(datagramPacketConsumer -> datagramPacketConsumer.accept(packet));
    }
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
        InetSocketAddress gameUdpSocket = new InetSocketAddress(InetAddress.getLoopbackAddress(), faGamePort);

        DatagramSocket peerRelaySocket = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        peerRelaySocket.connect(gameUdpSocket);
        peerSocketsByUid.put(peerUid, peerRelaySocket);

        logger.debug("Mapped peer {} to socket {}", peerUid, peerRelaySocket.getLocalSocketAddress());
        forwardSocket(executorService, peerRelaySocket, packet -> {
          packet.setSocketAddress(originalPeerAddress);

          if (logger.isTraceEnabled()) {
            logger.trace("Forwarding {} bytes from FA to peer {} ({}): {}", packet.getLength(), peerUid,
                originalPeerAddress, new String(packet.getData(), 0, packet.getLength(), US_ASCII));
          }

          outgoingPackageForwarder.accept(packet);
        });

        synchronized (onPacketFromOutsideListeners) {
          onPacketFromOutsideListeners.add(packet -> {
            try {
              if (logger.isTraceEnabled()) {
                logger.trace("Forwarding {} bytes from peer {} ({}) to FA: {}", packet.getLength(), peerUid,
                    packet.getSocketAddress(), new String(packet.getData(), 0, packet.getLength(), US_ASCII));
              }
              packet.setSocketAddress(gameUdpSocket);
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
