package com.faforever.client.relay;

import com.faforever.client.connectivity.DatagramGateway;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GameType;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.relay.event.GameFullEvent;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.user.UserService;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import static com.faforever.client.net.SocketUtil.readSocket;
import static com.github.nocatch.NoCatch.noCatch;
import static java.net.InetAddress.getLoopbackAddress;
import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * <p>Acts as a proxy between the game and the "outside world" (server and peers). See <a
 * href="https://github.com/micheljung/downlords-faf-client/wiki/Application-Design#connection-overview">the wiki
 * page</a> for a graphical explanation.</p> <p>Being a proxy includes rewriting the sender/receiver of all outgoing and
 * incoming packages. Apart from being necessary, this makes us IPv6 compatible.</p>
 */
public class LocalRelayServerImpl implements LocalRelayServer {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * A collection of runnables to be executed whenever the game connected to this relay server.
   */
  private final Collection<Runnable> onGameConnectedListeners;

  /**
   * Consumer for packages that come from "outside" and need to be forwarded to the game.
   */
  private final Consumer<DatagramPacket> incomingPacketConsumer;

  /**
   * Maps socket addresses of peers (their public IP/port) to datagram sockets opened by this class. So every peer has
   * its own proxy socket the game connects to.
   */
  private final Map<SocketAddress, DatagramSocket> proxySocketsByOriginalAddress;

  /**
   * Maps player UIDs to socket addresses of peers (their public IP/port).
   */
  private final Map<Integer, SocketAddress> originalAddressByUid;
  private final BooleanProperty started;

  @Resource
  UserService userService;
  @Resource
  PreferencesService preferencesService;
  @Resource
  FafService fafService;
  @Resource
  ThreadPoolExecutor threadPoolExecutor;
  @Resource
  GameService gameService;
  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;
  @Resource
  ReportingService reportingService;
  @Resource
  PlatformService platformService;
  @Resource
  MapService mapService;
  @Resource
  EventBus eventBus;

  private FaDataOutputStream gameOutputStream;
  private FaDataInputStream gameInputStream;
  private LobbyMode lobbyMode;
  /**
   * The server socket that acts as a proxy for the FAF server. The game sees this as a GPGNet server.
   */
  private ServerSocket serverSocket;
  /**
   * The socket that is created as soon as the game connects to {@link #serverSocket}.
   */
  private Socket gameSocket;
  /**
   * Future that is completed as soon as {@link #serverSocket} is up.
   */
  private CompletableFuture<Integer> gpgPortFuture;
  /**
   * A consumer that forwards game packets to the "outside world".
   */
  private DatagramGateway gateway;
  /**
   * The datagram socket address (IP/port) on which the game accepts packages.
   */
  private CompletableFuture<InetSocketAddress> gameUdpSocketFuture;

  public LocalRelayServerImpl() {
    proxySocketsByOriginalAddress = new HashMap<>();
    originalAddressByUid = new HashMap<>();
    onGameConnectedListeners = new ArrayList<>();
    lobbyMode = LobbyMode.DEFAULT_LOBBY;
    incomingPacketConsumer = this::forwardPacket;
    started = new SimpleBooleanProperty();
  }

  @Override
  public void addOnGameConnectedListener(Runnable listener) {
    onGameConnectedListeners.add(listener);
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
  public CompletionStage<Integer> start(DatagramGateway gateway) {
    synchronized (started) {
      if (started.get()) {
        logger.warn("Local relay server was already running, restarting");
        close();
      }
    }

    logger.debug("Starting relay server");
    this.gateway = gateway;

    gateway.addOnPacketListener(incomingPacketConsumer);

    gameUdpSocketFuture = new CompletableFuture<>();
    gpgPortFuture = new CompletableFuture<>();
    threadPoolExecutor.execute(this::innerStart);
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
    synchronized (started) {
      if (!started.get()) {
        return;
      }

      logger.info("Closing relay server");

      proxySocketsByOriginalAddress.values().forEach(IOUtils::closeQuietly);
      proxySocketsByOriginalAddress.clear();
      originalAddressByUid.clear();

      if (gateway != null) {
        gateway.removeOnPacketListener(incomingPacketConsumer);
      }
      IOUtils.closeQuietly(serverSocket);
      IOUtils.closeQuietly(gameSocket);

      started.set(false);
    }
  }

  private void forwardPacket(DatagramPacket packet) {
    DatagramSocket relaySocket = createOrGetRelaySocket(packet.getSocketAddress());
    noCatch(() -> {
      if (logger.isTraceEnabled()) {
        logger.trace("Forwarding {} bytes from peer '{}' through '{}': {}", packet.getLength(),
            packet.getSocketAddress(), relaySocket.getLocalSocketAddress(),
            new String(packet.getData(), 0, packet.getLength(), US_ASCII));
      }
      packet.setSocketAddress(getGameSocketAddress());
      relaySocket.send(packet);
    });
  }

  /**
   * Opens a local UDP socket that serves as a proxy for a peer to FA. In other words, instead of connecting to peer X
   * directly, a local port is opened which represents that peer. FA will then data to this port, thinking it's peer X.
   * All data received on that port is then forwarded to the original peer and any data received on the public UDP
   * socket is forwarded through its peer socket.
   *
   * @param originalSocketAddress the original address of the peer, to receive data from and send data to
   * @return the UDP socket the peer has been bound to
   */
  private DatagramSocket createOrGetRelaySocket(SocketAddress originalSocketAddress) {
    if (!proxySocketsByOriginalAddress.containsKey(originalSocketAddress)) {
      try {
        DatagramSocket relaySocket = new DatagramSocket(new InetSocketAddress(getLoopbackAddress(), 0));
        logger.debug("Mapping peer {} to relay socket {}", originalSocketAddress, relaySocket.getLocalSocketAddress());

        proxySocketsByOriginalAddress.put(originalSocketAddress, relaySocket);

        readSocket(threadPoolExecutor, relaySocket, packet -> {
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
    noCatch(() -> {
      try (ServerSocket serverSocket = new ServerSocket(0, 0, getLoopbackAddress())) {
        LocalRelayServerImpl.this.serverSocket = serverSocket;

        int localPort = serverSocket.getLocalPort();
        gpgPortFuture.complete(localPort);

        logger.info("GPG relay server listening on port {}", localPort);
        synchronized (started) {
          started.set(true);
        }

        try (Socket faSocket = serverSocket.accept()) {
          LocalRelayServerImpl.this.gameSocket = faSocket;
          logger.debug("Forged Alliance connected to relay server from {}:{}", faSocket.getInetAddress(), faSocket.getPort());

          onGameConnectedListeners.forEach(Runnable::run);

          LocalRelayServerImpl.this.gameInputStream = createFaInputStream(faSocket.getInputStream());
          LocalRelayServerImpl.this.gameOutputStream = createFaOutputStream(faSocket.getOutputStream());
          redirectGpgConnection();
        }
      }
    });
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
      //noinspection InfiniteLoopStatement
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
    GpgClientCommand command = gpgClientMessage.getCommand();
    if (isIdleLobbyMessage(gpgClientMessage)) {
      String username = userService.getUsername();
      if (lobbyMode == null) {
        throw new IllegalStateException("lobbyMode has not been set");
      }

      int faGamePort = SocketUtils.findAvailableUdpPort();
      logger.debug("Picked port for FA to listen: {}", faGamePort);

      handleCreateLobby(new CreateLobbyServerMessage(lobbyMode, faGamePort, username, userService.getUid(), 1));
      gameUdpSocketFuture.complete(new InetSocketAddress(getLoopbackAddress(), faGamePort));
    } else if (command == GpgClientCommand.REHOST) {
      gameService.prepareForRehost().exceptionally(throwable -> {
        logger.warn("Game could not be rehosted", throwable);
        notificationService.addNotification(
            new ImmediateNotification(
                i18n.get("errorTitle"),
                i18n.get("game.create.failed"),
                Severity.ERROR,
                throwable,
                Collections.singletonList(new ReportAction(i18n, reportingService, throwable))));
        return null;
      });
    } else if (command == GpgClientCommand.JSON_STATS) {
      logger.debug("Received game stats: {}", gpgClientMessage.getArgs().get(0));
    } else if (command == GpgClientCommand.GAME_FULL) {
      eventBus.post(new GameFullEvent(null));
      return;
    }

    fafService.sendGpgMessage(gpgClientMessage);
  }

  /**
   * Returns {@code true} if the game lobby is "idle", which basically means the game has been started (into lobby) and
   * does now need to be told on which port to listen on.
   */
  private boolean isIdleLobbyMessage(GpgClientMessage gpgClientMessage) {
    return gpgClientMessage.getCommand() == GpgClientCommand.GAME_STATE
        && gpgClientMessage.getArgs().get(0).equals("Idle");
  }

  private void handleCreateLobby(CreateLobbyServerMessage createLobbyServerMessage) throws IOException {
    writeToFa(createLobbyServerMessage);
  }

  private void writeToFa(GpgServerMessage gpgServerMessage) {
    try {
      writeFaProtocolHeader(gpgServerMessage);
      gameOutputStream.writeArgs(gpgServerMessage.getArgs());
      gameOutputStream.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeFaProtocolHeader(GpgServerMessage gpgServerMessage) throws IOException {
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

    ConnectToPeerMessage clone = connectToPeerMessage.clone();
    clone.setPeerAddress((InetSocketAddress) createOrGetRelaySocket(peerAddress).getLocalSocketAddress());

    writeToFa(clone);
  }

  private void handleJoinGame(JoinGameMessage joinGameMessage) {
    InetSocketAddress originalAddress = joinGameMessage.getPeerAddress();
    originalAddressByUid.put(joinGameMessage.getPeerUid(), originalAddress);

    JoinGameMessage clone = joinGameMessage.clone();
    clone.setPeerAddress((InetSocketAddress) createOrGetRelaySocket(originalAddress).getLocalSocketAddress());

    writeToFa(clone);
  }
}
