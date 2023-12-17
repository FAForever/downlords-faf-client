package com.faforever.client.notification;

import com.faforever.client.audio.AudioService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PersistentNotificationsControllerTest extends PlatformTest {

  @InjectMocks
  private PersistentNotificationsController instance;

  @Mock
  private NotificationService notificationService;
  @Mock
  private AudioService audioService;
  @Mock
  private UiService uiService;

  private ObservableSet<PersistentNotification> persistentNotifications;

  @BeforeEach
  public void setUp() throws Exception {
    persistentNotifications = FXCollections.observableSet();
    when(notificationService.getPersistentNotifications()).thenReturn(persistentNotifications);

    instance.persistentNotificationsRoot = new Pane();

    loadFxml("theme/persistent_notifications.fxml", clazz -> instance);
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
    ).when(audioService).playInfoNotificationSound();

    onNotificationAdded(Severity.INFO);

    future.get(2, TimeUnit.SECONDS);
  }

  @SuppressWarnings("unchecked")
  private void onNotificationAdded(Severity severity) {
    PersistentNotificationController notificationController = mock(PersistentNotificationController.class);
    when(notificationController.getRoot()).thenReturn(new Pane());

    when(uiService.loadFxml("theme/persistent_notification.fxml")).thenReturn(notificationController);

    PersistentNotification notification = new PersistentNotification("", severity);

    persistentNotifications.add(notification);

    verify(notificationController).setNotification(notification);
  }

  @Test
  public void testOnWarnNotificationAdded() throws Exception {
    CompletableFuture<Void> future = new CompletableFuture<>();
    doAnswer(
        invocation -> future.complete(null)
    ).when(audioService).playWarnNotificationSound();

    onNotificationAdded(Severity.WARN);

    future.get(2, TimeUnit.SECONDS);
  }

  @Test()
  public void testOnErrorNotificationAdded() throws Exception {
    CompletableFuture<Void> future = new CompletableFuture<>();
    doAnswer(
        invocation -> future.complete(null)
    ).when(audioService).playErrorNotificationSound();

    onNotificationAdded(Severity.ERROR);

    future.get(2, TimeUnit.SECONDS);
  }
}
