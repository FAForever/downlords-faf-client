package com.faforever.client.legacy;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.core.env.Environment;

public class ModsServerAccessorImplTest {

  @Mock
  private Environment environment;
  @Mock
  private I18n i18n;
  @Mock
  private NotificationService notificationService;

  private ModsServerAccessorImpl instance;

  @Before
  public void setUp() throws Exception {
    instance = new ModsServerAccessorImpl();
    instance.environment = environment;
    instance.i18n = i18n;
    instance.notificationService = notificationService;

  }

  @Test
  public void testConnect() throws Exception {

  }

  @Test
  public void testDisconnect() throws Exception {

  }

  @Test
  public void testSearchMod() throws Exception {

  }

  @Test
  public void testOnServerMessage() throws Exception {

  }
}
