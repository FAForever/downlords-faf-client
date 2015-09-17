package com.faforever.client.patch;

import com.faforever.client.game.GameType;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskGroup;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.util.Callback;
import com.faforever.client.util.TestResources;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.core.env.Environment;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.faforever.client.patch.GitRepositoryGameUpdateService.InstallType.RETAIL;
import static com.faforever.client.patch.GitRepositoryGameUpdateService.STEAM_API_DLL;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class GitRepositoryGameUpdateServiceTest extends AbstractPlainJavaFxTest {

  private static final String GIT_PATCH_URL = "git://dummy/repo.git";

  @Rule
  public final TemporaryFolder reposDirectory = new TemporaryFolder();

  @Rule
  public final TemporaryFolder fafBinDirectory = new TemporaryFolder();

  @Rule
  public final TemporaryFolder faDirectory = new TemporaryFolder();

  private GitRepositoryGameUpdateService instance;
  private ForgedAlliancePrefs forgedAlliancePrefs;

  /**
   * The directory containing the cloned patch repository
   */
  private Path binaryPatchRepoDirectory;
  private Path faBinDirectory;

  @Before
  public void setUp() throws Exception {
    instance = new GitRepositoryGameUpdateService();
    instance.environment = mock(Environment.class);
    instance.preferencesService = mock(PreferencesService.class);
    instance.taskService = mock(TaskService.class);
    instance.i18n = mock(I18n.class);
    instance.gitWrapper = mock(GitWrapper.class);
    instance.notificationService = mock(NotificationService.class);
    Preferences preferences = mock(Preferences.class);
    forgedAlliancePrefs = mock(ForgedAlliancePrefs.class);

    when(instance.environment.getProperty("patch.git.url")).thenReturn(GIT_PATCH_URL);
    when(instance.preferencesService.getPreferences()).thenReturn(preferences);
    when(instance.preferencesService.getFafReposDirectory()).thenReturn(reposDirectory.getRoot().toPath());
    when(instance.preferencesService.getFafBinDirectory()).thenReturn(fafBinDirectory.getRoot().toPath());
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(forgedAlliancePrefs.getPath()).thenReturn(faDirectory.getRoot().toPath());
    mockTaskService();

    binaryPatchRepoDirectory = instance.preferencesService.getFafReposDirectory().resolve(GitRepositoryGameUpdateService.REPO_NAME);
    faBinDirectory = forgedAlliancePrefs.getPath().resolve("bin");

    instance.postConstruct();
  }

  @SuppressWarnings("unchecked")
  private void mockTaskService() throws Exception {
    doAnswer((InvocationOnMock invocation) -> {
      PrioritizedTask<Boolean> prioritizedTask = invocation.getArgumentAt(1, PrioritizedTask.class);
      prioritizedTask.run();

      Callback<Boolean> callback = invocation.getArgumentAt(2, Callback.class);

      Future<Throwable> throwableFuture = WaitForAsyncUtils.asyncFx(prioritizedTask::getException);
      Throwable throwable = throwableFuture.get(1, TimeUnit.SECONDS);
      if (throwable != null) {
        callback.error(throwable);
      } else {
        Future<Boolean> result = WaitForAsyncUtils.asyncFx(prioritizedTask::getValue);
        callback.success(result.get(1, TimeUnit.SECONDS));
      }

      return null;
    }).when(instance.taskService).submitTask(any(), any(), any());
  }

  @Test
  public void testPatchInBackgroundFaDirectoryUnspecified() throws Exception {
    when(forgedAlliancePrefs.getPath()).thenReturn(null);

    instance.updateInBackground(GameType.FAF.getString(), null, null, null);

    verifyZeroInteractions(instance.taskService);
  }

  @Test
  public void testPatchInBackgroundRepoDirectoryDoesNotExist() throws Exception {
    assertTrue("Repo directory was expected not be inexistent, but it existed", Files.notExists(binaryPatchRepoDirectory));

    prepareFaBinaries();

    doAnswer(invocation -> {
      prepareLocalPatchRepo();
      return null;
    }).when(instance.gitWrapper).clone(GIT_PATCH_URL, binaryPatchRepoDirectory);

    instance.updateInBackground(GameType.FAF.getString(), null, null, null);

    verify(instance.gitWrapper).clone(GIT_PATCH_URL, binaryPatchRepoDirectory);
    verify(instance.taskService).submitTask(eq(TaskGroup.NET_HEAVY), any(), any());
    verifyNotification(Severity.INFO);
  }

  private void prepareFaBinaries() throws IOException {
    Path faBinDirectory = faDirectory.getRoot().toPath().resolve("bin");
    Files.createDirectories(faBinDirectory);

    TestResources.copyResource("/patch/GDFBinary.dll", faBinDirectory.resolve("GDFBinary.dll"));
    TestResources.copyResource("/patch/testFile1.txt", faBinDirectory.resolve("testFile1.txt"));
    TestResources.copyResource("/patch/testFile2.txt", faBinDirectory.resolve("testFile2.txt"));
  }

  private void prepareLocalPatchRepo() throws IOException {
    TestResources.copyResource("/patch/retail.json", binaryPatchRepoDirectory.resolve(RETAIL.migrationDataFileName));
    TestResources.copyResource("/patch/bsdiff4/040943c20d9e1f7de7f496b1202a600d", binaryPatchRepoDirectory.resolve("bsdiff4/040943c20d9e1f7de7f496b1202a600d"));
  }

  private void verifyNotification(Severity severity) {
    ArgumentCaptor<PersistentNotification> captor = ArgumentCaptor.forClass(PersistentNotification.class);
    verify(instance.notificationService).addNotification(captor.capture());
    verifyNoMoreInteractions(instance.notificationService);
    assertThat(captor.getValue().getSeverity(), is(severity));
  }

  @Test
  public void testPatchInBackgroundThrowsException() throws Exception {
    doAnswer((InvocationOnMock invocation) -> {
      Callback callback = invocation.getArgumentAt(2, Callback.class);
      callback.error(new Exception("This exception mimicks that something went wrong"));
      return null;
    }).when(instance.taskService).submitTask(any(), any(), any());

    instance.updateInBackground(GameType.FAF.getString(), null, null, null);

    verifyNotification(Severity.WARN);
  }

  @Test
  public void testPatchInBackgroundSuccessful() throws Exception {
    prepareFaBinaries();
    prepareLocalPatchRepo();

    instance.updateInBackground(GameType.FAF.getString(), null, null, null);

    verifyNotification(Severity.INFO);
  }

  @Test
  public void testCheckForUpdatesInBackgroundPatchingIsNeeded() throws Exception {
    instance.checkForUpdateInBackground();

    verifyNotification(Severity.INFO);
  }

  @Test
  public void testCheckForUpdatesInBackgroundPatchingIsNeededBecauseRepoDirDoesntExist() throws Exception {
    instance.checkForUpdateInBackground();

    verifyNotification(Severity.INFO);
  }

  @Test
  public void testCheckForUpdatesInBackgroundThrowsException() throws Exception {
    doAnswer((InvocationOnMock invocation) -> {
      Callback callback = invocation.getArgumentAt(2, Callback.class);
      callback.error(new Exception("This exception mimicks that something went wrong"));
      return null;
    }).when(instance.taskService).submitTask(any(), any(), any());

    instance.checkForUpdateInBackground();

    verifyNotification(Severity.WARN);
  }

  @Test
  public void testCheckForUpdatesInBackgroundRepoDirectoryMissing() throws Exception {
    assertTrue(Files.notExists(binaryPatchRepoDirectory));

    instance.checkForUpdateInBackground();

    verifyNotification(Severity.INFO);
  }

  @Test
  public void testCheckForUpdatesInBackgroundRemoteHeadDifferent() throws Exception {
    Files.createDirectories(binaryPatchRepoDirectory);

    when(instance.gitWrapper.getLocalHead(binaryPatchRepoDirectory)).thenReturn("1234");
    when(instance.gitWrapper.getRemoteHead(binaryPatchRepoDirectory)).thenReturn("5678");

    instance.checkForUpdateInBackground();

    verifyNotification(Severity.INFO);
  }

  @Test
  public void testCheckForUpdatesInBackgroundLocalFilesMissing() throws Exception {
    prepareLocalPatchRepo();

    when(instance.gitWrapper.getLocalHead(binaryPatchRepoDirectory)).thenReturn("1234");
    when(instance.gitWrapper.getRemoteHead(binaryPatchRepoDirectory)).thenReturn("1234");

    instance.checkForUpdateInBackground();

    verifyNotification(Severity.INFO);
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

    when(instance.gitWrapper.getLocalHead(binaryPatchRepoDirectory)).thenReturn("1234");
    when(instance.gitWrapper.getRemoteHead(binaryPatchRepoDirectory)).thenReturn("1234");

    instance.checkForUpdateInBackground();

    verifyNotification(Severity.INFO);
  }

  @Test
  public void testCheckForUpdatesInBackgroundEverythingUpToDate() throws Exception {
    prepareLocalPatchRepo();
    prepareFaBinaries();

    instance.updateInBackground(GameType.FAF.getString(), null, null, null);
    verifyNotification(Severity.INFO);

    when(instance.gitWrapper.getLocalHead(binaryPatchRepoDirectory)).thenReturn("1234");
    when(instance.gitWrapper.getRemoteHead(binaryPatchRepoDirectory)).thenReturn("1234");

    instance.checkForUpdateInBackground();

    verifyNoMoreInteractions(instance.notificationService);
  }

  @Test
  public void testGuessInstallTypeRetail() throws Exception {
    instance.checkForUpdateInBackground();

    assertTrue(Files.notExists(faBinDirectory.resolve(STEAM_API_DLL)));

    GitRepositoryGameUpdateService.InstallType installType = instance.guessInstallType();
    assertThat(installType, is(GitRepositoryGameUpdateService.InstallType.RETAIL));
  }

  @Test
  public void testGuessInstallTypeSteam() throws Exception {
    instance.checkForUpdateInBackground();

    Files.createDirectories(faBinDirectory);
    Files.createFile(faBinDirectory.resolve(STEAM_API_DLL));

    GitRepositoryGameUpdateService.InstallType installType = instance.guessInstallType();
    assertThat(installType, is(GitRepositoryGameUpdateService.InstallType.STEAM));
  }
}
