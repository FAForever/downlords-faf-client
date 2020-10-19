package com.faforever.client.io;

import com.faforever.client.api.dto.FeaturedModFile;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FeaturedModFileCacheServiceTest {

  @Rule
  public TemporaryFolder cacheDirectory = new TemporaryFolder();
  @Rule
  public TemporaryFolder targetDirectory = new TemporaryFolder();
  @Mock
  private PreferencesService preferenceService;
  private FeaturedModFileCacheService instance;
  private Preferences preferences;

  @Before
  public void setUp() throws Exception {
    preferences = new Preferences();
    when(preferenceService.getPreferences()).thenReturn(preferences);
    when(preferenceService.getFeaturedModCachePath()).thenReturn(cacheDirectory.getRoot().toPath());
    instance = new FeaturedModFileCacheService(preferenceService);
  }

  @Test
  public void testMoveFeaturedModFileFromCacheWithExistingOldFile() throws IOException {
    preferences.setGameDataCacheActivated(true);
    final FeaturedModFile featuredModFile = new FeaturedModFile();
    featuredModFile.setId("1");
    final String fakeHashOfNewFile = "ksadduhoashaodiw";
    featuredModFile.setMd5(fakeHashOfNewFile);
    final String fileName = "test.faf";
    featuredModFile.setName(fileName);
    final String group = "gamedata";
    featuredModFile.setGroup(group);

    final Path groupFolderInTarget = targetDirectory.newFolder(group).toPath();
    final Path targetPath = Files.createFile(groupFolderInTarget.resolve(fileName));
    Files.writeString(targetPath, "old file");
    final String hashOldFile = instance.readHashFromFile(targetPath);

    final Path groupFolderInCache = cacheDirectory.newFolder(group).toPath();
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