package com.faforever.client.replay;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.discord.DiscordSpectateEvent;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.domain.ReplayBean.ChatMessage;
import com.faforever.client.domain.ReplayBean.GameOption;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameService;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.ReplayMapper;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.CopyErrorAction;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.GetHelpAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.UserService;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.client.vault.search.SearchController.SortOrder;
import com.faforever.commons.api.dto.Game;
import com.faforever.commons.api.dto.GameReviewsSummary;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import com.faforever.commons.api.elide.ElideNavigatorOnId;
import com.faforever.commons.replay.ReplayDataParser;
import com.faforever.commons.replay.ReplayMetadata;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.net.UrlEscapers;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.CompressorException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.faforever.client.notification.Severity.WARN;
import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static com.github.nocatch.NoCatch.noCatch;
import static java.net.URLDecoder.decode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.move;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class ReplayService {

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
  private static final Pattern invalidCharacters = Pattern.compile("[?@*%{}<>|\"]");

  private final ClientProperties clientProperties;
  private final PreferencesService preferencesService;
  private final UserService userService;
  private final ReplayFileReader replayFileReader;
  private final NotificationService notificationService;
  private final GameService gameService;
  private final PlayerService playerService;
  private final TaskService taskService;
  private final I18n i18n;
  private final ReportingService reportingService;
  private final ApplicationContext applicationContext;
  private final PlatformService platformService;
  private final FafApiAccessor fafApiAccessor;
  private final ModService modService;
  private final MapService mapService;
  private final ReplayMapper replayMapper;
  protected List<ReplayBean> localReplays = new ArrayList<>();

  @VisibleForTesting
  static Integer parseSupComVersion(ReplayDataParser parser) {
    String[] versionParts = parser.getReplayPatchFieldId().split("\\.");
    return Integer.parseInt(versionParts[versionParts.length - 1]);
  }

  @VisibleForTesting
  static String parseMapFolderName(ReplayDataParser parser) {
    String mapPath = parser.getMap();
    //mapPath looks like /maps/my_awesome_map.v008/my_awesome_map.lua
    Matcher matcher = invalidCharacters.matcher(mapPath);
    if (matcher.find()) {
      throw new IllegalArgumentException("Map Name Contains Invalid Characters");
    }
    return mapPath.split("/")[2];
  }

  @VisibleForTesting
  static Set<String> parseModUIDs(ReplayDataParser parser) {
    return parser.getMods().values().stream()
        .map(map -> (String) map.getOrDefault("uid", null))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  @VisibleForTesting
  static String guessModByFileName(String fileName) {
    String[] splitFileName = fileName.split("\\.");
    if (splitFileName.length > 2) {
      return splitFileName[splitFileName.length - 2];
    }
    return KnownFeaturedMod.DEFAULT.getTechnicalName();
  }

  @Async
  public CompletableFuture<Tuple2<List<ReplayBean>, Integer>> loadLocalReplayPage(int pageSize, int page) throws IOException {
    String replayFileGlob = clientProperties.getReplay().getReplayFileGlob();

    Path replaysDirectory = preferencesService.getReplaysDirectory();
    if (Files.notExists(replaysDirectory)) {
      noCatch(() -> createDirectories(replaysDirectory));
    }

    int skippedReplays = pageSize * (page - 1);

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(replaysDirectory, replayFileGlob)) {
      Stream<Path> filesStream = StreamSupport.stream(directoryStream.spliterator(), false)
          .sorted(Comparator.comparing(path -> noCatch(() -> Files.getLastModifiedTime((Path) path))).reversed());

      List<Path> filesList = filesStream.collect(Collectors.toList());
      int numPages = filesList.size() / pageSize;

      List<CompletableFuture<ReplayBean>> replayFutures = filesList.stream()
          .skip(skippedReplays)
          .limit(pageSize)
          .map(this::tryLoadingLocalReplay)
          .filter(e -> !e.isCompletedExceptionally())
          .collect(Collectors.toList());

      return Mono.fromFuture(CompletableFuture.allOf(replayFutures.toArray(new CompletableFuture[0]))
          .thenApply(ignoredVoid ->
              replayFutures.stream()
                  .map(CompletableFuture::join)
                  .filter(Objects::nonNull)
                  .collect(Collectors.toList()))
      ).zipWith(Mono.just(numPages)).toFuture();
    }
  }


  private CompletableFuture<ReplayBean> tryLoadingLocalReplay(Path replayFile) {
    try {
      ReplayDataParser replayData = replayFileReader.parseReplay(replayFile);
      ReplayMetadata replayMetadata = replayData.getMetadata();

      CompletableFuture<FeaturedModBean> featuredModFuture = modService.getFeaturedMod(replayMetadata.getFeaturedMod());
      CompletableFuture<Optional<MapVersionBean>> mapVersionFuture = mapService.findByMapFolderName(replayMetadata.getMapname());

      return CompletableFuture.allOf(featuredModFuture, mapVersionFuture).thenApply(ignoredVoid -> {
        MapVersionBean mapVersion = mapVersionFuture.join().orElse(null);
        FeaturedModBean featuredMod = featuredModFuture.join();
        if (mapVersion == null) {
          log.warn("Could not find map for replay file '{}'", replayFile);
        }
        return replayMapper.map(replayMetadata, replayFile, featuredMod, mapVersion);
      });
    } catch (Exception e) {
      log.warn("Could not read replay file '{}'", replayFile, e);
      moveCorruptedReplayFile(replayFile);
      return CompletableFuture.completedFuture(null);
    }
  }

  private void moveCorruptedReplayFile(Path replayFile) {
    Path corruptedReplaysDirectory = preferencesService.getCorruptedReplaysDirectory();
    noCatch(() -> createDirectories(corruptedReplaysDirectory));

    Path target = corruptedReplaysDirectory.resolve(replayFile.getFileName());

    log.debug("Moving corrupted replay file from {} to {}", replayFile, target);

    try {
      move(replayFile, target);
    } catch (IOException e) {
      log.warn("Failed to move corrupt replay to " + target, e);
      return;
    }

    notificationService.addNotification(new PersistentNotification(
        i18n.get("corruptedReplayFiles.notification"), WARN,
        singletonList(
            new Action(i18n.get("corruptedReplayFiles.show"), event -> platformService.reveal(replayFile))
        )
    ));
  }

  public void runReplay(ReplayBean item) {
    if (item.getReplayFile() != null) {
      try {
        runReplayFile(item.getReplayFile());
      } catch (Exception e) {
        log.warn("Could not read replay file '{}'", item.getReplayFile(), e);
        notificationService.addImmediateErrorNotification(e, "replay.couldNotParse");
      }
    } else {
      runOnlineReplay(item.getId());
    }
  }


  public void runLiveReplay(int gameId) {
    GameBean game = gameService.getByUid(gameId);
    if (game == null) {
      throw new RuntimeException("There's no game with ID: " + gameId);
    }
    /* A courtesy towards the replay server so we can see in logs who we're dealing with. */
    String playerName = playerService.getCurrentPlayer().getUsername();

    URI uri = UriComponentsBuilder.newInstance()
        .scheme(FAF_LIFE_PROTOCOL)
        .host(clientProperties.getReplay().getRemoteHost())
        .path("/" + gameId + "/" + playerName + SUP_COM_REPLAY_FILE_ENDING)
        .queryParam("map", UrlEscapers.urlFragmentEscaper().escape(game.getMapFolderName()))
        .queryParam("mod", game.getFeaturedMod())
        .build()
        .toUri();

    noCatch(() -> runLiveReplay(uri));
  }


  public void runLiveReplay(URI uri) {
    log.debug("Running replay from URL: {}", uri);
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
                List.of(new CopyErrorAction(i18n, reportingService, throwable), new GetHelpAction(i18n, reportingService), new DismissAction(i18n))
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

  public CompletableFuture<Path> downloadReplay(int id) {
    ReplayDownloadTask task = applicationContext.getBean(ReplayDownloadTask.class);
    task.setReplayId(id);
    return taskService.submitTask(task).getFuture();
  }

  /**
   * Reads the specified replay file in order to add more information to the specified replay instance.
   */
  public void enrich(ReplayBean replay, Path path) {
    try {
      ReplayDataParser replayDataParser = replayFileReader.parseReplay(path);
      replay.getChatMessages().setAll(replayDataParser.getChatMessages().stream()
          .map(chatMessage -> new ChatMessage(chatMessage.getTime(), chatMessage.getSender(), chatMessage.getMessage()))
          .collect(Collectors.toList())
      );
      replay.getGameOptions().setAll(replayDataParser.getGameOptions().stream()
          .map(gameOption -> new GameOption(gameOption.getKey(), gameOption.getValue()))
          .collect(Collectors.toList())
      );
      if (replay.getMapVersion() == null) {
        MapVersionBean mapVersion = new MapVersionBean();
        MapBean map = new MapBean();
        mapVersion.setMap(map);
        String mapFolderName = parseMapFolderName(replayDataParser);
        mapVersion.setFolderName(mapFolderName);
        mapVersion.setMap(map);
        map.setDisplayName(mapFolderName);
        replay.setMapVersion(mapVersion);
      }
    } catch (Exception e) {
      log.warn("Could not read replay file '{}'", path, e);
      return;
    }
  }


  @SneakyThrows
  public CompletableFuture<Integer> getSize(int id) {
    return CompletableFuture.supplyAsync(() -> noCatch(() -> new URL(String.format(clientProperties.getVault().getReplayDownloadUrlFormat(), id))
        .openConnection()
        .getContentLength()));
  }


  public boolean replayChangedRating(ReplayBean replay) {
    return replay.getTeamPlayerStats().values().stream()
        .flatMap(Collection::stream)
        .flatMap(playerStats -> playerStats.getLeaderboardRatingJournals().stream())
        .anyMatch(ratingJournal -> ratingJournal.getMeanAfter() != null && ratingJournal.getDeviationAfter() != null);
  }

  public void runReplayFile(Path path) throws IOException, CompressorException {
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
        .thenAccept((path) -> {
          try {
            runReplayFile(path);
          } catch (IOException | CompressorException e) {
            throw new RuntimeException(e);
          }
        })
        .exceptionally(throwable -> {
          if (throwable.getCause() instanceof FileNotFoundException) {
            log.warn("Replay not available on server yet", throwable);
            notificationService.addImmediateWarnNotification("replayNotAvailable", replayId);
          } else {
            log.error("Replay could not be started", throwable);
            notificationService.addImmediateErrorNotification(throwable, "replayCouldNotBeStarted", replayId);
          }
          return null;
        });
  }

  private void runFafReplayFile(Path path) throws IOException, CompressorException {
    ReplayDataParser replayData = replayFileReader.parseReplay(path);
    byte[] rawReplayBytes = replayData.getData();

    Path tempSupComReplayFile = preferencesService.getCacheDirectory().resolve(TEMP_SCFA_REPLAY_FILE_NAME);

    createDirectories(tempSupComReplayFile.getParent());
    Files.copy(new ByteArrayInputStream(rawReplayBytes), tempSupComReplayFile, StandardCopyOption.REPLACE_EXISTING);

    ReplayMetadata replayMetadata = replayData.getMetadata();
    String gameType = replayMetadata.getFeaturedMod();
    Integer replayId = replayMetadata.getUid();
    Map<String, Integer> modVersions = replayMetadata.getFeaturedModVersions();
    String mapName = parseMapFolderName(replayData);

    Set<String> simMods = parseModUIDs(replayData);

    Integer version = parseSupComVersion(replayData);

    gameService.runWithReplay(tempSupComReplayFile, replayId, gameType, version, modVersions, simMods, mapName);
  }

  private void runSupComReplayFile(Path path) throws IOException, CompressorException {
    ReplayDataParser replayData = replayFileReader.parseReplay(path);

    Integer version = parseSupComVersion(replayData);
    String mapName = parseMapFolderName(replayData);
    String fileName = path.getFileName().toString();
    String gameType = guessModByFileName(fileName);
    Set<String> simMods = parseModUIDs(replayData);

    gameService.runWithReplay(path, null, gameType, version, emptyMap(), simMods, mapName);
  }

  @EventListener
  public void onDiscordGameJoinEvent(DiscordSpectateEvent discordSpectateEvent) {
    Integer replayId = discordSpectateEvent.getReplayId();
    runLiveReplay(replayId);
  }

  @Cacheable(value = CacheNames.REPLAYS_RECENT, sync = true)
  public CompletableFuture<Tuple2<List<ReplayBean>, Integer>> getNewestReplaysWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<Game> navigator = ElideNavigator.of(Game.class).collection()
        .setFilter(qBuilder().instant("endTime").after(Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), false))
        .addSortingRule("endTime", false);
    return getReplayPage(navigator, count, page);
  }

  @Cacheable(value = CacheNames.REPLAYS_SEARCH, sync = true)
  public CompletableFuture<Tuple2<List<ReplayBean>, Integer>> getReplaysForPlayerWithPageCount(int playerId, int count, int page) {
    ElideNavigatorOnCollection<Game> navigator = ElideNavigator.of(Game.class).collection()
        .setFilter(qBuilder().intNum("playerStats.player.id").eq(playerId).and().instant("endTime").after(Instant.now().minus(365, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), false))
        .addSortingRule("endTime", false);
    return getReplayPage(navigator, count, page);
  }

  @Cacheable(value = CacheNames.REPLAYS_LIKED, sync = true)
  public CompletableFuture<Tuple2<List<ReplayBean>, Integer>> getHighestRatedReplaysWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<GameReviewsSummary> navigator = ElideNavigator.of(GameReviewsSummary.class).collection()
        .addSortingRule("lowerBound", false)
        .pageSize(count)
        .pageNumber(page);
    return fafApiAccessor.getManyWithPageCount(navigator)
        .map(tuple -> tuple.mapT1(mods ->
            mods.stream().map(dto -> replayMapper.map(dto.getGame(), new CycleAvoidingMappingContext())).collect(toList())
        ))
        .toFuture();
  }

  @Cacheable(value = CacheNames.REPLAYS_SEARCH, sync = true)
  public CompletableFuture<Tuple2<List<ReplayBean>, Integer>> findByQueryWithPageCount(SearchConfig searchConfig, int count, int page) {
    SortConfig sortConfig = searchConfig.getSortConfig();
    ElideNavigatorOnCollection<Game> navigator = ElideNavigator.of(Game.class).collection()
        .addSortingRule(sortConfig.getSortProperty(), sortConfig.getSortOrder().equals(SortOrder.ASC));
    return getReplayPage(navigator, searchConfig.getSearchQuery(), count, page);
  }

  @Cacheable(value = CacheNames.REPLAYS_SEARCH, sync = true)
  public CompletableFuture<Optional<ReplayBean>> findById(int id) {
    ElideNavigatorOnId<Game> navigator = ElideNavigator.of(Game.class).id(String.valueOf(id));
    return fafApiAccessor.getOne(navigator)
        .map(dto -> replayMapper.map(dto, new CycleAvoidingMappingContext()))
        .toFuture()
        .thenApply(Optional::ofNullable);
  }

  @Cacheable(value = CacheNames.REPLAYS_MINE, sync = true)
  public CompletableFuture<Tuple2<List<ReplayBean>, Integer>> getOwnReplaysWithPageCount(int count, int page) {
    return getReplaysForPlayerWithPageCount(userService.getUserId(), count, page);
  }

  private CompletableFuture<Tuple2<List<ReplayBean>, Integer>> getReplayPage(ElideNavigatorOnCollection<Game> navigator, int count, int page) {
    navigator.pageSize(count).pageNumber(page);
    return fafApiAccessor.getManyWithPageCount(navigator)
        .map(tuple -> tuple.mapT1(mods ->
            mods.stream().map(dto -> replayMapper.map(dto, new CycleAvoidingMappingContext())).collect(toList())
        ))
        .toFuture();
  }

  private CompletableFuture<Tuple2<List<ReplayBean>, Integer>> getReplayPage(ElideNavigatorOnCollection<Game> navigator, String customFilter, int count, int page) {
    navigator.pageNumber(page).pageSize(count);
    return fafApiAccessor.getManyWithPageCount(navigator, customFilter)
        .map(tuple -> tuple.mapT1(mods ->
            mods.stream().map(dto -> replayMapper.map(dto, new CycleAvoidingMappingContext())).collect(toList())
        ))
        .toFuture();
  }
}
