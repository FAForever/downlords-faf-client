package com.faforever.client.update;

import com.faforever.client.fx.HostService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskGroup;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.Bytes;
import com.faforever.client.util.Callback;
import com.google.common.annotations.VisibleForTesting;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Arrays;

import static com.faforever.client.notification.Severity.INFO;
import static com.faforever.client.notification.Severity.WARN;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.defaultString;

public class ClientUpdateServiceImpl implements ClientUpdateService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String DEVELOPMENT_VERSION_STRING = "dev";

  @Autowired
  Environment environment;

  @Autowired
  TaskService taskService;

  @Autowired
  NotificationService notificationService;

  @Autowired
  I18n i18n;

  @Autowired
  HostService hostService;

  @Autowired
  PreferencesService preferencesService;

  @VisibleForTesting
  ComparableVersion currentVersion;

  public ClientUpdateServiceImpl() {
    currentVersion = new ComparableVersion(
        defaultString(getClass().getPackage().getImplementationVersion(), DEVELOPMENT_VERSION_STRING)
    );
  }

  @Override
  public void checkForUpdateInBackground() {
    taskService.submitTask(TaskGroup.NET_LIGHT, new CheckForUpdateTask(environment, i18n, currentVersion),
        new Callback<UpdateInfo>() {

          @Override
          public void success(UpdateInfo updateInfo) {
            if (updateInfo == null) {
              return;
            }

            notificationService.addNotification(
                new PersistentNotification(
                    i18n.get("clientUpdateAvailable.notification", updateInfo.getName(), Bytes.formatSize(updateInfo.getSize())),
                    INFO,
                    Arrays.asList(
                        new Action(
                            i18n.get("clientUpdateAvailable.downloadAndInstall"),
                            event -> downloadAndInstallInBackground(updateInfo)
                        ),
                        new Action(
                            i18n.get("clientUpdateAvailable.releaseNotes"),
                            Action.Type.OK_STAY,
                            event -> hostService.showDocument(updateInfo.getReleaseNotesUrl().toExternalForm())
                        ),
                        new Action(i18n.get("clientUpdateAvailable.ignore"))
                    )
                )
            );
          }

          @Override
          public void error(Throwable e) {
            logger.warn("Client update check failed", e);
          }
        }
    );
  }

  @Override
  public ComparableVersion getCurrentVersion() {
    return currentVersion;
  }

  private void install(Path binaryPath) {
    // TODO probably need to make this executable on unix
    String command = binaryPath.toAbsolutePath().toString();
    try {
      logger.info("Starting installer at {}", command);
      new ProcessBuilder(command).inheritIO().start();
    } catch (IOException e) {
      logger.warn("Installation could not be started", e);
    }
  }

  private void downloadAndInstallInBackground(UpdateInfo updateInfo) {
    taskService.submitTask(TaskGroup.NET_HEAVY, new DownloadUpdateTask(i18n, preferencesService, updateInfo), new Callback<Path>() {
      @Override
      public void success(Path result) {
        install(result);
      }

      @Override
      public void error(Throwable e) {
        notificationService.addNotification(
            new PersistentNotification(i18n.get("clientUpdateDownloadFailed.notification"),
                WARN,
                singletonList(
                    new Action(i18n.get("clientUpdateDownloadFailed.retry"), event -> downloadAndInstallInBackground(updateInfo))
                )
            )
        );
      }
    });
  }
}
