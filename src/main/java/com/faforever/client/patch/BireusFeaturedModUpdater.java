package com.faforever.client.patch;

import com.faforever.client.FafClientApplication;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.Validator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.brutus5000.bireus.data.Repository;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Lazy
@Component
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
@Slf4j
public class BireusFeaturedModUpdater implements FeaturedModUpdater {

  private static final int PROTOCOL_VERSION = 1;
  private final TaskService taskService;
  private final ApplicationContext applicationContext;
  private final ObjectMapper objectMapper;

  public BireusFeaturedModUpdater(TaskService taskService, ApplicationContext applicationContext, ObjectMapper objectMapper) {
    this.taskService = taskService;
    this.applicationContext = applicationContext;
    this.objectMapper = objectMapper;
  }

  @Override
  public CompletableFuture<PatchResult> updateMod(FeaturedMod featuredMod, @Nullable Integer version) {
    BireusFeaturedModUpdateTask task = applicationContext.getBean(BireusFeaturedModUpdateTask.class);
    task.setVersion(version);
    task.setFeaturedMod(featuredMod);

    return taskService.submitTask(task).getFuture();
  }

  @Override
  public boolean canUpdate(FeaturedMod featuredMod) {
    URL repoUrl = featuredMod.getBireusUrl();
    if (repoUrl == null) {
      return false;
    }

    try {
      URL repoInfoUrl = new URL(repoUrl + "/" + Repository.BIREUS_INFO_FILE);
      @SuppressWarnings("unchecked")
      Map<String, Object> map = objectMapper.readValue(repoInfoUrl, Map.class);
      return Validator.isInt(String.valueOf(map.get("protocol")))
          && ((int) map.get("protocol")) == PROTOCOL_VERSION;
    } catch (IOException e) {
      log.warn("Error while testing {}", repoUrl, e);
      return false;
    }
  }
}
