package com.faforever.client.patch;

import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.ModInfoBeanBuilder;
import com.faforever.client.mod.ModService;
import com.faforever.client.util.TestResources;
import com.faforever.commons.mod.MountPoint;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static com.faforever.client.preferences.PreferencesService.FORGED_ALLIANCE_EXE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GitFeaturedModUpdateTaskTest {

  private static final String GIT_PATCH_URL = "git://dummy/repo.git";

  @Rule
  public final TemporaryFolder faDirectory = new TemporaryFolder();

  @Mock
  private GitWrapper gitWrapper;
  @Mock
  private I18n i18n;
  @Mock
  private ModService modService;

  /**
   * The directory containing the cloned preferences repository
   */
  private Path clonedRepoDir;
  private GitFeaturedModUpdateTaskImpl instance;

  @Before
  public void setUp() throws Exception {
    instance = new GitFeaturedModUpdateTaskImpl(i18n, gitWrapper, modService);

    Path reposDirectory = faDirectory.getRoot().toPath().resolve("repos");
    clonedRepoDir = reposDirectory.resolve("faf");
  }

  @Test
  public void testUpdateInBackgroundRepoDirectoryDoesNotExist() throws Exception {
    assertTrue("Repo directory was expected to be inexistent, but it existed", Files.notExists(clonedRepoDir));

    prepareFaBinaries();

    doAnswer(invocation -> {
      fakeClone();
      return null;
    }).when(gitWrapper).clone(eq(GIT_PATCH_URL), eq(clonedRepoDir), any(ProgressMonitor.class));
    when(modService.readModVersion(clonedRepoDir)).thenReturn(new ComparableVersion("3663"));
    when(modService.extractModInfo(any(), eq(clonedRepoDir))).thenReturn(
        ModInfoBeanBuilder.create().mountPoints(
            Arrays.asList(
                new MountPoint(Paths.get("env"), "/env"),
                new MountPoint(Paths.get("projectiles"), "/projectiles")
            )).get());

    instance.setRepositoryDirectory(clonedRepoDir);
    instance.setRef("develop");
    instance.setGameRepositoryUrl(GIT_PATCH_URL);

    PatchResult result = instance.call();

    assertThat(result, not(nullValue()));
    assertThat(result.getVersion(), is(new ComparableVersion("3663")));
    assertThat(result.getMountPoints(), hasSize(2));
    assertThat(result.getMountPoints().get(0).getMountPath(), is("/env"));
    assertThat(result.getMountPoints().get(0).getDirectory(), is(Paths.get("env")));

    verify(gitWrapper).clone(eq(GIT_PATCH_URL), eq(clonedRepoDir), any(ProgressMonitor.class));
  }

  private void prepareFaBinaries() throws IOException {
    Path faBinDirectory = faDirectory.getRoot().toPath().resolve("bin");
    Files.createDirectories(faBinDirectory);

    try (RandomAccessFile randomAccessFile = new RandomAccessFile(faBinDirectory.resolve(FORGED_ALLIANCE_EXE).toFile(), "rw")) {
      randomAccessFile.setLength(1024);
    }
  }

  private void fakeClone() throws IOException {
    TestResources.copyResource("/featured_mod/mod_info.lua", clonedRepoDir.resolve("mod_info.lua"));
  }
}
