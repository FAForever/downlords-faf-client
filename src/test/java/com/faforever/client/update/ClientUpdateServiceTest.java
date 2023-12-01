package com.faforever.client.update;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.os.OsPosix;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.update.ClientUpdateService.InstallerExecutionException;
import com.faforever.client.user.LoginService;
import com.faforever.commons.io.Bytes;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.springframework.beans.factory.ObjectFactory;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.notification.Severity.INFO;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClientUpdateServiceTest extends ServiceTest {

  private ClientUpdateService instance;

  @TempDir
  public Path fafBinDirectory;
  @Spy
  private OperatingSystem operatingSystem = new OsPosix();
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private TaskService taskService;
  @Mock
  private PlatformService platformService;
  @Mock
  private ObjectFactory<CheckForBetaUpdateTask> checkForBetaUpdateTaskFactory;
  @Mock
  private ObjectFactory<CheckForUpdateTask> checkForUpdateTaskFactory;
  @Mock
  private ObjectFactory<DownloadUpdateTask> downloadUpdateTaskFactory;
  @Mock
  private LoginService loginService;

  @Mock
  private CheckForUpdateTask checkForUpdateTask;
  @Mock
  private CheckForBetaUpdateTask checkForBetaUpdateTask;
  @Spy
  private Preferences preferences;

  private final BooleanProperty loggedIn = new SimpleBooleanProperty();

  @BeforeEach
  public void setUp() throws Exception {
    UpdateInfo normalUpdateInfo = new UpdateInfo("v0.4.9.1-alpha", "test.exe", new URL("http://www.example.com"), 56098816, new URL("http://www.example.com"), false);
    UpdateInfo betaUpdateInfo = new UpdateInfo("v0.4.9.0-RC1", "test.exe", new URL("http://www.example.com"), 56098816, new URL("http://www.example.com"), true);

    when(loginService.loggedInProperty()).thenReturn(loggedIn);
    when(checkForUpdateTaskFactory.getObject()).thenReturn(checkForUpdateTask);
    when(checkForBetaUpdateTaskFactory.getObject()).thenReturn(checkForBetaUpdateTask);
    when(taskService.submitTask(any(CheckForUpdateTask.class))).thenReturn(checkForUpdateTask);
    when(taskService.submitTask(any(CheckForBetaUpdateTask.class))).thenReturn(checkForBetaUpdateTask);
    when(checkForUpdateTask.getFuture()).thenReturn(CompletableFuture.completedFuture(normalUpdateInfo));
    when(checkForBetaUpdateTask.getFuture()).thenReturn(CompletableFuture.completedFuture(betaUpdateInfo));

    instance = new ClientUpdateService(operatingSystem, taskService, notificationService, i18n, platformService, loginService, preferences, checkForBetaUpdateTaskFactory, checkForUpdateTaskFactory, downloadUpdateTaskFactory);

    instance.afterPropertiesSet();
  }

  /**
   * Never version is available on server.
   */
  @Test
  public void testCheckForUpdateInBackgroundUpdateAvailable() throws Exception {
    preferences.setPreReleaseCheckEnabled(false);

    try (MockedStatic<Version> mockedVersion = mockStatic(Version.class)) {
      mockedVersion.when(Version::getCurrentVersion).thenReturn("v0.4.8.0-alpha");
      mockedVersion.when(() -> Version.shouldUpdate(anyString(), anyString())).thenCallRealMethod();
      mockedVersion.when(() -> Version.removePrefix(anyString())).thenCallRealMethod();
      mockedVersion.when(() -> Version.followsSemverPattern(anyString())).thenCallRealMethod();
      instance.checkForUpdateInBackground();
    }

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
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testCheckForBetaUpdateInBackgroundUpdateAvailable(boolean supportsUpdateInstall) throws Exception {
    when(operatingSystem.supportsUpdateInstall()).thenReturn(supportsUpdateInstall);

    preferences.setPreReleaseCheckEnabled(true);

    try (MockedStatic<Version> mockedVersion = mockStatic(Version.class)) {
      mockedVersion.when(Version::getCurrentVersion).thenReturn("v0.4.8.0-alpha");
      mockedVersion.when(() -> Version.shouldUpdate(anyString(), anyString())).thenCallRealMethod();
      mockedVersion.when(() -> Version.removePrefix(anyString())).thenCallRealMethod();
      mockedVersion.when(() -> Version.followsSemverPattern(anyString())).thenCallRealMethod();
      instance.checkForUpdateInBackground();
    }

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
    } catch (InstallerExecutionException ignored) {
    }
    verify(platformService).setUnixExecutableAndWritableBits(faExePath);
  }
}
