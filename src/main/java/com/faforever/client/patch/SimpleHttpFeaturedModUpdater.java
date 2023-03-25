package com.faforever.client.patch;

import com.faforever.client.domain.FeaturedModBean;
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
  public CompletableFuture<PatchResult> updateMod(FeaturedModBean featuredMod, @Nullable Integer version) {
    SimpleHttpFeaturedModUpdaterTask task = simpleHttpFeaturedModUpdaterTaskFactory.getObject();
    task.setVersion(version);
    task.setFeaturedMod(featuredMod);

    return taskService.submitTask(task).getFuture();
  }

  @Override
  public boolean canUpdate(FeaturedModBean featuredMod) {
    return true;
  }
}
