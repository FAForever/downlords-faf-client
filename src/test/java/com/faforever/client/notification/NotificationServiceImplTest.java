package com.faforever.client.notification;

import javafx.collections.SetChangeListener;
import javafx.collections.SetChangeListener.Change;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NotificationServiceImplTest {

  private NotificationServiceImpl instance;

  @Before
  public void setUp() throws Exception {
    instance = new NotificationServiceImpl();
  }

  @Test
  public void testAddNotificationPersistent() throws Exception {
    instance.addNotification(new PersistentNotification("text", Severity.INFO));
  }

  @Test
  public void testAddNotificationImmediate() throws Exception {
    instance.addNotification(new ImmediateNotification("title", "text", Severity.INFO));
  }

  @Test
  public void testAddNotificationTransient() throws Exception {
    instance.addNotification(new TransientNotification("title", "text"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAddPersistentNotificationListener() throws Exception {
    SetChangeListener<PersistentNotification> listener = mock(SetChangeListener.class);

    instance.addPersistentNotificationListener(listener);
    instance.addNotification(mock(PersistentNotification.class));

    verify(listener).onChanged(any(Change.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAddTransientNotificationListener() throws Exception {
    OnTransientNotificationListener listener = mock(OnTransientNotificationListener.class);

    instance.addTransientNotificationListener(listener);

    TransientNotification notification = mock(TransientNotification.class);
    instance.addNotification(notification);

    verify(listener).onTransientNotification(notification);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAddImmediateNotificationListener() throws Exception {
    OnImmediateNotificationListener listener = mock(OnImmediateNotificationListener.class);

    instance.addImmediateNotificationListener(listener);

    ImmediateNotification notification = mock(ImmediateNotification.class);
    instance.addNotification(notification);

    verify(listener).onImmediateNotification(notification);
  }

  @Test
  public void testGetPersistentNotifications() throws Exception {
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
