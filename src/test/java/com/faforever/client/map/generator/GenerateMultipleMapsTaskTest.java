package com.faforever.client.map.generator;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.os.OsPosix;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.test.PlatformTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

public class GenerateMultipleMapsTaskTest extends PlatformTest {

  private GenerateMultipleMapsTask instance;

  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;
  @Spy
  private OperatingSystem operatingSystem = new OsPosix();

  @BeforeEach
  public void setup() {
    instance = new GenerateMultipleMapsTask(notificationService, i18n, operatingSystem, forgedAlliancePrefs);
    lenient().when(i18n.get(anyString(), any())).thenReturn("test");
  }

  @Test
  public void testCallWithoutVersionThrowsException() throws Exception {
    instance.setSeed(123L);
    instance.setBaseOptions(GeneratorOptions.builder().build());
    instance.setMapCount(3);
    assertEquals("Version hasn't been set.",
                 assertThrows(NullPointerException.class, () -> instance.call()).getMessage());
  }

  @Test
  public void testCallWithNullBaseOptions() throws Exception {
    instance.setVersion(new org.apache.maven.artifact.versioning.ComparableVersion("2.0.0"));
    instance.setSeed(123L);
    instance.setMapCount(3);

    assertThrows(NullPointerException.class, () -> instance.call());
  }
}
