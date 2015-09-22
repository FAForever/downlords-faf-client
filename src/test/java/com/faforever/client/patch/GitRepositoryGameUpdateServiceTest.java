package com.faforever.client.patch;

import com.faforever.client.game.GameType;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.faforever.client.patch.GitRepositoryGameUpdateService.STEAM_API_DLL;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class GitRepositoryGameUpdateServiceTest extends AbstractPlainJavaFxTest {

  private static final long TIMEOUT = 5000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
  @Rule
  public final TemporaryFolder reposDirectory = new TemporaryFolder();
  @Rule
  public TemporaryFolder faDirectory = new TemporaryFolder();

  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private Environment environment;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private TaskService taskService;
  @Mock
  private I18n i18n;
  @Mock
  private GitWrapper gitWrapper;
  @Mock
  private NotificationService notificationService;
  @Mock
  private Preferences preferences;

  private Path faBinDirectory;
  private GitRepositoryGameUpdateService instance;

  @Before
  public void setUp() throws Exception {
    instance = new GitRepositoryGameUpdateService();
    instance.preferencesService = preferencesService;
    instance.taskService = taskService;
    instance.i18n = i18n;
    instance.gitWrapper = gitWrapper;
    instance.notificationService = notificationService;
    instance.applicationContext = applicationContext;

    GitCheckGameUpdateTask gameUpdateTask = mock(GitCheckGameUpdateTask.class, withSettings().useConstructor());
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferencesService.getFafReposDirectory()).thenReturn(reposDirectory.getRoot().toPath());
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(forgedAlliancePrefs.getPath()).thenReturn(faDirectory.getRoot().toPath());
    when(applicationContext.getBean(GitCheckGameUpdateTask.class)).thenReturn(gameUpdateTask);

    faBinDirectory = faDirectory.getRoot().toPath().resolve("bin");
    Files.createDirectories(faBinDirectory);

    instance.postConstruct();
  }

  @Test
  public void testUpdateInBackgroundFaDirectoryUnspecified() throws Exception {
    when(forgedAlliancePrefs.getPath()).thenReturn(null);

    instance.updateInBackground(GameType.FAF.getString(), null, null, null);

    verifyZeroInteractions(instance.taskService);
  }

  @Test
  public void testUpdateInBackgroundThrowsException() throws Exception {
    GitGameUpdateTask task = mock(GitGameUpdateTask.class, withSettings().useConstructor());
    when(applicationContext.getBean(GitGameUpdateTask.class)).thenReturn(task);

    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new Exception("This exception mimicks that something went wrong"));

    when(taskService.submitTask(task)).thenReturn(future);

    future = instance.updateInBackground(GameType.FAF.getString(), null, null, null);

    future.get(TIMEOUT, TIMEOUT_UNIT);

    ArgumentCaptor<PersistentNotification> captor = ArgumentCaptor.forClass(PersistentNotification.class);
    verify(notificationService).addNotification(captor.capture());
    assertThat(captor.getValue().getSeverity(), is(Severity.WARN));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCheckForUpdatesInBackgroundPatchingIsNeeded() throws Exception {
    when(taskService.submitTask(any())).thenReturn(CompletableFuture.completedFuture(true));

    CompletableFuture<Void> future = instance.checkForUpdateInBackground();

    assertThat(future.get(TIMEOUT, TIMEOUT_UNIT), is(nullValue()));
    assertThat(future.isCompletedExceptionally(), is(false));
  }

  @Test
  public void testCheckForUpdatesInBackgroundThrowsException() throws Exception {
    GitCheckGameUpdateTask task = mock(GitCheckGameUpdateTask.class, withSettings().useConstructor());
    when(applicationContext.getBean(GitCheckGameUpdateTask.class)).thenReturn(task);

    CompletableFuture<Boolean> exceptionFuture = new CompletableFuture<>();
    exceptionFuture.completeExceptionally(new Exception("This exception mimicks that something went wrong"));

    when(taskService.submitTask(task)).thenReturn(exceptionFuture);

    CompletableFuture<Void> future = instance.checkForUpdateInBackground();

    future.get(TIMEOUT, TIMEOUT_UNIT);

    ArgumentCaptor<PersistentNotification> captor = ArgumentCaptor.forClass(PersistentNotification.class);
    verify(notificationService).addNotification(captor.capture());
    assertThat(captor.getValue().getSeverity(), is(Severity.WARN));
  }

  @Test
  public void testGuessInstallTypeRetail() throws Exception {
    assertTrue(Files.notExists(faBinDirectory.resolve(STEAM_API_DLL)));

    GitRepositoryGameUpdateService.InstallType installType = instance.guessInstallType();
    assertThat(installType, is(GitRepositoryGameUpdateService.InstallType.RETAIL));
  }

  @Test
  public void testGuessInstallTypeSteam() throws Exception {
    Files.createDirectories(faBinDirectory);
    Files.createFile(faBinDirectory.resolve(STEAM_API_DLL));

    GitRepositoryGameUpdateService.InstallType installType = instance.guessInstallType();
    assertThat(installType, is(GitRepositoryGameUpdateService.InstallType.STEAM));
  }
}
