package com.faforever.client.game;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GamePathHandlerTest extends ServiceTest {
  @Mock
  private PlatformService platformService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;

  @InjectMocks
  private GamePathHandler instance;

  @Test
  public void testNotificationOnNull() throws Exception {
    when(platformService.askForPath(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
    CompletableFuture<Void> future = instance.chooseAndValidateGameDirectory();
    verify(notificationService).addImmediateWarnNotification("gamePath.select.noneChosen");
    assertThat(future.isCompletedExceptionally(), is(true));
  }

  @Test
  public void testImmediateNotificationOnUrgentEvent() {
    instance.notifyMissingGamePath(true);

    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  @Test
  public void testPersistentNotificationOnDefaultEvent() {
    instance.notifyMissingGamePath(false);

    verify(notificationService).addNotification(any(PersistentNotification.class));
  }
}
