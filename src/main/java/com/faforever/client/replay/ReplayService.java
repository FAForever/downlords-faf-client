package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameService;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.replay.Replay.ChatMessage;
import com.faforever.client.replay.Replay.GameOption;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.task.TaskService;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.commons.replay.ReplayData;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.net.UrlEscapers;
import com.google.common.primitives.Bytes;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.faforever.client.notification.Severity.WARN;
import static com.github.nocatch.NoCatch.noCatch;
import static java.net.URLDecoder.decode;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.move;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;


@Lazy
@Service
@Slf4j
public class ReplayService {

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
  private static final long MAX_REPLAYS = 300;

  private final ClientProperties clientProperties;
  private final PreferencesService preferencesService;
  private final ReplayFileReader replayFileReader;
  private final NotificationService notificationService;
  private final GameService gameService;
  private final TaskService taskService;
  private final I18n i18n;
  private final ReportingService reportingService;
  private final ApplicationContext applicationContext;
  private final PlatformService platformService;
  private final ReplayServer replayServer;
  private final FafService fafService;
  private final ModService modService;
  private final MapService mapService;

  @Inject
  public ReplayService(ClientProperties clientProperties, PreferencesService preferencesService,
                       ReplayFileReader replayFileReader, NotificationService notificationService,
                       GameService gameService, TaskService taskService, I18n i18n,
                       ReportingService reportingService, ApplicationContext applicationContext,
                       PlatformService platformService, ReplayServer replayServer, FafService fafService,
                       ModService modService, MapService mapService) {
    this.clientProperties = clientProperties;
    this.preferencesService = preferencesService;
    this.replayFileReader = replayFileReader;
    this.notificationService = notificationService;
    this.gameService = gameService;
    this.taskService = taskService;
    this.i18n = i18n;
    this.reportingService = reportingService;
    this.applicationContext = applicationContext;
    this.platformService = platformService;
    this.replayServer = replayServer;
    this.fafService = fafService;
    this.modService = modService;
    this.mapService = mapService;
  }

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
    return KnownFeaturedMod.DEFAULT.getTechnicalName();
  }

  /**
   * Loads some, but not all, local replays. Loading all local replays could result in OOME.
   */
  @SneakyThrows
  public Collection<Replay> getLocalReplays() {
    Collection<Replay> replayInfos = new ArrayList<>();

    String replayFileGlob = clientProperties.getReplay().getReplayFileGlob();

    Path replaysDirectory = preferencesService.getReplaysDirectory();
    if (Files.notExists(replaysDirectory)) {
      noCatch(() -> createDirectories(replaysDirectory));
    }

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(replaysDirectory, replayFileGlob)) {
      StreamSupport.stream(directoryStream.spliterator(), false)
          .limit(MAX_REPLAYS)
          .forEach(replayFile -> {
            try {
              LocalReplayInfo replayInfo = replayFileReader.parseMetaData(replayFile);
              FeaturedMod featuredMod = modService.getFeaturedMod(replayInfo.getFeaturedMod()).getNow(FeaturedMod.UNKNOWN);

              mapService.findByMapFolderName(replayInfo.getMapname())
                  .thenAccept(mapBean -> replayInfos.add(new Replay(replayInfo, replayFile, featuredMod, mapBean.orElse(null))));
            } catch (Exception e) {
              logger.warn("Could not read replay file '{}'", replayFile, e);
              moveCorruptedReplayFile(replayFile);
            }
          });
    }

    return replayInfos;
  }

  private void moveCorruptedReplayFile(Path replayFile) {
    Path corruptedReplaysDirectory = preferencesService.getCorruptedReplaysDirectory();
    noCatch(() -> createDirectories(corruptedReplaysDirectory));

    Path target = corruptedReplaysDirectory.resolve(replayFile.getFileName());

    logger.debug("Moving corrupted replay file from {} to {}", replayFile, target);

    noCatch(() -> move(replayFile, target));

    notificationService.addNotification(new PersistentNotification(
        i18n.get("corruptedReplayFiles.notification"), WARN,
        singletonList(
            new Action(i18n.get("corruptedReplayFiles.show"), event -> platformService.reveal(replayFile))
        )
    ));
  }


  public void runReplay(Replay item) {
    if (item.getReplayFile() != null) {
      runReplayFile(item.getReplayFile());
    } else {
      runOnlineReplay(item.getId());
    }
  }


  public void runLiveReplay(int gameId, int playerId) {
    Game game = gameService.getByUid(gameId);
    if (game == null) {
      throw new RuntimeException("There's no game with ID: " + gameId);
    }

    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setScheme(FAF_LIFE_PROTOCOL);
    // TODO check if this host is correct
    uriBuilder.setHost(clientProperties.getReplay().getRemoteHost());
    uriBuilder.setPath("/" + gameId + "/" + playerId + SUP_COM_REPLAY_FILE_ENDING);
    uriBuilder.addParameter("map", UrlEscapers.urlFragmentEscaper().escape(game.getMapFolderName()));
    uriBuilder.addParameter("mod", game.getFeaturedMod());

    noCatch(() -> runLiveReplay(uriBuilder.build()));
  }


  public void runLiveReplay(URI uri) {
    logger.debug("Running replay from URL: {}", uri);
    if (!uri.getScheme().equals(FAF_LIFE_PROTOCOL)) {
      throw new IllegalArgumentException("Invalid protocol: " + uri.getScheme());
    }

    Map<String, String> queryParams = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(uri.getQuery());

    String gameType = queryParams.get("mod");
    String mapName = noCatch(() -> decode(queryParams.get("map"), UTF_8.name()));
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


  public CompletableFuture<Integer> startReplayServer(int gameUid) {
    return replayServer.start(gameUid);
  }


  public void stopReplayServer() {
    replayServer.stop();
  }


  public void runReplay(Integer replayId) {
    runOnlineReplay(replayId);
  }


  public CompletableFuture<List<Replay>> getNewestReplays(int topElementCount, int page) {
    return fafService.getNewestReplays(topElementCount, page);
  }


  public CompletableFuture<List<Replay>> getHighestRatedReplays(int topElementCount, int page) {
    return fafService.getHighestRatedReplays(topElementCount, page);
  }


  public CompletableFuture<List<Replay>> findByQuery(String query, int maxResults, int page, SortConfig sortConfig) {
    return fafService.findReplaysByQuery(query, maxResults, page, sortConfig);
  }


  public CompletableFuture<Optional<Replay>> findById(int id) {
    return fafService.findReplayById(id);

  }


  public CompletableFuture<Path> downloadReplay(int id) {
    ReplayDownloadTask task = applicationContext.getBean(ReplayDownloadTask.class);
    task.setReplayId(id);
    return taskService.submitTask(task).getFuture();
  }

  /**
   * Reads the specified replay file in order to add more information to the specified replay instance.
   */
  public void enrich(Replay replay, Path path) {
    ReplayData replayData = replayFileReader.parseReplay(path);
    replay.getChatMessages().setAll(replayData.getChatMessages().stream()
        .map(chatMessage -> new ChatMessage(chatMessage.getTime(), chatMessage.getSender(), chatMessage.getMessage()))
        .collect(Collectors.toList())
    );
    replay.getGameOptions().setAll(replayData.getGameOptions().stream()
        .map(gameOption -> new GameOption(gameOption.getKey(), gameOption.getValue()))
        .collect(Collectors.toList())
    );
  }


  @SneakyThrows
  public CompletableFuture<Integer> getSize(int id) {
    return CompletableFuture.supplyAsync(() -> noCatch(() -> new URL(String.format(clientProperties.getVault().getReplayDownloadUrlFormat(), id))
        .openConnection()
        .getContentLength()));
  }


  public boolean replayChangedRating(Replay replay) {
    return replay.getTeamPlayerStats().values().stream()
        .flatMap(Collection::stream)
        .anyMatch(playerStats -> playerStats.getAfterMean() != null && playerStats.getAfterDeviation() != null);
  }

  @SneakyThrows

  public void runReplayFile(Path path) {
    log.debug("Starting replay file: {}", path.toAbsolutePath());

    String fileName = path.getFileName().toString();
    if (fileName.endsWith(FAF_REPLAY_FILE_ENDING)) {
      runFafReplayFile(path);
    } else if (fileName.endsWith(SUP_COM_REPLAY_FILE_ENDING)) {
      runSupComReplayFile(path);
    }
  }

  private void runOnlineReplay(int replayId) {
    downloadReplay(replayId)
        .thenAccept(this::runReplayFile)
        .exceptionally(throwable -> {
          notificationService.addNotification(new ImmediateNotification(
              i18n.get("errorTitle"),
              i18n.get("replayCouldNotBeStarted", replayId),
              Severity.ERROR, throwable,
              singletonList(new ReportAction(i18n, reportingService, throwable)))
          );

          return null;
        });
  }

  private void runFafReplayFile(Path path) throws IOException {
    byte[] rawReplayBytes = replayFileReader.readRawReplayData(path);

    Path tempSupComReplayFile = preferencesService.getCacheDirectory().resolve(TEMP_SCFA_REPLAY_FILE_NAME);

    createDirectories(tempSupComReplayFile.getParent());
    Files.copy(new ByteArrayInputStream(rawReplayBytes), tempSupComReplayFile, StandardCopyOption.REPLACE_EXISTING);

    LocalReplayInfo replayInfo = replayFileReader.parseMetaData(path);
    String gameType = replayInfo.getFeaturedMod();
    Integer replayId = replayInfo.getUid();
    Map<String, Integer> modVersions = replayInfo.getFeaturedModVersions();
    String mapName = replayInfo.getMapname();

    Set<String> simMods = replayInfo.getSimMods() != null ? replayInfo.getSimMods().keySet() : emptySet();

    Integer version = parseSupComVersion(rawReplayBytes);

    gameService.runWithReplay(tempSupComReplayFile, replayId, gameType, version, modVersions, simMods, mapName);
  }

  private void runSupComReplayFile(Path path) {
    byte[] rawReplayBytes = replayFileReader.readRawReplayData(path);

    Integer version = parseSupComVersion(rawReplayBytes);
    String mapName = parseMapName(rawReplayBytes);
    String fileName = path.getFileName().toString();
    String gameType = guessModByFileName(fileName);

    gameService.runWithReplay(path, null, gameType, version, emptyMap(), emptySet(), mapName);
  }
}
