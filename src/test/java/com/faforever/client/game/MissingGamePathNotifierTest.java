package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.ui.preferences.GameDirectoryRequiredHandler;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class MissingGamePathNotifierTest extends ServiceTest {
  @Mock
  private I18n i18n;
  @Mock
  private NotificationService notificationService;
  @Mock
  private GameDirectoryRequiredHandler gameDirectoryRequiredHandler;

  @InjectMocks
  private MissingGamePathNotifier instance;

  @Test
  public void testImmediateNotificationOnUrgentEvent() {
    instance.onMissingGamePathEvent(true);

    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  @Test
  public void testPersistentNotificationOnDefaultEvent() {
    instance.onMissingGamePathEvent(false);

    verify(notificationService).addNotification(any(PersistentNotification.class));
  }
}
