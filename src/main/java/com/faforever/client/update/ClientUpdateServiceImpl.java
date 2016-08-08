package com.faforever.client.update;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.task.TaskService;
import com.google.common.annotations.VisibleForTesting;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Arrays;

import static com.faforever.client.io.Bytes.formatSize;
import static com.faforever.client.notification.Severity.INFO;
import static com.faforever.client.notification.Severity.WARN;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.defaultString;

public class ClientUpdateServiceImpl implements ClientUpdateService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String DEVELOPMENT_VERSION_STRING = "dev";

  @Resource
  TaskService taskService;
  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;
  @Resource
  PlatformService platformService;
  @Resource
  ApplicationContext applicationContext;

  @VisibleForTesting
  ComparableVersion currentVersion;

  public ClientUpdateServiceImpl() {
    currentVersion = new ComparableVersion(
        defaultString(getClass().getPackage().getImplementationVersion(), DEVELOPMENT_VERSION_STRING)
    );
  }

  @Override
  public void checkForUpdateInBackground() {
    CheckForUpdateTask task = applicationContext.getBean(CheckForUpdateTask.class);
    task.setCurrentVersion(currentVersion);

    taskService.submitTask(task).thenAccept(updateInfo -> {
      if (updateInfo == null) {
        return;
      }

      notificationService.addNotification(
          new PersistentNotification(
              i18n.get("clientUpdateAvailable.notification", updateInfo.getName(), formatSize(updateInfo.getSize(), i18n.getLocale())),
              INFO,
              Arrays.asList(
                  new Action(
                      i18n.get("clientUpdateAvailable.downloadAndInstall"),
                      event -> downloadAndInstallInBackground(updateInfo)
                  ),
                  new Action(
                      i18n.get("clientUpdateAvailable.releaseNotes"),
                      Action.Type.OK_STAY,
                      event -> platformService.showDocument(updateInfo.getReleaseNotesUrl().toExternalForm())
                  )
              )
          )
      );
    }).exceptionally(throwable -> {
      logger.warn("Client update check failed", throwable);
      return null;
    });
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
    DownloadUpdateTask task = applicationContext.getBean(DownloadUpdateTask.class);
    task.setUpdateInfo(updateInfo);

    taskService.submitTask(task)
        .thenAccept(this::install)
        .exceptionally(throwable -> {
          logger.warn("Error while downloading client update", throwable);
          notificationService.addNotification(
              new PersistentNotification(i18n.get("clientUpdateDownloadFailed.notification"),
                  WARN,
                  singletonList(
                      new Action(i18n.get("clientUpdateDownloadFailed.retry"), event -> downloadAndInstallInBackground(updateInfo))
                  )
              )
          );
          return null;
        });
  }
}
