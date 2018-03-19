package com.faforever.client.os;

import com.faforever.client.replay.ReplayService;
import com.install4j.api.launcher.StartupNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * When a file type is associated with the client and the user opens such a file, this class will handle the opening
 * event. Since Install4j only allows a single listener, all file types need to be handled in this class.
 */
@Component
@Slf4j
public class FileOpeningHandler implements ApplicationRunner {

  private final ReplayService replayService;

  public FileOpeningHandler(ReplayService replayService) {
    this.replayService = replayService;
  }

  @PostConstruct
  public void postConstruct() {
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
    replayService.runReplayFile(filePath);
  }

  @Override
  public void run(ApplicationArguments args) {
    String[] sourceArgs = args.getSourceArgs();
    if (sourceArgs.length > 0) {
      runReplay(Paths.get(sourceArgs[0]));
    }
  }
}
