package com.faforever.client.os;

import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.test.ServiceTest;
import org.apache.commons.compress.compressors.CompressorException;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

import java.io.IOException;
import java.nio.file.Path;

import static org.mockito.Mockito.verify;

public class FileOpeningHandlerTest extends ServiceTest {

  @Mock
  private ReplayService replayService;
  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private FileOpeningHandler instance;

  @Test
  public void run() throws IOException, CompressorException {
    ApplicationArguments args = new DefaultApplicationArguments("foo.fafreplay");
    instance.run(args);

    verify(replayService).runReplayFile(Path.of("foo.fafreplay"));
  }
}
