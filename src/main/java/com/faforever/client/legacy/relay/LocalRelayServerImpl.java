package com.faforever.client.legacy.relay;

import com.faforever.client.game.GameType;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.io.QDataInputStream;
import com.faforever.client.legacy.proxy.Proxy;
import com.faforever.client.legacy.proxy.ProxyUtils;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.relay.FaDataInputStream;
import com.faforever.client.relay.FaDataOutputStream;
import com.faforever.client.relay.LocalRelayServer;
import com.faforever.client.relay.OnConnectionAcceptedListener;
import com.faforever.client.relay.OnReadyListener;
import com.faforever.client.user.UserService;
import com.faforever.client.util.SocketAddressUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static com.faforever.client.legacy.relay.GpgClientCommand.AUTHENTICATE;
import static com.faforever.client.util.ConcurrentUtil.executeInBackground;
import static java.util.Arrays.asList;

public class LocalRelayServerImpl implements LocalRelayServer, Proxy.OnP2pProxyInitializedListener {

  @VisibleForTesting
  static final String GAME_STATE_LAUNCHING = "Launching";
  @VisibleForTesting
  static final String GAME_STATE_LOBBY = "Lobby";
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final BooleanProperty p2pProxyEnabled;
  private final Gson gson;
  private final Collection<OnReadyListener> onReadyListeners;
  private final Collection<OnConnectionAcceptedListener> onConnectionAcceptedListeners;

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

  private int port;
  private FaDataOutputStream faOutputStream;
  private FaDataInputStream faInputStream;
  private ServerWriter serverWriter;
  private InputStream fafInputStream;
  private LobbyMode lobbyMode;
  private ServerSocket serverSocket;
  private boolean stopped;
  private Socket fafSocket;
  private Socket faSocket;
  private Consumer<Void> gameLaunchedListener;

  public LocalRelayServerImpl() {
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(GpgServerCommandServerCommand.class, GpgServerCommandTypeAdapter.INSTANCE)
        .create();
    onReadyListeners = new ArrayList<>();
    onConnectionAcceptedListeners = new ArrayList<>();
    p2pProxyEnabled = new SimpleBooleanProperty(false);
    lobbyMode = LobbyMode.DEFAULT_LOBBY;
  }

  @Override
  public void addOnReadyListener(OnReadyListener listener) {
    onReadyListeners.add(listener);
  }

  @Override
  public void addOnConnectionAcceptedListener(OnConnectionAcceptedListener listener) {
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
    proxy.addOnP2pProxyInitializedListener(this);

    executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        start();
        return null;
      }
    });
  }

  @Override
  @PreDestroy
  public void close() {
    stopped = true;
    IOUtils.closeQuietly(serverSocket);
    IOUtils.closeQuietly(fafSocket);
    IOUtils.closeQuietly(faSocket);
  }

  @Override
  public void setGameLaunchedListener(Consumer<Void> gameLaunchedListener) {
    this.gameLaunchedListener = gameLaunchedListener;
  }

  private void disconnect() {
    IOUtils.closeQuietly(fafSocket);
    IOUtils.closeQuietly(faSocket);
  }

  @PostConstruct
  void postConstruct() {
    startInBackground();
    lobbyServerAccessor.addOnGameLaunchListener(this::updateLobbyModeFromGameInfo);
  }

  private void updateLobbyModeFromGameInfo(GameLaunchInfo gameLaunchInfo) {
    if (GameType.LADDER_1V1.getString().equals(gameLaunchInfo.getMod())) {
      lobbyMode = LobbyMode.NO_LOBBY;
    } else {
      lobbyMode = LobbyMode.DEFAULT_LOBBY;
    }
  }

  private void start() throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
      this.serverSocket = serverSocket;
      port = serverSocket.getLocalPort();

      logger.info("Relay server listening on port {}", port);

      onReadyListeners.forEach(OnReadyListener::onReady);

      while (!stopped) {
        try (Socket faSocket = serverSocket.accept()) {
          this.faSocket = faSocket;
          logger.debug("Forged Alliance connected to relay server from {}:{}", faSocket.getInetAddress(), faSocket.getPort());

          onConnectionAcceptedListeners.forEach(OnConnectionAcceptedListener::onConnectionAccepted);

          try (Socket fafSocket = new Socket(environment.getProperty("relay.host"), environment.getProperty("relay.port", int.class))) {
            this.fafSocket = fafSocket;
            logger.info("Connected to FAF relay server at {}", SocketAddressUtil.toString((InetSocketAddress) fafSocket.getRemoteSocketAddress()));

            this.faInputStream = createFaInputStream(faSocket.getInputStream());
            this.faOutputStream = createFaOutputStream(faSocket.getOutputStream());
            this.fafInputStream = fafSocket.getInputStream();
            this.serverWriter = createServerWriter(fafSocket.getOutputStream());

            serverWriter.write(new GpgClientMessage(AUTHENTICATE, asList(lobbyServerAccessor.getSessionId(), userService.getUid())));

            redirectGameToServer();
            redirectServerToGame();
          }
        } catch (SocketException | EOFException e) {
          if (serverSocket.isClosed()) {
            logger.info("Closed local relay server");
          } else if (faSocket.isClosed()) {
            logger.debug("Forged Alliance disconnected from relay server", e);
          }
        }
      }
    }
  }

  private ServerWriter createServerWriter(OutputStream outputStream) throws IOException {
    ServerWriter serverWriter = new ServerWriter(outputStream);
    serverWriter.registerMessageSerializer(new GpgClientMessageSerializer(), GpgClientMessage.class);
    return serverWriter;
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
    executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        try {
          redirectGameToServer(faInputStream, serverWriter, this);
        } catch (EOFException | SocketException e) {
          logger.info("Forged Alliance disconnected from local relay server (" + e.getMessage() + ")");
        } finally {
          logger.debug("No longer redirecting from game to server");
          disconnect();
        }
        return null;
      }
    });
  }

  /**
   * Reads data from the FAF server and redirects it to FA.
   */
  private void redirectServerToGame() throws IOException {
    try (QDataInputStream dataInput = new QDataInputStream(new DataInputStream(new BufferedInputStream(fafInputStream)))) {
      while (!stopped) {
        dataInput.skipBlockSize();
        String message = dataInput.readQString();

        logger.debug("Message from FAF relay server: {}", message);

        GpgServerMessage gpgServerMessage = gson.fromJson(message, GpgServerMessage.class);
        dispatchServerCommand(gpgServerMessage.getCommand(), message);
      }
    } catch (EOFException | SocketException e) {
      logger.info("Disconnected from FAF relay server (" + e.getMessage() + ")");
    } finally {
      logger.debug("No longer redirecting from Server to Game");
      disconnect();
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
  private void redirectGameToServer(FaDataInputStream faInputStream, ServerWriter serverWriter, Task<Void> task) throws IOException {
    while (!task.isCancelled()) {
      String message = faInputStream.readString();

      logger.debug("Received message from FA: {}", message);

      List<Object> chunks = faInputStream.readChunks();
      GpgClientMessage gpgClientMessage = new GpgClientMessage(message, chunks);

      if (p2pProxyEnabled.get()) {
        updateProxyState(gpgClientMessage);
      }

      handleDataFromFa(gpgClientMessage);
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
        && gpgClientMessage.getAction() == GpgClientCommand.GAME_STATE
        && GAME_STATE_LAUNCHING.equals(gpgClientMessage.getChunks().get(0))) {
      gameLaunchedListener.accept(null);
    }

    serverWriter.write(gpgClientMessage);
  }

  private boolean isHostGameMessage(GpgClientMessage gpgClientMessage) {
    return gpgClientMessage.getAction() == GpgClientCommand.GAME_STATE
        && gpgClientMessage.getChunks().get(0).equals("Idle");
  }

  private void updateProxyState(GpgClientMessage gpgClientMessage) {
    GpgClientCommand action = gpgClientMessage.getAction();
    List<Object> chunks = gpgClientMessage.getChunks();

    logger.debug("Received '{}' with chunks: {}", action.getString(), chunks);

    switch (action) {
      case PROCESS_NAT_PACKET:
        chunks.set(0, proxy.translateToPublic((String) chunks.get(0)));
        break;
      case DISCONNECTED:
        proxy.updateConnectedState((Integer) chunks.get(0), false);
        break;
      case CONNECTED:
        proxy.updateConnectedState((Integer) chunks.get(0), true);
        break;
      case GAME_STATE:
        switch ((String) chunks.get(0)) {
          case GAME_STATE_LAUNCHING:
            proxy.setGameLaunched(true);
            break;
          case GAME_STATE_LOBBY:
            proxy.setGameLaunched(false);
            break;
        }
        break;
      case BOTTLENECK:
        proxy.setBottleneck(true);
        break;
      case BOTTLENECK_CLEARED:
        proxy.setBottleneck(false);
        break;

      default:
        // Do nothing
    }
  }

  private void dispatchServerCommand(GpgServerCommandServerCommand command, String jsonString) throws IOException {
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
        // CreateLobby was already sent to FAF by this relayer, so the server message is discarded
        logger.debug("Discarding message from server: {}", jsonString);
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
      case CONNECTIVITY_STATE:
        logger.debug("Ignoring ConnectivityState message");
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
    serverWriter.write(GpgClientMessage.pong());
  }

  private void handleSendNatPacket(SendNatPacketMessage sendNatPacketMessage) throws IOException {
    if (p2pProxyEnabled.get()) {
      String publicAddress = sendNatPacketMessage.getPublicAddress();

      proxy.registerP2pPeerIfNecessary(publicAddress);

      sendNatPacketMessage.setPublicAddress(proxy.translateToLocal(publicAddress));
    }

    writeToFaUdp(sendNatPacketMessage);
  }

  private void handleP2pReconnect() throws SocketException {
    proxy.initializeP2pProxy();
    p2pProxyEnabled.set(true);
  }

  private void handleConnectToPeer(ConnectToPeerMessage connectToPeerMessage) throws IOException {
    if (p2pProxyEnabled.get()) {
      String peerAddress = connectToPeerMessage.getPeerAddress();
      int peerUid = connectToPeerMessage.getPeerUid();

      proxy.registerP2pPeerIfNecessary(peerAddress);

      connectToPeerMessage.setPeerAddress(proxy.translateToLocal(peerAddress));
      proxy.setUidForPeer(peerAddress, peerUid);
    }

    writeToFa(connectToPeerMessage);
  }

  private void handleJoinGame(JoinGameMessage joinGameMessage) throws IOException {
    if (p2pProxyEnabled.get()) {
      String peerAddress = joinGameMessage.getPeerAddress();
      int peerUid = joinGameMessage.getPeerUid();

      proxy.registerP2pPeerIfNecessary(peerAddress);

      joinGameMessage.setPeerAddress(proxy.translateToLocal(peerAddress));
      proxy.setUidForPeer(peerAddress, peerUid);
    }

    writeToFa(joinGameMessage);
  }

  private void handleCreateLobby(CreateLobbyServerMessage createLobbyServerMessage) throws IOException {
    int peerUid = createLobbyServerMessage.getUid();
    proxy.setUid(peerUid);

    if (p2pProxyEnabled.get()) {
      createLobbyServerMessage.setPort(ProxyUtils.translateToProxyPort(proxy.getPort()));
    }

    writeToFa(createLobbyServerMessage);
  }

  private void handleConnectToProxy(ConnectToProxyMessage connectToProxyMessage) throws IOException {
    int playerNumber = connectToProxyMessage.getPlayerNumber();
    int peerUid = connectToProxyMessage.getPeerUid();

    InetSocketAddress proxySocket = proxy.bindAndGetProxySocketAddress(playerNumber, peerUid);

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
    faOutputStream.writeUdpArgs(gpgServerMessage.getArgs());
    faOutputStream.flush();
  }

  private void writeToFa(GpgServerMessage gpgServerMessage) throws IOException {
    writeHeader(gpgServerMessage);
    faOutputStream.writeArgs(gpgServerMessage.getArgs());
    faOutputStream.flush();
  }

  private void writeHeader(GpgServerMessage gpgServerMessage) throws IOException {
    String commandString = gpgServerMessage.getCommand().getString();

    int headerSize = commandString.length();
    String headerField = commandString.replace("\t", "/t").replace("\n", "/n");

    logger.debug("Writing data to FA, command: {}, args: {}", commandString, gpgServerMessage.getArgs());

    faOutputStream.writeInt(headerSize);
    faOutputStream.writeString(headerField);
  }

  @Override
  public void onP2pProxyInitialized() {
    p2pProxyEnabled.set(true);
  }

  @VisibleForTesting
  boolean isP2pProxyEnabled() {
    return p2pProxyEnabled.get();
  }

  @VisibleForTesting
  void addOnP2pProxyEnabledChangeListener(ChangeListener<Boolean> listener) {
    p2pProxyEnabled.addListener(listener);
  }
}
