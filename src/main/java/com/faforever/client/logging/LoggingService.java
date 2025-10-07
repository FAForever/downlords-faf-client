package com.faforever.client.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.preferences.DeveloperPrefs;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
  private static final Pattern REPLAY_LOG_PATTERN = Pattern.compile("replay(_\\d*)?.log");
  private static final int NUMBER_GAME_LOGS_STORED = 10;

  private final OperatingSystem operatingSystem;
  private final DeveloperPrefs developerPrefs;
  private final ChangeListener<String> logLevelChangeListener = (observable, oldValue, newValue) -> setLoggingLevel(newValue);

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

    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    context.reset();
    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(context);
    configurator.doConfigure(this.getClass().getResourceAsStream("/logback-spring.xml"));

    JavaFxUtil.addAndTriggerListener(developerPrefs.logLevelProperty(), new WeakChangeListener<>(logLevelChangeListener));
  }

  public Path getNewGameLogFile(int gameUID) {return getNewLogFile(gameUID, GAME_LOG_PATTERN);}
  public Path getNewReplayLogFile(int gameUID) {return getNewLogFile(gameUID, REPLAY_LOG_PATTERN);}

  private Path getNewLogFile(int gameUID, Pattern logPattern) {
    try (Stream<Path> listOfLogFiles = Files.list(operatingSystem.getLoggingDirectory())) {
      listOfLogFiles
          .filter(logPath -> logPattern.matcher(logPath.getFileName().toString()).matches())
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
    return operatingSystem.getLoggingDirectory().resolve(String.format(logPattern == REPLAY_LOG_PATTERN ? "replay_%d.log" : "game_%d.log", gameUID));
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

  public void setLoggingLevel(String level) {
    LogLevel targetLogLevel = switch (level) {
      case "TRACE" -> LogLevel.TRACE;
      case "DEBUG" -> LogLevel.DEBUG;
      case "WARN" -> LogLevel.WARN;
      case "ERROR" -> LogLevel.ERROR;
      default -> LogLevel.INFO;
    };
    log.info("Switching FA Forever logging configuration to {}", targetLogLevel);
    LoggingSystem loggingSystem = LoggingSystem.get(LoggingService.class.getClassLoader());
    loggingSystem.getLoggerConfigurations().stream().filter(config -> config.getName().startsWith("com.faforever"))
        .forEach(config -> loggingSystem.setLogLevel(config.getName(), targetLogLevel));

    switch (targetLogLevel) {
      case TRACE -> log.trace("Confirming trace logging");
      case DEBUG -> log.debug("Confirming debug logging");
      case INFO -> log.info("Confirming info logging");
      case WARN -> log.warn("Confirming warn logging");
      case ERROR -> log.error("Confirming error logging");
    }
  }
}
