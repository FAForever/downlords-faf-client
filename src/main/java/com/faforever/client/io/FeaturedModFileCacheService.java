package com.faforever.client.io;

import com.faforever.client.api.dto.FeaturedModFile;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.ResourceLocks;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Calendar;

@Service
@Slf4j
public class FeaturedModFileCacheService implements InitializingBean {
  private final Path cacheDirectory;
  private int cacheLifeTime;
  private Calendar calendar;
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public FeaturedModFileCacheService(PreferencesService preferencesService) {
    this.cacheDirectory = preferencesService.getCacheDirectory();
    this.cacheLifeTime = preferencesService.getPreferences().getCacheLifeTime() * 1000 * 3600 * 24;
    this.calendar = Calendar.getInstance();
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
      logger.error(e.toString());
    } finally {
      ResourceLocks.freeDiskLock();
    }
  }

  /**
   * Cleanup method, on service start, we'll get rid of old files.
   */
  @Override
  public void afterPropertiesSet() {
    Path[] directoryPaths = new Path[] {
        this.cacheDirectory.resolve("bin"),
        this.cacheDirectory.resolve("gamedata")
    };

    try {
      for (Path directoryPath : directoryPaths) {
        Files.walk(directoryPath).forEach(this::deleteCachedFileIfNeeded);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void deleteCachedFileIfNeeded(Path filePath) {
    try {
      if (Files.isDirectory(filePath)) {
        return;
      }

      FileTime lastAccessTime = Files.readAttributes(filePath, BasicFileAttributes.class).lastAccessTime();
      if (lastAccessTime.toMillis() + cacheLifeTime < calendar.getTimeInMillis()) {
        System.out.println("deleting: " + filePath.toString());
        Files.deleteIfExists(filePath);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
