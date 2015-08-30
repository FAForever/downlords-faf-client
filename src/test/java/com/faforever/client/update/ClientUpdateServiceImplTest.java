package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.util.Bytes;
import com.faforever.client.util.Callback;
import com.google.common.io.CharStreams;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.core.env.Environment;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.faforever.client.notification.Severity.INFO;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ClientUpdateServiceImplTest extends AbstractPlainJavaFxTest {

  private ClientUpdateServiceImpl instance;

  @Mock
  private Environment environment;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private TaskService taskService;

  private ServerSocket fafLobbyServerSocket;

  @Before
  public void setUp() throws Exception {
    instance = new ClientUpdateServiceImpl();
    instance.environment = environment;
    instance.notificationService = notificationService;
    instance.i18n = i18n;
    instance.taskService = taskService;
  }

  @After
  public void tearDown() throws Exception {
    if (fafLobbyServerSocket != null) {
      fafLobbyServerSocket.close();
    }
  }

  @Test
  public void testGetUpdateIsNewer() throws Exception {
    mockTaskService();
    startFakeGitHubApiServer();

    int port = fafLobbyServerSocket.getLocalPort();
    when(environment.getProperty("github.releases.url")).thenReturn("http://localhost:" + port);
    when(environment.getProperty("github.releases.timeout", int.class)).thenReturn(100);

    instance.checkForUpdateInBackground();

    ArgumentCaptor<PersistentNotification> captor = ArgumentCaptor.forClass(PersistentNotification.class);

    verify(notificationService).addNotification(captor.capture());
    PersistentNotification persistentNotification = captor.getValue();

    verify(i18n).get("clientUpdateAvailable.notification", "v0.4.7-alpha", Bytes.formatSize(56079360L));
    assertThat(persistentNotification.getSeverity(), is(INFO));
  }

  @SuppressWarnings("unchecked")
  private void mockTaskService() {
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
    }).when(taskService).submitTask(any(), any(), any());
  }

  private void startFakeGitHubApiServer() throws IOException {
    fafLobbyServerSocket = new ServerSocket(0);
    System.out.println("Fake server listening on " + fafLobbyServerSocket.getLocalPort());

    WaitForAsyncUtils.async(() -> {
      try (Socket socket = fafLobbyServerSocket.accept();
           Reader sampleReader = new InputStreamReader(getClass().getResourceAsStream("/sample-github-releases-response.txt"));
           OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream())) {

        String response = CharStreams.toString(sampleReader);

        outputStreamWriter.write(response);
        outputStreamWriter.flush();

        Thread.sleep(500);
      } catch (InterruptedException | IOException e) {
        System.out.println("Closing fake GitHub HTTP server: " + e.getMessage());
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testGetUpdateIsOlder() throws Exception {
    instance.currentVersion = new ComparableVersion("v1.0.0");
    mockTaskService();
    startFakeGitHubApiServer();

    int port = fafLobbyServerSocket.getLocalPort();
    when(environment.getProperty("github.releases.url")).thenReturn("http://localhost:" + port);
    when(environment.getProperty("github.releases.timeout", int.class)).thenReturn(100);

    instance.checkForUpdateInBackground();

    verifyZeroInteractions(notificationService);
  }
}
