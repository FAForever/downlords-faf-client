package com.faforever.client.network;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.Callback;
import javafx.application.HostServices;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.faforever.client.task.PrioritizedTask.Priority.LOW;
import static com.faforever.client.task.TaskGroup.NET_LIGHT;

public class PortCheckServiceImpl implements PortCheckService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String PORT_UNREACHABLE_NOTIFICATION_ID = "portUnreachable";
  private static final int REQUEST_DELAY = 1000;
  private static final int TIMEOUT = 5000;
  // TODO send a random number that the server should send back
  private static final String EXPECTED_ANSWER = "OK";

  @Autowired
  TaskService taskService;

  @Autowired
  Environment environment;

  @Autowired
  NotificationService notificationService;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  HostServices hostServices;

  @Autowired
  I18n i18n;

  private Collection<GamePortCheckListener> gamePortCheckListeners;

  public PortCheckServiceImpl() {
    gamePortCheckListeners = new ArrayList<>();
  }

  @Override
  public void checkGamePortInBackground() {
    gamePortCheckListeners.forEach(GamePortCheckListener::onGamePortCheckStarted);

    int port = preferencesService.getPreferences().getForgedAlliance().getPort();

    taskService.submitTask(NET_LIGHT, new PrioritizedTask<Boolean>(i18n.get("portCheckTask.title"), LOW) {
      @Override
      protected Boolean call() throws Exception {
        return checkPort(port);
      }
    }, new Callback<Boolean>() {
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

  @NotNull
  private Boolean checkPort(int port) throws IOException {
    String remoteHost = environment.getProperty("portCheck.host");
    Integer remotePort = environment.getProperty("portCheck.port", Integer.class);

    logger.info("Checking reachability of UDP port {} using port checker service at {}:{}", port, remoteHost, remotePort);

    sendDelayedPackageRequest(remoteHost, remotePort, port);

    try (DatagramSocket datagramSocket = new DatagramSocket(port)) {
      datagramSocket.setSoTimeout(TIMEOUT);

      byte[] buffer = new byte[EXPECTED_ANSWER.length()];
      datagramSocket.receive(new DatagramPacket(buffer, buffer.length));

      logger.info("UDP port {} is reachable", port);

      return true;
    } catch (SocketTimeoutException e) {
      logger.info("UDP port {} is unreachable", port);
      return false;
    }
  }

  /**
   * Notifies the user about port unreachability.
   */
  private void notifyPortUnreachable(int port) {
    List<Action> actions = Arrays.asList(
        new Action(
            i18n.get("portCheckTask.help"),
            event -> hostServices.showDocument(environment.getProperty("portCheck.helpUrl"))
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

  /**
   * Sends a request to the given external port checker server to send a UDP package to the given port. This request is
   * sent with a short delay to allow the UDP port to be opened.
   */
  private void sendDelayedPackageRequest(final String remoteHost, final Integer remotePort, final int port) {
    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        try {
          try (Socket socket = new Socket(remoteHost, remotePort);
               DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
            dataOutputStream.writeInt(port);
          }
        } catch (ConnectException e) {
          logger.warn("Port check server is not reachable");
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }, REQUEST_DELAY);
  }
}
