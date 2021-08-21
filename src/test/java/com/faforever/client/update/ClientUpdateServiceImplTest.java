package com.faforever.client.update;

import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.update.ClientUpdateServiceImpl.InstallerExecutionException;
import com.faforever.commons.io.Bytes;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.notification.Severity.INFO;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
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
  @Mock
  private CheckForUpdateTask checkForUpdateTask;
  @Mock
  private CheckForBetaUpdateTask checkForBetaUpdateTask;
  @Mock
  private EventBus eventBus;

  private Preferences preferences;

  @BeforeEach
  public void setUp() throws Exception {
    UpdateInfo normalUpdateInfo = new UpdateInfo("v0.4.9.1-alpha", "test.exe", new URL("http://www.example.com"), 56098816, new URL("http://www.example.com"), false);
    UpdateInfo betaUpdateInfo = new UpdateInfo("v0.4.9.0-RC1", "test.exe", new URL("http://www.example.com"), 56098816, new URL("http://www.example.com"), true);
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.setLatestRelease(new ClientConfiguration.ReleaseInfo());
    clientConfiguration.getLatestRelease().setVersion("v0.4.9.1-alpha");
    preferences = PreferencesBuilder.create().defaultValues().get();


    doReturn(checkForUpdateTask).when(applicationContext).getBean(CheckForUpdateTask.class);
    doReturn(checkForBetaUpdateTask).when(applicationContext).getBean(CheckForBetaUpdateTask.class);
    doReturn(checkForUpdateTask).when(taskService).submitTask(any(CheckForUpdateTask.class));
    doReturn(checkForBetaUpdateTask).when(taskService).submitTask(any(CheckForBetaUpdateTask.class));
    doReturn(CompletableFuture.completedFuture(normalUpdateInfo)).when(checkForUpdateTask).getFuture();
    doReturn(CompletableFuture.completedFuture(betaUpdateInfo)).when(checkForBetaUpdateTask).getFuture();
    when(preferencesService.getPreferences()).thenReturn(preferences);

    instance = new ClientUpdateServiceImpl(taskService, notificationService, i18n, platformService, applicationContext, preferencesService, eventBus);
  }

  /**
   * Never version is available on server.
   */
  @Test
  public void testCheckForUpdateInBackgroundUpdateAvailable() throws Exception {
    instance.currentVersion = "v0.4.8.0-alpha";

    preferences.setPreReleaseCheckEnabled(false);
    instance.checkForUpdateInBackground();

    verify(taskService).submitTask(checkForUpdateTask);

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
  public void testCheckForBetaUpdateInBackgroundUpdateAvailable() throws Exception {
    instance.currentVersion = "v0.4.8.0-alpha";

    preferences.setPreReleaseCheckEnabled(true);
    instance.checkForUpdateInBackground();

    verify(taskService).submitTask(checkForUpdateTask);

    ArgumentCaptor<PersistentNotification> captor = ArgumentCaptor.forClass(PersistentNotification.class);

    verify(notificationService).addNotification(captor.capture());
    PersistentNotification persistentNotification = captor.getValue();

    verify(i18n).get("clientUpdateAvailable.prereleaseNotification", "v0.4.9.1-alpha", Bytes.formatSize(56079360L, i18n.getUserSpecificLocale()));
    assertThat(persistentNotification.getSeverity(), is(INFO));
  }

  @Test
  public void testUnixExecutableBitIsSet() throws Exception {
    Path faExePath = Files.createFile(fafBinDirectory.resolve("ForgedAlliance.exe"));
    try {
      instance.install(faExePath);
    } catch (InstallerExecutionException ignored) {}
    verify(platformService).setUnixExecutableAndWritableBits(faExePath);
  }
}
