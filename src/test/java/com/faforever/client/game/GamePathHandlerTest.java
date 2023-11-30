package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.ui.preferences.event.GameDirectoryChosenEvent;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;

public class GamePathHandlerTest extends ServiceTest {
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;

  @InjectMocks
  private GamePathHandler instance;

  @Test
  public void testNotificationOnEmptyString() throws Exception {
    CompletableFuture<Path> completableFuture = new CompletableFuture<>();
    instance.onGameDirectoryChosenEvent(new GameDirectoryChosenEvent(null, completableFuture));
    verify(notificationService).addImmediateWarnNotification("gamePath.select.noneChosen");
    assertThat(completableFuture.isCompletedExceptionally(), is(true));
  }

  @Test
  public void testNotificationOnNull() throws Exception {
    CompletableFuture<Path> completableFuture = new CompletableFuture<>();
    instance.onGameDirectoryChosenEvent(new GameDirectoryChosenEvent(null, completableFuture));
    verify(notificationService).addImmediateWarnNotification("gamePath.select.noneChosen");
    assertThat(completableFuture.isCompletedExceptionally(), is(true));
  }
}
