package com.faforever.client.notification;

import com.faforever.client.audio.AudioService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.collections.SetChangeListener;
import javafx.collections.SetChangeListener.Change;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PersistentNotificationsControllerTest extends AbstractPlainJavaFxTest {

  private PersistentNotificationsController instance;

  @Before
  public void setUp() throws Exception {
    instance = new PersistentNotificationsController();
    instance.audioService = mock(AudioService.class);
    instance.notificationService = mock(NotificationService.class);
    instance.uiService = mock(UiService.class);
    instance.persistentNotificationsRoot = new Pane();

    loadFxml("theme/persistent_notifications.fxml", clazz -> instance);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testPostConstruct() throws Exception {
    verify(instance.notificationService).getPersistentNotifications();
    verify(instance.notificationService).addPersistentNotificationListener(any(SetChangeListener.class));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertEquals(instance.persistentNotificationsRoot, instance.getRoot());
  }

  @Test
  public void testOnInfoNotificationAdded() throws Exception {
    CompletableFuture<Void> future = new CompletableFuture<>();
    doAnswer(
        invocation -> future.complete(null)
    ).when(instance.audioService).playInfoNotificationSound();

    onNotificationAdded(Severity.INFO);

    future.get(2, TimeUnit.SECONDS);
  }

  @SuppressWarnings("unchecked")
  private void onNotificationAdded(Severity severity) {
    PersistentNotificationController notificationController = mock(PersistentNotificationController.class);
    when(notificationController.getRoot()).thenReturn(new Pane());

    when(instance.uiService.loadFxml("theme/persistent_notification.fxml")).thenReturn(notificationController);

    ArgumentCaptor<SetChangeListener> argument = ArgumentCaptor.forClass(SetChangeListener.class);
    verify(instance.notificationService).addPersistentNotificationListener(argument.capture());

    SetChangeListener listener = argument.getValue();

    PersistentNotification notification = mock(PersistentNotification.class);
    when(notification.getSeverity()).thenReturn(severity);

    Change change = mock(Change.class);
    when(change.wasAdded()).thenReturn(true);
    when(change.getElementAdded()).thenReturn(notification);

    listener.onChanged(change);

    verify(notificationController).setNotification(notification);
  }

  @Test
  public void testOnWarnNotificationAdded() throws Exception {
    CompletableFuture<Void> future = new CompletableFuture<>();
    doAnswer(
        invocation -> future.complete(null)
    ).when(instance.audioService).playWarnNotificationSound();

    onNotificationAdded(Severity.WARN);

    future.get(2, TimeUnit.SECONDS);
  }

  @Test()
  public void testOnErrorNotificationAdded() throws Exception {
    CompletableFuture<Void> future = new CompletableFuture<>();
    doAnswer(
        invocation -> future.complete(null)
    ).when(instance.audioService).playErrorNotificationSound();

    onNotificationAdded(Severity.ERROR);

    future.get(2, TimeUnit.SECONDS);
  }
}
