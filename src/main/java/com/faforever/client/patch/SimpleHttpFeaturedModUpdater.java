package com.faforever.client.patch;

import com.faforever.client.FafClientApplication;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.task.TaskService;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;


@Lazy
@Component
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
public class SimpleHttpFeaturedModUpdater implements FeaturedModUpdater {

  private final TaskService taskService;
  private final ApplicationContext applicationContext;


  public SimpleHttpFeaturedModUpdater(TaskService taskService, ApplicationContext applicationContext) {
    this.taskService = taskService;
    this.applicationContext = applicationContext;
  }

  @Override
  public CompletableFuture<PatchResult> updateMod(FeaturedMod featuredMod, @Nullable Integer version) {
    SimpleHttpFeaturedModUpdaterTask task = applicationContext.getBean(SimpleHttpFeaturedModUpdaterTask.class);
    task.setVersion(version);
    task.setFeaturedMod(featuredMod);

    return taskService.submitTask(task).getFuture();
  }

  @Override
  public boolean canUpdate(FeaturedMod featuredMod) {
    return true;
  }
}
