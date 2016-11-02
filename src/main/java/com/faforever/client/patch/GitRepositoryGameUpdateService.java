package com.faforever.client.patch;

import com.faforever.client.game.FeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.task.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class GitRepositoryGameUpdateService extends AbstractUpdateService implements GameUpdateService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  @Resource
  TaskService taskService;
  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;
  @Resource
  GitWrapper gitWrapper;
  @Resource
  ApplicationContext applicationContext;
  /**
   * Path to the local binary-patch Git repository.
   */
  private Path gameRepositoryDirectory;

  @Override
  protected boolean checkDirectories() {
    return super.checkDirectories();
  }

  @PostConstruct
  void postConstruct() {
    gameRepositoryDirectory = preferencesService.getFafReposDirectory().resolve("faf");
  }

  @Override
  public CompletionStage<Void> updateInBackground(String gameType, Integer version, Map<String, Integer> modVersions, Set<String> simModUids) {
    if (!checkDirectories()) {
      logger.warn("Aborted patching since directories aren't initialized properly");
      return CompletableFuture.completedFuture(null);
    }

    GitGameUpdateTask task = applicationContext.getBean(GitGameUpdateTask.class);
    task.setVersion(String.valueOf(version));
    task.setSimMods(simModUids);
    // FIXME get from API
    task.setGameRepositoryUri("https://github.com/FAForever/fa.git");

    return taskService.submitTask(task).getFuture().thenAccept(aVoid -> notificationService.addNotification(
        new PersistentNotification(
            i18n.get("faUpdateSucceeded.notification"),
            Severity.INFO
        )
    )).exceptionally(throwable -> {
      notificationService.addNotification(
          new PersistentNotification(
              i18n.get("updateFailed.notification"),
              Severity.WARN,
              Collections.singletonList(
                  new Action(i18n.get("updateCheckFailed.retry"), event -> checkForUpdateInBackground())
              )
          )
      );
      return null;
    });
  }

  @Override
  public CompletionStage<Void> checkForUpdateInBackground() {
    GitCheckGameUpdateTask task = applicationContext.getBean(GitCheckGameUpdateTask.class);
    task.setGameRepositoryDirectory(gameRepositoryDirectory);

    return taskService.submitTask(task).getFuture().thenAccept(needsPatching -> {
      if (needsPatching) {
        notificationService.addNotification(
            new PersistentNotification(
                i18n.get("faUpdateAvailable.notification"),
                Severity.INFO,
                Arrays.asList(
                    new Action(i18n.get("faUpdateAvailable.updateLater")),
                    new Action(i18n.get("faUpdateAvailable.updateNow"),
                        event -> updateInBackground(FeaturedMod.DEFAULT.getString(), null, null, null))
                )
            )
        );
      }
    }).exceptionally(throwable -> {
      notificationService.addNotification(
          new PersistentNotification(
              i18n.get("updateCheckFailed.notification"),
              Severity.WARN,
              Collections.singletonList(
                  new Action(i18n.get("updateCheckFailed.retry"), event -> checkForUpdateInBackground())
              )
          )
      );
      return null;
    });
  }
}
