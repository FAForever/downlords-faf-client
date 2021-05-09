package com.faforever.client.os;

import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.ReplayService;
import org.apache.commons.compress.compressors.CompressorException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

import java.io.IOException;
import java.nio.file.Paths;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class FileOpeningHandlerTest {

  @Mock
  private ReplayService replayService;
  @Mock
  private NotificationService notificationService;

  private FileOpeningHandler instance;

  @Before
  public void setUp() throws Exception {
    instance = new FileOpeningHandler(replayService, notificationService);
  }

  @Test
  public void run() throws IOException, CompressorException {
    ApplicationArguments args = new DefaultApplicationArguments("foo.fafreplay");
    instance.run(args);

    verify(replayService).runReplayFile(Paths.get("foo.fafreplay"));
  }
}
