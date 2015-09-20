package com.faforever.client.patch;

import com.faforever.client.task.TaskService;
import com.faforever.client.util.Callback;
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
  public CompletableFuture<Void> updateInBackground(String gameType, Integer version, Map<String, Integer> modVersions, Set<String> simModUids) {
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

    CompletableFuture<Void> future = new CompletableFuture<>();

    taskService.submitTask(updateTask, new Callback<Void>() {
      @Override
      public void success(Void result) {
        future.complete(result);
      }

      @Override
      public void error(Throwable e) {
        future.completeExceptionally(e);
      }
    });

    return future;
  }

  @Override
  public CompletableFuture<Void> checkForUpdateInBackground() {
    logger.info("Ignoring update check since the current server implementation doesn't allow to do so easily");
    return null;
  }
}
