package com.faforever.client.portcheck;

import com.faforever.client.fx.HostService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Opens the game port and connects to the FAF relay server in order the see whether data on the game port is received.
 */
public class DownlordsPortCheckServiceImpl implements PortCheckService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String PORT_UNREACHABLE_NOTIFICATION_ID = "portUnreachable";

  @Autowired
  TaskService taskService;

  @Autowired
  Environment environment;

  @Autowired
  NotificationService notificationService;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  HostService hostService;

  @Autowired
  I18n i18n;

  @Autowired
  ApplicationContext applicationContext;

  private Collection<GamePortCheckListener> gamePortCheckListeners;

  public DownlordsPortCheckServiceImpl() {
    gamePortCheckListeners = new ArrayList<>();
  }

  @Override
  public void checkGamePortInBackground() {
    gamePortCheckListeners.forEach(GamePortCheckListener::onGamePortCheckStarted);

    int port = preferencesService.getPreferences().getForgedAlliance().getPort();

    PortCheckTask task = applicationContext.getBean(PortCheckTask.class);
    task.setPort(port);

    taskService.submitTask(task, new Callback<Boolean>() {
      @Override
      public void success(Boolean result) {
        if (!result) {
          notifyPortUnreachable(port);
        }
        for (GamePortCheckListener gamePortCheckListener : gamePortCheckListeners) {
          gamePortCheckListener.onGamePortCheckResult(result);
        }
      }

      @Override
      public void error(Throwable e) {
        logger.info("Port check failed", e);
      }
    });
  }

  @Override
  public void addGamePortCheckListener(GamePortCheckListener listener) {
    gamePortCheckListeners.add(listener);
  }

  /**
   * Notifies the user about port unreachability.
   */

  private void notifyPortUnreachable(int port) {
    List<Action> actions = Arrays.asList(
        new Action(
            i18n.get("portCheckTask.help"),
            event -> hostService.showDocument(environment.getProperty("portCheck.helpUrl"))
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
