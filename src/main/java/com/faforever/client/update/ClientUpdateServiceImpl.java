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
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.notification.Severity.INFO;
import static com.faforever.client.notification.Severity.WARN;
import static com.faforever.commons.io.Bytes.formatSize;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;


@Lazy
@Service
@Slf4j
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
public class ClientUpdateServiceImpl implements ClientUpdateService, InitializingBean {

  private final TaskService taskService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final PlatformService platformService;
  private final ApplicationContext applicationContext;
  private final PreferencesService preferencesService;
  private final EventBus eventBus;

  @VisibleForTesting
  ComparableVersion currentVersion;

  public ClientUpdateServiceImpl(
      TaskService taskService,
      NotificationService notificationService,
      I18n i18n,
      PlatformService platformService,
      ApplicationContext applicationContext,
      PreferencesService preferencesService,
      EventBus eventBus) {
    this.taskService = taskService;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.platformService = platformService;
    this.applicationContext = applicationContext;
    this.preferencesService = preferencesService;
    this.eventBus = eventBus;

    currentVersion = Version.getCurrentVersion();
    log.info("Current version: {}", currentVersion);
  }

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  @Override
  public CompletableFuture<Optional<UpdateInfo>> checkForUpdateInBackground() {
    CompletableFuture<Optional<UpdateInfo>> task;
    if (preferencesService.getPreferences().isPreReleaseCheckEnabled()) {
      task = taskService.submitTask(applicationContext.getBean(CheckForBetaUpdateTask.class)).getFuture();
    } else {
      task = taskService.submitTask(applicationContext.getBean(CheckForReleaseUpdateTask.class)).getFuture();
    }

    return task.exceptionally(throwable -> {
      log.warn("Client update check failed", throwable);
      return Optional.empty();
    });
  }

  @Subscribe
  public void onLoggedInEvent(LoggedInEvent loggedInEvent) {
    checkForUpdateInBackground().thenAccept(updateInfoOptional -> {
      if (updateInfoOptional.isEmpty()) {
        return;
      }
      UpdateInfo updateInfo = updateInfoOptional.get();
      if (preferencesService.getPreferences().isAutoUpdate()) {
        updateIfNecessary(updateInfo);
      } else {
        notificationService.addNotification(new PersistentNotification(
            i18n.get(updateInfo.isPrerelease() ? "clientUpdateAvailable.prereleaseNotification" : "clientUpdateAvailable.notification", updateInfo.getName(), formatSize(updateInfo.getSize(), i18n.getUserSpecificLocale())),
            INFO, asList(
            new Action(i18n.get("clientUpdateAvailable.downloadAndInstall"), event -> updateInBackground(updateInfo)),
            new Action(i18n.get("clientUpdateAvailable.releaseNotes"), Action.Type.OK_STAY,
                event -> platformService.showDocument(updateInfo.getReleaseNotesUrl().toExternalForm())
            ))));
      }
    });
  }

  private void updateIfNecessary(UpdateInfo updateInfo) {
    if (updateInfo == null) {
      return;
    }

    if (!Version.shouldUpdate(updateInfo.getVersion())) {
      return;
    }

    if (!preferencesService.getPreferences().isAutoUpdate()) {
      return;
    }
    updateInBackground(updateInfo);
  }

  @Override
  public ComparableVersion getCurrentVersion() {
    return currentVersion;
  }

  public ClientUpdateTask updateInBackground(UpdateInfo updateInfo) {
    ClientUpdateTask task = applicationContext.getBean(ClientUpdateTask.class);
    task.setUpdateInfo(updateInfo);

    taskService.submitTask(task).getFuture()
        .exceptionally(throwable -> {
          log.warn("Update failed", throwable);
          notificationService.addNotification(
              new PersistentNotification(i18n.get("clientUpdateDownloadFailed.notification"), WARN, singletonList(
                  new Action(i18n.get("clientUpdateDownloadFailed.retry"), event -> updateInBackground(updateInfo))
              ))
          );
          return null;
        });

    return task;
  }
}
