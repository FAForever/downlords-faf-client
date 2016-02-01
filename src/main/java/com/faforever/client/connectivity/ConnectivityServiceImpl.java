package com.faforever.client.connectivity;

import com.faforever.client.fx.HostService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.SocketAddressUtil;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.relay.LocalRelayServer;
import com.faforever.client.relay.ProcessNatPacketMessage;
import com.faforever.client.relay.SendNatPacketMessage;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.UserService;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static com.faforever.client.net.NetUtil.readSocket;
import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Opens the game port and connects to the FAF relay server in order the see whether data on the game port is received.
 */
public class ConnectivityServiceImpl implements ConnectivityService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String PORT_UNREACHABLE_NOTIFICATION_ID = "portUnreachable";
  private final ObjectProperty<ConnectivityState> connectivityState;
  private final ObjectProperty<InetSocketAddress> externalSocketAddress;
  private final Collection<Consumer<DatagramPacket>> onPacketListeners;
  private final Consumer<DatagramPacket> packetConsumer;
  private final Consumer<DatagramPacket> publicSendStrategy;
  private final Consumer<DatagramPacket> turnSendStrategy;

  @Resource
  TaskService taskService;
  @Resource
  NotificationService notificationService;
  @Resource
  PreferencesService preferencesService;
  @Resource
  HostService hostService;
  @Resource
  I18n i18n;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  FafService fafService;
  @Resource
  UserService userService;
  @Resource
  LocalRelayServer localRelayServer;
  @Resource
  TurnServerAccessor turnServerAccessor;
  @Value("${connectivity.helpUrl}")
  String connectivityHelpUrl;
  @Resource
  Executor executor;
  /**
   * The socket to the "outside", receives and sends game data.
   */
  private DatagramSocket publicSocket;
  private Consumer<DatagramPacket> sendStrategy;

  public ConnectivityServiceImpl() {
    connectivityState = new SimpleObjectProperty<>(ConnectivityState.UNKNOWN);
    externalSocketAddress = new SimpleObjectProperty<>();
    onPacketListeners = new LinkedList<>();
    packetConsumer = this::onIncomingPacket;

    publicSendStrategy = publicSendStrategy();
    turnSendStrategy = turnSendStrategy();
  }

  /**
   * Creates a forwarder that sends datagrams directly from a public UDP socket.
   */
  private Consumer<DatagramPacket> publicSendStrategy() {
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
  private Consumer<DatagramPacket> turnSendStrategy() {
    logger.info("Using TURN server");
    return datagramPacket -> turnServerAccessor.send(datagramPacket);
  }

  @PostConstruct
  void postConstruct() {
    fafService.addOnMessageListener(SendNatPacketMessage.class, this::onSendNatPacket);
    userService.loggedInProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        checkConnectivity();
      } else {
        connectivityState.set(ConnectivityState.UNKNOWN);
      }
    });

    preferencesService.getPreferences().getForgedAlliance().portProperty().addListener((observable, oldValue, newValue) -> {
      initPublicSocket(newValue.intValue());
    });
    initPublicSocket(preferencesService.getPreferences().getForgedAlliance().getPort());
  }

  @PreDestroy
  void preDestroy() {
    IOUtils.closeQuietly(publicSocket);
  }

  private void onIncomingPacket(DatagramPacket packet) {
    if (isNatPacket(packet.getData())) {
      handleNatPacket(packet);
    } else {
      handleGameData(packet);
    }
  }

  private boolean isNatPacket(byte[] data) {
    return data.length > 0 && data[0] == 0x08;
  }

  private void handleNatPacket(DatagramPacket packet) {
    if (logger.isTraceEnabled()) {
      logger.trace("Processing NAT package: {}", new String(packet.getData(), 0, packet.getLength(), US_ASCII));
    }

    String message = new String(packet.getData(), 1, packet.getLength() - 1);
    ProcessNatPacketMessage processNatPacketMessage = new ProcessNatPacketMessage((InetSocketAddress) packet.getSocketAddress(), message);
    fafService.sendGpgMessage(processNatPacketMessage);
  }

  private void handleGameData(DatagramPacket packet) {
    if (logger.isTraceEnabled()) {
      logger.trace("Game data from outside: {}", new String(packet.getData(), 0, packet.getLength(), US_ASCII));
    }

    onPacketListeners.forEach(listener -> listener.accept(packet));
  }

  private void initPublicSocket(int port) {
    IOUtils.closeQuietly(publicSocket);

    try {
      publicSocket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), port));
      readSocket(executor, publicSocket, packetConsumer);
      logger.info("Opened public UDP socket: {}", publicSocket.getLocalSocketAddress());
    } catch (SocketException | UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  private void onSendNatPacket(SendNatPacketMessage sendNatPacketMessage) {
    InetSocketAddress receiver = sendNatPacketMessage.getPublicAddress();
    String message = sendNatPacketMessage.getMessage();

    byte[] bytes = ("\b" + message).getBytes(US_ASCII);
    DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
    datagramPacket.setSocketAddress(receiver);

    logger.debug("Sending NAT packet to {}: {}", datagramPacket.getSocketAddress(), new String(datagramPacket.getData(), US_ASCII));
    publicSendStrategy.accept(datagramPacket);
  }

  @Override
  public CompletableFuture<Void> checkConnectivity() {
    this.connectivityState.set(ConnectivityState.UNKNOWN);

    UpnpPortForwardingTask upnpTask = applicationContext.getBean(UpnpPortForwardingTask.class);
    upnpTask.setPort(publicSocket.getLocalPort());

    ConnectivityCheckTask connectivityCheckTask = applicationContext.getBean(ConnectivityCheckTask.class);
    connectivityCheckTask.setPublicPort(publicSocket.getLocalPort());
    connectivityCheckTask.setDatagramGateway(this);

    return taskService.submitTask(upnpTask)
        .thenCompose(aVoid -> taskService.submitTask(connectivityCheckTask))
        .thenAccept(result -> {
          ConnectivityState state = result.getState();
          switch (state) {
            case BLOCKED:
              int publicPort = connectivityCheckTask.getPublicPort();
              logger.info("Port {} is unreachable", publicPort);
              notifyPortUnreachable(publicPort);
              break;
            default:
              logger.info("Connectivity check successful, state: {}, address: {}", state, SocketAddressUtil.toString(result.getSocketAddress()));
          }
          this.externalSocketAddress.set(result.getSocketAddress());
          this.connectivityState.set(state);
        })
        .exceptionally(throwable -> {
          logger.info("Port check failed", throwable);
          this.connectivityState.set(ConnectivityState.UNKNOWN);
          notificationService.addNotification(
              new PersistentNotification(i18n.get("portCheckTask.serverUnreachable"), Severity.WARN,
                  Collections.singletonList(
                      new Action(
                          i18n.get("portCheckTask.retry"),
                          event -> checkConnectivity()
                      ))
              ));
          return null;
        });
  }

  @Override
  public ReadOnlyObjectProperty<ConnectivityState> connectivityStateProperty() {
    return connectivityState;
  }

  @Override
  public ConnectivityState getConnectivityState() {
    return connectivityState.get();
  }

  @Override
  public InetSocketAddress getExternalSocketAddress() {
    return externalSocketAddress.get();
  }

  public void reset() {
    turnServerAccessor.removeOnPacketListener(packetConsumer);
    sendStrategy = null;
  }

  @Override
  public void connect() {
    logger.debug("Establishing connection");

    ConnectivityState connectivityState = this.connectivityState.get();
    switch (connectivityState) {
      case PUBLIC:
        reset();
        sendStrategy = publicSendStrategy;
        break;

      case STUN:
        turnServerAccessor.addOnPacketListener(packetConsumer);
        turnServerAccessor.connect();
        sendStrategy = turnSendStrategy;
        break;

      case BLOCKED:
        reset();
        throw new IllegalStateException("Can't connect");

      case UNKNOWN:
        reset();
        throw new IllegalStateException("Can't initialize connection when connectivity state is unknown");

      default:
        throw new AssertionError("Uncovered connectivity state: " + connectivityState);
    }
  }

  @Override
  public InetSocketAddress getRelayAddress() {
    return turnServerAccessor.getRelayAddress();
  }

  @VisibleForTesting
  public InetSocketAddress getPublicSocketAddress() {
    return (InetSocketAddress) publicSocket.getLocalSocketAddress();
  }

  /**
   * Notifies the user about port unreachability.
   */
  private void notifyPortUnreachable(int port) {
    List<Action> actions = Arrays.asList(
        new Action(
            i18n.get("portCheckTask.help"),
            event -> hostService.showDocument(connectivityHelpUrl)
        ),
        new Action(
            i18n.get("portCheckTask.neverShowAgain"),
            event -> preferencesService.getPreferences().getIgnoredNotifications().add(PORT_UNREACHABLE_NOTIFICATION_ID)
        ),
        new Action(
            i18n.get("portCheckTask.retry"),
            event -> checkConnectivity()
        )
    );

    notificationService.addNotification(
        new PersistentNotification(i18n.get("portCheckTask.unreachableNotification", port), Severity.WARN, actions)
    );
  }

  @Override
  public void addOnPacketListener(Consumer<DatagramPacket> consumer) {
    this.onPacketListeners.add(consumer);
  }

  @Override
  public void send(DatagramPacket datagramPacket) {
    if (!turnServerAccessor.isBound(datagramPacket.getSocketAddress())) {
      publicSendStrategy.accept(datagramPacket);
    } else {
      sendStrategy.accept(datagramPacket);
    }
  }

  @Override
  public void removeOnPacketListener(Consumer<DatagramPacket> listener) {
    this.onPacketListeners.remove(listener);
  }
}
