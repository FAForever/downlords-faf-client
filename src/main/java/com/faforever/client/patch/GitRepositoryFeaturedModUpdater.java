package com.faforever.client.patch;

import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.FeaturedModBean;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.google.common.eventbus.EventBus;
import com.google.common.hash.Hashing;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CompletionStage;

public class GitRepositoryFeaturedModUpdater implements FeaturedModUpdater {

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
  @Resource
  PreferencesService preferencesService;

  @Override
  public CompletionStage<PatchResult> updateMod(FeaturedModBean featuredMod, @Nullable Integer version) {
    String repoDirName = Hashing.md5().hashString(featuredMod.getGitUrl(), StandardCharsets.UTF_8).toString();
    Path repositoryDirectory = preferencesService.getGitReposDirectory().resolve(repoDirName);

    GitFeaturedModUpdateTask modUpdateTask = applicationContext.getBean(GitFeaturedModUpdateTask.class);
    modUpdateTask.setRepositoryDirectory(repositoryDirectory);
    modUpdateTask.setGameRepositoryUrl(featuredMod.getGitUrl());

    if (version != null) {
      modUpdateTask.setRef("refs/tags/" + version);
    } else {
      modUpdateTask.setRef("refs/remotes/origin/" + featuredMod.getGitBranch());
    }

    return taskService.submitTask(modUpdateTask).getFuture();
  }

  @Override
  public boolean canUpdate(FeaturedModBean featuredMod) {
    return featuredMod.getGitUrl() != null;
  }
}
