package com.faforever.client.update;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.faforever.commons.io.Bytes;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.update4j.Configuration;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.notification.Severity.INFO;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ClientUpdateServiceImplTest {

  private ClientUpdateServiceImpl instance;

  @Rule
  public TemporaryFolder fafBinDirectory = new TemporaryFolder();
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private TaskService taskService;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private PlatformService platformService;
  @Mock
  private PreferencesService preferencesService;
  private final Preferences preferences = new Preferences();
  @Mock
  private CheckForBetaUpdateTask checkForBetaUpdateTask;
  @Mock
  private CheckForReleaseUpdateTask checkForReleaseUpdateTask;

  @Before
  public void setUp() throws Exception {
    Configuration configuration = Configuration.builder().build();
    UpdateInfo normalUpdateInfo = new UpdateInfo("v0.4.9.1-alpha", new ComparableVersion("0.4.9.1-alpha"), configuration, 56098816, new URL("http://www.example.com"), false);
    UpdateInfo betaUpdateInfo = new UpdateInfo("v0.4.9.0-RC1", new ComparableVersion("0.4.9.0-RC1"), configuration, 56098816, new URL("http://www.example.com"), true);
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.setLatestRelease(new ClientConfiguration.ReleaseInfo());
    clientConfiguration.getLatestRelease().setVersion(new ComparableVersion("0.4.9.1-alpha"));

    doReturn(checkForReleaseUpdateTask).when(applicationContext).getBean(CheckForReleaseUpdateTask.class);
    doReturn(checkForBetaUpdateTask).when(applicationContext).getBean(CheckForBetaUpdateTask.class);
    doReturn(checkForReleaseUpdateTask).when(taskService).submitTask(checkForReleaseUpdateTask);
    doReturn(checkForBetaUpdateTask).when(taskService).submitTask(checkForBetaUpdateTask);
    doReturn(CompletableFuture.completedFuture(normalUpdateInfo)).when(checkForReleaseUpdateTask).getFuture();
    doReturn(CompletableFuture.completedFuture(betaUpdateInfo)).when(checkForBetaUpdateTask).getFuture();
    doReturn(preferences).when(preferencesService).getPreferences();

    instance = new ClientUpdateServiceImpl(taskService, notificationService, i18n, platformService, applicationContext, preferencesService);
  }

  /**
   * Never version is available on server.
   */
  @Test
  public void testCheckForUpdateInBackgroundUpdateAvailable() {
    instance.currentVersion = new ComparableVersion("v0.4.8.0-alpha");

    preferences.setAutoUpdate(false);
    preferences.setPrereleaseCheckEnabled(false);
    instance.checkForUpdateInBackground();

    verify(taskService).submitTask(checkForReleaseUpdateTask);

    ArgumentCaptor<PersistentNotification> captor = ArgumentCaptor.forClass(PersistentNotification.class);

    verify(notificationService).addNotification(captor.capture());
    PersistentNotification persistentNotification = captor.getValue();

    verify(i18n).get("clientUpdateAvailable.notification", "v0.4.9.0-RC1", Bytes.formatSize(56079360L, i18n.getUserSpecificLocale()));
    assertThat(persistentNotification.getSeverity(), is(INFO));
  }

  /**
   * Newer prerelease version is available on server.
   */
  @Test
  public void testCheckForBetaUpdateInBackgroundUpdateAvailable() {
    instance.currentVersion = new ComparableVersion("v0.4.8.0-alpha");

    preferences.setAutoUpdate(false);
    preferences.setPrereleaseCheckEnabled(true);
    instance.checkForUpdateInBackground();

    verify(taskService).submitTask(checkForBetaUpdateTask);

    ArgumentCaptor<PersistentNotification> captor = ArgumentCaptor.forClass(PersistentNotification.class);

    verify(notificationService).addNotification(captor.capture());
    PersistentNotification persistentNotification = captor.getValue();

    verify(i18n).get("clientUpdateAvailable.prereleaseNotification", "v0.4.9.1-alpha", Bytes.formatSize(56079360L, i18n.getUserSpecificLocale()));
    assertThat(persistentNotification.getSeverity(), is(INFO));
  }
}
