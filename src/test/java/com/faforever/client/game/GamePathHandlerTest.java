package com.faforever.client.game;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.ui.preferences.event.GameDirectoryChosenEvent;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class GamePathHandlerTest {
  @Mock
  private NotificationService notificationService;
  @Mock
  private EventBus eventBus;
  @Mock
  private I18n i18n;
  private GamePathHandler instance;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    PreferencesService preferenceService = new PreferencesService(new ClientProperties());
    instance = new GamePathHandler(notificationService, i18n, eventBus, preferenceService);
  }

  @Test
  public void testNotificationOnEmptyString() throws Exception {
    CompletableFuture<Path> completableFuture = new CompletableFuture<>();
    instance.onGameDirectoryChosenEvent(new GameDirectoryChosenEvent(null, Optional.of(completableFuture)));
    verify(notificationService).addNotification(any(ImmediateNotification.class));
    assertThat(completableFuture.isCompletedExceptionally(), is(true));
  }

  @Test
  public void testNotificationOnNull() throws Exception {
    CompletableFuture<Path> completableFuture = new CompletableFuture<>();
    instance.onGameDirectoryChosenEvent(new GameDirectoryChosenEvent(null, Optional.of(completableFuture)));
    verify(notificationService).addNotification(any(ImmediateNotification.class));
    assertThat(completableFuture.isCompletedExceptionally(), is(true));
  }
}
