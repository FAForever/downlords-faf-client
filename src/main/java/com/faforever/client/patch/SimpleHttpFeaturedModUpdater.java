package com.faforever.client.patch;

import com.faforever.client.task.TaskService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;


@Lazy
@Component
@RequiredArgsConstructor
public class SimpleHttpFeaturedModUpdater implements FeaturedModUpdater {

  private final TaskService taskService;
  private final ObjectFactory<SimpleHttpFeaturedModUpdaterTask> simpleHttpFeaturedModUpdaterTaskFactory;

  @Override
  public CompletableFuture<PatchResult> updateMod(String featuredModName, @Nullable Integer version,
                                                  boolean useReplayFolder) {
    SimpleHttpFeaturedModUpdaterTask task = simpleHttpFeaturedModUpdaterTaskFactory.getObject();
    task.setVersion(version);
    task.setFeaturedModName(featuredModName);
    task.setUseReplayFolder(useReplayFolder);

    return taskService.submitTask(task).getFuture();
  }
}
