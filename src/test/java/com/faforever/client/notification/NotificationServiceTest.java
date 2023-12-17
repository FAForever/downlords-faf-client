package com.faforever.client.notification;

import com.faforever.client.i18n.I18n;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.theme.UiService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NotificationServiceTest extends ServiceTest {

  @InjectMocks
  private NotificationService instance;

  @Mock
  private ReportingService reportingService;
  @Mock
  private I18n i18n;
  @Mock
  private ToastDisplayer toastDisplayer;
  @Mock
  private UiService uiService;

  @Test
  public void testAddTransientNotification() throws Exception {
    instance.addNotification(new TransientNotification("title", "text"));

    verify(toastDisplayer).addNotification(any());
  }

  @Test
  public void testAddImmediateNotification() throws Exception {
    ImmediateNotificationController notificationController = mock(ImmediateNotificationController.class);
    when(uiService.loadFxml("theme/immediate_notification.fxml")).thenReturn(notificationController);

    instance.addNotification(new ImmediateNotification("", "", Severity.INFO));

    verify(notificationController).setNotification(any());
  }

  @Test
  public void testAddServerNotification() throws Exception {
    ServerNotificationController notificationController = mock(ServerNotificationController.class);
    when(uiService.loadFxml("theme/server_notification.fxml")).thenReturn(notificationController);

    instance.addNotification(new ImmediateNotification("", "", Severity.INFO));

    verify(notificationController).setNotification(any());
  }

  @Test
  public void testPersistentNotification() throws Exception {
    assertThat(instance.getPersistentNotifications(), empty());

    PersistentNotification notification = mock(PersistentNotification.class);
    instance.addNotification(notification);

    assertThat(instance.getPersistentNotifications(), hasSize(1));
    assertSame(notification, instance.getPersistentNotifications().iterator().next());
  }

  @Test
  public void testRemoveNotification() throws Exception {
    assertThat(instance.getPersistentNotifications(), empty());

    PersistentNotification notification1 = mock(PersistentNotification.class);
    PersistentNotification notification2 = mock(PersistentNotification.class);

    instance.addNotification(notification1);
    instance.addNotification(notification2);

    assertThat(instance.getPersistentNotifications(), hasSize(2));

    instance.removeNotification(notification2);

    assertThat(instance.getPersistentNotifications(), hasSize(1));
    assertSame(notification1, instance.getPersistentNotifications().iterator().next());

    instance.removeNotification(notification1);

    assertThat(instance.getPersistentNotifications(), empty());
  }
}
