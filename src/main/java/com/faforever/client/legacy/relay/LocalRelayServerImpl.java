package com.faforever.client.legacy.relay;

import com.faforever.client.game.GameType;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.domain.GameLaunchMessageLobby;
import com.faforever.client.legacy.proxy.Proxy;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.relay.FaDataInputStream;
import com.faforever.client.relay.FaDataOutputStream;
import com.faforever.client.relay.LocalRelayServer;
import com.faforever.client.relay.OnReadyListener;
import com.faforever.client.user.UserService;
import com.faforever.client.util.SocketAddressUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class LocalRelayServerImpl implements LocalRelayServer {

  @VisibleForTesting
  static final String GAME_STATE_LAUNCHING = "Launching";
  @VisibleForTesting
  static final String GAME_STATE_LOBBY = "Lobby";
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Gson gson;
  private final Collection<OnReadyListener> onReadyListeners;
  private final Collection<Runnable> onConnectionAcceptedListeners;

  @Resource
  Proxy proxy;
  @Resource
  Environment environment;
  @Resource
  UserService userService;
  @Resource
  PreferencesService preferencesService;
  @Resource
  LobbyServerAccessor lobbyServerAccessor;
  @Resource
  ExecutorService executorService;

  private int port;
  private FaDataOutputStream gameOutputStream;
  private FaDataInputStream gameInputStream;
  private LobbyMode lobbyMode;
  private ServerSocket serverSocket;
  private boolean stopped;
  private Socket gameSocket;
  private Consumer<Void> gameLaunchedListener;

  public LocalRelayServerImpl() {
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(GpgServerMessageType.class, GpgServerCommandTypeAdapter.INSTANCE)
        .create();
    onReadyListeners = new ArrayList<>();
    onConnectionAcceptedListeners = new ArrayList<>();
    lobbyMode = LobbyMode.DEFAULT_LOBBY;
  }

  @Override
  public void addOnReadyListener(OnReadyListener listener) {
    onReadyListeners.add(listener);
  }

  @Override
  public void addOnConnectionAcceptedListener(Runnable listener) {
    onConnectionAcceptedListeners.add(listener);
  }

  @Override
  public int getPort() {
    return port;
  }

  /**
   * Starts a local, GPG-like server in background that FA can connect to. Received data is forwarded to the FAF server
   * and vice-versa.
   */
  @Override
  public void startInBackground() {
    CompletableFuture.runAsync(this::start, executorService);
  }

  @Override
  @PreDestroy
  public void close() {
    logger.info("Closing relay server");
    stopped = true;
    IOUtils.closeQuietly(serverSocket);
    IOUtils.closeQuietly(gameSocket);
  }

  @Override
  public void setGameLaunchedListener(Consumer<Void> gameLaunchedListener) {
    this.gameLaunchedListener = gameLaunchedListener;
  }

  @PostConstruct
  void postConstruct() {
    startInBackground();
    lobbyServerAccessor.addOnGameLaunchListener(this::updateLobbyModeFromGameInfo);
    lobbyServerAccessor.addOnGpgServerMessageListener(this::onGpgServerMessage);
  }

  private void updateLobbyModeFromGameInfo(GameLaunchMessageLobby gameLaunchMessage) {
    if (GameType.LADDER_1V1.getString().equals(gameLaunchMessage.getMod())) {
      lobbyMode = LobbyMode.NO_LOBBY;
    } else {
      lobbyMode = LobbyMode.DEFAULT_LOBBY;
    }
  }

  private void start() {
    try (ServerSocket serverSocket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
      this.serverSocket = serverSocket;
      port = serverSocket.getLocalPort();

      logger.info("Relay server listening on port {}", port);

      onReadyListeners.forEach(OnReadyListener::onReady);

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
      int gamePort = preferencesService.getPreferences().getForgedAlliance().getPort();
      String username = userService.getUsername();
      if (lobbyMode == null) {
        throw new IllegalStateException("lobbyMode has not been set");
      }

      handleCreateLobby(new CreateLobbyServerMessage(lobbyMode, gamePort, username, userService.getUid(), 1));
    } else if (gameLaunchedListener != null
        && gpgClientMessage.getCommand() == GpgClientCommand.GAME_STATE
        && GAME_STATE_LAUNCHING.equals(gpgClientMessage.getArgs().get(0))) {
      gameLaunchedListener.accept(null);
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
    int peerUid = createLobbyServerMessage.getUid();
    proxy.setUid(peerUid);

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

  private void onGpgServerMessage(GpgServerMessage message) {
    try {
      dispatchServerCommand(message.getMessageType(), message.getJsonString());
    } catch (IOException e) {
      logger.warn("Error while handling message: " + message, e);
    }
  }

  private void dispatchServerCommand(GpgServerMessageType command, String jsonString) throws IOException {
    switch (command) {
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

  private void handleSendNatPacket(SendNatPacketMessage sendNatPacketMessage) throws IOException {
    writeToFaUdp(sendNatPacketMessage);
  }

  private void handleConnectToPeer(ConnectToPeerMessage connectToPeerMessage) throws IOException {
    writeToFa(connectToPeerMessage);
  }

  private void handleJoinGame(JoinGameMessage joinGameMessage) throws IOException {
    writeToFa(joinGameMessage);
  }

  private void handleJoinProxy(JoinProxyMessage joinProxyMessage) throws IOException {
    int playerNumber = joinProxyMessage.getPlayerNumber();
    int peerUid = joinProxyMessage.getPeerUid();

    InetSocketAddress proxySocket = proxy.bindAndGetProxySocketAddress(playerNumber, peerUid);

    // Ask FA to join the game via the local proxy port
    JoinGameMessage joinGameMessage = new JoinGameMessage();
    joinGameMessage.setPeerAddress(SocketAddressUtil.toString(proxySocket));
    joinGameMessage.setUsername(joinProxyMessage.getUsername());
    joinGameMessage.setPeerUid(joinProxyMessage.getPeerUid());

    writeToFa(joinGameMessage);
  }

  private void writeToFaUdp(GpgServerMessage gpgServerMessage) throws IOException {
    writeHeader(gpgServerMessage);
    gameOutputStream.writeUdpArgs(gpgServerMessage.getArgs());
    gameOutputStream.flush();
  }

}
