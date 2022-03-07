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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
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

  @Test
  public void host() throws IOException, CompressorException {
    ApplicationArguments args = new DefaultApplicationArguments("foo.fafreplay");
    instance.host(args);

    verify(replayService).hostFromReplayFile(Path.of("foo.fafreplay"));
  }

  @Test
  public void testHostException() throws Exception {
    ApplicationArguments args = new DefaultApplicationArguments("foo.fafreplay");
    doThrow(new CompressorException("Compressor Error")).when(replayService).hostFromReplayFile(any());

    instance.host(args);
    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString());
  }

  @Test
  public void testRunException() throws Exception {
    ApplicationArguments args = new DefaultApplicationArguments("foo.fafreplay");
    doThrow(new CompressorException("Compressor Error")).when(replayService).runReplayFile(any());

    instance.run(args);
    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString());
  }
}
