package com.faforever.client.update;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.faforever.client.update.ClientUpdateServiceImpl.InstallerExecutionException;
import com.faforever.commons.io.Bytes;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.notification.Severity.INFO;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
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
  @Mock
  private CheckForUpdateTask checkForUpdateTask;

  private UpdateInfo updateInfo;
  private ClientConfiguration clientConfiguration;

  @Before
  public void setUp() throws Exception {
    updateInfo = new UpdateInfo("v0.4.8.1-alpha", "test.exe", new URL("http://www.example.com"), 56098816, new URL("http://www.example.com"));
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.setLatestRelease(new ClientConfiguration.ReleaseInfo());
    clientConfiguration.getLatestRelease().setVersion("v0.4.9.1-alpha");

    doReturn(checkForUpdateTask).when(applicationContext).getBean(CheckForUpdateTask.class);
    doReturn(checkForUpdateTask).when(taskService).submitTask(any(CheckForUpdateTask.class));
    doReturn(CompletableFuture.completedFuture(updateInfo)).when(checkForUpdateTask).getFuture();
    doReturn(clientConfiguration).when(preferencesService).getRemotePreferences();

    instance = new ClientUpdateServiceImpl(taskService, notificationService, i18n, platformService, applicationContext, preferencesService);
  }

  /**
   * Never version is available on server.
   */
  @Test
  public void testCheckForUpdateInBackgroundUpdateAvailable() throws Exception {
    instance.currentVersion = "v0.4.8.0-alpha";

    CheckForUpdateTask taskMock = mock(CheckForUpdateTask.class);

    instance.checkForRegularUpdateInBackground();

    verify(taskService).submitTask(checkForUpdateTask);

    ArgumentCaptor<PersistentNotification> captor = ArgumentCaptor.forClass(PersistentNotification.class);

    verify(notificationService).addNotification(captor.capture());
    PersistentNotification persistentNotification = captor.getValue();

    verify(i18n).get("clientUpdateAvailable.notification", "v0.4.8.1-alpha", Bytes.formatSize(56079360L, i18n.getUserSpecificLocale()));
    assertThat(persistentNotification.getSeverity(), is(INFO));
  }

  @Test
  public void testUnixExecutableBitIsSet() throws Exception {
    Path faExePath = fafBinDirectory.newFile("ForgedAlliance.exe").toPath();
    try {
      instance.install(faExePath);
    } catch (InstallerExecutionException e) {}
    verify(platformService).setUnixExecutableAndWritableBits(faExePath);
  }
}
