package com.faforever.client.connectivity;

import com.faforever.client.fx.HostService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.relay.LocalRelayServer;
import com.faforever.client.relay.PackageReceiver;
import com.faforever.client.relay.ProcessNatPacketMessage;
import com.faforever.client.relay.SendNatPacketMessage;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.SocketAddressUtil;
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

import static com.faforever.client.net.NetUtil.forwardSocket;
import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Opens the game port and connects to the FAF relay server in order the see whether data on the game port is received.
 */
public class ConnectivityServiceImpl implements ConnectivityService, PackageReceiver {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String PORT_UNREACHABLE_NOTIFICATION_ID = "portUnreachable";
  private final ObjectProperty<ConnectivityState> connectivityState;
  private final ObjectProperty<InetSocketAddress> externalSocketAddress;
  private final Collection<Consumer<DatagramPacket>> onPacketFromOutsideListeners;
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
  LocalRelayServer localRelayServer;
  @Resource
  TurnServerAccessor turnServerAccessor;
  @Value("${connectivity.helpUrl}")
  String connectivityHelpUrl;
  @Resource
  private Executor executor;
  /**
   * The socket to the "outside", receives and sends game data.
   */
  private DatagramSocket publicSocket;

  public ConnectivityServiceImpl() {
    connectivityState = new SimpleObjectProperty<>(ConnectivityState.UNKNOWN);
    externalSocketAddress = new SimpleObjectProperty<>();
    onPacketFromOutsideListeners = new LinkedList<>();
  }

  @PostConstruct
  void postConstruct() {
    fafService.addOnMessageListener(SendNatPacketMessage.class, this::onSendNatPacket);
    fafService.connectionStateProperty().addListener((observable, oldValue, newValue) -> {
      switch (newValue) {
        case CONNECTING:
        case DISCONNECTED:
          connectivityState.set(ConnectivityState.UNKNOWN);
          break;
        case CONNECTED:
          checkConnectivity();
          break;
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

  private void listenForPublicGameData() {
    forwardSocket(executor, publicSocket, packet -> {
      if (isNatPackage(packet.getData())) {
        if (logger.isTraceEnabled()) {
          logger.trace("Processing NAT package: {}", new String(packet.getData(), 0, packet.getLength(), US_ASCII));
        }

        String message = new String(packet.getData(), 1, packet.getLength() - 1);
        ProcessNatPacketMessage processNatPacketMessage = new ProcessNatPacketMessage((InetSocketAddress) packet.getSocketAddress(), message);
        fafService.sendGpgMessage(processNatPacketMessage);
      } else {
        if (logger.isTraceEnabled()) {
          logger.trace("Game data from outside: {}", new String(packet.getData(), 0, packet.getLength(), US_ASCII));
        }

        onPacketFromOutsideListeners.forEach(listener -> listener.accept(packet));

        for (Consumer<DatagramPacket> listener : onPacketFromOutsideListeners) {
          listener.accept(packet);
        }
      }
    });
  }

  private boolean isNatPackage(byte[] data) {
    return data.length > 0 && data[0] == 0x08;
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

  @Override
  public CompletableFuture<Void> checkConnectivity() {
    this.connectivityState.set(ConnectivityState.UNKNOWN);

    UpnpPortForwardingTask upnpTask = applicationContext.getBean(UpnpPortForwardingTask.class);
    upnpTask.setPort(publicSocket.getLocalPort());

    ConnectivityCheckTask connectivityCheckTask = applicationContext.getBean(ConnectivityCheckTask.class);
    connectivityCheckTask.setPublicPort(publicSocket.getLocalPort());

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

  @Override
  public Consumer<DatagramPacket> ensureConnection() {
    switch (connectivityState.get()) {
      case UNKNOWN:
      case PUBLIC:
        turnServerAccessor.disconnect();
        return publicForwarder();
      break;
      case STUN:
        turnServerAccessor.ensureConnected();
        turnServerAccessor.setOnDataListener(this::dispatchPacketFromOutside);
        return turnForwarder();
      break;
      case BLOCKED:
        turnServerAccessor.disconnect();
        throw new IllegalStateException("Can't connect");
      default:
        throw new AssertionError("Uncovered connectivity state: " + newValue);
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
  public void setOnPackageListener(Consumer<DatagramPacket> consumer) {
    this.packageConsumer = consumer;
  }
}
