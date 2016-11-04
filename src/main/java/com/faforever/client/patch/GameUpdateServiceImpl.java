package com.faforever.client.patch;

import com.faforever.client.game.FeaturedModBean;
import com.faforever.client.task.TaskService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class GameUpdateServiceImpl extends AbstractUpdateService implements GameUpdateService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Resource
  TaskService taskService;
  @Resource
  ApplicationContext applicationContext;

  private LegacyGameUpdateTask updateTask;

  @Override
  public CompletionStage<Void> updateInBackground(@NotNull FeaturedModBean featuredMod, @Nullable Integer version, @NotNull Map<String, Integer> modVersions, @NotNull Set<String> simModUids) {
    if (!checkDirectories()) {
      logger.warn("Aborted patching since directories aren't initialized properly");
      return CompletableFuture.completedFuture(null);
    }

    if (updateTask != null && !updateTask.isDone()) {
      logger.warn("Update is already in progress, ignoring request");
      return CompletableFuture.completedFuture(null);
    }

    updateTask = applicationContext.getBean(LegacyGameUpdateTask.class);
    updateTask.setFeaturedMod(featuredMod.getTechnicalName());
    updateTask.setSimMods(simModUids);
    updateTask.setModVersions(modVersions);

    if (version != null) {
      updateTask.setGameVersion(String.valueOf(version));
    }

    return taskService.submitTask(updateTask).getFuture();
  }
}
