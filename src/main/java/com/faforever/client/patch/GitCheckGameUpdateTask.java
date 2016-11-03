package com.faforever.client.patch;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;

public class GitCheckGameUpdateTask extends CompletableTask<Boolean> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Resource
  I18n i18n;

  @Resource
  PreferencesService preferencesService;

  @Resource
  GitWrapper gitWrapper;

  @Resource
  Environment environment;

  private Path gameRepositoryDirectory;
  private Gson gson;
  private Path migrationDataFile;

  public GitCheckGameUpdateTask() {
    super(Priority.LOW);
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();
  }

  @Override
  protected Boolean call() throws Exception {
    logger.info("Checking for FA update");

    updateTitle(i18n.get("updateCheckTask.title"));

    return (Files.notExists(gameRepositoryDirectory)
        || areNewPatchFilesAvailable());
  }

  /**
   * Checks whether the remote GIT repository has newer patch files available then the local repsitory.
   */
  private boolean areNewPatchFilesAvailable() throws IOException {
    String remoteHead = gitWrapper.getRemoteHead(gameRepositoryDirectory);
    String localHead = gitWrapper.getLocalHead(gameRepositoryDirectory);

    boolean needsPatching = !localHead.equals(remoteHead);

    if (needsPatching) {
      logger.info("New patch files are available ({})", remoteHead);
    } else {
      logger.info("Local patch repository is up to date ({})", remoteHead);
    }

    return needsPatching;
  }

  public void setGameRepositoryDirectory(Path gameRepositoryDirectory) {
    this.gameRepositoryDirectory = gameRepositoryDirectory;
  }

  public void setMigrationDataFile(Path migrationDataFile) {
    this.migrationDataFile = migrationDataFile;
  }
}
