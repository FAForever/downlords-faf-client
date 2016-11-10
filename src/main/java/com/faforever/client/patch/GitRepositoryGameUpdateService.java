package com.faforever.client.patch;

import com.faforever.client.game.FeaturedModBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.task.TaskService;
import com.google.common.eventbus.EventBus;
import com.google.common.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.faforever.client.notification.Severity.WARN;
import static java.util.Collections.singletonList;

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
  @Resource
  EventBus eventBus;

  @Override
  protected boolean checkDirectories() {
    return super.checkDirectories();
  }

  @Override
  public CompletionStage<Void> updateInBackground(FeaturedModBean featuredMod, Integer version, Map<String, Integer> modVersions, Set<String> simModUids) {
    if (!checkDirectories()) {
      logger.warn("Aborted patching since directories aren't initialized properly");
      return CompletableFuture.completedFuture(null);
    }

    String repoDirName = Hashing.md5().hashString(featuredMod.getGitUrl(), StandardCharsets.UTF_8).toString();

    GitGameUpdateTask task = applicationContext.getBean(GitGameUpdateTask.class);
    task.setSimMods(simModUids);
    task.setRepositoryDirectory(preferencesService.getFafReposDirectory().resolve(repoDirName));
    task.setGameRepositoryUrl(featuredMod.getGitUrl());

    if (version != null) {
      task.setRef("refs/tags/" + version);
    } else {
      task.setRef("refs/remotes/origin/" + featuredMod.getGitBranch());
    }

    return taskService.submitTask(task).getFuture()
        .whenComplete((aVoid, throwable) -> {
          if (throwable != null) {
            notificationService.addNotification(
                new PersistentNotification(i18n.get("updateFailed.notification"), WARN,
                    singletonList(new Action(i18n.get("updateFailed.retry"), event -> updateInBackground(featuredMod, version, modVersions, simModUids)))
                )
            );
          }
        });
  }
}
