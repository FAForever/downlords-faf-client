package com.faforever.client.replay;

import com.faforever.client.game.GameService;
import com.faforever.client.game.GameType;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskGroup;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.ByteCopier;
import com.faforever.client.util.Callback;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.primitives.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

public class ReplayServiceImpl implements ReplayService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Byte offset at which a SupCom replay's version number starts.
   */
  private static final int VERSION_OFFSET = 0x18;
  private static final String FAF_REPLAY_FILE_ENDING = ".fafreplay";
  private static final String SUP_COM_REPLAY_FILE_ENDING = ".scfareplay";
  private static final String FAF_LIFE_PROTOCOL = "faflive";
  private static final String GPGNET_SCHEME = "gpgnet";
  private static final String TEMP_SCFA_REPLAY_FILE_NAME = "temp.scfareplay";
  private static final String TEMP_FAF_REPLAY_FILE_NAME = "temp.fafreplay";

  @Autowired
  Environment environment;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  ReplayFileReader replayFileReader;

  @Autowired
  NotificationService notificationService;

  @Autowired
  GameService gameService;

  @Autowired
  TaskService taskService;

  @Autowired
  I18n i18n;

  @Autowired
  ReportingService reportingService;

  @Autowired
  ReplayServerAccessor replayServerAccessor;

  @Override
  public Collection<ReplayInfoBean> getLocalReplays() throws IOException {
    Collection<ReplayInfoBean> replayInfos = new ArrayList<>();

    String replayFileGlob = environment.getProperty("replayFileGlob");

    Path replaysDirectory = preferencesService.getReplaysDirectory();
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(replaysDirectory, replayFileGlob)) {
      for (Path replayFile : directoryStream) {
        LocalReplayInfo replayInfo = null;
        try {
          replayInfo = replayFileReader.readReplayInfo(replayFile);
          if (replayInfo == null) {
            moveCorruptedReplayFile(replayFile);
            continue;
          }
          replayInfos.add(new ReplayInfoBean(replayInfo, replayFile));
        } catch (Exception e) {
          logger.warn("Could not read replay file {}", replayFile);
          moveCorruptedReplayFile(replayFile);
        }
      }
    }

    return replayInfos;
  }

  private void moveCorruptedReplayFile(Path replayFile) throws IOException {
    Path corruptedReplaysDirectory = preferencesService.getCorruptedReplaysDirectory();
    Files.createDirectories(corruptedReplaysDirectory);

    Path target = corruptedReplaysDirectory.resolve(replayFile.getFileName());

    logger.debug("Moving corrupted replay file from {} to {}", replayFile, target);

    Files.move(replayFile, target);

    notificationService.addNotification(new PersistentNotification(
        i18n.get("corruptedReplayFiles.notification"),
        Severity.WARN,
        Collections.singletonList(
            new Action(i18n.get("corruptedReplayFiles.show"), event -> {
              try {
                // Argh, using AWT since JavaFX doesn't provide a proper method :-(
                Desktop.getDesktop().open(corruptedReplaysDirectory.toFile());
              } catch (IOException e) {
                logger.warn("Could not reveal corrupted replay directory", e);
              }
            })
        )
    ));
  }

  @Override
  public void getOnlineReplays(Callback<List<ReplayInfoBean>> callback) {
    replayServerAccessor.requestOnlineReplays(callback);
  }

  @Override
  public void runReplay(ReplayInfoBean item) {
    if (item.getReplayFile() != null) {
      runReplayFile(item.getReplayFile());
    } else {
      runOnlineReplay(item.getId());
    }
  }

  @Override
  public void runLiveReplay(URI uri) throws IOException {
    if (!uri.getScheme().equals(FAF_LIFE_PROTOCOL)) {
      throw new IllegalArgumentException("Invalid protocol: " + uri.getScheme());
    }

    Map<String, String> queryParams = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(uri.getQuery());

    String mod = queryParams.get("mod");
    String mapName = queryParams.get("map");
    Integer replayId = Integer.parseInt(queryParams.get(uri.getPath().split("/")[0]));

    try {
      URL gpgReplayUrl = new URL(GPGNET_SCHEME, uri.getHost(), uri.getPort(), uri.getPath());
      gameService.runWithReplay(gpgReplayUrl, replayId);
    } catch (MalformedURLException e) {
      throw new RuntimeException();
    }
  }

  private void runReplayFile(Path path) {
    try {
      String fileName = path.getFileName().toString();
      if (fileName.endsWith(FAF_REPLAY_FILE_ENDING)) {
        runFafReplayFile(path);
      } else if (fileName.endsWith(SUP_COM_REPLAY_FILE_ENDING)) {
        runSupComReplayFile(path);
      }
    } catch (IOException e) {
      logger.warn("Replay could not be started", e);
      notificationService.addNotification(new PersistentNotification(
          i18n.get("replayCouldNotBeStarted.text", path.getFileName()),
          Severity.WARN,
          Collections.singletonList(new Action(i18n.get("report"), event -> reportingService.reportError(e)))
      ));
    }
  }

  private void runFafReplayFile(Path path) throws IOException {
    byte[] rawReplayBytes = replayFileReader.readReplayData(path);

    Path tempSupComReplayFile = preferencesService.getCacheDirectory().resolve(TEMP_SCFA_REPLAY_FILE_NAME);

    Files.createDirectories(tempSupComReplayFile.getParent());
    Files.copy(new ByteArrayInputStream(rawReplayBytes), tempSupComReplayFile, StandardCopyOption.REPLACE_EXISTING);

    LocalReplayInfo replayInfo = replayFileReader.readReplayInfo(path);
    String gameType = replayInfo.getFeaturedMod();
    Integer replayId = replayInfo.getUid();
    Map<String, Integer> modVersions = replayInfo.getFeaturedModVersions();
    Set<String> simMods = replayInfo.getSimMods().keySet();

    Integer version = parseSupComVersion(rawReplayBytes);

    gameService.runWithReplay(tempSupComReplayFile, replayId, gameType, version, modVersions, simMods);
  }

  private void runSupComReplayFile(Path path) throws IOException {
    byte[] rawReplayBytes = replayFileReader.readReplayData(path);

    Integer version = parseSupComVersion(rawReplayBytes);
    String fileName = path.getFileName().toString();
    String gameType = guessModByFileName(fileName);

    gameService.runWithReplay(path, null, gameType, version, emptyMap(), emptySet());
  }

  private void runOnlineReplay(int replayId) {
    downloadReplayToTemporaryDirectory(replayId, new Callback<Path>() {
      @Override
      public void success(Path replayFile) {
        runReplayFile(replayFile);
      }

      @Override
      public void error(Throwable e) {
        notificationService.addNotification(new PersistentNotification(
            i18n.get("replayCouldNotBeDownloaded", replayId),
            Severity.ERROR,
            Collections.singletonList(new Action(i18n.get("report"), event -> reportingService.reportError(e)))
        ));
      }
    });
  }

  private void downloadReplayToTemporaryDirectory(int replayId, Callback<Path> callback) {
    String taskTitle = i18n.get("mapReplayTask.title", replayId);

    taskService.submitTask(TaskGroup.NET_HEAVY, new PrioritizedTask<Path>(taskTitle) {
      @Override
      protected Path call() throws Exception {
        String replayUrl = getReplayUrl(replayId, environment.getProperty("vault.replayDownloadUrl"));

        logger.info("Downloading replay {} from {}", replayId, replayUrl);

        HttpURLConnection urlConnection = (HttpURLConnection) new URL(replayUrl).openConnection();
        int bytesToRead = urlConnection.getContentLength();

        Path tempSupComReplayFile = preferencesService.getCacheDirectory().resolve(TEMP_FAF_REPLAY_FILE_NAME);

        Files.createDirectories(tempSupComReplayFile.getParent());

        try (InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
             OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(tempSupComReplayFile))) {

          ByteCopier.from(inputStream)
              .to(outputStream)
              .totalBytes(bytesToRead)
              .listener(this::updateProgress)
              .copy();

          return tempSupComReplayFile;
        }
      }
    }, callback);
  }

  private String getReplayUrl(int replayId, String baseUrl) {
    return String.format(baseUrl, replayId);
  }

  @VisibleForTesting
  static Integer parseSupComVersion(byte[] rawReplayBytes) {
    int versionDelimiterIndex = Bytes.indexOf(rawReplayBytes, (byte) 0x00);
    return Integer.parseInt(new String(rawReplayBytes, VERSION_OFFSET, versionDelimiterIndex - VERSION_OFFSET, US_ASCII));
  }

  @VisibleForTesting
  static String guessModByFileName(String fileName) {
    String[] splitFileName = fileName.split("\\.");
    if (splitFileName.length > 2) {
      return splitFileName[splitFileName.length - 2];
    }
    return GameType.DEFAULT.getString();
  }
}
