package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.io.Bytes;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.notification.Severity.INFO;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class ClientUpdateServiceImplTest extends AbstractPlainJavaFxTest {

  private ClientUpdateServiceImpl instance;

  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private TaskService taskService;
  @Mock
  private ApplicationContext applicationContext;

  @Before
  public void setUp() throws Exception {
    instance = new ClientUpdateServiceImpl();
    instance.notificationService = notificationService;
    instance.i18n = i18n;
    instance.taskService = taskService;
    instance.applicationContext = applicationContext;
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
    when(taskService.submitTask(taskMock)).thenReturn(CompletableFuture.completedFuture(updateInfo));

    instance.checkForUpdateInBackground();

    verify(taskService).submitTask(taskMock);

    ArgumentCaptor<PersistentNotification> captor = ArgumentCaptor.forClass(PersistentNotification.class);

    verify(notificationService).addNotification(captor.capture());
    PersistentNotification persistentNotification = captor.getValue();

    verify(i18n).get("clientUpdateAvailable.notification", "v0.4.8.1-alpha", Bytes.formatSize(56079360L));
    assertThat(persistentNotification.getSeverity(), is(INFO));
  }
}
