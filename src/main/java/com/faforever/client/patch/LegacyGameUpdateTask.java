package com.faforever.client.patch;

import com.faforever.client.i18n.I18n;
import com.faforever.client.io.ByteCopier;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.ResourceLocks;
import com.google.common.hash.Hashing;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import net.dongliu.vcdiff.VcdiffDecoder;
import net.dongliu.vcdiff.exception.VcdiffDecodeException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
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

import static com.faforever.client.game.KnownFeaturedMod.FAF;
import static com.faforever.client.game.KnownFeaturedMod.LADDER_1V1;
import static com.github.nocatch.NoCatch.noCatch;
import static com.google.common.net.UrlEscapers.urlPathSegmentEscaper;
import static javafx.collections.FXCollections.observableArrayList;

public class LegacyGameUpdateTask extends CompletableTask<Void> implements UpdateServerResponseListener {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final long TIMEOUT = 30;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;
  private final ObservableList<String> filesToUpdate;
  @Resource
  I18n i18n;
  @Resource
  ModService modService;
  @Resource
  NotificationService notificationService;
  @Resource
  UpdateServerAccessor updateServerAccessor;
  @Resource
  PreferencesService preferencesService;
  @Resource
  Environment environment;
  private String targetDirectoryName;
  private String featuredMod;
  private Set<String> simMods;
  private Map<String, Integer> modVersions;
  private int numberOfFilesToUpdate;
  private String gameVersion;

  public LegacyGameUpdateTask() {
    super(Priority.HIGH);
    filesToUpdate = observableArrayList();
  }

  @Override
  protected Void call() throws Exception {
    updateTitle(i18n.get("prepareGameUpdateTask.preparing"));

    copyGameFilesToFafBinDirectory();

    updateServerAccessor.connect(this);

    downloadMissingSimMods();
    modService.enableSimMods(simMods);

    try {
      if (FAF.getString().equals(featuredMod) || LADDER_1V1.getString().equals(featuredMod)) {
        updateFiles("bin", "FAF");
        updateFiles("gamedata", "FAFGAMEDATA");
      } else {
        updateFiles("bin", "FAF");
        updateFiles("gamedata", "FAFGAMEDATA");
        updateFiles("bin", featuredMod);
        updateFiles("gamedata", featuredMod + "GameData");
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
    logger.info("Copying game files from FA to FAF folder");

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

    filesToUpdate.setAll(filesToUpdateFuture.get(TIMEOUT, TIMEOUT_UNIT));
    numberOfFilesToUpdate = filesToUpdate.size();

    requestFiles(targetDirectoryName, fileGroup);

    CountDownLatch filesUpdatedLatch = new CountDownLatch(1);
    filesToUpdate.addListener((Observable observable) -> {
      synchronized (filesToUpdate) {
        updateTitle(i18n.get("updatingGameTask.updatingFile", numberOfFilesToUpdate - filesToUpdate.size(), numberOfFilesToUpdate));
        if (filesToUpdate.isEmpty()) {
          filesUpdatedLatch.countDown();
        }
      }
    });
    filesUpdatedLatch.await();

    logger.debug("File group '{}' for game type '{}' has been updated", fileGroup, featuredMod);
  }

  private void downloadMod(String uid) {
    noCatch(() -> {
      String modPath = updateServerAccessor.requestSimPath(uid).toCompletableFuture().get(TIMEOUT, TIMEOUT_UNIT);
      URL url = new URL(environment.getProperty("vault.modRoot") + urlPathSegmentEscaper().escape(modPath.replace("mods/", "")));

      modService.downloadAndInstallMod(url)
          .exceptionally(throwable -> {
            logger.warn("Mod '" + uid + "' could not be downloaded", throwable);
            return null;
          }).toCompletableFuture().get();

      updateServerAccessor.incrementModDownloadCount(uid);
    });
  }

  private void requestFiles(String targetDirectoryName, String fileGroup) throws IOException {
    this.targetDirectoryName = targetDirectoryName;
    Path targetDirectory = preferencesService.getFafDataDirectory().resolve(targetDirectoryName);

    synchronized (filesToUpdate) {
      for (String filename : filesToUpdate) {
        Path fileToPatch = targetDirectory.resolve(filename);

        logger.debug("Updating file {}", fileToPatch.toAbsolutePath());

        if (Files.notExists(fileToPatch)) {
          if (gameVersion != null) {
            if (FAF.getString().equals(featuredMod) || LADDER_1V1.getString().equals(featuredMod) || fileGroup.equals("FAF") || fileGroup.equals("FAFGAMEDATA")) {
              updateServerAccessor.requestVersion(targetDirectoryName, filename, gameVersion);
            } else {
              updateServerAccessor.requestModVersion(targetDirectoryName, filename, modVersions);
            }
          } else {
            updateServerAccessor.requestPath(targetDirectoryName, filename);
          }
        } else {
          String currentMd5 = com.google.common.io.Files.hash(fileToPatch.toFile(), Hashing.md5()).toString();
          if (gameVersion != null) {
            if (FAF.getString().equals(featuredMod) || LADDER_1V1.getString().equals(featuredMod) || fileGroup.equals("FAF") || fileGroup.equals("FAFGAMEDATA")) {
              updateServerAccessor.patchTo(targetDirectoryName, filename, currentMd5, gameVersion);
            } else {
              updateServerAccessor.modPatchTo(targetDirectoryName, filename, currentMd5, modVersions);
            }
          } else {
            updateServerAccessor.update(targetDirectoryName, filename, currentMd5);
          }
        }
      }
    }
  }

  @Override
  public void onFileUpToDate(String file) {
    logger.debug("File is already up to date: {}", file);
    synchronized (filesToUpdate) {
      filesToUpdate.remove(file);
    }
  }

  @Override
  public void onFileUrl(String targetDirectoryName, String fileToCopy, String url) {
    Path targetFile = preferencesService.getFafDataDirectory().resolve(targetDirectoryName).resolve(fileToCopy);
    try {
      downloadFile(new URL(url), targetFile);
      synchronized (filesToUpdate) {
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
      synchronized (filesToUpdate) {
        filesToUpdate.remove(fileToUpdate);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onVersionPatchNotFound(String response) {
    updateServerAccessor.requestVersion(targetDirectoryName, response, gameVersion);
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

    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

    try (InputStream inputStream = url.openStream();
         OutputStream outputStream = Files.newOutputStream(tempFile)) {
      ResourceLocks.acquireDownloadLock();

      updateTitle(i18n.get("downloadingGamePatchTask.downloadingFile", url));
      ByteCopier.from(inputStream)
          .to(outputStream)
          .totalBytes(urlConnection.getContentLength())
          .listener(this::updateProgress)
          .copy();

      Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      ResourceLocks.freeDownloadLock();
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException e) {
        logger.warn("Could not delete temporary file: " + tempFile.toAbsolutePath(), e);
      }
    }
  }

  public void setSimMods(@NotNull Set<String> simMods) {
    this.simMods = simMods;
  }

  public void setModVersions(@NotNull Map<String, Integer> modVersions) {
    this.modVersions = modVersions;
  }

  public void setFeaturedMod(@NotNull String featuredMod) {
    this.featuredMod = featuredMod;
  }

  public void setGameVersion(String gameVersion) {
    this.gameVersion = gameVersion;
  }
}
