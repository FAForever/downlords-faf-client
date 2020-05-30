package com.faforever.client.patch;

import com.faforever.client.api.dto.FeaturedModFile;
import com.faforever.client.i18n.I18n;
import com.faforever.client.io.FeaturedModFileCacheService;
import com.faforever.client.io.DownloadService;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.ResourceLocks;
import com.faforever.client.util.UpdaterUtil;
import com.google.common.hash.Hashing;
import lombok.RequiredArgsConstructor;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SimpleHttpFeaturedModUpdaterTask extends CompletableTask<PatchResult> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final FafService fafService;
  private final PreferencesService preferencesService;
  private final DownloadService downloadService;
  private final I18n i18n;
  private final FeaturedModFileCacheService featuredModFileCacheService;

  private FeaturedMod featuredMod;
  private Integer version;

  public SimpleHttpFeaturedModUpdaterTask(
      FafService fafService,
      PreferencesService preferencesService,
      DownloadService downloadService,
      I18n i18n,
      FeaturedModFileCacheService featuredModFileCacheService
  ) {
    super(Priority.HIGH);

    this.fafService = fafService;
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

    List<FeaturedModFile> featuredModFiles = fafService.getFeaturedModFiles(featuredMod, version).get();
    Path fafDataDirectory = preferencesService.getFafDataDirectory();

    Path initFile = null;
    Path cacheFilePath;
    Path targetPath;

    Map<String, String> knownTargetHashes = new HashMap<>();

    // Download to cache if file exists in target place, otherwise, download to direct place.
    for (FeaturedModFile featuredModFile : featuredModFiles) {
      targetPath = fafDataDirectory.resolve(featuredModFile.getGroup()).resolve(featuredModFile.getName());
      cacheFilePath = featuredModFileCacheService.getCachedFilePath(featuredModFile);

      String existingTargetFileHash = null;
      if (Files.exists(targetPath)) {
        existingTargetFileHash = com.google.common.io.Files.hash(targetPath.toFile(), Hashing.md5()).toString();
        knownTargetHashes.put(targetPath.toString(), existingTargetFileHash);
      }

      if (!featuredModFile.getMd5().equals(existingTargetFileHash)) {
        logger.info(String.format("downloading: %s", cacheFilePath.toString()));
        downloadFeaturedModFile(featuredModFile, cacheFilePath);
      }

      if ("bin".equals(featuredModFile.getGroup()) && initFileName.equalsIgnoreCase(featuredModFile.getName())) {
        initFile = targetPath;
      }
    }

    for (FeaturedModFile featuredModFile : featuredModFiles) {
      targetPath = fafDataDirectory.resolve(featuredModFile.getGroup()).resolve(featuredModFile.getName());
      cacheFilePath = featuredModFileCacheService.getCachedFilePath(featuredModFile);
      String existingTargetFileHash = knownTargetHashes.get(targetPath.toString());

      if (!featuredModFile.getMd5().equals(existingTargetFileHash)) {
        logger.info(String.format("copying featured mod file: %s to %s", cacheFilePath.toString(), targetPath.toString()));
        featuredModFileCacheService.copyFeaturedModFileFromCache(cacheFilePath, targetPath);
      }
    }

    Assert.isTrue(initFile != null && Files.exists(initFile), "'" + initFileName + "' could be found.");

    int maxVersion = featuredModFiles.stream()
        .mapToInt(mod -> Integer.parseInt(mod.getVersion()))
        .max()
        .orElseThrow(() -> new IllegalStateException("No version found"));

    return PatchResult.withLegacyInitFile(new ComparableVersion(String.valueOf(maxVersion)), initFile);
  }

  private void downloadFeaturedModFile(FeaturedModFile featuredModFile, Path targetPath) throws java.io.IOException {
    Files.createDirectories(targetPath.getParent());
    updateMessage(i18n.get("updater.downloadingFile", featuredModFile.getName()));

    String url = featuredModFile.getUrl();
    downloadService.downloadFile(new URL(url), targetPath, this::updateProgress);
    UpdaterUtil.extractMoviesIfPresent(targetPath, preferencesService.getFafDataDirectory());
  }

  public void setFeaturedMod(FeaturedMod featuredMod) {
    this.featuredMod = featuredMod;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
