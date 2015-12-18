package com.faforever.client.connectivity;

import com.faforever.client.fx.HostService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.relay.ConnectivityStateMessage;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.util.SocketUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
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
  @Mock
  private FafService fafService;
  private IntegerProperty portProperty;

  @Before
  public void setUp() throws Exception {
    instance = new ConnectivityServiceImpl();
    instance.taskService = taskService;
    instance.preferencesService = preferencesService;
    i18n = instance.i18n = i18n;
    instance.hostService = hostService;
    instance.applicationContext = applicationContext;
    instance.notificationService = notificationService;
    instance.fafService = fafService;

    portProperty = new SimpleIntegerProperty(SocketUtils.findAvailableUdpPort());

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(forgedAlliancePrefs.getPort()).thenReturn(portProperty.get());
    when(forgedAlliancePrefs.portProperty()).thenReturn(portProperty);
  }

  @Test
  public void testGetConnectivityStateInitiallyUnknown() throws Exception {
    assertThat(instance.getConnectivityState(), is(ConnectivityState.UNKNOWN));
  }

  @Test
  public void testCheckGamePortInBackgroundBlockedTriggersNotification() throws Exception {
    ConnectivityCheckTask connectivityCheckTask = mock(ConnectivityCheckTask.class);
    when(applicationContext.getBean(ConnectivityCheckTask.class)).thenReturn(connectivityCheckTask);
    when(taskService.submitTask(connectivityCheckTask)).thenReturn(
        CompletableFuture.completedFuture(new ConnectivityStateMessage(ConnectivityState.BLOCKED))
    );

    UpnpPortForwardingTask upnpPortForwardingTask = mockUpnpPortForwardingTask();

    instance.checkConnectivity().get(1, TimeUnit.SECONDS);
    assertThat(instance.getConnectivityState(), is(ConnectivityState.BLOCKED));

    verify(taskService).submitTask(connectivityCheckTask);
    verify(connectivityCheckTask).setPublicPort(any());
    verify(taskService).submitTask(upnpPortForwardingTask);
    verify(upnpPortForwardingTask).setPort(anyInt());
    verify(notificationService).addNotification(any(PersistentNotification.class));
    assertThat(instance.getConnectivityState(), is(ConnectivityState.BLOCKED));
  }

  private UpnpPortForwardingTask mockUpnpPortForwardingTask() {
    UpnpPortForwardingTask upnpPortForwardingTask = mock(UpnpPortForwardingTask.class);
    when(applicationContext.getBean(UpnpPortForwardingTask.class)).thenReturn(upnpPortForwardingTask);
    when(taskService.submitTask(upnpPortForwardingTask)).thenReturn(CompletableFuture.completedFuture(null));
    return upnpPortForwardingTask;
  }

  @Test
  public void testCheckGamePortInBackgroundStunDoesntTriggerNotification() throws Exception {
    ConnectivityCheckTask connectivityCheckTask = mock(ConnectivityCheckTask.class);
    when(applicationContext.getBean(ConnectivityCheckTask.class)).thenReturn(connectivityCheckTask);
    when(taskService.submitTask(connectivityCheckTask)).thenReturn(
        CompletableFuture.completedFuture(new ConnectivityStateMessage(ConnectivityState.STUN))
    );

    UpnpPortForwardingTask upnpPortForwardingTask = mockUpnpPortForwardingTask();

    instance.checkConnectivity().get(1, TimeUnit.SECONDS);
    assertThat(instance.getConnectivityState(), is(ConnectivityState.STUN));

    verify(taskService).submitTask(connectivityCheckTask);
    verify(taskService).submitTask(upnpPortForwardingTask);
    verify(upnpPortForwardingTask).setPort(anyInt());
    verifyZeroInteractions(notificationService);
    assertThat(instance.getConnectivityState(), is(ConnectivityState.STUN));
  }

  @Test
  public void testCheckGamePortInBackgroundPublicDoesntTriggerNotification() throws Exception {
    ConnectivityCheckTask connectivityCheckTask = mock(ConnectivityCheckTask.class);
    when(applicationContext.getBean(ConnectivityCheckTask.class)).thenReturn(connectivityCheckTask);
    when(taskService.submitTask(connectivityCheckTask)).thenReturn(
        CompletableFuture.completedFuture(new ConnectivityStateMessage(ConnectivityState.PUBLIC))
    );

    UpnpPortForwardingTask upnpPortForwardingTask = mockUpnpPortForwardingTask();

    instance.checkConnectivity().get(1, TimeUnit.SECONDS);

    verify(taskService).submitTask(connectivityCheckTask);
    verify(taskService).submitTask(upnpPortForwardingTask);
    verify(upnpPortForwardingTask).setPort(anyInt());
    verifyZeroInteractions(notificationService);
    assertThat(instance.getConnectivityState(), is(ConnectivityState.PUBLIC));
  }

  @Test
  public void testCheckGamePortInBackgroundFailsTriggersNotification() throws Exception {
    ArgumentCaptor<PersistentNotification> persistentNotificationCaptor = ArgumentCaptor.forClass(PersistentNotification.class);
    CompletableFuture<ConnectivityStateMessage> completableFuture = new CompletableFuture<>();
    completableFuture.completeExceptionally(new Exception("junit test exception"));

    ConnectivityCheckTask connectivityCheckTask = mock(ConnectivityCheckTask.class);
    when(applicationContext.getBean(ConnectivityCheckTask.class)).thenReturn(connectivityCheckTask);
    when(taskService.submitTask(connectivityCheckTask)).thenReturn(completableFuture);

    UpnpPortForwardingTask upnpPortForwardingTask = mockUpnpPortForwardingTask();

    instance.checkConnectivity().get(1, TimeUnit.SECONDS);

    verify(taskService).submitTask(connectivityCheckTask);
    verify(taskService).submitTask(upnpPortForwardingTask);
    verify(upnpPortForwardingTask).setPort(anyInt());
    verify(notificationService).addNotification(persistentNotificationCaptor.capture());
    assertThat(instance.getConnectivityState(), is(ConnectivityState.UNKNOWN));

    assertThat(persistentNotificationCaptor.getValue().getSeverity(), is(Severity.WARN));
    assertThat(persistentNotificationCaptor.getValue().getActions(), hasSize(1));
  }
}
