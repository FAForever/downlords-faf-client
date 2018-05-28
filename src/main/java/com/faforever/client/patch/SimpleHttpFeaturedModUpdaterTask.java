package com.faforever.client.patch;

import com.faforever.client.api.dto.FeaturedModFile;
import com.faforever.client.i18n.I18n;
import com.faforever.client.io.DownloadService;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.CompletableTask;
import com.google.common.hash.Hashing;
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
import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SimpleHttpFeaturedModUpdaterTask extends CompletableTask<PatchResult> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final FafService fafService;
  private final PreferencesService preferencesService;
  private final DownloadService downloadService;
  private final I18n i18n;

  private FeaturedMod featuredMod;
  private Integer version;

  public SimpleHttpFeaturedModUpdaterTask(FafService fafService, PreferencesService preferencesService, DownloadService downloadService, I18n i18n) {
    super(Priority.HIGH);

    this.fafService = fafService;
    this.preferencesService = preferencesService;
    this.downloadService = downloadService;
    this.i18n = i18n;
  }

  @Override
  protected PatchResult call() throws Exception {
    String initFileName = "init_" + featuredMod.getTechnicalName() + ".lua";

    updateTitle(i18n.get("updater.taskTitle"));
    updateMessage(i18n.get("updater.readingFileList"));

    List<FeaturedModFile> featuredModFiles = fafService.getFeaturedModFiles(featuredMod, version).get();

    Path initFile = null;
    for (FeaturedModFile featuredModFile : featuredModFiles) {
      Path targetPath = preferencesService.getFafDataDirectory()
          .resolve(featuredModFile.getGroup())
          .resolve(featuredModFile.getName());

      if (Files.exists(targetPath)
          && featuredModFile.getMd5().equals(com.google.common.io.Files.hash(targetPath.toFile(), Hashing.md5()).toString())) {
        logger.debug("Already up to date: {}", targetPath);
      } else {
        Files.createDirectories(targetPath.getParent());
        updateMessage(i18n.get("updater.downloadingFile", targetPath.getFileName()));
        downloadService.downloadFile(new URL(featuredModFile.getUrl()), targetPath, this::updateProgress);
      }

      if ("bin".equals(featuredModFile.getGroup()) && initFileName.equalsIgnoreCase(featuredModFile.getName())) {
        initFile = targetPath;
      }
    }

    Assert.isTrue(initFile != null && Files.exists(initFile), "'" + initFileName + "' could be found.");

    int maxVersion = featuredModFiles.stream()
        .mapToInt(mod -> Integer.parseInt(mod.getVersion()))
        .max()
        .orElseThrow(() -> new IllegalStateException("No version found"));

    return PatchResult.withLegacyInitFile(new ComparableVersion(String.valueOf(maxVersion)), initFile);
  }

  public void setFeaturedMod(FeaturedMod featuredMod) {
    this.featuredMod = featuredMod;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
