package com.faforever.client.connectivity;

import com.faforever.client.fx.HostService;
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
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ConnectivityServiceImplTest extends AbstractPlainJavaFxTest {

  private ConnectivityServiceImpl instance;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private TaskService taskService;
  @Mock
  private I18n i18n;
  @Mock
  private HostService hostService;
  @Mock
  private Preferences preferences;
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private NotificationService notificationService;

  @Before
  public void setUp() throws Exception {
    instance = new ConnectivityServiceImpl();
    instance.taskService = taskService;
    instance.preferencesService = preferencesService;
    i18n = instance.i18n = i18n;
    instance.hostService = hostService;
    instance.applicationContext = applicationContext;
    instance.notificationService = notificationService;

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(forgedAlliancePrefs.getPort()).thenReturn(6112);
  }

  @Test
  public void testGetConnectivityStateInitiallyUnknown() throws Exception {
    assertThat(instance.getConnectivityState(), is(ConnectivityState.UNKNOWN));
  }

  @Test
  public void testCheckGamePortInBackgroundBlockedTriggersNotification() throws Exception {
    ConnectivityCheckTask connectivityCheckTask = mock(ConnectivityCheckTask.class);
    when(applicationContext.getBean(ConnectivityCheckTask.class)).thenReturn(connectivityCheckTask);
    when(taskService.submitTask(connectivityCheckTask)).thenReturn(CompletableFuture.completedFuture(ConnectivityState.BLOCKED));

    CompletableFuture<ConnectivityState> result = instance.checkGamePortInBackground();
    assertThat(result.get(1, TimeUnit.SECONDS), is(ConnectivityState.BLOCKED));

    verify(taskService).submitTask(connectivityCheckTask);
    verify(notificationService).addNotification(any(PersistentNotification.class));
    assertThat(instance.getConnectivityState(), is(ConnectivityState.BLOCKED));
  }

  @Test
  public void testCheckGamePortInBackgroundStunDoesntTriggerNotification() throws Exception {
    ConnectivityCheckTask connectivityCheckTask = mock(ConnectivityCheckTask.class);
    when(applicationContext.getBean(ConnectivityCheckTask.class)).thenReturn(connectivityCheckTask);
    when(taskService.submitTask(connectivityCheckTask)).thenReturn(CompletableFuture.completedFuture(ConnectivityState.STUN));

    CompletableFuture<ConnectivityState> result = instance.checkGamePortInBackground();
    assertThat(result.get(1, TimeUnit.SECONDS), is(ConnectivityState.STUN));

    verify(taskService).submitTask(connectivityCheckTask);
    verifyZeroInteractions(notificationService);
    assertThat(instance.getConnectivityState(), is(ConnectivityState.STUN));
  }

  @Test
  public void testCheckGamePortInBackgroundPublicDoesntTriggerNotification() throws Exception {
    ConnectivityCheckTask connectivityCheckTask = mock(ConnectivityCheckTask.class);
    when(applicationContext.getBean(ConnectivityCheckTask.class)).thenReturn(connectivityCheckTask);
    when(taskService.submitTask(connectivityCheckTask)).thenReturn(CompletableFuture.completedFuture(ConnectivityState.PUBLIC));

    CompletableFuture<ConnectivityState> result = instance.checkGamePortInBackground();
    assertThat(result.get(1, TimeUnit.SECONDS), is(ConnectivityState.PUBLIC));

    verify(taskService).submitTask(connectivityCheckTask);
    verifyZeroInteractions(notificationService);
    assertThat(instance.getConnectivityState(), is(ConnectivityState.PUBLIC));
  }

  @Test
  public void testCheckGamePortInBackgroundFailsTriggersNotification() throws Exception {
    ArgumentCaptor<PersistentNotification> persistentNotificationCaptor = ArgumentCaptor.forClass(PersistentNotification.class);
    CompletableFuture<ConnectivityState> completableFuture = new CompletableFuture<>();
    completableFuture.completeExceptionally(new Exception("junit test exception"));

    ConnectivityCheckTask connectivityCheckTask = mock(ConnectivityCheckTask.class);
    when(applicationContext.getBean(ConnectivityCheckTask.class)).thenReturn(connectivityCheckTask);
    when(taskService.submitTask(connectivityCheckTask)).thenReturn(completableFuture);

    CompletableFuture<ConnectivityState> result = instance.checkGamePortInBackground();
    assertThat(result.get(1, TimeUnit.SECONDS), is(nullValue()));

    verify(taskService).submitTask(connectivityCheckTask);
    verify(notificationService).addNotification(persistentNotificationCaptor.capture());
    assertThat(instance.getConnectivityState(), is(nullValue()));

    assertThat(persistentNotificationCaptor.getValue().getSeverity(), is(Severity.WARN));
    assertThat(persistentNotificationCaptor.getValue().getActions(), hasSize(1));
  }
}
