package com.faforever.client.patch;

import com.faforever.client.mod.FeaturedModBean;
import com.faforever.client.task.TaskService;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.util.concurrent.CompletionStage;

public class SimpleHttpFeaturedModUpdater implements FeaturedModUpdater {

  @Resource
  TaskService taskService;
  @Resource
  ApplicationContext applicationContext;

  @Override
  public CompletionStage<PatchResult> updateMod(FeaturedModBean featuredMod, @Nullable Integer version) {
    SimpleHttpFeaturedModUpdaterTask task = applicationContext.getBean(SimpleHttpFeaturedModUpdaterTask.class);
    task.setVersion(version);
    task.setFeaturedMod(featuredMod);

    return taskService.submitTask(task).getFuture();
  }

  @Override
  public boolean canUpdate(FeaturedModBean featuredMod) {
    return true;
  }
}
