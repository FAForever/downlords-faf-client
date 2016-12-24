package com.faforever.client.patch;

import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.Mod;
import com.faforever.client.mod.ModService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.util.Assert;
import javafx.beans.InvalidationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GitFeaturedModUpdateTaskImpl extends CompletableTask<PatchResult> implements GitFeaturedModUpdateTask {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final PropertiesProgressMonitor progressMonitor;

  private final I18n i18n;
  private final GitWrapper gitWrapper;
  private final ModService modService;

  private String gameRepositoryUrl;
  private String ref;
  private Path repositoryDirectory;

  @Inject
  public GitFeaturedModUpdateTaskImpl(I18n i18n, GitWrapper gitWrapper, ModService modService) {
    super(Priority.MEDIUM);
    progressMonitor = new PropertiesProgressMonitor();
    this.i18n = i18n;
    this.gitWrapper = gitWrapper;
    this.modService = modService;
  }

  @Override
  protected PatchResult call() throws Exception {
    logger.info("Updating preferences files from {}@{}", gameRepositoryUrl, ref);
    updateTitle(i18n.get("updater.git.taskTitle"));

    checkout(repositoryDirectory, gameRepositoryUrl, ref);

    Path modInfoLuaFile = repositoryDirectory.resolve("mod_info.lua");
    if (Files.notExists(modInfoLuaFile)) {
      throw new IllegalStateException("Could not find " + modInfoLuaFile.toAbsolutePath());
    }


    try (InputStream inputStream = Files.newInputStream(modInfoLuaFile)) {
      Mod mod = modService.extractModInfo(inputStream, repositoryDirectory);
      return new PatchResult(modService.readModVersion(repositoryDirectory), mod.getMountPoints(), mod.getHookDirectories());
    }
  }

  private void checkout(Path gitRepoDir, String gitRepoUrl, String ref) throws IOException {
    Assert.checkNullIllegalState(gitRepoDir, "Parameter 'gitRepoDir' must not be null");
    Assert.checkNullIllegalState(gitRepoUrl, "Parameter 'gitRepoUrl' must not be null");
    Assert.checkNullIllegalState(ref, "Parameter 'ref' must not be null");

    InvalidationListener messageUpdatingListener = observable -> updateMessage(
        i18n.get("updater.git.progressFormat", progressMonitor.getTitle(), progressMonitor.getTasksDone(), progressMonitor.getTotalTasks())
    );
    progressMonitor.titleProperty().addListener(messageUpdatingListener);
    progressMonitor.tasksDoneProperty().addListener(messageUpdatingListener);
    progressMonitor.workUnitsDoneProperty().addListener((observable, oldValue, newValue)
        -> updateProgress(progressMonitor.getWorkUnitsDone(), progressMonitor.getTotalWork()));

    if (Files.notExists(gitRepoDir)) {
      Files.createDirectories(gitRepoDir.getParent());
      gitWrapper.clone(gitRepoUrl, gitRepoDir, progressMonitor);
    } else {
      gitWrapper.fetch(gitRepoDir, progressMonitor);
    }
    updateTitle(i18n.get("updater.git.checkingOut"));
    gitWrapper.checkoutRef(gitRepoDir, ref);
  }

  @Override
  @PreDestroy
  protected void cancelled() {
    super.cancelled();
    progressMonitor.setCancelled(true);
  }

  @Override
  public void setGameRepositoryUrl(String gameRepositoryUrl) {
    this.gameRepositoryUrl = gameRepositoryUrl;
  }

  @Override
  public void setRef(String ref) {
    this.ref = ref;
  }

  @Override
  public void setRepositoryDirectory(Path repositoryDirectory) {
    this.repositoryDirectory = repositoryDirectory;
  }
}
