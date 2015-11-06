package com.faforever.client.patch;

import com.faforever.client.task.TaskService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class GameUpdateServiceImpl extends AbstractPatchService implements GameUpdateService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  TaskService taskService;

  @Autowired
  ApplicationContext applicationContext;

  private UpdateGameFilesTask updateTask;

  @Override
  public CompletableFuture<Void> updateInBackground(@NotNull String gameType, @Nullable Integer version, @NotNull Map<String, Integer> modVersions, @NotNull Set<String> simModUids) {
    if (!checkDirectories()) {
      logger.warn("Aborted patching since directories aren't initialized properly");
      return CompletableFuture.completedFuture(null);
    }

    if (updateTask != null && !updateTask.isDone()) {
      logger.warn("Update is already in progress, ignoring request");
      return CompletableFuture.completedFuture(null);
    }

    updateTask = applicationContext.getBean(UpdateGameFilesTask.class);
    updateTask.setGameType(gameType);
    updateTask.setSimMods(simModUids);
    updateTask.setModVersions(modVersions);

    return taskService.submitTask(updateTask);
  }

  @Override
  public CompletableFuture<Void> checkForUpdateInBackground() {
    logger.info("Ignoring update check since the current server implementation doesn't allow to do so easily");
    return null;
  }
}
