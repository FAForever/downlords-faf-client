package com.faforever.client.portcheck;

import com.faforever.client.fx.HostService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.relay.LobbyAction;
import com.faforever.client.legacy.relay.LobbyMessage;
import com.faforever.client.legacy.relay.RelayClientMessageSerializer;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.upnp.UpnpService;
import com.faforever.client.util.Callback;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

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
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.faforever.client.task.PrioritizedTask.Priority.LOW;
import static com.faforever.client.task.TaskGroup.IMMEDIATE;

/**
 * Opens the game port and connects to the FAF relay server in order the see whether data on the game port is received.
 */
public class PortCheckServiceImpl implements PortCheckService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String PORT_UNREACHABLE_NOTIFICATION_ID = "portUnreachable";
  private static final int REQUEST_DELAY = 1000;
  private static final int TIMEOUT = 5000;

  /**
   * Half an hour.
   */
  private static final long UPNP_LIFETIME = 1800;

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
  UpnpService upnpService;

  private final Collection<GamePortCheckListener> gamePortCheckListeners;

  public PortCheckServiceImpl() {
    gamePortCheckListeners = new ArrayList<>();
  }

  @Override
  public void checkGamePortInBackground() {
    gamePortCheckListeners.forEach(GamePortCheckListener::onGamePortCheckStarted);

    int port = preferencesService.getPreferences().getForgedAlliance().getPort();

    taskService.submitTask(IMMEDIATE, new PrioritizedTask<Boolean>(i18n.get("portCheckTask.title"), LOW) {
      @Override
      protected Boolean call() throws Exception {
        Boolean successful = checkPort(port);
        if (!successful) {
          logger.info("Port check failed, trying UPnP");
          upnpService.forwardPort(port);
          successful = checkPort(port);
        }
        return successful;
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
    String remoteHost = environment.getProperty("relay.host");
    Integer remotePort = environment.getProperty("relay.port", Integer.class);

    logger.info("Checking reachability of UDP port {} using port checker service at {}:{}", port, remoteHost, remotePort);

    sendDelayedPackageRequest(remoteHost, remotePort, port);

    try (DatagramSocket datagramSocket = new DatagramSocket(port)) {
      datagramSocket.setSoTimeout(TIMEOUT);

      byte[] buffer = new byte[1024];
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
               ServerWriter serverWriter = createServerWriter(socket)) {
            serverWriter.write(new LobbyMessage(LobbyAction.GAME_STATE, Collections.singletonList("Idle")));
          }
        } catch (ConnectException e) {
          logger.warn("Port check server is not reachable");
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }, REQUEST_DELAY);
  }



  private ServerWriter createServerWriter(Socket fafSocket) throws IOException {
    ServerWriter serverWriter = new ServerWriter(fafSocket.getOutputStream());
    serverWriter.registerMessageSerializer(new RelayClientMessageSerializer(), LobbyMessage.class);
    return serverWriter;
  }
}
