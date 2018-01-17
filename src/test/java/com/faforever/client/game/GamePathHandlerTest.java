package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.notificationEvents.ShowImmediateNotificationEvent;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.ui.preferences.event.GameDirectoryChosenEvent;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class GamePathHandlerTest {
  @Mock
  private ApplicationEventPublisher applicationEventPublisher;
  @Mock
  private EventBus eventBus;
  @Mock
  private I18n i18n;
  private GamePathHandler instance;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    PreferencesService preferenceService = new PreferencesService();
    instance = new GamePathHandler(i18n, eventBus, applicationEventPublisher, preferenceService);
  }

  @Test
  public void testNotificationOnEmptyString() throws Exception {
    instance.onGameDirectoryChosenEvent(new GameDirectoryChosenEvent(Paths.get("")));
    verify(applicationEventPublisher).publishEvent(any(ShowImmediateNotificationEvent.class));
  }

  @Test
  public void testNotificationOnNull() throws Exception {
    instance.onGameDirectoryChosenEvent(new GameDirectoryChosenEvent(null));
    verify(applicationEventPublisher).publishEvent(any(ShowImmediateNotificationEvent.class));
  }
}