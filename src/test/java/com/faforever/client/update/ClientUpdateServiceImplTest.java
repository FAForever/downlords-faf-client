package com.faforever.client.update;

import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.update.ClientUpdateServiceImpl.InstallerExecutionException;
import com.faforever.commons.io.Bytes;
import com.google.common.eventbus.EventBus;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.update4j.Configuration;

import java.net.URL;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.notification.Severity.INFO;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ClientUpdateServiceImplTest extends ServiceTest {

  private ClientUpdateServiceImpl instance;

  @TempDir
  public Path fafBinDirectory;
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
  @Mock
  private EventBus eventBus;

  private Preferences preferences;

  @BeforeEach
  public void setUp() throws Exception {
    Configuration configuration = Configuration.builder().build();
    UpdateInfo normalUpdateInfo = new UpdateInfo("v0.4.9.1-alpha", new ComparableVersion("0.4.9.1-alpha"), Optional.empty(), configuration, 56098816, new URL("http://www.example.com"), false);
    UpdateInfo betaUpdateInfo = new UpdateInfo("v0.4.9.0-RC1", new ComparableVersion("0.4.9.0-RC1"), Optional.empty(), configuration, 56098816, new URL("http://www.example.com"), true);
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.setLatestRelease(new ClientConfiguration.ReleaseInfo());
    clientConfiguration.getLatestRelease().setVersion(new ComparableVersion("0.4.9.1-alpha"));
    preferences = PreferencesBuilder.create().defaultValues().get();

    doReturn(checkForReleaseUpdateTask).when(applicationContext).getBean(CheckForReleaseUpdateTask.class);
    doReturn(checkForBetaUpdateTask).when(applicationContext).getBean(CheckForBetaUpdateTask.class);
    doReturn(checkForReleaseUpdateTask).when(taskService).submitTask(checkForReleaseUpdateTask);
    doReturn(checkForBetaUpdateTask).when(taskService).submitTask(checkForBetaUpdateTask);
    doReturn(CompletableFuture.completedFuture(normalUpdateInfo)).when(checkForReleaseUpdateTask).getFuture();
    doReturn(CompletableFuture.completedFuture(betaUpdateInfo)).when(checkForBetaUpdateTask).getFuture();
    when(preferencesService.getPreferences()).thenReturn(preferences);

    instance = new ClientUpdateServiceImpl(taskService, notificationService, i18n, platformService, applicationContext, preferencesService, eventBus);
  }

  /**
   * Never version is available on server.
   */
  @Test
  public void testCheckForUpdateInBackgroundUpdateAvailable() {
    instance.currentVersion = new ComparableVersion("v0.4.8.0-alpha");

    preferences.setAutoUpdate(false);
    preferences.setPreReleaseCheckEnabled(false);
    instance.checkForUpdateInBackground();

    verify(taskService).submitTask(checkForReleaseUpdateTask);

    // The method used to trigger a notification but it should no longer
    verifyNoInteractions(notificationService);
  }

  /**
   * Newer prerelease version is available on server.
   */
  @Test
  public void testCheckForBetaUpdateInBackgroundUpdateAvailable() {
    instance.currentVersion = new ComparableVersion("v0.4.8.0-alpha");

    preferences.setAutoUpdate(false);
    preferences.setPreReleaseCheckEnabled(true);
    instance.checkForUpdateInBackground();

    verify(taskService).submitTask(checkForBetaUpdateTask);

    // The method used to trigger a notification but it should no longer
    verifyNoInteractions(notificationService);
  }
}
