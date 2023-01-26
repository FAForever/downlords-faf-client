package com.faforever.client.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.preferences.PreferencesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Lazy(value = false)
@Service
@RequiredArgsConstructor
public class LoggingService implements InitializingBean {

  private static final Pattern GAME_LOG_PATTERN = Pattern.compile("game(_\\d*)?.log");
  private static final int NUMBER_GAME_LOGS_STORED = 10;

  private final OperatingSystem operatingSystem;
  private final PreferencesService preferencesService;

  @Override
  public void afterPropertiesSet() throws IOException, InterruptedException, JoranException {
    Path loggingDirectory = operatingSystem.getLoggingDirectory();
    System.setProperty("LOG_FILE", loggingDirectory
        .resolve("client.log")
        .toString());

    System.setProperty("ICE_ADVANCED_LOG", loggingDirectory
        .resolve("iceAdapterLogs")
        .resolve("advanced-ice-adapter.log")
        .toString());

    System.setProperty("MAP_GENERATOR_LOG", loggingDirectory
        .resolve("mapGeneratorLogs")
        .resolve("map-generator.log")
        .toString());

    System.setProperty("IRC_LOG", loggingDirectory
        .resolve("ircLogs")
        .resolve("irc.log")
        .toString());

    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    loggerContext.reset();

    ContextInitializer ci = new ContextInitializer(loggerContext);
    ci.configureByResource(LoggingService.class.getResource("/logback-spring.xml"));

    JavaFxUtil.addAndTriggerListener(preferencesService.getPreferences().getDeveloper().logLevelProperty(), (observable) -> setLoggingLevel());
  }

  public Path getNewGameLogFile(int gameUID) {
    try (Stream<Path> listOfLogFiles = Files.list(operatingSystem.getLoggingDirectory())) {
      listOfLogFiles
          .filter(logPath -> GAME_LOG_PATTERN.matcher(logPath.getFileName().toString()).matches())
          .sorted(Comparator.<Path>comparingLong(logPath -> logPath.toFile().lastModified()).reversed())
          .skip(NUMBER_GAME_LOGS_STORED - 1)
          .forEach(logPath -> {
            try {
              Files.delete(logPath);
            } catch (IOException e) {
              log.warn("Could not delete log file `{}`", logPath, e);
            }
          });
    } catch (IOException e) {
      log.error("Could not list log directory", e);
    }
    return operatingSystem.getLoggingDirectory().resolve(String.format("game_%d.log", gameUID));
  }

  public Optional<Path> getMostRecentGameLogFile() {
    try (Stream<Path> listOfLogFiles = Files.list(operatingSystem.getLoggingDirectory())) {
      return listOfLogFiles
          .filter(p -> GAME_LOG_PATTERN.matcher(p.getFileName().toString()).matches()).max(Comparator.comparingLong(p -> p.toFile().lastModified()));
    } catch (IOException e) {
      log.error("Could not list log directory.", e);
      return Optional.empty();
    }
  }

  public void setLoggingLevel() {
    preferencesService.storeInBackground();
    Level targetLogLevel = Level.toLevel(preferencesService.getPreferences().getDeveloper().getLogLevel());
    final LoggerContext loggerContext = ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())).getLoggerContext();
    loggerContext.getLoggerList()
        .stream()
        .filter(logger -> logger.getName().startsWith("com.faforever"))
        .forEach(logger -> ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(logger.getName())).setLevel(targetLogLevel));

    log.info("Switching FA Forever logging configuration to {}", targetLogLevel);
    if (Level.TRACE.equals(targetLogLevel)) {
      log.trace("Confirming trace logging");
    } else if (Level.DEBUG.equals(targetLogLevel)) {
      log.debug("Confirming debug logging");
    } else if (Level.INFO.equals(targetLogLevel)) {
      log.info("Confirming info logging");
    } else if (Level.WARN.equals(targetLogLevel)) {
      log.warn("Confirming warn logging");
    } else if (Level.ERROR.equals(targetLogLevel)) {
      log.error("Confirming error logging");
    } else {
      log.error("Unknown log level set");
    }
  }
}
