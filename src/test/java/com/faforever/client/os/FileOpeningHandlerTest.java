package com.faforever.client.os;

import com.faforever.client.replay.ReplayService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

import java.nio.file.Paths;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class FileOpeningHandlerTest {

  @Mock
  private ReplayService replayService;

  private FileOpeningHandler instance;

  @Before
  public void setUp() throws Exception {
    instance = new FileOpeningHandler(replayService);
  }

  @Test
  public void run() {
    ApplicationArguments args = new DefaultApplicationArguments(new String[]{"foo.fafreplay"});
    instance.run(args);

    verify(replayService).runReplayFile(Paths.get("foo.fafreplay"));
  }
}
