package com.faforever.client.map.generator;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.startsWith;

@RunWith(MockitoJUnitRunner.class)
public class GenerateMapTaskTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder customMapsDirectory = new TemporaryFolder();

  private GenerateMapTask instance;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private EventBus eventBus;
  @Mock
  private I18n i18n;

  @Before
  public void setUp() throws Exception {
    instance = new GenerateMapTask(preferencesService, notificationService, i18n, eventBus);
  }

  @Test
  public void testCallWithoutVersionThrowsException() throws Exception {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage(startsWith("Version hasn't been set"));

    instance.call();
  }
}
