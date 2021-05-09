package com.faforever.client.os;

import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.ReplayService;
import com.install4j.api.launcher.StartupNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.CompressorException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    Path filePath = Paths.get(parameters.replace("\"", ""));
    runReplay(filePath);
  }

  private void runReplay(Path filePath) {
    try {
      replayService.runReplayFile(filePath);
    } catch (CompressorException | IOException e) {
      notificationService.addImmediateErrorNotification(e, "replay.couldNotParse");
    }
  }

  @Override
  public void run(ApplicationArguments args) {
    String[] sourceArgs = args.getSourceArgs();
    if (sourceArgs.length > 0) {
      runReplay(Paths.get(sourceArgs[0]));
    }
  }
}
