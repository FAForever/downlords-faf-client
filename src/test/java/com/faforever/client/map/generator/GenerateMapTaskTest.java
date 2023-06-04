package com.faforever.client.map.generator;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GenerateMapTaskTest extends ServiceTest {

  private GenerateMapTask instance;


  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;

  @BeforeEach
  public void setup() {
    instance = new GenerateMapTask(notificationService, i18n, null, null);
  }

  @Test
  public void testCallWithoutVersionThrowsException() throws Exception {
    assertEquals("Version hasn't been set.", assertThrows(NullPointerException.class, () -> instance.call()).getMessage());
  }
}
