package com.faforever.client.notification;

import com.faforever.client.audio.AudioController;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.collections.SetChangeListener;
import javafx.collections.SetChangeListener.Change;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
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
    instance.audioController = mock(AudioController.class);
    instance.notificationService = mock(NotificationService.class);
    instance.applicationContext = mock(ApplicationContext.class);
    instance.persistentNotificationsRoot = new Pane();

    instance.postConstruct();
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
    ).when(instance.audioController).playInfoNotificationSound();

    onNotificationAdded(Severity.INFO);

    future.get(100, TimeUnit.MILLISECONDS);
  }

  @SuppressWarnings("unchecked")
  private void onNotificationAdded(Severity severity) {
    PersistentNotificationController notificationController = mock(PersistentNotificationController.class);
    when(notificationController.getRoot()).thenReturn(new Pane());

    when(instance.applicationContext.getBean(PersistentNotificationController.class)).thenReturn(notificationController);

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
    ).when(instance.audioController).playWarnNotificationSound();

    onNotificationAdded(Severity.WARN);

    future.get(100, TimeUnit.MILLISECONDS);
  }

  @Test()
  public void testOnErrorNotificationAdded() throws Exception {
    CompletableFuture<Void> future = new CompletableFuture<>();
    doAnswer(
        invocation -> future.complete(null)
    ).when(instance.audioController).playErrorNotificationSound();

    onNotificationAdded(Severity.ERROR);

    future.get(100, TimeUnit.MILLISECONDS);
  }
}
