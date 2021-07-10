package com.faforever.client.io;

import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesBuilder;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.commons.api.dto.FeaturedModFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FeaturedModFileCacheServiceTest {

  @TempDir
  public Path cacheDirectory;
  @TempDir
  public Path targetDirectory;
  @Mock
  private PreferencesService preferenceService;
  private FeaturedModFileCacheService instance;

  @BeforeEach
  public void setUp() throws Exception {
    Preferences preferences = PreferencesBuilder.create().defaultValues().gameDataCacheActivated(true).get();
    when(preferenceService.getPreferences()).thenReturn(preferences);
    when(preferenceService.getFeaturedModCachePath()).thenReturn(cacheDirectory);
    instance = new FeaturedModFileCacheService(preferenceService);
  }

  @Disabled("junit 5 does not yet support having multiple temp directories see https://github.com/junit-team/junit5/issues/1967")
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

    final Path groupFolderInTarget = Files.createDirectory(targetDirectory.resolve(group));
    final Path targetPath = Files.createFile(groupFolderInTarget.resolve(fileName));
    Files.writeString(targetPath, "old file");
    final String hashOldFile = instance.readHashFromFile(targetPath);

    final Path groupFolderInCache = Files.createDirectory(cacheDirectory.resolve(group));
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