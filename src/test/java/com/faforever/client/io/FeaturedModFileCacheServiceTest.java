package com.faforever.client.io;

import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesBuilder;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.dto.FeaturedModFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class FeaturedModFileCacheServiceTest extends ServiceTest {

  @TempDir
  public Path tempDirectory;
  public Path cacheDirectory;
  public Path targetDirectory;
  @Mock
  private PreferencesService preferenceService;
  private FeaturedModFileCacheService instance;

  @BeforeEach
  public void setUp() throws Exception {
    cacheDirectory = Files.createDirectories(tempDirectory.resolve("cache"));
    targetDirectory = Files.createDirectories(tempDirectory.resolve("target"));
    Preferences preferences = PreferencesBuilder.create().defaultValues().gameDataCacheActivated(true).get();
    when(preferenceService.getPreferences()).thenReturn(preferences);
    when(preferenceService.getFeaturedModCachePath()).thenReturn(cacheDirectory);
    instance = new FeaturedModFileCacheService(preferenceService);
  }

  @Test
  public void testMoveFeaturedModFileFromCacheWithExistingOldFile() throws IOException {
    final FeaturedModFile featuredModFile = new FeaturedModFile();
    featuredModFile.setId("1");
    final String fakeHashOfNewFile = "ksadduhoashaodiw";
    featuredModFile.setMd5(fakeHashOfNewFile);
    final String fileName = "test.faf";
    featuredModFile.setName(fileName);
    final String group = "gamedata";
    featuredModFile.setGroup(group);

    final Path groupFolderInTarget = Files.createDirectories(targetDirectory.resolve(group));
    final Path targetPath = Files.createFile(groupFolderInTarget.resolve(fileName));
    Files.writeString(targetPath, "old file");
    final String hashOldFile = instance.readHashFromFile(targetPath);

    final Path groupFolderInCache = Files.createDirectories(cacheDirectory.resolve(group));
    final Path cachePathNewFile = groupFolderInCache.resolve(fakeHashOfNewFile);
    Files.createFile(cachePathNewFile);
    Files.writeString(cachePathNewFile, "newly downloaded or cached file");
    final String hashNewFile = instance.readHashFromFile(cachePathNewFile);

    assertThat(instance.getCachedFilePath(featuredModFile), is(cachePathNewFile));

    instance.moveFeaturedModFileFromCache(featuredModFile, targetPath);

    final Path oldFileNowInCachePath = groupFolderInCache.resolve(hashOldFile);
    assertTrue(Files.isRegularFile(oldFileNowInCachePath));
    assertEquals(hashOldFile, instance.readHashFromFile(oldFileNowInCachePath));

    assertTrue(Files.isRegularFile(targetPath));
    assertEquals(hashNewFile, instance.readHashFromFile(targetPath));
  }
}