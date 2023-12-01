package com.faforever.client.logging;

import com.faforever.client.os.OperatingSystem;
import com.faforever.client.os.OsPosix;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class LoggingServiceTest extends ServiceTest {

  private static final Pattern GAME_LOG_PATTERN = Pattern.compile("game(_\\d*)?.log");

  @InjectMocks
  private LoggingService instance;
  @Spy
  private OperatingSystem operatingSystem = new OsPosix();


  @BeforeEach
  public void setUp() throws Exception {
    when(operatingSystem.getLoggingDirectory()).thenReturn(Path.of("."));
  }

  @Test
  public void testGetNewLogFile() throws Exception {
    assertTrue(GAME_LOG_PATTERN.matcher(instance.getNewGameLogFile(0).getFileName().toString()).matches());
  }

  @Test
  public void testGetMostRecentLogFile() throws Exception {
    Files.createDirectories(operatingSystem.getLoggingDirectory());
    Files.write(instance.getNewGameLogFile(0), new byte[]{});
    Files.write(instance.getNewGameLogFile(1), new byte[]{});
    Thread.sleep(10);
    Files.write(instance.getNewGameLogFile(2), new byte[]{});
    assertEquals("game_2.log", instance.getMostRecentGameLogFile().get().getFileName().toString());

    Files.delete(instance.getNewGameLogFile(0));
    Files.delete(instance.getNewGameLogFile(1));
    Files.delete(instance.getNewGameLogFile(2));
  }
}
