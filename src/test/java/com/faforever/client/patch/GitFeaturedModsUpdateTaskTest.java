package com.faforever.client.patch;

import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.ModService;
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GitFeaturedModsUpdateTaskTest extends AbstractPlainJavaFxTest {

  private static final String GIT_PATCH_URL = "git://dummy/repo.git";

  @Rule
  public final TemporaryFolder faDirectory = new TemporaryFolder();

  @Rule
  public final TemporaryFolder fafBinDirectory = new TemporaryFolder();

  @Mock
  private GitWrapper gitWrapper;
  @Mock
  private TaskService taskService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private Environment environment;
  @Mock
  private I18n i18n;
  @Mock
  private Preferences preferences;
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;
  @Mock
  private ModService modService;

  /**
   * The directory containing the cloned game repository
   */
  private Path clonedRepoDir;
  private GitFeaturedModsUpdateTask instance;

  @Before
  public void setUp() throws Exception {
    instance = new GitFeaturedModsUpdateTask();
    instance.preferencesService = preferencesService;
    instance.gitWrapper = gitWrapper;
    instance.environment = environment;
    instance.i18n = i18n;
    instance.modService = modService;

    Path reposDirectory = faDirectory.getRoot().toPath().resolve("repos");
    clonedRepoDir = reposDirectory.resolve("faf");

    when(preferencesService.getGitReposDirectory()).thenReturn(reposDirectory);
    when(preferencesService.getFafBinDirectory()).thenReturn(fafBinDirectory.getRoot().toPath());
    when(environment.getProperty("patch.git.url")).thenReturn(GIT_PATCH_URL);
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(forgedAlliancePrefs.getPath()).thenReturn(faDirectory.getRoot().toPath());

    instance.postConstruct();
  }

  @Test
  public void testUpdateInBackgroundRepoDirectoryDoesNotExist() throws Exception {
    assertTrue("Repo directory was expected to be inexistent, but it existed", Files.notExists(clonedRepoDir));

    prepareFaBinaries();

    doAnswer(invocation -> {
      prepareLocalPatchRepo();
      return null;
    }).when(gitWrapper).clone(GIT_PATCH_URL, clonedRepoDir);

    instance.setRepositoryDirectory(clonedRepoDir);
    instance.setRef("develop");
    instance.setGameRepositoryUrl(GIT_PATCH_URL);

    assertThat(instance.call(), is(nullValue()));
    verify(gitWrapper).clone(GIT_PATCH_URL, clonedRepoDir);
  }

  private void prepareFaBinaries() throws IOException {
    Path faBinDirectory = faDirectory.getRoot().toPath().resolve("bin");
    Files.createDirectories(faBinDirectory);

    TestResources.copyResource("/patch/GDFBinary.dll", faBinDirectory.resolve("GDFBinary.dll"));
    TestResources.copyResource("/patch/testFile1.txt", faBinDirectory.resolve("testFile1.txt"));
    TestResources.copyResource("/patch/testFile2.txt", faBinDirectory.resolve("testFile2.txt"));
  }

  private void prepareLocalPatchRepo() throws IOException {
    TestResources.copyResource("/patch/bsdiff4/040943c20d9e1f7de7f496b1202a600d", clonedRepoDir.resolve("bsdiff4/040943c20d9e1f7de7f496b1202a600d"));
  }
}
