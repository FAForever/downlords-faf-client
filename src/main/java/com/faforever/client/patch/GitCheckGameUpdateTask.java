package com.faforever.client.patch;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.DigestUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class GitCheckGameUpdateTask extends AbstractPrioritizedTask<Boolean> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  I18n i18n;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  GitWrapper gitWrapper;

  @Autowired
  Environment environment;

  private Path binaryPatchRepoDirectory;
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

    return (Files.notExists(binaryPatchRepoDirectory)
        || areNewPatchFilesAvailable()
        || !areLocalFilesPatched());
  }

  /**
   * Checks whether the remote GIT repository has newer patch files available then the local repsitory.
   */
  private boolean areNewPatchFilesAvailable() throws IOException {
    String remoteHead = gitWrapper.getRemoteHead(binaryPatchRepoDirectory);
    String localHead = gitWrapper.getLocalHead(binaryPatchRepoDirectory);

    boolean needsPatching = !localHead.equals(remoteHead);

    if (needsPatching) {
      logger.info("New patch files are available ({})", remoteHead);
    } else {
      logger.info("Local patch repository is up to date ({})", remoteHead);
    }

    return needsPatching;
  }

  private boolean areLocalFilesPatched() throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(migrationDataFile, StandardCharsets.UTF_8)) {
      MigrationData migrationData = gson.fromJson(reader, MigrationData.class);

      for (Map.Entry<String, String> entry : migrationData.postPatchVerify.entrySet()) {
        String fileName = entry.getKey();
        String expectedMd5 = entry.getValue();

        Path fileToCheck = preferencesService.getFafBinDirectory().resolve(fileName);

        if (Files.notExists(fileToCheck)) {
          logger.info("Missing file: {}", fileToCheck);
          return false;
        }

        byte[] bytesOfFileToCheck = Files.readAllBytes(fileToCheck);

        String actualMd5 = DigestUtils.md5DigestAsHex(bytesOfFileToCheck);
        if (!actualMd5.equals(expectedMd5)) {
          logger.info("At least one binary file is out of date: {}. Expected checksum: {} but got: {}", fileName, expectedMd5, actualMd5);
          return false;
        }
      }
    }

    logger.info("All binary files are up to date");

    return true;
  }

  public void setBinaryPatchRepoDirectory(Path binaryPatchRepoDirectory) {
    this.binaryPatchRepoDirectory = binaryPatchRepoDirectory;
  }

  public void setMigrationDataFile(Path migrationDataFile) {
    this.migrationDataFile = migrationDataFile;
  }
}
