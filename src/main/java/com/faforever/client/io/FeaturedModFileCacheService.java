package com.faforever.client.io;

import com.faforever.client.api.dto.FeaturedModFile;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.ResourceLocks;
import com.faforever.client.util.UpdaterUtil;
import com.google.common.hash.Hashing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.stream.Stream;

import static com.google.common.io.Files.hash;


@Service
@Slf4j
@RequiredArgsConstructor
public class FeaturedModFileCacheService implements InitializingBean {
  private final PreferencesService preferencesService;

  public boolean isCached(FeaturedModFile featuredModFile) throws IOException {
    return Files.exists(getCachedFilePath(featuredModFile));
  }

  public String readHashFromFile(Path filePath) throws IOException {
    return hash(filePath.toFile(), Hashing.md5()).toString();
  }

  private Path getCachedFilePath(String hash, String group) {
    return preferencesService.getFeaturedModCachePath()
        .resolve(group)
        .resolve(hash);
  }

  public Path getCachedFilePath(FeaturedModFile featuredModFile) throws IOException {
    return getCachedFilePath(featuredModFile.getMd5(), featuredModFile.getGroup());
  }

  private Path getCachedFilePath(Path targetPath) throws IOException {
    return getCachedFilePath(readHashFromFile(targetPath), targetPath.getParent().getFileName().toString());
  }

  public void moveFeaturedModFileFromCache(FeaturedModFile featuredModFile, Path targetPath) throws IOException {
    Files.createDirectories(targetPath.getParent());
    ResourceLocks.acquireDiskLock();

    try {
      if (Files.exists(targetPath) && preferencesService.getPreferences().isGameDataCacheActivated()) {
        //We want to keep the old file for now in case it is needed again for example for old replays
        moveFeaturedModFileToCache(targetPath);
      }
      Files.move(getCachedFilePath(featuredModFile), targetPath, StandardCopyOption.REPLACE_EXISTING);
      UpdaterUtil.extractMoviesIfPresent(targetPath, preferencesService.getFafDataDirectory());
    } finally {
      ResourceLocks.freeDiskLock();
    }
  }

  private void moveFeaturedModFileToCache(Path targetPath) throws IOException {
    Files.move(targetPath, getCachedFilePath(targetPath), StandardCopyOption.REPLACE_EXISTING);
  }

  /**
   * Cleanup method, on service start, we'll get rid of old files.
   */
  @Override
  public void afterPropertiesSet() {
    Path cacheDirectory = preferencesService.getFeaturedModCachePath();
    if (!Files.isDirectory(cacheDirectory)) {
      try {
        Files.createDirectories(cacheDirectory);
      } catch (IOException e) {
        log.error("Could not create Featured Mod Cache directory in ''{}''", cacheDirectory);
        throw new RuntimeException(MessageFormat.format("Could not create Featured Mod Cache directory in ''{}''." +
            " You might have to delete it or check if the needed permission are given.", cacheDirectory));
      }
    }
    cleanUnusedFilesFromCache();
  }

  private void cleanUnusedFilesFromCache() {
    try (Stream<Path> pathElements = Files.walk(preferencesService.getFeaturedModCachePath())) {
      pathElements
          .filter(Files::isRegularFile)
          .forEach(this::deleteCachedFileIfNeeded);
    } catch (IOException e) {
      log.error("Cleaning featured mod files cache failed", e);
    }
  }

  /**
   * Per directory cleanup old files.
   */
  private void deleteCachedFileIfNeeded(Path filePath) {
    try {
      ResourceLocks.acquireDiskLock();

      FileTime lastAccessTime = Files.readAttributes(filePath, BasicFileAttributes.class).lastAccessTime();
      OffsetDateTime comparableLastAccessTime = OffsetDateTime.ofInstant(lastAccessTime.toInstant(), ZoneId.systemDefault());
      final boolean olderThanCacheTime = comparableLastAccessTime.plusDays(preferencesService.getPreferences().getCacheLifeTimeInDays()).isBefore(OffsetDateTime.now());
      final boolean gameDataCacheActivated = preferencesService.getPreferences().isGameDataCacheActivated();
      if (olderThanCacheTime || !gameDataCacheActivated) {
        log.debug("Deleting cached file ''{}'' ", filePath.toString());
        Files.deleteIfExists(filePath);
      }
    } catch (IOException e) {
      log.error("Exception during deleting the cache files", e);
    } finally {
      ResourceLocks.freeDiskLock();
    }
  }
}
