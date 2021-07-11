package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.preferences.event.MissingGamePathEvent;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MissingGamePathNotifierTest {
  @Mock
  private I18n i18n;
  @Mock
  private NotificationService notificationService;

  private MissingGamePathNotifier instance;
  private EventBus eventBus;

  @BeforeEach
  public void setUp() {
    eventBus = new EventBus();
    instance = new MissingGamePathNotifier(eventBus, i18n, notificationService);
    instance.afterPropertiesSet();
  }

  @Test
  public void testImmediateNotificationOnUrgentEvent() {
    eventBus.post(new MissingGamePathEvent(true));

    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  @Test
  public void testPersistentNotificationOnDefaultEvent() {
    eventBus.post(new MissingGamePathEvent());

    verify(notificationService).addNotification(any(PersistentNotification.class));
  }
}
