package com.faforever.client.map.generator;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class GenerateMapTaskTest {
  private GenerateMapTask instance;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private EventBus eventBus;
  @Mock
  private I18n i18n;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new GenerateMapTask(preferencesService, notificationService, i18n, eventBus);
  }

  @Test
  public void testCallWithoutVersionThrowsException() throws Exception {
    assertEquals("Version hasn't been set.", assertThrows(NullPointerException.class, () -> instance.call()).getMessage());
  }
}
