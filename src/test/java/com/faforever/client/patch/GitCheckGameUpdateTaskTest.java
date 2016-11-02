package com.faforever.client.patch;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.util.TestResources;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class GitCheckGameUpdateTaskTest extends AbstractPlainJavaFxTest {

  private static final String GIT_PATCH_URL = "git://dummy/repo.git";
  private static final long TIMEOUT = 5000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

  @Rule
  public final TemporaryFolder reposDirectory = new TemporaryFolder();
  @Rule
  public final TemporaryFolder fafBinDirectory = new TemporaryFolder();
  @Rule
  public final TemporaryFolder faDirectory = new TemporaryFolder();

  /**
   * The directory containing the cloned patch repository
   */
  private Path binaryPatchRepoDirectory;
  private GitCheckGameUpdateTask instance;

  @Mock
  private TaskService taskService;
  @Mock
  private Environment environment;
  @Mock
  private GitWrapper gitWrapper;
  @Mock
  private I18n i18n;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private Preferences preferences;
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;

  @Before
  public void setUp() throws Exception {
    instance = new GitCheckGameUpdateTask();
    instance.environment = environment;
    instance.preferencesService = preferencesService;
    instance.i18n = i18n;
    instance.gitWrapper = gitWrapper;

    Path reposDirectory = faDirectory.getRoot().toPath().resolve("repos");
    binaryPatchRepoDirectory = reposDirectory.resolve("faf");

    instance.setGameRepositoryDirectory(binaryPatchRepoDirectory);

    when(preferencesService.getFafReposDirectory()).thenReturn(reposDirectory);
    when(preferencesService.getFafBinDirectory()).thenReturn(fafBinDirectory.getRoot().toPath());
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(forgedAlliancePrefs.getPath()).thenReturn(faDirectory.getRoot().toPath());
    when(environment.getProperty("patch.git.url")).thenReturn(GIT_PATCH_URL);
  }

  @Test
  public void testCheckForUpdatesInBackgroundRepoDirectoryMissing() throws Exception {
    assertTrue(Files.notExists(binaryPatchRepoDirectory));

    assertThat(instance.call(), is(true));
  }

  @Test
  public void testCheckForUpdatesInBackgroundRemoteHeadDifferent() throws Exception {
    Files.createDirectories(binaryPatchRepoDirectory);

    when(gitWrapper.getLocalHead(binaryPatchRepoDirectory)).thenReturn("1234");
    when(gitWrapper.getRemoteHead(binaryPatchRepoDirectory)).thenReturn("5678");

    assertThat(instance.call(), is(true));
  }

  @Test
  public void testCheckForUpdatesInBackgroundLocalFilesMissing() throws Exception {
    prepareLocalPatchRepo();

    when(gitWrapper.getLocalHead(binaryPatchRepoDirectory)).thenReturn("1234");
    when(gitWrapper.getRemoteHead(binaryPatchRepoDirectory)).thenReturn("1234");

    assertThat(instance.call(), is(true));
  }

  private void prepareLocalPatchRepo() throws IOException {
    TestResources.copyResource("/patch/bsdiff4/040943c20d9e1f7de7f496b1202a600d", binaryPatchRepoDirectory.resolve("bsdiff4/040943c20d9e1f7de7f496b1202a600d"));
  }

  @Test
  public void testCheckForUpdatesInBackgroundFileOutOfDate() throws Exception {
    prepareLocalPatchRepo();

    // Copy all files to the FAF bin directory as if they were patched
    Path fafBinPath = fafBinDirectory.getRoot().toPath();
    TestResources.copyResource("/patch/GDFBinary.dll", fafBinPath.resolve("GDFBinary.dll"));
    TestResources.copyResource("/patch/testFile1.txt", fafBinPath.resolve("testFile1.txt"));
    TestResources.copyResource("/patch/testFile2.txt", fafBinPath.resolve("fooBar.exe"));

    // Then modify one file
    Files.write(fafBinPath.resolve("testFile1.txt"), Collections.singletonList("foo"));

    when(gitWrapper.getLocalHead(binaryPatchRepoDirectory)).thenReturn("1234");
    when(gitWrapper.getRemoteHead(binaryPatchRepoDirectory)).thenReturn("1234");

    assertThat(instance.call(), is(true));
  }

  @Test
  public void testCheckForUpdatesInBackgroundEverythingUpToDate() throws Exception {
    prepareLocalPatchRepo();
    prepareFaBinaries();

    GitGameUpdateTask gitGameUpdateTask = new GitGameUpdateTask();
    gitGameUpdateTask.environment = environment;
    gitGameUpdateTask.gitWrapper = gitWrapper;
    gitGameUpdateTask.i18n = i18n;
    gitGameUpdateTask.preferencesService = preferencesService;
    gitGameUpdateTask.call();

    when(gitWrapper.getLocalHead(binaryPatchRepoDirectory)).thenReturn("1234");
    when(gitWrapper.getRemoteHead(binaryPatchRepoDirectory)).thenReturn("1234");

    assertThat(instance.call(), is(false));
  }

  private void prepareFaBinaries() throws IOException {
    Path faBinDirectory = faDirectory.getRoot().toPath().resolve("bin");
    Files.createDirectories(faBinDirectory);

    TestResources.copyResource("/patch/GDFBinary.dll", faBinDirectory.resolve("GDFBinary.dll"));
    TestResources.copyResource("/patch/testFile1.txt", faBinDirectory.resolve("testFile1.txt"));
    TestResources.copyResource("/patch/testFile2.txt", faBinDirectory.resolve("testFile2.txt"));
  }

  @Test
  public void testCheckForUpdatesInBackgroundPatchingIsNeededBecauseRepoDirDoesntExist() throws Exception {
    assertThat(instance.call(), is(true));
  }
}
