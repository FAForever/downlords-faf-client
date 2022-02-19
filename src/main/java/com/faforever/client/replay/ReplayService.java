package com.faforever.client.replay;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.FeaturedModBean;
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
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.FileSizeReader;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.CompressorException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class ReplayService {

  private static final String FAF_REPLAY_FILE_ENDING = ".fafreplay";
  public static final String SUP_COM_REPLAY_FILE_ENDING = ".scfareplay";
  private static final String TEMP_SCFA_REPLAY_FILE_NAME = "temp.scfareplay";
  private static final Pattern invalidCharacters = Pattern.compile("[?@*%{}<>|\"]");

  private final ClientProperties clientProperties;
  private final PreferencesService preferencesService;
  private final UserService userService;
  private final ReplayFileReader replayFileReader;
  private final NotificationService notificationService;
  private final GameService gameService;
  private final TaskService taskService;
  private final I18n i18n;
  private final ApplicationContext applicationContext;
  private final PlatformService platformService;
  private final FafApiAccessor fafApiAccessor;
  private final ModService modService;
  private final MapService mapService;
  private final FileSizeReader fileSizeReader;
  private final ReplayMapper replayMapper;

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

    Path replaysDirectory = preferencesService.getPreferences().getData().getReplaysDirectory();
    if (Files.notExists(replaysDirectory)) {
      Files.createDirectories(replaysDirectory);
    }

    int skippedReplays = pageSize * (page - 1);

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(replaysDirectory, replayFileGlob)) {
      Stream<Path> filesStream = StreamSupport.stream(directoryStream.spliterator(), false)
          .sorted(Comparator.comparing(path -> {
            try {
              return Files.getLastModifiedTime((Path) path);
            } catch (IOException e) {
              log.warn("Could not get last modified time of file {}", path, e);
              return FileTime.from(Instant.EPOCH);
            }
          }).reversed());

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
        return replayMapper.map(replayData, replayFile, featuredMod, mapVersion);
      }).exceptionally(throwable -> {
        log.warn("Could not read replay file '{}'", replayFile, throwable);
        moveCorruptedReplayFile(replayFile);
        return null;
      });
    } catch (Exception e) {
      log.warn("Could not read replay file '{}'", replayFile, e);
      moveCorruptedReplayFile(replayFile);
      return CompletableFuture.completedFuture(null);
    }
  }

  private void moveCorruptedReplayFile(Path replayFile) {
    Path corruptedReplaysDirectory = preferencesService.getPreferences().getData().getCorruptedReplaysDirectory();
    try {
      Files.createDirectories(corruptedReplaysDirectory);
    } catch (IOException e) {
      log.warn("Failed to create corrupted replays directory", e);
      return;
    }

    Path target = corruptedReplaysDirectory.resolve(replayFile.getFileName());

    log.debug("Moving corrupted replay file from {} to {}", replayFile, target);

    try {
      Files.move(replayFile, target);
    } catch (IOException e) {
      log.warn("Failed to move corrupt replay to {}", target, e);
      return;
    }

    notificationService.addNotification(new PersistentNotification(
        i18n.get("corruptedReplayFiles.notification"), WARN,
        singletonList(
            new Action(i18n.get("corruptedReplayFiles.show"), event -> platformService.reveal(replayFile))
        )
    ));
  }

  public boolean deleteReplayFile(Path replayFile) {
    try {
      Files.delete(replayFile);
      return true;
    } catch (IOException e) {
      log.error("Failed to delete local replay file {}", replayFile, e);
      notificationService.addImmediateErrorNotification(e, "replay.couldNotDeleteLocal");
    }
    return false;
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
      replay.getGameOptions().add(0, new GameOption("FAF Version", String.valueOf(parseSupComVersion(replayDataParser))));
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
    }
  }

  public CompletableFuture<Integer> getFileSize(ReplayBean replay) {
    try {
      return fileSizeReader.getFileSize(new URL(String.format(clientProperties.getVault().getReplayDownloadUrlFormat(), replay.getId())));
    } catch (MalformedURLException e) {
      log.warn("Could not open connection to replay download", e);
      return CompletableFuture.completedFuture(-1);
    }
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

    Path tempSupComReplayFile = preferencesService.getPreferences().getData().getCacheDirectory().resolve(TEMP_SCFA_REPLAY_FILE_NAME);

    Files.createDirectories(tempSupComReplayFile.getParent());
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
