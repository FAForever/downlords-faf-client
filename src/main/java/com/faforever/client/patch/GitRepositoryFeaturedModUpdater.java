package com.faforever.client.patch;

import com.faforever.client.FafClientApplication;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;


@Lazy
@Component
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
public class GitRepositoryFeaturedModUpdater implements FeaturedModUpdater {

  private static final String NON_WORD_CHARACTER_PATTERN = "[^\\w]";
  private static final KnownFeaturedMod[] BLACKLISTED_FEATURED_MODS = {KnownFeaturedMod.COOP};

  private final TaskService taskService;
  private final ApplicationContext applicationContext;
  private final PreferencesService preferencesService;

  @Inject
  public GitRepositoryFeaturedModUpdater(TaskService taskService, ApplicationContext applicationContext, PreferencesService preferencesService) {
    this.taskService = taskService;
    this.applicationContext = applicationContext;
    this.preferencesService = preferencesService;
  }

  @Override
  @SuppressWarnings("unchecked")
  public CompletableFuture<PatchResult> updateMod(FeaturedMod featuredMod, @Nullable Integer version) {
    String repoDirName = featuredMod.getGitUrl().replaceAll(NON_WORD_CHARACTER_PATTERN, "");
    Path repositoryDirectory = preferencesService.getPatchReposDirectory().resolve(repoDirName);

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
  public boolean canUpdate(FeaturedMod featuredMod) {
    return featuredMod.getGitUrl() != null && !Arrays.stream(BLACKLISTED_FEATURED_MODS).
        anyMatch(knownFeaturedMod -> knownFeaturedMod.getTechnicalName().equals(featuredMod.getTechnicalName()));
  }
}
