package com.faforever.client.replay;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.GameService;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.task.TaskService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.net.UrlEscapers;
import com.google.common.primitives.Bytes;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.faforever.client.notification.Severity.WARN;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

public class ReplayServiceImpl implements ReplayService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Byte offset at which a SupCom replay's version number starts.
   */
  private static final int VERSION_OFFSET = 0x18;
  private static final int MAP_NAME_OFFSET = 0x2D;
  private static final String FAF_REPLAY_FILE_ENDING = ".fafreplay";
  private static final String SUP_COM_REPLAY_FILE_ENDING = ".scfareplay";
  private static final String FAF_LIFE_PROTOCOL = "faflive";
  private static final String GPGNET_SCHEME = "gpgnet";
  private static final String TEMP_SCFA_REPLAY_FILE_NAME = "temp.scfareplay";

  @Resource
  Environment environment;
  @Resource
  PreferencesService preferencesService;
  @Resource
  ReplayFileReader replayFileReader;
  @Resource
  NotificationService notificationService;
  @Resource
  GameService gameService;
  @Resource
  TaskService taskService;
  @Resource
  I18n i18n;
  @Resource
  ReportingService reportingService;
  @Resource
  ReplayServerAccessor replayServerAccessor;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  PlatformService platformService;
  @Resource
  ReplayServer replayServer;

  @VisibleForTesting
  static Integer parseSupComVersion(byte[] rawReplayBytes) {
    int versionDelimiterIndex = Bytes.indexOf(rawReplayBytes, (byte) 0x00);
    return Integer.parseInt(new String(rawReplayBytes, VERSION_OFFSET, versionDelimiterIndex - VERSION_OFFSET, US_ASCII));
  }

  @VisibleForTesting
  static String parseMapName(byte[] rawReplayBytes) {
    int mapDelimiterIndex = Bytes.indexOf(rawReplayBytes, new byte[]{0x00, 0x0D, 0x0A, 0x1A});
    String mapPath = new String(rawReplayBytes, MAP_NAME_OFFSET, mapDelimiterIndex - MAP_NAME_OFFSET, US_ASCII);
    return mapPath.split("/")[2];
  }

  @VisibleForTesting
  static String guessModByFileName(String fileName) {
    String[] splitFileName = fileName.split("\\.");
    if (splitFileName.length > 2) {
      return splitFileName[splitFileName.length - 2];
    }
    return KnownFeaturedMod.DEFAULT.getString();
  }

  @Override
  public Collection<ReplayInfoBean> getLocalReplays() throws IOException {
    Collection<ReplayInfoBean> replayInfos = new ArrayList<>();

    String replayFileGlob = environment.getProperty("replayFileGlob");

    Path replaysDirectory = preferencesService.getReplaysDirectory();
    if (!Files.notExists(replaysDirectory)) {
      Files.createDirectories(replaysDirectory);
    }
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(replaysDirectory, replayFileGlob)) {
      for (Path replayFile : directoryStream) {
        try {
          LocalReplayInfo replayInfo = replayFileReader.readReplayInfo(replayFile);
          replayInfos.add(new ReplayInfoBean(replayInfo, replayFile));
        } catch (Exception e) {
          logger.warn("Could not read replay file {} ({})", replayFile, e.getMessage());
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
        i18n.get("corruptedReplayFiles.notification"), WARN,
        singletonList(
            new Action(i18n.get("corruptedReplayFiles.show"), event -> platformService.reveal(replayFile))
        )
    ));
  }

  @Override
  public CompletionStage<List<ReplayInfoBean>> getOnlineReplays() {
    return replayServerAccessor.requestOnlineReplays();
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
  public void runLiveReplay(int gameId, int playerId) throws IOException {
    GameInfoBean gameInfoBean = gameService.getByUid(gameId);
    if (gameInfoBean == null) {
      throw new RuntimeException("There's no game with ID: " + gameId);
    }

    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setScheme(FAF_LIFE_PROTOCOL);
    uriBuilder.setHost(environment.getProperty("lobby.host"));
    uriBuilder.setPath("/" + gameId + "/" + playerId + SUP_COM_REPLAY_FILE_ENDING);
    uriBuilder.addParameter("map", UrlEscapers.urlFragmentEscaper().escape(gameInfoBean.getMapFolderName()));
    uriBuilder.addParameter("mod", gameInfoBean.getFeaturedMod());

    try {
      runLiveReplay(uriBuilder.build());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void runLiveReplay(URI uri) throws IOException {
    logger.debug("Running replay from URL: {}", uri);
    if (!uri.getScheme().equals(FAF_LIFE_PROTOCOL)) {
      throw new IllegalArgumentException("Invalid protocol: " + uri.getScheme());
    }

    Map<String, String> queryParams = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(uri.getQuery());

    String gameType = queryParams.get("mod");
    String mapName = URLDecoder.decode(queryParams.get("map"), StandardCharsets.UTF_8.name());
    Integer gameId = Integer.parseInt(uri.getPath().split("/")[1]);

    try {
      URI replayUri = new URI(GPGNET_SCHEME, null, uri.getHost(), uri.getPort(), uri.getPath(), null, null);
      gameService.runWithLiveReplay(replayUri, gameId, gameType, mapName)
          .exceptionally(throwable -> {
            notificationService.addNotification(new ImmediateNotification(
                i18n.get("errorTitle"),
                i18n.get("liveReplayCouldNotBeStarted"),
                Severity.ERROR, throwable,
                asList(new DismissAction(i18n), new ReportAction(i18n, reportingService, throwable))
            ));
            return null;
          });
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public CompletableFuture<Void> startReplayServer(int gameUid) {
    return replayServer.start(gameUid);
  }

  @Override
  public void stopReplayServer() {
    replayServer.stop();
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
      notificationService.addNotification(new ImmediateNotification(
          i18n.get("errorTitle"),
          i18n.get("replayCouldNotBeStarted", path.getFileName()),
          WARN, e, singletonList(new ReportAction(i18n, reportingService, e))
      ));
    }
  }

  private void runOnlineReplay(int replayId) {
    downloadReplayToTemporaryDirectory(replayId)
        .thenAccept(this::runReplayFile)
        .exceptionally(throwable -> {
          notificationService.addNotification(new ImmediateNotification(
              i18n.get("errorTitle"),
              i18n.get("replayCouldNotBeDownloaded", replayId),
              Severity.ERROR, throwable,
              singletonList(new ReportAction(i18n, reportingService, throwable)))
          );

          return null;
        });
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
    String mapName = replayInfo.getMapname();

    Set<String> simMods = replayInfo.getSimMods() != null ? replayInfo.getSimMods().keySet() : emptySet();

    Integer version = parseSupComVersion(rawReplayBytes);

    gameService.runWithReplay(tempSupComReplayFile, replayId, gameType, version, modVersions, simMods, mapName);
  }

  private void runSupComReplayFile(Path path) {
    byte[] rawReplayBytes = replayFileReader.readReplayData(path);

    Integer version = parseSupComVersion(rawReplayBytes);
    String mapName = parseMapName(rawReplayBytes);
    String fileName = path.getFileName().toString();
    String gameType = guessModByFileName(fileName);

    gameService.runWithReplay(path, null, gameType, version, emptyMap(), emptySet(), mapName);
  }

  private CompletionStage<Path> downloadReplayToTemporaryDirectory(int replayId) {
    ReplayDownloadTask task = applicationContext.getBean(ReplayDownloadTask.class);
    task.setReplayId(replayId);
    return taskService.submitTask(task).getFuture();
  }
}
