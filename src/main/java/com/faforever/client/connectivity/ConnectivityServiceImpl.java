package com.faforever.client.connectivity;

import com.faforever.client.fx.HostService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.relay.LocalRelayServer;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.SocketAddressUtil;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
  FafService fafService;
  @Resource
  LocalRelayServer localRelayServer;

  @Value("${connectivity.helpUrl}")
  String connectivityHelpUrl;

  private ObjectProperty<ConnectivityState> connectivityState;
  private ObjectProperty<InetSocketAddress> externalSocketAddress;

  public ConnectivityServiceImpl() {
    connectivityState = new SimpleObjectProperty<>(ConnectivityState.UNKNOWN);
    externalSocketAddress = new SimpleObjectProperty<>();
  }

  @Override
  public CompletableFuture<Void> checkConnectivity() {
    this.connectivityState.set(ConnectivityState.UNKNOWN);

    UpnpPortForwardingTask upnpTask = applicationContext.getBean(UpnpPortForwardingTask.class);
    upnpTask.setPort(localRelayServer.getPublicPort());

    ConnectivityCheckTask connectivityCheckTask = applicationContext.getBean(ConnectivityCheckTask.class);
    connectivityCheckTask.setPublicPort(localRelayServer.getPublicPort());

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
  public InetSocketAddress getRelayAddress() {
    return localRelayServer.getRelayAddress();
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
