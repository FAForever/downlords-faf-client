package com.faforever.client.connectivity;

import com.faforever.client.fx.HostService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.net.SocketAddress;
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
  @Value("${portCheck.helpUrl}")
  String connectivityHelpUrl;
  private ConnectivityState connectivityState;
  private TurnClient turnClient;

  @Override
  public ConnectivityState getConnectivityState() {
    return connectivityState;
  }

  @Override
  public CompletableFuture<ConnectivityState> checkGamePortInBackground() {
    int port = preferencesService.getPreferences().getForgedAlliance().getPort();

    PortCheckTask task = applicationContext.getBean(PortCheckTask.class);
    task.setPort(port);

    return taskService.submitTask(task).thenApply(result -> {
      switch (result) {
        case BLOCKED:
          logger.info("Port is not reachable");
          notifyPortUnreachable(port);
          break;
        default:
          logger.info("Port check successful, state: {}", result);
      }
      this.connectivityState = result;
      return result;
    }).exceptionally(throwable -> {
      logger.info("Port check failed", throwable);
      notificationService.addNotification(
          new PersistentNotification(i18n.get("portCheckTask.serverUnreachable"), Severity.WARN,
              Collections.singletonList(
                  new Action(
                      i18n.get("portCheckTask.retry"),
                      event -> checkGamePortInBackground()
                  ))
          ));
      return null;
    });
  }

  @Override
  public CompletableFuture<SocketAddress> ensureReachability() {
    if (connectivityState == ConnectivityState.PUBLIC) {
      return CompletableFuture.completedFuture(null);
    }
    if (turnClient == null) {
      turnClient = applicationContext.getBean(TurnClient.class);
    }
    return turnClient.connect();
  }

  @Override
  public void disconnect() {
    IOUtils.closeQuietly(turnClient);
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
            event -> checkGamePortInBackground()
        )
    );

    notificationService.addNotification(
        new PersistentNotification(i18n.get("portCheckTask.unreachableNotification", port), Severity.WARN, actions)
    );
  }
}
