package com.faforever.client.update;

import com.faforever.client.FafClientApplication;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.event.LoggedInEvent;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.notification.Severity.INFO;
import static com.faforever.client.notification.Severity.WARN;
import static com.faforever.commons.io.Bytes.formatSize;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.defaultString;


@Lazy
@Service
@Slf4j
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
public class ClientUpdateServiceImpl implements ClientUpdateService {

  private static final String DEVELOPMENT_VERSION_STRING = "dev";

  private final TaskService taskService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final PlatformService platformService;
  private final ApplicationContext applicationContext;
  private final PreferencesService preferencesService;

  private final CompletableFuture<UpdateInfo> updateInfoFuture;
  private final CompletableFuture<UpdateInfo> updateInfoBetaFuture;

  @VisibleForTesting
  String currentVersion;

  public static class InstallerExecutionException extends UncheckedIOException {
    public InstallerExecutionException(String message, IOException cause) {
      super(message, cause);
    }
  }

  public ClientUpdateServiceImpl(
      TaskService taskService,
      NotificationService notificationService,
      I18n i18n,
      PlatformService platformService,
      ApplicationContext applicationContext,
      PreferencesService preferencesService) {
    this.taskService = taskService;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.platformService = platformService;
    this.applicationContext = applicationContext;
    this.preferencesService = preferencesService;

    currentVersion = defaultString(Version.getCurrentVersion(), DEVELOPMENT_VERSION_STRING);
    log.info("Current version: {}", currentVersion);

    CheckForUpdateTask task = applicationContext.getBean(CheckForUpdateTask.class);
    this.updateInfoFuture = taskService.submitTask(task).getFuture();

    CheckForBetaUpdateTask betaTask = applicationContext.getBean(CheckForBetaUpdateTask.class);
    this.updateInfoBetaFuture = taskService.submitTask(betaTask).getFuture();
  }

  /**
   * Returns information about the newest update. Returns {@code null} if no update is available.
   */
  @Override
  public CompletableFuture<UpdateInfo> getNewestUpdate() {
    return updateInfoFuture;
  }

  @Override
  public void checkForUpdateInBackground() {
    if (preferencesService.getPreferences().getPreReleaseCheckEnabled()) {
      checkForBetaUpdateInBackground();
    } else {
      checkForRegularUpdateInBackground();
    }
  }

  @EventListener
  public void onLoggedInEvent(LoggedInEvent loggedInEvent) {
    checkForUpdateInBackground();
  }

  /**
   * Creates an update notification with actions to download and install latest release
   */
  private void checkForRegularUpdateInBackground() {
    notificationOnUpdate(updateInfoFuture);
  }

  /**
   * Creates an update notification with actions to download and install latest beta release
   */
  private void checkForBetaUpdateInBackground() {
    notificationOnUpdate(updateInfoBetaFuture);
  }

  private void notificationOnUpdate(CompletableFuture<UpdateInfo> updateInfoSupplier) {
    updateInfoSupplier.thenAccept(updateInfo -> {
      if (updateInfo == null) {
        return;
      }

      if (!Version.shouldUpdate(getCurrentVersion(), updateInfo.getName())) {
        return;
      }

      notificationService.addNotification(new PersistentNotification(
          i18n.get(updateInfo.isPrerelease() ? "clientUpdateAvailable.prereleaseNotification" : "clientUpdateAvailable.notification", updateInfo.getName(), formatSize(updateInfo.getSize(), i18n.getUserSpecificLocale())),
          INFO, asList(
          new Action(i18n.get("clientUpdateAvailable.downloadAndInstall"), event -> downloadAndInstallInBackground(updateInfo)),
          new Action(i18n.get("clientUpdateAvailable.releaseNotes"), Action.Type.OK_STAY,
              event -> platformService.showDocument(updateInfo.getReleaseNotesUrl().toExternalForm())
          )))
      );
    }).exceptionally(throwable -> {
      log.warn("Client update check failed", throwable);
      return null;
    });
  }

  @Override
  public String getCurrentVersion() {
    return currentVersion;
  }

  @VisibleForTesting
  void install(Path binaryPath) {
    try {
      platformService.setUnixExecutableAndWritableBits(binaryPath);
    } catch (IOException e) {
      throw new InstallerExecutionException("Unix execute bit could not be set", e);
    }
    String command = binaryPath.toAbsolutePath().toString();
    try {
      log.info("Starting installer at {}", command);
      new ProcessBuilder(command).inheritIO().start();
    } catch (IOException e) {
      throw new InstallerExecutionException("Installation could not be started", e);
    }
  }

  public DownloadUpdateTask downloadAndInstallInBackground(UpdateInfo updateInfo) {
    DownloadUpdateTask task = applicationContext.getBean(DownloadUpdateTask.class);
    task.setUpdateInfo(updateInfo);

    taskService.submitTask(task).getFuture()
        .thenAccept(this::install)
        .exceptionally(throwable -> {
          if (throwable instanceof InstallerExecutionException) {
            log.warn(throwable.getMessage(), throwable.getCause());
          }
          log.warn("Error while downloading client update", throwable);
          notificationService.addNotification(
              new PersistentNotification(i18n.get("clientUpdateDownloadFailed.notification"), WARN, singletonList(
                  new Action(i18n.get("clientUpdateDownloadFailed.retry"), event -> downloadAndInstallInBackground(updateInfo))
              ))
          );
          return null;
        });

    return task;
  }
}
