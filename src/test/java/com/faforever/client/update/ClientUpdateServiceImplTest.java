package com.faforever.client.update;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.io.Bytes;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.task.TaskService;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.notification.Severity.INFO;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@RunWith(MockitoJUnitRunner.class)
public class ClientUpdateServiceImplTest {

  private ClientUpdateServiceImpl instance;

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

  @Before
  public void setUp() throws Exception {
    instance = new ClientUpdateServiceImpl(taskService, notificationService, i18n, platformService, applicationContext);

    doAnswer(invocation -> invocation.getArgument(0)).when(taskService).submitTask(any());
  }

  /**
   * Never version is available on server.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testCheckForUpdateInBackgroundUpdateAvailable() throws Exception {
    instance.currentVersion = new ComparableVersion("v0.4.8.0-alpha");

    CheckForUpdateTask taskMock = mock(CheckForUpdateTask.class, withSettings().useConstructor());

    when(applicationContext.getBean(CheckForUpdateTask.class)).thenReturn(taskMock);

    UpdateInfo updateInfo = new UpdateInfo("v0.4.8.1-alpha", "test.exe", new URL("http://www.example.com"), 56098816, new URL("http://www.example.com"));
    when(taskMock.getFuture()).thenReturn(CompletableFuture.completedFuture(updateInfo));

    instance.checkForUpdateInBackground();

    verify(taskService).submitTask(taskMock);

    ArgumentCaptor<PersistentNotification> captor = ArgumentCaptor.forClass(PersistentNotification.class);

    verify(notificationService).addNotification(captor.capture());
    PersistentNotification persistentNotification = captor.getValue();

    verify(i18n).get("clientUpdateAvailable.notification", "v0.4.8.1-alpha", Bytes.formatSize(56079360L, i18n.getLocale()));
    assertThat(persistentNotification.getSeverity(), is(INFO));
  }
}
