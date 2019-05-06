package com.faforever.client.update;

import com.faforever.client.FafClientApplication;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;

import static com.faforever.client.notification.Severity.INFO;
import static com.faforever.client.notification.Severity.WARN;
import static com.faforever.commons.io.Bytes.formatSize;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.defaultString;


@Lazy
@Service
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
public class ClientUpdateServiceImpl implements ClientUpdateService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String DEVELOPMENT_VERSION_STRING = "dev";

  private final TaskService taskService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final PlatformService platformService;
  private final ApplicationContext applicationContext;

  @VisibleForTesting
  ComparableVersion currentVersion;

  public ClientUpdateServiceImpl(
      TaskService taskService,
      NotificationService notificationService,
      I18n i18n,
      PlatformService platformService,
      ApplicationContext applicationContext
  ) {
    this.taskService = taskService;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.platformService = platformService;
    this.applicationContext = applicationContext;

    currentVersion = new ComparableVersion(
        defaultString(Version.VERSION, DEVELOPMENT_VERSION_STRING)
    );
    logger.info("Current version: {}", currentVersion);
  }

  /**
   * Returns information about an available update. Returns {@code null} if no update is available.
   */
  public void checkForUpdateInBackground() {
    CheckForUpdateTask task = applicationContext.getBean(CheckForUpdateTask.class);
    task.setCurrentVersion(currentVersion);

    taskService.submitTask(task).getFuture().thenAccept(updateInfo -> {
      if (updateInfo == null) {
        return;
      }
      notificationService.addNotification(new PersistentNotification(
          i18n.get("clientUpdateAvailable.notification", updateInfo.getName(), formatSize(updateInfo.getSize(), i18n.getUserSpecificLocale())),
          INFO, asList(
          new Action(i18n.get("clientUpdateAvailable.downloadAndInstall"), event -> downloadAndInstallInBackground(updateInfo)),
          new Action(i18n.get("clientUpdateAvailable.releaseNotes"), Action.Type.OK_STAY,
              event -> platformService.showDocument(updateInfo.getReleaseNotesUrl().toExternalForm())
          )))
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

  @VisibleForTesting
  void install(Path binaryPath) {
  	try {
      platformService.setUnixExecutableAndWritableBits(binaryPath);
  	} catch (IOException e) {
      logger.warn("Unix execute bit could not be set", e);
  	}
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

    taskService.submitTask(task).getFuture()
        .thenAccept(this::install)
        .exceptionally(throwable -> {
          logger.warn("Error while downloading client update", throwable);
          notificationService.addNotification(
              new PersistentNotification(i18n.get("clientUpdateDownloadFailed.notification"), WARN, singletonList(
                  new Action(i18n.get("clientUpdateDownloadFailed.retry"), event -> downloadAndInstallInBackground(updateInfo))
              ))
          );
          return null;
        });
  }
}
