package com.faforever.client.notification;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.notificationEvents.ShowImmediateNotificationEvent;
import com.faforever.client.notification.notificationEvents.ShowPersistentNotificationEvent;
import com.faforever.client.notification.notificationEvents.ShowTransientNotificationEvent;
import com.faforever.client.reporting.SupportService;
import javafx.collections.SetChangeListener;
import javafx.collections.SetChangeListener.Change;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NotificationServiceImplTest {

  private NotificationServiceImpl instance;

  @Mock
  private SupportService supportService;
  @Mock
  private I18n i18n;

  @Before
  public void setUp() throws Exception {
    instance = new NotificationServiceImpl(supportService, i18n);
  }

  @Test
  public void testAddNotificationPersistent() throws Exception {
    instance.onPersistentNotification(new ShowPersistentNotificationEvent(new PersistentNotification("text", Severity.INFO)));
  }

  @Test
  public void testAddNotificationImmediate() throws Exception {
    instance.onImmediateNotification(new ShowImmediateNotificationEvent(new ImmediateNotification("title", "text", Severity.INFO)));
  }

  @Test
  public void testAddNotificationTransient() throws Exception {
    instance.onTransientNotification(new ShowTransientNotificationEvent(new TransientNotification("title", "text")));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAddPersistentNotificationListener() throws Exception {
    SetChangeListener<PersistentNotification> listener = mock(SetChangeListener.class);

    instance.addPersistentNotificationListener(listener);
    instance.onPersistentNotification(new ShowPersistentNotificationEvent(mock(PersistentNotification.class)));

    WaitForAsyncUtils.waitForFxEvents();
    verify(listener).onChanged(any(Change.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAddTransientNotificationListener() throws Exception {
    OnTransientNotificationListener listener = mock(OnTransientNotificationListener.class);

    instance.addTransientNotificationListener(listener);

    TransientNotification notification = mock(TransientNotification.class);
    instance.onTransientNotification(new ShowTransientNotificationEvent(notification));

    WaitForAsyncUtils.waitForFxEvents();
    verify(listener).onTransientNotification(notification);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAddImmediateNotificationListener() throws Exception {
    OnImmediateNotificationListener listener = mock(OnImmediateNotificationListener.class);

    instance.addImmediateNotificationListener(listener);

    ImmediateNotification notification = mock(ImmediateNotification.class);
    instance.onImmediateNotification(new ShowImmediateNotificationEvent(notification));

    WaitForAsyncUtils.waitForFxEvents();
    verify(listener).onImmediateNotification(notification);
  }

  @Test
  public void testGetPersistentNotifications() throws Exception {
    assertThat(instance.getPersistentNotifications(), empty());

    PersistentNotification notification = mock(PersistentNotification.class);
    instance.onPersistentNotification(new ShowPersistentNotificationEvent(notification));

    WaitForAsyncUtils.waitForFxEvents();
    assertThat(instance.getPersistentNotifications(), hasSize(1));
    assertSame(notification, instance.getPersistentNotifications().iterator().next());
  }

  @Test
  public void testRemoveNotification() throws Exception {
    assertThat(instance.getPersistentNotifications(), empty());

    PersistentNotification notification1 = mock(PersistentNotification.class);
    PersistentNotification notification2 = mock(PersistentNotification.class);

    instance.onPersistentNotification(new ShowPersistentNotificationEvent(notification1));
    instance.onPersistentNotification(new ShowPersistentNotificationEvent(notification2));

    WaitForAsyncUtils.waitForFxEvents();
    assertThat(instance.getPersistentNotifications(), hasSize(2));

    instance.removeNotification(notification2);

    assertThat(instance.getPersistentNotifications(), hasSize(1));
    assertSame(notification1, instance.getPersistentNotifications().iterator().next());

    instance.removeNotification(notification1);

    assertThat(instance.getPersistentNotifications(), empty());
  }
}
