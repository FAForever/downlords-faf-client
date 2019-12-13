package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.discord.DiscordSpectateEvent;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameService;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.LocalReplaysChangedEvent;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateErrorNotification;
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
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.net.UrlEscapers;
import com.google.common.primitives.Bytes;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.faforever.client.notification.Severity.WARN;
import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static com.github.nocatch.NoCatch.noCatch;
import static java.lang.Thread.sleep;
import static java.net.URLDecoder.decode;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.move;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class ReplayService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Byte offset at which a SupCom replay's version number starts.
   */
  private static final int VERSION_OFFSET = 0x18;
  private static final int MAP_NAME_OFFSET = 0x2D;
  private static final byte[] MAP_FOLDER_START_PATTERN = new byte[]
      {0x53, 0x63, 0x65, 0x6E, 0x61, 0x72, 0x69, 0x6F, 0x46, 0x69, 0x6C, 0x65, 0x00, 0x01};
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
  private final FafService fafService;
  private final ModService modService;
  private final MapService mapService;
  private final ApplicationEventPublisher publisher;
  private final ExecutorService executorService;
  private Thread directoryWatcherThread;
  private WatchService watchService;
  private List<Replay> localReplays = new ArrayList<Replay>();

  public void startLoadingAndWatchingLocalReplays() {
    Path replaysDirectory = preferencesService.getReplaysDirectory();
    if (Files.notExists(replaysDirectory)) {
      noCatch(() -> createDirectories(replaysDirectory));
    }

    LoadLocalReplaysTask loadLocalReplaysTask = applicationContext.getBean(LoadLocalReplaysTask.class);
    taskService.submitTask(loadLocalReplaysTask).getFuture().thenAccept( replays -> {
      localReplays.clear();
      localReplays.addAll(replays);
      publisher.publishEvent(new LocalReplaysChangedEvent(this, replays, new ArrayList<Replay>()));
    });

    try {
      Optional.ofNullable(directoryWatcherThread).ifPresent(Thread::interrupt);
      directoryWatcherThread = startDirectoryWatcher(replaysDirectory);
    } catch (IOException e) {
      logger.warn("Failed to start watching the local replays directory");
    }
  }

  public Collection<Replay> getLocalReplays() {
    return localReplays;
  }

  protected Thread startDirectoryWatcher(Path replaysDirectory) throws IOException {
    Thread thread = new Thread(() -> noCatch(() -> {
      try (WatchService watcher = replaysDirectory.getFileSystem().newWatchService()) {
        replaysDirectory.register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        while (!Thread.interrupted()) {
          WatchKey key = watcher.take();
          onLocalReplaysWatchEvent(key);
          key.reset();
        }
      } catch (InterruptedException e) {
        logger.debug("Local replay directory watcher terminated ({})", e.getMessage());
      }
    }));
    thread.setDaemon(true);
    thread.start();
    return thread;
  }

  private void onLocalReplaysWatchEvent(WatchKey key) {
    Collection<Replay> newReplays = new ArrayList<Replay>();
    Collection<Replay> deletedReplays = new ArrayList<Replay>();
    for (WatchEvent<?> watchEvent : key.pollEvents()) {
      Path path = (Path) watchEvent.context();
      Path fullPathToReplay = preferencesService.getReplaysDirectory().resolve(path);

      if (watchEvent.kind() == ENTRY_CREATE) {
        tryLoadingLocalReplay(fullPathToReplay)
            .thenAccept(newReplay -> newReplays.add(newReplay));
      } else if (watchEvent.kind() == ENTRY_DELETE) {
        Optional<Replay> existingReplay = localReplays
            .stream()
            .filter(replay -> replay.getReplayFile().compareTo(fullPathToReplay) == 0)
            .findFirst();

        if (existingReplay.isPresent()) {
          Replay deletedReplay = existingReplay.get();
          deletedReplays.add(deletedReplay);
          localReplays.remove(deletedReplay);
        }
      }
    }

    localReplays.addAll(newReplays);
    publisher.publishEvent(new LocalReplaysChangedEvent(this, newReplays, deletedReplays));
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
  static String parseMapFolderName(byte[] rawReplayBytes) {
    // "ScenarioFile" in hex
    int mapStartIndex = Bytes.indexOf(rawReplayBytes, MAP_FOLDER_START_PATTERN) + MAP_FOLDER_START_PATTERN.length;
    int mapEndIndex = 0;

    for (mapEndIndex = mapStartIndex; mapEndIndex < rawReplayBytes.length - 1; mapEndIndex++) {
      //0x00 0x01 is the field delimiter
      if (rawReplayBytes[mapEndIndex] == 0x00 && rawReplayBytes[mapEndIndex+1] == 0x01) {
        break;
      }
    }

    String mapPath = new String(rawReplayBytes, mapStartIndex, mapEndIndex + 1 - mapStartIndex, US_ASCII);
    //mapPath looks like /maps/my_awesome_map.v008/my_awesome_map.lua
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
  @Async
  public CompletableFuture<Collection<Replay>> loadLocalReplays() throws IOException {
    String replayFileGlob = clientProperties.getReplay().getReplayFileGlob();

    Path replaysDirectory = preferencesService.getReplaysDirectory();
    if (Files.notExists(replaysDirectory)) {
      noCatch(() -> createDirectories(replaysDirectory));
    }


    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(replaysDirectory, replayFileGlob)) {
      List<CompletableFuture<Replay>> replayFutures = StreamSupport.stream(directoryStream.spliterator(), false)
          .sorted(Comparator.comparing(path -> noCatch(() -> Files.getLastModifiedTime((Path) path))).reversed())
          .limit(MAX_REPLAYS)
          .map( replayFile -> tryLoadingLocalReplay(replayFile))
          .filter(e -> !e.isCompletedExceptionally())
          .collect(Collectors.toList());

      CompletableFuture[] replayFuturesArray = replayFutures.toArray(new CompletableFuture[replayFutures.size()]);
      return CompletableFuture.allOf(replayFuturesArray)
          .thenApply(ignoredVoid ->
              replayFutures.stream()
                  .map(future -> future.join())
                  .collect(Collectors.toList()));

    }
  }

  @Async
  private CompletableFuture<Replay> tryLoadingLocalReplay(Path replayFile)  {
    try {
      LocalReplayInfo replayInfo = replayFileReader.parseMetaData(replayFile);

      CompletableFuture<FeaturedMod> featuredModFuture = modService.getFeaturedMod(replayInfo.getFeaturedMod());
      CompletableFuture<Optional<MapBean>> mapBeanFuture = mapService.findByMapFolderName(replayInfo.getMapname());

      return CompletableFuture.allOf(featuredModFuture, mapBeanFuture).thenApply(ignoredVoid  -> {
        Optional<MapBean> mapBean = mapBeanFuture.join();
        if (!mapBean.isPresent()) {
          throw new CompletionException(new FileNotFoundException());
        }
        return new Replay(replayInfo, replayFile, featuredModFuture.join(), mapBean.get());
      });
    } catch (Exception e) {
      logger.warn("Could not read replay file '{}'", replayFile, e);
      moveCorruptedReplayFile(replayFile);
      return CompletableFuture.failedFuture(e);
    }
  }

  private void moveCorruptedReplayFile(Path replayFile) {
    Path corruptedReplaysDirectory = preferencesService.getCorruptedReplaysDirectory();
    noCatch(() -> createDirectories(corruptedReplaysDirectory));

    Path target = corruptedReplaysDirectory.resolve(replayFile.getFileName());

    logger.debug("Moving corrupted replay file from {} to {}", replayFile, target);

    try {
      move(replayFile, target);
    } catch (IOException e) {
      logger.warn("Failed to move corrupt replay to " + target, e);
      return;
    }

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

    URI uri = UriComponentsBuilder.newInstance()
        .scheme(FAF_LIFE_PROTOCOL)
        .host(clientProperties.getReplay().getRemoteHost())
        .path("/" + gameId + "/" + playerId + SUP_COM_REPLAY_FILE_ENDING)
        .queryParam("map", UrlEscapers.urlFragmentEscaper().escape(game.getMapFolderName()))
        .queryParam("mod", game.getFeaturedMod())
        .build()
        .toUri();

    noCatch(() -> runLiveReplay(uri));
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


  public void runReplay(Integer replayId) {
    runOnlineReplay(replayId);
  }


  public CompletableFuture<List<Replay>> getNewestReplays(int topElementCount, int page) {
    return fafService.getNewestReplays(topElementCount, page);
  }

  public CompletableFuture<List<Replay>> getReplaysForPlayer(int playerId, int maxResults, int page, SortConfig sortConfig) {
    Condition<?> filterCondition = qBuilder().intNum("playerStats.player.id").eq(playerId);
    String query = filterCondition.query(new RSQLVisitor());
    return fafService.findReplaysByQuery(query, maxResults, page, sortConfig);
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
          notificationService.addNotification(new ImmediateErrorNotification(
              i18n.get("errorTitle"), i18n.get("replayCouldNotBeStarted", replayId), throwable, i18n, reportingService
          ));
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

    // For some reason in the coop replay the map name is null in the metadata
    // So we just take it directly from the replay data.
    if (StringUtils.isEmptyOrNull(mapName)) {
      mapName = parseMapFolderName(rawReplayBytes);
    }


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

  @EventListener
  public void onDiscordGameJoinEvent(DiscordSpectateEvent discordSpectateEvent) {
    Integer replayId = discordSpectateEvent.getReplayId();
    Integer playerId = discordSpectateEvent.getPlayerId();

    runLiveReplay(replayId, playerId);
  }
}
