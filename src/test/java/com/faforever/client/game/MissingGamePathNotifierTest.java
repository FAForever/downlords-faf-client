package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.notificationEvents.ShowImmediateNotificationEvent;
import com.faforever.client.notification.notificationEvents.ShowPersistentNotificationEvent;
import com.faforever.client.preferences.event.MissingGamePathEvent;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MissingGamePathNotifierTest {
  @Mock
  private I18n i18n;
  @Mock
  private ApplicationEventPublisher applicationEventPublisher;

  private MissingGamePathNotifier instance;
  private EventBus eventBus;

  @Before
  public void setUp() {
    eventBus = new EventBus();
    instance = new MissingGamePathNotifier(eventBus, i18n, applicationEventPublisher);
    instance.postConstruct();
  }

  @Test
  public void testImmediateNotificationOnUrgentEvent() {
    eventBus.post(new MissingGamePathEvent(true));

    verify(applicationEventPublisher).publishEvent(any(ShowImmediateNotificationEvent.class));
  }

  @Test
  public void testPersistentNotificationOnDefaultEvent() {
    eventBus.post(new MissingGamePathEvent());

    verify(applicationEventPublisher).publishEvent(any(ShowPersistentNotificationEvent.class));
  }
}
