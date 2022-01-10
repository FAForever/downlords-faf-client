package com.faforever.client.map.generator;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.ServiceTest;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GenerateMapTaskTest extends ServiceTest {
  @InjectMocks
  private GenerateMapTask instance;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private EventBus eventBus;
  @Mock
  private I18n i18n;

  @Test
  public void testCallWithoutVersionThrowsException() throws Exception {
    assertEquals("Version hasn't been set.", assertThrows(NullPointerException.class, () -> instance.call()).getMessage());
  }
}
