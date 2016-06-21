package com.faforever.client.patch;

import com.faforever.client.i18n.I18n;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.google.common.hash.Hashing;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jbsdiff.InvalidHeaderException;
import jbsdiff.Patch;
import org.apache.commons.compress.compressors.CompressorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;

public class GitGameUpdateTask extends AbstractPrioritizedTask<Void> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String BINARY_PATCH_DIRECTORY = "bsdiff4";
  private final Gson gson;
  @Resource
  I18n i18n;
  @Resource
  PreferencesService preferencesService;
  @Resource
  GitWrapper gitWrapper;
  @Resource
  Environment environment;
  private Path binaryPatchRepoDirectory;
  private String patchRepositoryUri;
  private Path migrationDataFile;

  public GitGameUpdateTask() {
    super(Priority.MEDIUM);
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();
  }

  @PostConstruct
  void postConstruct() {
    updateTitle(i18n.get("patchTask.title"));
    patchRepositoryUri = environment.getProperty("patch.git.url");
  }


  @Override
  protected Void call() throws Exception {
    if (Files.notExists(binaryPatchRepoDirectory)) {
      clonePatchRepository();
    }

    try (BufferedReader reader = Files.newBufferedReader(migrationDataFile, StandardCharsets.UTF_8)) {
      MigrationData migrationData = gson.fromJson(reader, MigrationData.class);

      copyGameFilesToFafBinDirectory(migrationData);

      Set<Map.Entry<String, String>> entries = migrationData.postPatchVerify.entrySet();

      long progress = 0;
      updateProgress(progress, entries.size());

      for (Map.Entry<String, String> entry : entries) {
        String fileName = entry.getKey();
        String expectedMd5AfterPatch = entry.getValue();

        Path fileToPatch = preferencesService.getFafBinDirectory().resolve(fileName);
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

  private void clonePatchRepository() {
    gitWrapper.clone(patchRepositoryUri, binaryPatchRepoDirectory);
  }

  protected void copyGameFilesToFafBinDirectory(MigrationData migrationData) throws IOException {
    Path fafBinDirectory = preferencesService.getFafBinDirectory();
    Files.createDirectories(fafBinDirectory);

    for (Map.Entry<String, String> entry : migrationData.prePatchCopyRename.entrySet()) {
      String oldName = entry.getKey();
      String newName = entry.getValue() != null ? entry.getValue() : oldName;

      Path source = preferencesService.getPreferences().getForgedAlliance().getPath().resolve("bin").resolve(oldName);
      Path destination = fafBinDirectory.resolve(newName);

      logger.debug("Copying file '{}' to '{}'", source, destination);

      Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

      if (OperatingSystem.current() == OperatingSystem.WINDOWS) {
        Files.setAttribute(destination, "dos:readonly", false);
      }
    }
  }

  private Path getPatchFile(byte[] bytesOfFileToPatch) {
    Path patchSourceDirectory = binaryPatchRepoDirectory.resolve(BINARY_PATCH_DIRECTORY);
    return patchSourceDirectory.resolve(Hashing.md5().hashBytes(bytesOfFileToPatch).toString());
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
    String md5OfPatchedFile = Hashing.md5().hashBytes(Files.readAllBytes(fileToPatch)).toString();

    if (!md5OfPatchedFile.equals(expectedMd5AfterPatch)) {
      throw new PatchingFailedException(
          String.format("Patching failed for file: '%s'. Expected checksum: %s but got: %s",
              fileToPatch, expectedMd5AfterPatch, md5OfPatchedFile)
      );
    }
  }

  public void setBinaryPatchRepoDirectory(Path binaryPatchRepoDirectory) {
    this.binaryPatchRepoDirectory = binaryPatchRepoDirectory;
  }

  public void setMigrationDataFile(Path migrationDataFile) {
    this.migrationDataFile = migrationDataFile;
  }
}
