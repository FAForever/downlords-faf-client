package com.faforever.client.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.preferences.PreferencesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
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
@Lazy
@Service
@RequiredArgsConstructor
public class LoggingService implements InitializingBean {

  /**
   * Points to the FAF data directory where log files, config files and others are held. The returned value varies
   * depending on the operating system.
   */
  public static final Path FAF_LOG_DIRECTORY;
  public static final Path FAF_ICE_LOG_DIRECTORY;
  public static final Path FAF_MAP_GENERATOR_LOG_DIRECTORY;
  public static final Path FAF_IRC_LOG_DIRECTORY;
  private static final Pattern GAME_LOG_PATTERN = Pattern.compile("game(_\\d*)?.log");
  private static final int NUMBER_GAME_LOGS_STORED = 10;

  static {
    if (org.bridj.Platform.isWindows()) {
      FAF_LOG_DIRECTORY = Path.of(System.getenv("APPDATA")).resolve(PreferencesService.APP_DATA_SUB_FOLDER).resolve("logs");
    } else {
      FAF_LOG_DIRECTORY = Path.of(System.getProperty("user.home")).resolve(PreferencesService.USER_HOME_SUB_FOLDER).resolve("logs");
    }

    System.setProperty("LOG_FILE", LoggingService.FAF_LOG_DIRECTORY
        .resolve("client.log")
        .toString());

    FAF_ICE_LOG_DIRECTORY = FAF_LOG_DIRECTORY
        .resolve("iceAdapterLogs");
    System.setProperty("ICE_ADVANCED_LOG", FAF_ICE_LOG_DIRECTORY
        .resolve("advanced-ice-adapter.log")
        .toString());

    FAF_MAP_GENERATOR_LOG_DIRECTORY = FAF_LOG_DIRECTORY
        .resolve("mapGeneratorLogs");
    System.setProperty("MAP_GENERATOR_LOG", FAF_MAP_GENERATOR_LOG_DIRECTORY
        .resolve("map-generator.log")
        .toString());

    FAF_IRC_LOG_DIRECTORY =  FAF_LOG_DIRECTORY
        .resolve("ircLogs");
    System.setProperty("IRC_LOG", FAF_IRC_LOG_DIRECTORY
        .resolve("irc.log")
        .toString());

    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  private final PreferencesService preferencesService;

  public static void configureLogging() {
    // Calling this method causes the class to be initialized (static initializers) which in turn causes the logger to initialize.
    log.info("Logger initialized");
  }

  @Override
  public void afterPropertiesSet() throws IOException, InterruptedException {
    JavaFxUtil.addAndTriggerListener(preferencesService.getPreferences().getDeveloper().logLevelProperty(), (observable) -> setLoggingLevel());
  }

  public Path getNewGameLogFile(int gameUID) {
    try (Stream<Path> listOfLogFiles = Files.list(FAF_LOG_DIRECTORY)) {
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
    return FAF_LOG_DIRECTORY.resolve(String.format("game_%d.log", gameUID));
  }

  public Optional<Path> getMostRecentGameLogFile() {
    try (Stream<Path> listOfLogFiles = Files.list(FAF_LOG_DIRECTORY)) {
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
