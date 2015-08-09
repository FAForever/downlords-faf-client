package com.faforever.client.patch;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.Callback;
import com.faforever.client.util.OperatingSystem;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jbsdiff.InvalidHeaderException;
import jbsdiff.Patch;
import org.apache.commons.compress.compressors.CompressorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.DigestUtils;

import javax.annotation.PostConstruct;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.faforever.client.task.PrioritizedTask.Priority.LOW;
import static com.faforever.client.task.TaskGroup.NET_HEAVY;
import static com.faforever.client.task.TaskGroup.NET_LIGHT;

public class GitRepositoryPatchService implements PatchService {

  @VisibleForTesting
  enum InstallType {
    RETAIL("retail.json"),
    STEAM("steam.json");

    final String migrationDataFileName;

    InstallType(String migrationDataFileName) {
      this.migrationDataFileName = migrationDataFileName;
    }
  }

  @VisibleForTesting
  static final String REPO_NAME = "binary-patch";
  @VisibleForTesting
  static final String STEAM_API_DLL = "steam_api.dll";

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String BINARY_PATCH_DIRECTORY = "bsdiff4";

  private final Gson gson;

  @Autowired
  Environment environment;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  TaskService taskService;

  @Autowired
  NotificationService notificationService;

  @Autowired
  I18n i18n;

  @Autowired
  GitWrapper gitWrapper;

  /**
   * Path containing the FAF "bin" directory (e. g. "%PROGRAMDATA\FAForever\bin")
   */
  private Path fafBinDirectory;
  /**
   * Path to the local binary-patch Git repository.
   */
  private Path binaryPatchRepoDirectory;
  /**
   * Path containing the binary patch files within the binary-patch Git repository.
   */
  private Path patchSourceDirectory;
  private Path faBinDirectory;
  private String patchRepositoryUri;

  public GitRepositoryPatchService() {
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();
  }

  @PostConstruct
  void postConstruct() {
    patchRepositoryUri = environment.getProperty("patch.git.url");
  }

  @Override
  public void patchInBackground() {
    if (!initAndCheckDirectories()) {
      logger.warn("Aborted patching since directories aren't initialized properly");
      return;
    }

    Callback<Void> callback = new Callback<Void>() {
      @Override
      public void success(Void result) {
        notificationService.addNotification(
            new PersistentNotification(
                i18n.get("faUpdateSucceeded.notification"),
                Severity.INFO
            )
        );
      }

      @Override
      public void error(Throwable e) {
        notificationService.addNotification(
            new PersistentNotification(
                i18n.get("updateFailed.notification"),
                Severity.WARN,
                Collections.singletonList(
                    new Action(i18n.get("updateCheckFailed.retry"), event -> checkForUpdatesInBackground())
                )
            )
        );
      }
    };


    taskService.submitTask(NET_HEAVY, new PrioritizedTask<Void>(i18n.get("patchTask.title"), LOW) {
      @Override
      protected Void call() throws Exception {

        if (Files.notExists(binaryPatchRepoDirectory)) {
          clonePatchRepository();
        }

        Path migrationDataFile = getMigrationDataFile();

        try (BufferedReader reader = Files.newBufferedReader(migrationDataFile, StandardCharsets.UTF_8)) {
          MigrationData migrationData = gson.fromJson(reader, MigrationData.class);

          copyGameFilesToFafBinDirectory(migrationData);

          Set<Map.Entry<String, String>> entries = migrationData.postPatchVerify.entrySet();

          long progress = 0;
          updateProgress(progress, entries.size());

          for (Map.Entry<String, String> entry : entries) {
            String fileName = entry.getKey();
            String expectedMd5AfterPatch = entry.getValue();

            Path fileToPatch = fafBinDirectory.resolve(fileName);
            byte[] bytesOfFileToPatch = Files.readAllBytes(fileToPatch);
            Path patchFile = getPatchFile(bytesOfFileToPatch);

            if (Files.notExists(patchFile)) {
              updateProgress(++progress, entries.size());
              continue;
            }

            patchFile(fileToPatch, bytesOfFileToPatch, patchFile);
            verifyPatchedFile(expectedMd5AfterPatch, fileToPatch);

            logger.info("Patching successful for file: {}", fileToPatch);

            updateProgress(++progress, entries.size());
          }
        }

        logger.info("All files have been patched successfully");
        return null;
      }
    }, callback);
  }

  /**
   * Since it's possible that the user has changed or never specified the game path, this method needs to be called
   * every time before any work is done.
   *
   * @return {@code true} if directories are set up correctly
   */
  private boolean initAndCheckDirectories() {
    fafBinDirectory = preferencesService.getFafBinDirectory();
    binaryPatchRepoDirectory = preferencesService.getFafReposDirectory().resolve(REPO_NAME);
    patchSourceDirectory = binaryPatchRepoDirectory.resolve(BINARY_PATCH_DIRECTORY);
    Path faDirectory = preferencesService.getPreferences().getForgedAlliance().getPath();
    if (faDirectory == null) {
      return false;
    }
    faBinDirectory = faDirectory.resolve("bin");
    return true;
  }

  private void clonePatchRepository() {
    gitWrapper.clone(patchRepositoryUri, binaryPatchRepoDirectory);
  }

  private Path getMigrationDataFile() {
    String migrationDataFileName = guessInstallType().migrationDataFileName;
    return binaryPatchRepoDirectory.resolve(migrationDataFileName);
  }

  private void copyGameFilesToFafBinDirectory(MigrationData migrationData) throws IOException {
    Files.createDirectories(fafBinDirectory);

    for (Map.Entry<String, String> entry : migrationData.prePatchCopyRename.entrySet()) {
      String oldName = entry.getKey();
      String newName = entry.getValue() != null ? entry.getValue() : oldName;

      Path source = faBinDirectory.resolve(oldName);
      Path destination = fafBinDirectory.resolve(newName);

      logger.debug("Copying file '{}' to '{}'", source, destination);

      Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

      if (OperatingSystem.current() == OperatingSystem.WINDOWS) {
        Files.setAttribute(destination, "dos:readonly", false);
      }
    }
  }

  private Path getPatchFile(byte[] bytesOfFileToPatch) {
    return patchSourceDirectory.resolve(DigestUtils.md5DigestAsHex(bytesOfFileToPatch));
  }

  private void patchFile(Path fileToPatch, byte[] bytesOfFileToPatch, Path patchFile) throws IOException, CompressorException, InvalidHeaderException {
    logger.info("Patching file {}", fileToPatch);

    try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(fileToPatch))) {
      Patch.patch(
          bytesOfFileToPatch,
          Files.readAllBytes(patchFile),
          outputStream
      );
    }
  }

  private void verifyPatchedFile(String expectedMd5AfterPatch, Path fileToPatch) throws IOException {
    String md5OfPatchedFile = DigestUtils.md5DigestAsHex(Files.readAllBytes(fileToPatch));

    if (!md5OfPatchedFile.equals(expectedMd5AfterPatch)) {
      throw new PatchingFailedException(
          String.format("Patching failed for file: '%s'. Expected checksum: %s but got: %s",
              fileToPatch, expectedMd5AfterPatch, md5OfPatchedFile)
      );
    }
  }

  @VisibleForTesting
  InstallType guessInstallType() {
    if (Files.exists(faBinDirectory.resolve(STEAM_API_DLL))) {
      return InstallType.STEAM;
    } else {
      return InstallType.RETAIL;
    }
  }

  @Override
  public void checkForUpdatesInBackground() {
    Callback<Boolean> callback = new Callback<Boolean>() {
      @Override
      public void success(Boolean needsPatching) {
        if (!needsPatching) {
          return;
        }

        notificationService.addNotification(
            new PersistentNotification(
                i18n.get("faUpdateAvailable.notification"),
                Severity.INFO,
                Arrays.asList(
                    new Action(i18n.get("faUpdateAvailable.updateLater")),
                    new Action(i18n.get("faUpdateAvailable.updateNow"), event -> patchInBackground())
                )
            )
        );
      }

      @Override
      public void error(Throwable e) {
        notificationService.addNotification(
            new PersistentNotification(
                i18n.get("updateCheckFailed.notification"),
                Severity.WARN,
                Collections.singletonList(
                    new Action(i18n.get("updateCheckFailed.retry"), event -> checkForUpdatesInBackground())
                )
            )
        );
      }
    };

    taskService.submitTask(NET_LIGHT, new PrioritizedTask<Boolean>(i18n.get("updateCheckTask.title"), LOW) {
      @Override
      protected Boolean call() throws Exception {
        logger.info("Checking for FA update");

        return initAndCheckDirectories() &&
            (Files.notExists(binaryPatchRepoDirectory)
                || areNewPatchFilesAvailable()
                || !areLocalFilesPatched());
      }
    }, callback);
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
    Path migrationDataFile = getMigrationDataFile();

    try (BufferedReader reader = Files.newBufferedReader(migrationDataFile, StandardCharsets.UTF_8)) {
      MigrationData migrationData = gson.fromJson(reader, MigrationData.class);

      for (Map.Entry<String, String> entry : migrationData.postPatchVerify.entrySet()) {
        String fileName = entry.getKey();
        String expectedMd5 = entry.getValue();

        Path fileToCheck = fafBinDirectory.resolve(fileName);

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
}
