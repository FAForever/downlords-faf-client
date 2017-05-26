package com.faforever.client.connectivity;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.net.SocketAddressUtil;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.relay.ProcessNatPacketMessage;
import com.faforever.client.relay.SendNatPacketMessage;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.UserService;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.apache.commons.compress.utils.IOUtils;
import org.ice4j.TransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.SocketUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import static com.faforever.client.net.SocketUtil.readSocket;
import static com.github.nocatch.NoCatch.noCatch;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Collections.singletonList;
import static org.ice4j.Transport.UDP;

/**
 * Opens the game port and connects to the FAF relay server in order the see whether data on the game port is received.
 */
@Service
public class ConnectivityServiceImpl implements ConnectivityService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Prefix of NAT packages containing a bind statement, e.g. "Bind 1234" which means "Bind peer with UID 1234 to a
   * channel".
   */
  private static final String BIND_PREFIX = "Bind";
  private static final String PORT_UNREACHABLE_NOTIFICATION_ID = "portUnreachable";
  private final ObjectProperty<ConnectivityState> connectivityState;
  private final ObjectProperty<InetSocketAddress> externalSocketAddress;
  private final Collection<Consumer<DatagramPacket>> onPacketListeners;
  private final Consumer<DatagramPacket> packetConsumer;

  @Resource
  TaskService taskService;
  @Resource
  NotificationService notificationService;
  @Resource
  PreferencesService preferencesService;
  @Resource
  PlatformService platformService;
  @Resource
  I18n i18n;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  FafService fafService;
  @Resource
  UserService userService;
  @Resource
  TurnServerAccessor turnServerAccessor;
  @Resource
  ThreadPoolExecutor threadPoolExecutor;
  /**
   * The socket to the "outside", receives and sends game data.
   */
  private DatagramSocket publicSocket;

  public ConnectivityServiceImpl() {
    connectivityState = new SimpleObjectProperty<>(ConnectivityState.UNKNOWN);
    externalSocketAddress = new SimpleObjectProperty<>();
    onPacketListeners = new LinkedList<>();
    packetConsumer = this::onIncomingPacket;
  }

  @PostConstruct
  void postConstruct() {
    fafService.addOnMessageListener(SendNatPacketMessage.class, this::onSendNatPacket);
    fafService.addOnMessageListener(LoginMessage.class, loginMessage -> checkConnectivity());
    fafService.connectionStateProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != ConnectionState.CONNECTED) {
        connectivityState.set(ConnectivityState.UNKNOWN);
      }
    });

    preferencesService.getPreferences().getForgedAlliance().portProperty().addListener((observable, oldValue, newValue) ->
        initPublicSocket(newValue.intValue())
    );
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
      handleIncomingData(packet);
    }
  }

  private boolean isNatPacket(byte[] data) {
    return data.length > 0 && data[0] == 0x08;
  }

  private void handleNatPacket(DatagramPacket packet) {
    if (logger.isTraceEnabled()) {
      logger.trace("Processing NAT package from {}: {}", packet.getSocketAddress(), new String(packet.getData(), 0, packet.getLength(), US_ASCII));
    }

    String message = new String(packet.getData(), 1, packet.getLength() - 1);
    ProcessNatPacketMessage processNatPacketMessage = new ProcessNatPacketMessage((InetSocketAddress) packet.getSocketAddress(), message);
    fafService.sendGpgGameMessage(processNatPacketMessage);

    if (message.startsWith(BIND_PREFIX)) {
      turnServerAccessor.bind(new TransportAddress((InetSocketAddress) packet.getSocketAddress(), UDP));
    }
  }

  private void handleIncomingData(DatagramPacket packet) {
    if (logger.isTraceEnabled()) {
      logger.trace("Incoming data from {}: {}", packet.getSocketAddress(), new String(packet.getData(), 0, packet.getLength(), US_ASCII));
    }

    onPacketListeners.forEach(listener -> listener.accept(packet));
  }

  private void onSendNatPacket(SendNatPacketMessage sendNatPacketMessage) {
    InetSocketAddress receiver = sendNatPacketMessage.getPublicAddress();
    String message = sendNatPacketMessage.getMessage();

    // If a NatPacket is received but the connectivity state is not yet determined, use a random port.
    // This is the case when the connectivity check is running and the port is not publicly reachable. The reason we
    // then chose a random port is because the NAT may still have a mapping from the last client session. If so, we
    // would receive all packets send by the server and therefore end up being detected PUBLIC even though we're not.
    if (isDefaultPortAndStateUnknown()) {
      logger.info("Switching to random port for STUN connectivity");
      initPublicSocket(SocketUtils.findAvailableUdpPort());
    }

    byte[] bytes = ("\b" + message).getBytes(US_ASCII);
    DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
    datagramPacket.setSocketAddress(receiver);

    logger.debug("Sending NAT packet to {}: {}", datagramPacket.getSocketAddress(), new String(datagramPacket.getData(), US_ASCII));
    send(datagramPacket);
  }

  /**
   * Returns true if the currently opened port is the default port (as set by the user, or 6112) or the connectivity
   * state is yet unknown.
   */
  private boolean isDefaultPortAndStateUnknown() {
    return publicSocket.getLocalPort() == preferencesService.getPreferences().getForgedAlliance().getPort()
        && connectivityState.get() == ConnectivityState.UNKNOWN;
  }

  private void initPublicSocket(int port) {
    IOUtils.closeQuietly(publicSocket);

    try {
      publicSocket = new DatagramSocket(port);
      readSocket(threadPoolExecutor, publicSocket, packetConsumer);
      logger.info("Opened public UDP socket: {}", publicSocket.getLocalSocketAddress());
    } catch (SocketException e) {
      notificationService.addNotification(
          new PersistentNotification(i18n.get("portCheckTask.portOccupied", port), Severity.WARN, singletonList(
              new Action(i18n.get("portCheckTask.retry"), event -> initPublicSocket(port))
              // TODO add action that allows the user to change the port
          ))
      );
    }
  }

  @Override
  public CompletionStage<Void> checkConnectivity() {
    this.connectivityState.set(ConnectivityState.UNKNOWN);
    resetPort();

    UpnpPortForwardingTask upnpTask = applicationContext.getBean(UpnpPortForwardingTask.class);
    upnpTask.setPort(publicSocket.getLocalPort());

    ConnectivityCheckTask connectivityCheckTask = applicationContext.getBean(ConnectivityCheckTask.class);
    connectivityCheckTask.setPublicPort(publicSocket.getLocalPort());
    connectivityCheckTask.setDatagramGateway(this);

    return taskService.submitTask(upnpTask).getFuture()
        .thenCompose(aVoid -> taskService.submitTask(connectivityCheckTask).getFuture())
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
          fafService.setExternalSocketPort(externalSocketAddress.get().getPort());
          this.connectivityState.set(state);
        })
        .exceptionally(throwable -> {
          logger.info("Port check failed", throwable);
          this.connectivityState.set(ConnectivityState.UNKNOWN);
          notificationService.addNotification(
              new PersistentNotification(i18n.get("portCheckTask.serverUnreachable"), Severity.WARN,
                  singletonList(new Action(i18n.get("portCheckTask.retry"), event -> checkConnectivity()))
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

  @Override
  public void connect() {
    logger.debug("Establishing connection");

    ConnectivityState connectivityState = this.connectivityState.get();
    switch (connectivityState) {
      case PUBLIC:
        break;

      case STUN:
        turnServerAccessor.addOnPacketListener(packetConsumer);
        turnServerAccessor.connect();
        break;

      case BLOCKED:
        throw new IllegalStateException("Can't connect");

      case UNKNOWN:
        throw new IllegalStateException("Can't initialize connection when connectivity state is unknown");

      default:
        throw new AssertionError("Uncovered connectivity state: " + connectivityState);
    }
  }

  @Override
  public InetSocketAddress getRelayAddress() {
    return turnServerAccessor.getRelayAddress();
  }

  private void resetPort() {
    int gamePort = preferencesService.getPreferences().getForgedAlliance().getPort();
    if (publicSocket == null || publicSocket.getLocalPort() != gamePort) {
      initPublicSocket(gamePort);
    }
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
            // This is only hardcoded here because the whole code will be thrown away once ICE is ready.
            event -> platformService.showDocument("http://wiki.faforever.com/index.php?title=Connection_issues_and_solutions")
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
    if (turnServerAccessor.isBound((InetSocketAddress) datagramPacket.getSocketAddress())) {
      turnServerAccessor.send(datagramPacket);
    } else {
      noCatch(() -> publicSocket.send(datagramPacket));
    }
  }

  @Override
  public void removeOnPacketListener(Consumer<DatagramPacket> listener) {
    this.onPacketListeners.remove(listener);
  }
}
