package com.faforever.client.logging;

import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoggingServiceTest extends ServiceTest {

  private static final Pattern GAME_LOG_PATTERN = Pattern.compile("game(_\\d*)?.log");

  private LoggingService instance;
  @Mock
  private PreferencesService preferencesService;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new LoggingService(preferencesService);
  }

  @Test
  public void testGetNewLogFile() throws Exception {
    assertTrue(GAME_LOG_PATTERN.matcher(instance.getNewGameLogFile(0).getFileName().toString()).matches());
  }
}
