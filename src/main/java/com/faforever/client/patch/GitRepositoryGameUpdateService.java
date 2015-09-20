package com.faforever.client.patch;

import com.faforever.client.game.GameType;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.Callback;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class GitRepositoryGameUpdateService extends AbstractPatchService implements GameUpdateService {

  @VisibleForTesting
  enum InstallType {
    RETAIL("retail.json"),
    STEAM("steam.json");

    final String migrationDataFileName;

    InstallType(String migrationDataFileName) {
      this.migrationDataFileName = migrationDataFileName;
    }
  }

  @VisibleForTesting
  static final String REPO_NAME = "binary-patch";

  @VisibleForTesting
  static final String STEAM_API_DLL = "steam_api.dll";

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  TaskService taskService;

  @Autowired
  NotificationService notificationService;

  @Autowired
  I18n i18n;

  @Autowired
  GitWrapper gitWrapper;

  @Autowired
  ApplicationContext applicationContext;

  /**
   * Path to the local binary-patch Git repository.
   */
  private Path binaryPatchRepoDirectory;

  @Override
  protected boolean checkDirectories() {
    return super.checkDirectories();
  }

  @PostConstruct
  void postConstruct() {
    binaryPatchRepoDirectory = preferencesService.getFafReposDirectory().resolve(REPO_NAME);
  }

  @Override
  public CompletableFuture<Void> updateInBackground(String gameType, Integer version, Map<String, Integer> modVersions, Set<String> simModUids) {
    if (!checkDirectories()) {
      logger.warn("Aborted patching since directories aren't initialized properly");
      return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<Void> future = new CompletableFuture<>();
    Callback<Void> callback = new Callback<Void>() {
      @Override
      public void success(Void result) {
        notificationService.addNotification(
            new PersistentNotification(
                i18n.get("faUpdateSucceeded.notification"),
                Severity.INFO
            )
        );
        future.complete(null);
      }

      @Override
      public void error(Throwable e) {
        notificationService.addNotification(
            new PersistentNotification(
                i18n.get("updateFailed.notification"),
                Severity.WARN,
                Collections.singletonList(
                    new Action(i18n.get("updateCheckFailed.retry"), event -> checkForUpdateInBackground())
                )
            )
        );
        future.completeExceptionally(e);
      }
    };


    GitGameUpdateTask task = new GitGameUpdateTask();
    task.setBinaryPatchRepoDirectory(binaryPatchRepoDirectory);
    task.setMigrationDataFile(getMigrationDataFile());
    taskService.submitTask(task, callback);
    return future;
  }

  @Override
  public CompletableFuture<Void> checkForUpdateInBackground() {
    CompletableFuture<Void> future = new CompletableFuture<>();

    Callback<Boolean> callback = new Callback<Boolean>() {
      @Override
      public void success(Boolean needsPatching) {
        if (needsPatching) {
          notificationService.addNotification(
              new PersistentNotification(
                  i18n.get("faUpdateAvailable.notification"),
                  Severity.INFO,
                  Arrays.asList(
                      new Action(i18n.get("faUpdateAvailable.updateLater")),
                      new Action(i18n.get("faUpdateAvailable.updateNow"),
                          event -> updateInBackground(GameType.DEFAULT.getString(), null, null, null))
                  )
              )
          );
        }
        future.complete(null);
      }

      @Override
      public void error(Throwable e) {
        notificationService.addNotification(
            new PersistentNotification(
                i18n.get("updateCheckFailed.notification"),
                Severity.WARN,
                Collections.singletonList(
                    new Action(i18n.get("updateCheckFailed.retry"), event -> checkForUpdateInBackground())
                )
            )
        );
        future.completeExceptionally(e);
      }
    };

    GitCheckGameUpdateTask task = applicationContext.getBean(GitCheckGameUpdateTask.class);
    task.setBinaryPatchRepoDirectory(binaryPatchRepoDirectory);
    task.setMigrationDataFile(getMigrationDataFile());
    taskService.submitTask(task, callback);
    return future;
  }

  private Path getMigrationDataFile() {
    String migrationDataFileName = guessInstallType().migrationDataFileName;
    return binaryPatchRepoDirectory.resolve(migrationDataFileName);
  }

  @VisibleForTesting
  InstallType guessInstallType() {
    Path faBinDirectory = preferencesService.getPreferences().getForgedAlliance().getPath().resolve("bin");
    if (Files.notExists(faBinDirectory)) {
      throw new IllegalStateException("Directory does not exist: " + faBinDirectory);
    }

    if (Files.exists(faBinDirectory.resolve(STEAM_API_DLL))) {
      return InstallType.STEAM;
    } else {
      return InstallType.RETAIL;
    }
  }
}
