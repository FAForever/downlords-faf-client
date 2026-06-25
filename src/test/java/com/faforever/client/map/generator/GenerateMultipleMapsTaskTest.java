package com.faforever.client.map.generator;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GenerateMultipleMapsTaskTest extends ServiceTest {

  private GenerateMultipleMapsTask instance;

  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;

  @BeforeEach
  public void setup() {
    instance = new GenerateMultipleMapsTask(notificationService, i18n, null, forgedAlliancePrefs);
  }

  @Test
  public void testCallWithoutVersionThrowsException() throws Exception {
    instance.setSeed(123L);
    instance.setMapCount(3);
    assertEquals("Version hasn't been set.",
                 assertThrows(NullPointerException.class, () -> instance.call()).getMessage());
  }

  @Test
  public void testCallWithoutSeedThrowsException() throws Exception {
    instance.setVersion(new org.apache.maven.artifact.versioning.ComparableVersion("2.0.0"));
    instance.setMapCount(3);
    assertEquals("Seed hasn't been set.", assertThrows(NullPointerException.class, () -> instance.call()).getMessage());
  }

  @Test
  public void testCallWithNullSeed() throws Exception {
    instance.setVersion(new org.apache.maven.artifact.versioning.ComparableVersion("2.0.0"));
    instance.setSeed(null);
    instance.setMapCount(3);

    assertThrows(NullPointerException.class, () -> instance.call());
  }
}
