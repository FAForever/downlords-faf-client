package com.faforever.client.os;

import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.user.LoginService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

import java.io.IOException;
import java.nio.file.Path;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class FileOpeningHandlerTest extends ServiceTest {

  @Mock
  private ReplayService replayService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private LoginService loginService;

  @InjectMocks
  private FileOpeningHandler instance;

  @Test
  public void testRunLocalReplay() throws IOException {
    ObjectProperty<ConnectionState> connectionState = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);
    when(loginService.connectionStateProperty()).thenReturn(connectionState);

    ApplicationArguments args = new DefaultApplicationArguments("foo.fafreplay");
    instance.run(args);
    verifyNoInteractions(replayService);

    connectionState.set(ConnectionState.CONNECTED);
    verify(replayService).runReplayFile(Path.of("foo.fafreplay"));
  }
}
