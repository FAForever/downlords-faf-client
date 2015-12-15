package com.faforever.client.connectivity;

import com.faforever.client.fx.HostService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.relay.SendNatPacketMessage;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.TaskService;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Opens the game port and connects to the FAF relay server in order the see whether data on the game port is received.
 */
public class ConnectivityServiceImpl implements ConnectivityService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String PORT_UNREACHABLE_NOTIFICATION_ID = "portUnreachable";

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
  TurnClient turnClient;
  @Resource
  FafService fafService;

  @Value("${connectivity.helpUrl}")
  String connectivityHelpUrl;

  private ObjectProperty<ConnectivityState> connectivityState;
  private DatagramSocket publicSocket;
  private Consumer<DatagramPacket> forwarder;

  public ConnectivityServiceImpl() {
    connectivityState = new SimpleObjectProperty<>(ConnectivityState.UNKNOWN);
  }

  @PostConstruct
  void postConstruct() {
    preferencesService.getPreferences().getForgedAlliance().portProperty().addListener((observable, oldValue, newValue) -> {
      initPublicSocket(newValue.intValue());
    });
    initPublicSocket(preferencesService.getPreferences().getForgedAlliance().getPort());

    fafService.addOnMessageListener(SendNatPacketMessage.class, this::onSendNatPacket);
  }

  private void initPublicSocket(int port) {
    IOUtils.closeQuietly(publicSocket);

    try {
      publicSocket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), port));
      logger.info("Opened public UDP socket: {}", publicSocket);
    } catch (SocketException | UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  @PreDestroy
  void preDestroy() {
    IOUtils.closeQuietly(publicSocket);
  }

  private void onSendNatPacket(SendNatPacketMessage sendNatPacketMessage) {
    if (sendNatPacketMessage.getTarget() != MessageTarget.CONNECTIVITY) {
      return;
    }

    InetSocketAddress received = sendNatPacketMessage.getPublicAddress();
    String message = sendNatPacketMessage.getMessage();

    logger.debug("Sending NAT packet to {}: {}", received, message);

    byte[] bytes = ('\u0008' + message).getBytes(US_ASCII);
    DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
    datagramPacket.setSocketAddress(received);
    try {
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
    connectivityCheckTask.setPublicSocket(publicSocket);

    return taskService.submitTask(upnpTask)
        .thenCompose(aVoid -> taskService.submitTask(connectivityCheckTask))
        .thenAccept(result -> {
          switch (result) {
            case BLOCKED:
              logger.info("Port {} is unreachable", connectivityCheckTask.getPublicSocket());
              notifyPortUnreachable(publicSocket.getLocalPort());
              break;
            default:
              logger.info("Connectivity check successful, state: {}", result);
          }
          this.connectivityState.set(result);
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
  public CompletableFuture<InetSocketAddress> setUpConnection() {
    ConnectivityState connectivityState = this.connectivityState.get();
    switch (connectivityState) {
      case PUBLIC:
        forwarder = publicForwarder();
        return CompletableFuture.completedFuture(null);
      case STUN:
        forwarder = turnForwarder();
        return turnClient.connect();
      case BLOCKED:
        throw new IllegalStateException("Can't connect");
      case UNKNOWN:
        throw new IllegalStateException("Connectivity state has not been set");
      default:
        throw new AssertionError("Uncovered connectivity state: " + connectivityState);
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
    return datagramPacket -> turnClient.send(datagramPacket);
  }

  @Override
  public void closeRelayConnection() {
    IOUtils.closeQuietly(turnClient);
  }

  @Override
  public void sendGameData(DatagramPacket datagramPacket) {
    forwarder.accept(datagramPacket);
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
  public int getExternalPort() {
    switch (connectivityState.get()) {
      case PUBLIC:
        return preferencesService.getPreferences().getForgedAlliance().getPort();
      case STUN:
        return turnClient.getMappedAddress().getPort();
      default:
        return -1;
    }
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
}
