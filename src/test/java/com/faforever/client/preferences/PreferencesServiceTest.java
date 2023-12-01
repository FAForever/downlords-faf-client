package com.faforever.client.preferences;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.os.OsPosix;
import com.faforever.client.test.ServiceTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class PreferencesServiceTest extends ServiceTest {


  private PreferencesService instance;

  @Spy
  private OperatingSystem operatingSystem = new OsPosix();
  @Spy
  private ClientProperties clientProperties;
  @Spy
  private Preferences preferences;

  @BeforeEach
  public void setUp() throws Exception {
    when(operatingSystem.getPreferencesDirectory()).thenReturn(Path.of("."));
    instance = new PreferencesService(clientProperties, preferences, new ObjectMapper());
  }

//  @Test
//  public void testPreferencesSerializable() throws Exception {
//    Preferences preferences = PreferencesBuilder.create().defaultValues().get();
//    assertDoesNotThrow(() -> objectMapper.readValue(objectMapper.writeValueAsString(preferences), Preferences.class));
//  }

  @Test
  public void testIsVaultBasePathInvalidForAscii() {
    preferences.getForgedAlliance().setVaultBaseDirectory(Path.of("C:\\User\\test"));
    assertFalse(instance.isVaultBasePathInvalidForAscii());

    preferences.getForgedAlliance().setVaultBaseDirectory(Path.of("C:\\Юзер\\test"));
    assertTrue(instance.isVaultBasePathInvalidForAscii());
  }
}
