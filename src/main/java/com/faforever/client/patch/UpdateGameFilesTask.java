package com.faforever.client.patch;

import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.util.ByteCopier;
import com.faforever.client.util.OperatingSystem;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import net.dongliu.vcdiff.VcdiffDecoder;
import net.dongliu.vcdiff.exception.VcdiffDecodeException;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.faforever.client.game.GameType.FAF;
import static com.faforever.client.game.GameType.LADDER_1V1;
import static com.faforever.client.task.PrioritizedTask.Priority.HIGH;

public class UpdateGameFilesTask extends PrioritizedTask<Void> implements UpdateServerResponseListener {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final long TIMEOUT = 30;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;
  private static final Object FILES_TO_UPDATE_LOCK = new Object();

  @Autowired
  I18n i18n;
  @Autowired
  ModService modService;
  @Autowired
  NotificationService notificationService;
  @Autowired
  UpdateServerAccessor updateServerAccessor;
  @Autowired
  PreferencesService preferencesService;

  private String targetDirectoryName;
  private String gameType;
  private Set<String> simMods;
  private String targetVersion;
  private ObservableList<String> filesToUpdate;
  private Map<String, Integer> modVersions;
  private int numberOfFilesToUpdate;

  @PostConstruct
  public void postConstruct() {
    setPriority(HIGH);
  }

  @Override
  protected Void call() throws Exception {
    updateTitle(i18n.get("prepareGameUpdateTask.preparing"));

    copyGameFilesToFafBinDirectory();

    updateServerAccessor.connect(this);

    downloadMissingSimMods();

    try {
      if (FAF.getString().equals(gameType) || LADDER_1V1.getString().equals(gameType)) {
        updateFiles("bin", "FAF");
        updateFiles("gamedata", "FAFGAMEDATA");
      } else {
        updateFiles("bin", "FAF");
        updateFiles("gamedata", "FAFGAMEDATA");
        updateFiles("bin", gameType);
        updateFiles("gamedata", gameType + "GameData");
      }
    } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
      notificationService.addNotification(
          new PersistentNotification(i18n.get("update.error.updateFailed", e.getLocalizedMessage()), Severity.WARN)
      );
      throw e;
    } finally {
      updateServerAccessor.disconnect();
    }
    return null;
  }

  protected void copyGameFilesToFafBinDirectory() throws IOException {
    logger.info("Copying game files from FA to FAF folder, if necessary", gameType, targetVersion);

    Path faBinDirectory = preferencesService.getPreferences().getForgedAlliance().getPath().resolve("bin");
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(faBinDirectory)) {
      for (Path source : directoryStream) {
        Path destination = preferencesService.getFafBinDirectory().resolve(source.getFileName());

        if (Files.exists(destination)) {
          continue;
        }

        logger.debug("Copying file '{}' to '{}'", source, destination);

        Files.createDirectories(destination.getParent());
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

        if (OperatingSystem.current() == OperatingSystem.WINDOWS) {
          Files.setAttribute(destination, "dos:readonly", false);
        }
      }
    }
  }

  private void downloadMissingSimMods() throws InterruptedException, ExecutionException, TimeoutException, IOException {
    Set<String> uidsOfRequiredSimMods = simMods;
    if (uidsOfRequiredSimMods.isEmpty()) {
      return;
    }

    Set<String> uidsOfInstalledMods = modService.getInstalledModUids();
    Set<String> uidsOfModsToInstall = uidsOfRequiredSimMods.stream()
        .filter(uid -> !uidsOfInstalledMods.contains(uid))
        .collect(Collectors.toSet());

    for (String uid : uidsOfModsToInstall) {
      downloadMod(uid);
    }
  }

  private void updateFiles(String targetDirectoryName, String fileGroup)
      throws IOException, InterruptedException, ExecutionException, TimeoutException {
    CompletableFuture<List<String>> filesToUpdateFuture = new CompletableFuture<>();

    updateServerAccessor.requestFilesToUpdate(fileGroup)
        .thenApply(filesToUpdateFuture::complete)
        .exceptionally(throwable -> {
          logger.warn("Files could not be updated", throwable);
          return null;
        });

    filesToUpdate = FXCollections.observableList(filesToUpdateFuture.get(TIMEOUT, TIMEOUT_UNIT));
    requestFiles(targetDirectoryName, fileGroup);

    numberOfFilesToUpdate = filesToUpdate.size();

    CountDownLatch filesUpdatedLatch = new CountDownLatch(1);
    filesToUpdate.addListener((Observable observable) -> {
      updateProgress();
      if (filesToUpdate.isEmpty()) {
        filesUpdatedLatch.countDown();
      }
    });
    filesUpdatedLatch.await();

    logger.debug("File group '{}' for game type '{}' has been updated", fileGroup, gameType);
  }

  private void downloadMod(String uid) throws ExecutionException, InterruptedException, TimeoutException {
    String modPath = updateServerAccessor.requestSimPath(uid).get(TIMEOUT, TIMEOUT_UNIT);

    modService.downloadAndInstallMod(modPath).get(TIMEOUT, TIMEOUT_UNIT);

    updateServerAccessor.incrementModDownloadCount(uid);
  }

  private void requestFiles(String targetDirectoryName, String fileGroup) throws IOException {
    this.targetDirectoryName = targetDirectoryName;
    Path targetDirectory = preferencesService.getFafDataDirectory().resolve(targetDirectoryName);

    synchronized (FILES_TO_UPDATE_LOCK) {
      for (String filename : filesToUpdate) {
        Path fileToPatch = targetDirectory.resolve(filename);

        logger.debug("Updating file {}", fileToPatch.toAbsolutePath());

        if (Files.notExists(fileToPatch)) {
          if (targetVersion != null) {
            if (FAF.getString().equals(gameType) || LADDER_1V1.getString().equals(gameType) || fileGroup.equals("FAF") || fileGroup.equals("FAFGAMEDATA")) {
              updateServerAccessor.requestVersion(targetDirectoryName, filename, targetVersion);
            } else {
              updateServerAccessor.requestModVersion(targetDirectoryName, filename, modVersions);
            }
          } else {
            updateServerAccessor.requestPath(targetDirectoryName, filename);
          }
        } else {
          if (targetVersion != null) {
            if (FAF.getString().equals(gameType) || LADDER_1V1.getString().equals(gameType) || fileGroup.equals("FAF") || fileGroup.equals("FAFGAMEDATA")) {
              updateServerAccessor.patchTo(targetDirectoryName, filename, targetVersion);
            } else {
              updateServerAccessor.modPatchTo(targetDirectoryName, filename, modVersions);
            }
          } else {
            try (InputStream inputStream = Files.newInputStream(fileToPatch)) {
              String currentMd5 = DigestUtils.md5Hex(inputStream);
              updateServerAccessor.update(targetDirectoryName, filename, currentMd5);
            }
          }
        }
      }
    }
  }

  private void updateProgress() {
    updateTitle(i18n.get("updatingGameTask.updatingFile", numberOfFilesToUpdate - filesToUpdate.size(), numberOfFilesToUpdate));
    updateProgress(numberOfFilesToUpdate - filesToUpdate.size(), numberOfFilesToUpdate);
  }

  @Override
  public void onFileUpToDate(String file) {
    logger.debug("File is already up to date: {}", file);
    synchronized (FILES_TO_UPDATE_LOCK) {
      filesToUpdate.remove(file);
    }
  }

  @Override
  public void onFileUrl(String targetDirectoryName, String fileToCopy, String url) {
    Path targetFile = preferencesService.getFafDataDirectory().resolve(targetDirectoryName).resolve(fileToCopy);
    try {
      downloadFile(new URL(url), targetFile);
      synchronized (FILES_TO_UPDATE_LOCK) {
        filesToUpdate.remove(fileToCopy);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onPatchUrl(String targetDirectoryName, String fileToUpdate, String url) {
    Path targetFile = preferencesService.getFafDataDirectory().resolve(targetDirectoryName).resolve(fileToUpdate);
    Path patchFile = preferencesService.getFafDataDirectory().resolve(targetDirectoryName).resolve("patch.tmp");

    try {
      downloadFile(new URL(url), patchFile);
      applyPatch(patchFile, targetFile);
      synchronized (FILES_TO_UPDATE_LOCK) {
        filesToUpdate.remove(fileToUpdate);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onVersionPatchNotFound(String response) {
    updateServerAccessor.requestVersion(targetDirectoryName, response, targetVersion);
  }

  @Override
  public void onVersionModPatchNotFound(String response) {
    updateServerAccessor.requestModVersion(targetDirectoryName, response, modVersions);
  }

  @Override
  public void onPatchNotFound(String response) {
    updateServerAccessor.request(targetDirectoryName, response);
  }

  private void applyPatch(Path patchFile, Path targetFile) throws IOException {
    Path oldFile = targetFile.getParent().resolve(targetFile.getFileName().toString() + ".old");
    Files.move(targetFile, oldFile);

    try {
      VcdiffDecoder.decode(oldFile.toFile(), patchFile.toFile(), targetFile.toFile());
    } catch (VcdiffDecodeException e) {
      Files.delete(targetFile);
      Files.move(oldFile, targetFile);
      throw new IOException(e);
    }

    Files.delete(oldFile);
    Files.delete(patchFile);
  }

  private void downloadFile(URL url, Path targetFile) throws IOException {
    logger.debug("Downloading file {} to {}", url, targetFile);

    Files.createDirectories(targetFile.getParent());
    Path tempFile = targetFile.getParent().resolve(targetFile.getFileName().toString() + ".tmp");

    try (InputStream inputStream = url.openStream();
         OutputStream outputStream = Files.newOutputStream(tempFile)) {
      updateTitle(i18n.get("downloadingGamePatchTask.downloadingFile", url));
      ByteCopier.from(inputStream)
          .to(outputStream)
          .listener(this::updateProgress)
          .copy();

      Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException e) {
        logger.warn("Could not delete temporary file: " + tempFile.toAbsolutePath(), e);
      }
    }
  }

  public void setSimMods(Set<String> simMods) {
    this.simMods = simMods;
  }

  public void setModVersions(Map<String, Integer> modVersions) {
    this.modVersions = modVersions;
  }

  public void setGameType(String gameType) {
    this.gameType = gameType;
  }
}
