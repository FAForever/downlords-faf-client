package com.faforever.client.logging;

import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.nio.file.Files;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoggingServiceTest extends ServiceTest {

  private static final Pattern GAME_LOG_PATTERN = Pattern.compile("game(_\\d*)?.log");

  private LoggingService instance;
  @Mock
  private PreferencesService preferencesService;

  @BeforeEach
  public void setUp() throws Exception {
    Preferences preferences = PreferencesBuilder.create().defaultValues().get();
    when(preferencesService.getPreferences()).thenReturn(preferences);

    instance = new LoggingService(preferencesService);
  }

  @Test
  public void testGetNewLogFile() throws Exception {
    assertTrue(GAME_LOG_PATTERN.matcher(instance.getNewGameLogFile(0).getFileName().toString()).matches());
  }

  @Test
  public void testGetMostRecentLogFile() throws Exception {
    Files.createDirectories(LoggingService.FAF_LOG_DIRECTORY);
    Files.write(instance.getNewGameLogFile(0), new byte[]{});
    Files.write(instance.getNewGameLogFile(1), new byte[]{});
    Thread.sleep(10);
    Files.write(instance.getNewGameLogFile(2), new byte[]{});
    assertEquals("game_2.log", instance.getMostRecentGameLogFile().get().getFileName().toString());

    Files.delete(instance.getNewGameLogFile(0));
    Files.delete(instance.getNewGameLogFile(1));
    Files.delete(instance.getNewGameLogFile(2));
  }

  @Test
  public void testSetLoggingLevel() throws Exception {
    instance.setLoggingLevel();
    verify(preferencesService).storeInBackground();
    verify(preferencesService).getPreferences();
  }
}
