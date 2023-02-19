package com.faforever.client.game;

import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.ui.preferences.event.GameDirectoryChosenEvent;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;

public class GamePathHandlerTest extends ServiceTest {
  @Mock
  private NotificationService notificationService;
  @Mock
  private EventBus eventBus;
  @Mock
  private I18n i18n;

  @InjectMocks
  private GamePathHandler instance;

  @BeforeEach
  public void setUp() throws Exception {
    Preferences preferences = PreferencesBuilder.create().defaultValues().get();
  }

  @Test
  public void testNotificationOnEmptyString() throws Exception {
    CompletableFuture<Path> completableFuture = new CompletableFuture<>();
    instance.onGameDirectoryChosenEvent(new GameDirectoryChosenEvent(null, Optional.of(completableFuture)));
    verify(notificationService).addImmediateWarnNotification("gamePath.select.noneChosen");
    assertThat(completableFuture.isCompletedExceptionally(), is(true));
  }

  @Test
  public void testNotificationOnNull() throws Exception {
    CompletableFuture<Path> completableFuture = new CompletableFuture<>();
    instance.onGameDirectoryChosenEvent(new GameDirectoryChosenEvent(null, Optional.of(completableFuture)));
    verify(notificationService).addImmediateWarnNotification("gamePath.select.noneChosen");
    assertThat(completableFuture.isCompletedExceptionally(), is(true));
  }
}
