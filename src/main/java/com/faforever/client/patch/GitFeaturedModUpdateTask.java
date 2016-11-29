package com.faforever.client.patch;

import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.ModService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;

public class GitFeaturedModUpdateTask extends CompletableTask<PatchResult> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Resource
  I18n i18n;
  @Resource
  PreferencesService preferencesService;
  @Resource
  GitWrapper gitWrapper;
  @Resource
  Environment environment;
  @Resource
  ModService modService;

  private String gameRepositoryUri;
  private String ref;
  private Path repositoryDirectory;

  public GitFeaturedModUpdateTask() {
    super(Priority.MEDIUM);
  }

  @PostConstruct
  void postConstruct() {
    updateTitle(i18n.get("patchTask.title"));
  }

  @Override
  protected PatchResult call() throws Exception {
    logger.info("Updating game files from {}@{}", gameRepositoryUri, ref);

    checkout(repositoryDirectory, gameRepositoryUri, ref);

    Path modInfoLuaFile = repositoryDirectory.resolve("mod_info.lua");
    if (Files.notExists(modInfoLuaFile)) {
      throw new IllegalStateException("Could not find " + modInfoLuaFile.toAbsolutePath());
    }

    try (InputStream inputStream = Files.newInputStream(modInfoLuaFile)) {
      return new PatchResult(modService.readModVersion(repositoryDirectory), modService.readMountPoints(inputStream, repositoryDirectory));
    }
  }

  private void checkout(Path gitRepoDir, String gitRepoUrl, String ref) throws IOException {
    Assert.checkNullIllegalState(gitRepoDir, "Parameter 'gitRepoDir' must not be null");
    Assert.checkNullIllegalState(gitRepoUrl, "Parameter 'gitRepoUrl' must not be null");
    Assert.checkNullIllegalState(ref, "Parameter 'ref' must not be null");


    if (Files.notExists(gitRepoDir)) {
      Files.createDirectories(gitRepoDir.getParent());
      gitWrapper.clone(gitRepoUrl, gitRepoDir);
    } else {
      gitWrapper.fetch(gitRepoDir);
    }
    gitWrapper.checkoutRef(gitRepoDir, ref);
  }

  public void setGameRepositoryUrl(String gameRepositoryUri) {
    this.gameRepositoryUri = gameRepositoryUri;
  }

  public void setRef(String ref) {
    this.ref = ref;
  }

  public void setRepositoryDirectory(Path repositoryDirectory) {
    this.repositoryDirectory = repositoryDirectory;
  }
}
