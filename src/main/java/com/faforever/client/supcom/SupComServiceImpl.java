package com.faforever.client.supcom;

import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Callback;
import com.faforever.client.util.ConcurrentUtil;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.List;

public class SupComServiceImpl implements SupComService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  UserService userService;

  private Process process;

  @Override
  public void startGame(int uid, String mod, List<String> additionalArgs, Callback<Void> callback) {
    Path executable = preferencesService.getFafBinDirectory().resolve("ForgedAlliance.exe");

    List<String> launchCommand = LaunchCommandBuilder.create()
        .executable(executable)
        .uid(uid)
        .clan(userService.getClan())
        .country(userService.getCountry())
        .deviation(userService.getDeviation())
        .mean(userService.getMean())
        .username(userService.getUsername())
        .additionalArgs(additionalArgs)
//        .log(logFilePath)
//        .localGpgPort(localGpgPort)
        .build();

    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.inheritIO();
    processBuilder.directory(executable.getParent().toAbsolutePath().toFile());
    processBuilder.command(launchCommand);

    logger.info("Starting SupCom with command: " + processBuilder.command());

    try {
      process = processBuilder.start();

      ConcurrentUtil.executeInBackground(new Task<Void>() {
        @Override
        protected Void call() throws Exception {
          process.waitFor();
          return null;
        }
      });

    } catch (IOException e) {
      callback.error(e);
    }
  }
}
