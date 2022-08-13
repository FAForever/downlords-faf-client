package com.faforever.client.patch;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.io.ChecksumMismatchException;
import com.faforever.client.io.DownloadService;
import com.faforever.client.io.FeaturedModFileCacheService;
import com.faforever.client.mod.ModService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.commons.api.dto.FeaturedModFile;
import com.faforever.commons.fa.ForgedAllianceExePatcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class SimpleHttpFeaturedModUpdaterTask extends CompletableTask<PatchResult> {
  private final ModService modService;
  private final PreferencesService preferencesService;
  private final DownloadService downloadService;
  private final I18n i18n;
  private final FeaturedModFileCacheService featuredModFileCacheService;

  private FeaturedModBean featuredMod;
  private Integer version;

  public SimpleHttpFeaturedModUpdaterTask(
      ModService modService,
      PreferencesService preferencesService,
      DownloadService downloadService,
      I18n i18n,
      FeaturedModFileCacheService featuredModFileCacheService
  ) {
    super(Priority.HIGH);

    this.modService = modService;
    this.preferencesService = preferencesService;
    this.downloadService = downloadService;
    this.i18n = i18n;
    this.featuredModFileCacheService = featuredModFileCacheService;
  }

  @Override
  protected PatchResult call() throws Exception {
    String initFileName = "init_" + featuredMod.getTechnicalName() + ".lua";

    updateTitle(i18n.get("updater.taskTitle"));
    updateMessage(i18n.get("updater.readingFileList"));

    Path fafDataDirectory = preferencesService.getPreferences().getData().getBaseDataDirectory();

    List<FeaturedModFile> featuredModFiles = modService.getFeaturedModFiles(featuredMod, version).join();

    featuredModFiles
        .forEach(featuredModFile -> {
          Path targetPath = fafDataDirectory
              .resolve(featuredModFile.getGroup())
              .resolve(featuredModFile.getName());

          try {
            Files.createDirectories(targetPath.getParent());
            if (fileAlreadyLoaded(featuredModFile, targetPath)) {
              log.info("Featured mod file already prepared: `{}`", featuredModFile);
            } else {
              if (!featuredModFileCacheService.isCached(featuredModFile)) {
                Path cachedFilePath = featuredModFileCacheService.getCachedFilePath(featuredModFile);
                Files.createDirectories(cachedFilePath.getParent());
                if (PreferencesService.FORGED_ALLIANCE_EXE.equals(featuredModFile.getName())) {
                  patchOrDownloadForgedAllianceExe(featuredModFile, cachedFilePath, targetPath);
                } else {
                  downloadFeaturedModFile(featuredModFile, cachedFilePath);
                }
              }
              featuredModFileCacheService.copyFeaturedModFileFromCache(featuredModFile, targetPath);
            }
          }
          catch (IOException | NoSuchAlgorithmException | ChecksumMismatchException e) {
            log.error("Error updating featured mod file: `{}`", featuredModFile, e);
            throw new RuntimeException(e);
          }
        });

    Path initFile = featuredModFiles.stream()
        .filter(featuredModFile -> "bin".equals(featuredModFile.getGroup()) &&
            initFileName.equalsIgnoreCase(featuredModFile.getName()))
        .map(featuredModFile -> fafDataDirectory
            .resolve(featuredModFile.getGroup())
            .resolve(featuredModFile.getName()))
        .filter(Files::exists)
        .findAny()
        .orElseThrow(() -> new IllegalStateException("No init file found for featured mod: " + featuredMod.getTechnicalName()));

    int maxVersion = featuredModFiles.stream()
        .mapToInt(mod -> Integer.parseInt(mod.getVersion()))
        .max()
        .orElseThrow(() -> new IllegalStateException("No version found for featured mod: " + featuredMod.getTechnicalName()));

    return new PatchResult(new ComparableVersion(String.valueOf(maxVersion)), initFile);
  }

  private void patchOrDownloadForgedAllianceExe(FeaturedModFile featuredModFile, Path cachedFilePath, Path targetPath) throws IOException, ChecksumMismatchException, NoSuchAlgorithmException {
    if (Files.exists(targetPath)) {
      Files.createDirectories(cachedFilePath.getParent());
      Path tempFile = Files.createTempFile(cachedFilePath.getParent(), "download", null);
      // Make a copy of the currently installed ForgedAlliance.exe
      Files.copy(targetPath, tempFile, StandardCopyOption.REPLACE_EXISTING);
      int version = Integer.parseInt(featuredModFile.getVersion());
      ForgedAllianceExePatcher.patchVersion(tempFile, version);

      if (fileAlreadyLoaded(featuredModFile, tempFile)) {
        // Hash matches so use the patched version
        Files.move(tempFile, cachedFilePath, StandardCopyOption.REPLACE_EXISTING);
        log.debug("Using locally patched `{}` for version `{}`", featuredModFile.getName(), version);
        return;
      }
    }

    downloadFeaturedModFile(featuredModFile, cachedFilePath);
  }

  private boolean fileAlreadyLoaded(FeaturedModFile featuredModFile, Path targetPath) throws IOException {
    return Files.exists(targetPath)
        && Objects.equals(featuredModFile.getMd5(), featuredModFileCacheService.readHashFromFile(targetPath));
  }

  private void downloadFeaturedModFile(FeaturedModFile featuredModFile, Path targetPath) throws IOException, NoSuchAlgorithmException, ChecksumMismatchException {
    Files.createDirectories(targetPath.getParent());
    updateMessage(i18n.get("updater.downloadingFile", featuredModFile.getName()));

    String md5sum = featuredModFile.getMd5();

    // We can perform cloudflare hmac verification either with a query parameter or by sending a request header hmac with the value
    // Using a request header is preferred as this allows us to cache the url on cloudflare without the query string as the
    // query string effectively renders the cache ineffective.
    Map<String, String> requestParameters = Map.of(featuredModFile.getHmacParameter(), featuredModFile.getHmacToken());

    downloadService.downloadFile(new URL(featuredModFile.getCacheableUrl()), requestParameters, targetPath, this::updateProgress, md5sum);
  }

  public void setFeaturedMod(FeaturedModBean featuredMod) {
    this.featuredMod = featuredMod;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
