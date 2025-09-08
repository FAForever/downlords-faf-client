package com.faforever.client.os;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.user.LoginService;
import com.install4j.api.launcher.StartupNotification;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

/**
 * When a file type is associated with the client and the user opens such a file, this class will handle the opening
 * event. Since Install4j only allows a single listener, all file types need to be handled in this class.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FileOpeningHandler implements ApplicationRunner, InitializingBean {

  private final ReplayService replayService;
  private final NotificationService notificationService;
  private final LoginService loginService;

  @Override
  public void afterPropertiesSet() {
    log.debug("Registering file opening handler: {}", this.getClass().getName());
    StartupNotification.registerStartupListener(this::onStartup);
  }

  private void onStartup(String parameters) {
    log.debug("Handling startup: {}", parameters);
    if (parameters.split("\" \"").length > 2) {
      throw new IllegalArgumentException("Can't handle multiple files: " + parameters);
    }
    Path filePath = Path.of(parameters.replace("\"", ""));
    runReplay(filePath);
  }

  private void runReplay(Path filePath) {
    try {
      replayService.runReplayFile(filePath);
    } catch (IOException e) {
      notificationService.addImmediateErrorNotification(e, "replay.couldNotParse");
    }
  }

  @Override
  public void run(ApplicationArguments args) {
    String[] sourceArgs = args.getSourceArgs();
    if (sourceArgs.length > 0) {
      ChangeListener<ConnectionState> connectionStateListener = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends ConnectionState> observable, ConnectionState oldValue, ConnectionState newValue) {
          if (newValue == ConnectionState.CONNECTED) {
            JavaFxUtil.removeListener(loginService.connectionStateProperty(), this);
            runReplay(Path.of(sourceArgs[0]));
          }
        }
      };
      JavaFxUtil.addListener(loginService.connectionStateProperty(), connectionStateListener);
    }
  }
}
