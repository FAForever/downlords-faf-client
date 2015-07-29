package com.faforever.client.legacy.relay;

import com.faforever.client.game.FeaturedMod;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.io.QDataReader;
import com.faforever.client.legacy.proxy.Proxy;
import com.faforever.client.legacy.proxy.ProxyUtils;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.preferences.PreferencesService;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

public class LocalRelayServerImpl implements LocalRelayServer, Proxy.OnP2pProxyInitializedListener {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  Proxy proxy;

  @Autowired
  Environment environment;

  @Autowired
  UserService userService;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  LobbyServerAccessor lobbyServerAccessor;

  private BooleanProperty p2pProxyEnabled;
  private int port;
  private final Gson gson;
  private FaDataOutputStream faOutputStream;
  private FaDataInputStream faInputStream;
  private ServerWriter serverWriter;
  private InputStream fafInputStream;
  private LobbyMode lobbyMode;
  private Collection<OnReadyListener> onReadyListeners;
  private Collection<OnConnectionAcceptedListener> onConnectionAcceptedListeners;

  public LocalRelayServerImpl() {
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(RelayServerCommand.class, new RelayServerCommandTypeAdapter())
        .create();
    onReadyListeners = new ArrayList<>();
    onConnectionAcceptedListeners = new ArrayList<>();
    p2pProxyEnabled = new SimpleBooleanProperty(false);
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

  @PostConstruct
  void postConstruct() {
    startInBackground();
    lobbyServerAccessor.addOnGameLaunchListener(this::updateLobbyModeFromGameInfo);
  }

  private void updateLobbyModeFromGameInfo(GameLaunchInfo gameLaunchInfo) {
    FeaturedMod featuredMod = FeaturedMod.fromString(gameLaunchInfo.mod);
    switch (featuredMod) {
      case FAF:
      case BALANCE_TESTING:
        lobbyMode = LobbyMode.DEFAULT_LOBBY;
        break;
      case LADDER_1V1:
        lobbyMode = LobbyMode.NO_LOBBY;
        break;
    }
  }

  /**
   * Starts a local, GPG-like server in background that FA can connect to. Received data is forwarded to the FAF server
   * and vice-versa.
   */
  @Override
  public void startInBackground() {
    proxy.addOnProxyInitializedListener(this);

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

      onReadyListeners.forEach(OnReadyListener::onReady);

      while (!isCancelled()) {
        try (Socket faSocket = serverSocket.accept()) {
          logger.debug("Forged Alliance connected to relay server from {}:{}", faSocket.getInetAddress(), faSocket.getPort());

          onConnectionAcceptedListeners.forEach(OnConnectionAcceptedListener::onConnectionAccepted);

          try (Socket fafSocket = new Socket(environment.getProperty("relay.host"), environment.getProperty("relay.port", int.class))) {
            logger.info("Connected to FAF relay server at {}", SocketAddressUtil.toString((InetSocketAddress) fafSocket.getRemoteSocketAddress()));

            this.faInputStream = createFaInputStream(faSocket.getInputStream());
            this.faOutputStream = createFaOutputStream(faSocket.getOutputStream());
            this.fafInputStream = fafSocket.getInputStream();
            this.serverWriter = createServerWriter(fafSocket.getOutputStream());

            serverWriter.write(new LobbyMessage(LobbyAction.AUTHENTICATE, Collections.singletonList(userService.getSessionId())));

            startFaReader();
            redirectFafToFa();
          }
        } catch (SocketException | EOFException e) {
          logger.debug("Forged Alliance disconnected from relay server");
        }
      }
    }
  }

  private ServerWriter createServerWriter(OutputStream outputStream) throws IOException {
    ServerWriter serverWriter = new ServerWriter(outputStream);
    serverWriter.registerMessageSerializer(new RelayClientMessageSerializer(), LobbyMessage.class);
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
          redirectFaToFaf(faInputStream, serverWriter, this);
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
      while (!isCancelled()) {
        dataInput.skipBlockSize();
        String message = dataInput.readQString();

        logger.debug("Message from FAF relay server: {}", message);

        RelayServerMessage relayServerMessage = gson.fromJson(message, RelayServerMessage.class);

        dispatchServerCommand(relayServerMessage.getCommand(), message);
      }
    } catch (EOFException e) {
      logger.info("Disconnected from FAF relay server (EOF)");
    }
  }

  /**
   * Redirects any data read from the #faInputStream to the specified #serverWriter.
   *
   * @param faInputStream game input stream
   * @param serverWriter FAF relay server writer
   * @throws IOException
   */
  private void redirectFaToFaf(FaDataInputStream faInputStream, ServerWriter serverWriter, Task<Void> task) throws IOException {
    while (!task.isCancelled()) {
      LobbyAction action = LobbyAction.fromString(faInputStream.readString());
      List<Object> chunks = faInputStream.readChunks();
      LobbyMessage lobbyMessage = new LobbyMessage(action, chunks);

      if (p2pProxyEnabled.get()) {
        updateProxyState(lobbyMessage);
      }

      handleDataFromFa(lobbyMessage);
    }
  }

  private void handleDataFromFa(LobbyMessage lobbyMessage) throws IOException {
    if (isHostGameMessage(lobbyMessage)) {
      int gamePort = preferencesService.getPreferences().getForgedAlliance().getPort();
      String username = userService.getUsername();
      if (lobbyMode == null) {
        throw new IllegalStateException("lobbyMode has not been set");
      }

      handleCreateLobby(new CreateRelayServerMessage(lobbyMode, gamePort, username, userService.getUid(), 1));
    }

    serverWriter.write(lobbyMessage);
  }

  private boolean isHostGameMessage(LobbyMessage lobbyMessage) {
    return lobbyMessage.getAction() == LobbyAction.GAME_STATE
        && lobbyMessage.getChunks().get(0).equals("Idle");
  }

  private void updateProxyState(LobbyMessage lobbyMessage) {
    LobbyAction action = lobbyMessage.getAction();
    List<Object> chunks = lobbyMessage.getChunks();

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
          case "Launching":
            proxy.setGameLaunched(true);
            break;
          case "Lobby":
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
    serverWriter.write(LobbyMessage.pong());
  }

  private void handleSendNatPacket(SendNatPacketMessage sendNatPacketMessage) throws IOException {
    if (p2pProxyEnabled.get()) {
      String publicAddress = sendNatPacketMessage.getPublicAddress();

      proxy.registerPeerIfNecessary(publicAddress);

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

      proxy.registerPeerIfNecessary(peerAddress);

      connectToPeerMessage.setPeerAddress(proxy.translateToLocal(peerAddress));
      proxy.setUidForPeer(peerAddress, peerUid);
    }

    writeToFa(connectToPeerMessage);
  }

  private void handleJoinGame(JoinGameMessage joinGameMessage) throws IOException {
    if (p2pProxyEnabled.get()) {
      String peerAddress = joinGameMessage.getPeerAddress();
      int peerUid = joinGameMessage.getPeerUid();

      proxy.registerPeerIfNecessary(peerAddress);

      joinGameMessage.setPeerAddress(proxy.translateToLocal(peerAddress));
      proxy.setUidForPeer(peerAddress, peerUid);
    }

    writeToFa(joinGameMessage);
  }

  private void handleCreateLobby(CreateRelayServerMessage createRelayServerMessage) throws IOException {
    int peerUid = createRelayServerMessage.getUid();
    proxy.setUid(peerUid);

    if (p2pProxyEnabled.get()) {
      createRelayServerMessage.setPort(ProxyUtils.translateToProxyPort(proxy.getPort()));
    }

    writeToFa(createRelayServerMessage);
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

  private void writeToFaUdp(RelayServerMessage relayServerMessage) throws IOException {
    writeHeader(relayServerMessage);
    faOutputStream.writeUdpArgs(relayServerMessage.getArgs());
    faOutputStream.flush();
  }

  private void writeToFa(RelayServerMessage relayServerMessage) throws IOException {
    writeHeader(relayServerMessage);
    faOutputStream.writeArgs(relayServerMessage.getArgs());
    faOutputStream.flush();
  }

  private void writeHeader(RelayServerMessage relayServerMessage) throws IOException {
    String commandString = relayServerMessage.getCommand().getString();

    int headerSize = commandString.length();
    String headerField = commandString.replace("\t", "/t").replace("\n", "/n");

    logger.debug("Writing data to FA, command: {}, args: {}", commandString, relayServerMessage.getArgs());

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
