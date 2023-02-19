package com.faforever.client.update;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.event.LoggedInEvent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.notification.Severity.INFO;
import static com.faforever.client.notification.Severity.WARN;
import static com.faforever.commons.io.Bytes.formatSize;
import static java.util.Collections.singletonList;


@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class ClientUpdateService implements InitializingBean {

  private final OperatingSystem operatingSystem;
  private final TaskService taskService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final PlatformService platformService;
  private final ApplicationContext applicationContext;
  private final EventBus eventBus;
  private final Preferences preferences;

  private CompletableFuture<UpdateInfo> updateInfoFuture;
  private CompletableFuture<UpdateInfo> updateInfoBetaFuture;

  public static class InstallerExecutionException extends UncheckedIOException {
    public InstallerExecutionException(String message, IOException cause) {
      super(message, cause);
    }
  }

  @Override
  public void afterPropertiesSet() {
    log.info("Current version: {}", Version.getCurrentVersion());

    eventBus.register(this);

    CheckForUpdateTask task = applicationContext.getBean(CheckForUpdateTask.class);
    this.updateInfoFuture = taskService.submitTask(task).getFuture();

    CheckForBetaUpdateTask betaTask = applicationContext.getBean(CheckForBetaUpdateTask.class);
    this.updateInfoBetaFuture = taskService.submitTask(betaTask).getFuture();
  }

  /**
   * Returns information about the newest update. Returns {@code null} if no update is available.
   */
  public CompletableFuture<UpdateInfo> getNewestUpdate() {
    return updateInfoFuture;
  }

  public void checkForUpdateInBackground() {
    if (preferences.isPreReleaseCheckEnabled()) {
      checkForBetaUpdateInBackground();
    } else {
      checkForRegularUpdateInBackground();
    }
  }

  @Subscribe
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

      if (!Version.shouldUpdate(Version.getCurrentVersion(), updateInfo.getName())) {
        return;
      }

      List<Action> actions = new ArrayList<>();

      if (operatingSystem.supportsUpdateInstall()) {
        actions.add(new Action(i18n.get("clientUpdateAvailable.downloadAndInstall"), event -> downloadAndInstallInBackground(updateInfo)));
      }

      actions.add(new Action(i18n.get("clientUpdateAvailable.releaseNotes"), Action.Type.OK_STAY,
          event -> platformService.showDocument(updateInfo.getReleaseNotesUrl().toExternalForm())
      ));

      notificationService.addNotification(new PersistentNotification(
          i18n.get(updateInfo.isPrerelease() ? "clientUpdateAvailable.prereleaseNotification" : "clientUpdateAvailable.notification", updateInfo.getName(), formatSize(updateInfo.getSize(), i18n.getUserSpecificLocale())),
          INFO, actions)
      );
    }).exceptionally(throwable -> {
      log.error("Client update check failed", throwable);
      return null;
    });
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
      log.info("Starting installer as `{}`", command);
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
          log.error("Error while downloading client update", throwable);
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
