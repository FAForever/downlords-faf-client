package com.faforever.client.patch;

import com.faforever.client.mod.FeaturedModBean;
import com.faforever.client.task.TaskService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.concurrent.CompletionStage;


@Lazy
@Component
@Profile("!local")
public class SimpleHttpFeaturedModUpdater implements FeaturedModUpdater {

  private final TaskService taskService;
  private final ApplicationContext applicationContext;

  @Inject
  public SimpleHttpFeaturedModUpdater(TaskService taskService, ApplicationContext applicationContext) {
    this.taskService = taskService;
    this.applicationContext = applicationContext;
  }

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
