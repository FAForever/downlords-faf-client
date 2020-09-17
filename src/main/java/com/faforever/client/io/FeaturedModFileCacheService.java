package com.faforever.client.io;

import com.faforever.client.api.dto.FeaturedModFile;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.ResourceLocks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;


@Service
@Slf4j
public class FeaturedModFileCacheService implements InitializingBean {
  private final Path cacheDirectory;
  private int cacheLifeTimeInDays;

  public FeaturedModFileCacheService(PreferencesService preferencesService) {
    this.cacheDirectory = preferencesService.getCacheDirectory();
    this.cacheLifeTimeInDays = preferencesService.getPreferences().getCacheLifeTimeInDays();
  }

  private String getCachedFileName(FeaturedModFile featuredModFile) {
    return String.format(
        "%s.%s.%s.%s",
        featuredModFile.getId(),
        featuredModFile.getVersion(),
        featuredModFile.getMd5(),
        featuredModFile.getName()
    );
  }

  public Path getCachedFilePath(FeaturedModFile featuredModFile) {
    return cacheDirectory.resolve(featuredModFile.getGroup()).resolve(getCachedFileName(featuredModFile));
  }

  public void copyFeaturedModFileFromCache(Path cacheFilePath, Path targetPath) throws java.io.IOException {
    Files.createDirectories(targetPath.getParent());
    ResourceLocks.acquireDiskLock();

    try {
      Files.copy(cacheFilePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception e) {
      log.error(e.toString());
    } finally {
      ResourceLocks.freeDiskLock();
    }
  }

  /**
   * Cleanup method, on service start, we'll get rid of old files.
   */
  @Override
  public void afterPropertiesSet() {
    try {
      Files.walk(this.cacheDirectory).forEach(this::walkDirectoriesAndDeleteCachedFiles);
    } catch (IOException e) {
      log.error("Exception during gathering files", e);
    }
  }

  private void walkDirectoriesAndDeleteCachedFiles(Path directoryPath) {
    try {
      Files.walk(directoryPath).forEach(this::deleteCachedFileIfNeeded);
    } catch (IOException e) {
      log.error("Exception during gathering files per directory", e);
    }
  }

  /**
   * Per directory cleanup old files.
   */
  private void deleteCachedFileIfNeeded(Path filePath) {
    if (Files.isDirectory(filePath)) {
      return;
    }

    try {
      ResourceLocks.acquireDiskLock();

      FileTime lastAccessTime = Files.readAttributes(filePath, BasicFileAttributes.class).lastAccessTime();
      OffsetDateTime comparableLastAccessTime = OffsetDateTime.ofInstant(lastAccessTime.toInstant(), ZoneId.systemDefault());
      if (comparableLastAccessTime.plusDays(this.cacheLifeTimeInDays).isBefore(OffsetDateTime.now())) {
        log.info("deleting: " + filePath.toString());
        Files.deleteIfExists(filePath);
      }
    } catch (IOException e) {
      log.error("Exception during deleting the cache files", e);
    } finally {
      ResourceLocks.freeDiskLock();
    }
  }
}
