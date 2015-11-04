package com.faforever.client.game;

import com.faforever.client.fa.ForgedAllianceService;
import com.faforever.client.fa.RatingMode;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.OnGameInfoListener;
import com.faforever.client.legacy.OnGameTypeInfoListener;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.GameTypeInfo;
import com.faforever.client.legacy.proxy.Proxy;
import com.faforever.client.map.MapService;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.patch.GameUpdateService;
import com.faforever.client.play.PlayServices;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.rankedmatch.OnRankedMatchNotificationListener;
import com.faforever.client.relay.LocalRelayServer;
import com.faforever.client.stats.domain.Army;
import com.faforever.client.stats.domain.EconomyStat;
import com.faforever.client.stats.domain.EconomyStats;
import com.faforever.client.stats.domain.GameStats;
import com.faforever.client.stats.domain.SummaryStat;
import com.faforever.client.stats.domain.Unit;
import com.faforever.client.stats.domain.UnitCategory;
import com.faforever.client.stats.domain.UnitStat;
import com.faforever.client.stats.domain.UnitType;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.faforever.client.stats.domain.Unit.AEON_ACU;
import static com.faforever.client.stats.domain.Unit.AEON_SACU;
import static com.faforever.client.stats.domain.Unit.AEON_T1_ENGINEER;
import static com.faforever.client.stats.domain.Unit.AEON_T2_ENGINEER;
import static com.faforever.client.stats.domain.Unit.AEON_T3_ENGINEER;
import static com.faforever.client.stats.domain.Unit.AHWASSA;
import static com.faforever.client.stats.domain.Unit.ALUMINAR;
import static com.faforever.client.stats.domain.Unit.ATLANTIS;
import static com.faforever.client.stats.domain.Unit.C14_STAR_LIFTER;
import static com.faforever.client.stats.domain.Unit.C6_COURIER;
import static com.faforever.client.stats.domain.Unit.CHARIOT;
import static com.faforever.client.stats.domain.Unit.CONTINENTAL;
import static com.faforever.client.stats.domain.Unit.CORONA;
import static com.faforever.client.stats.domain.Unit.CYBRAN_ACU;
import static com.faforever.client.stats.domain.Unit.CYBRAN_SACU;
import static com.faforever.client.stats.domain.Unit.CYBRAN_T1_ENGINEER;
import static com.faforever.client.stats.domain.Unit.CYBRAN_T2_ENGINEER;
import static com.faforever.client.stats.domain.Unit.CYBRAN_T3_ENGINEER;
import static com.faforever.client.stats.domain.Unit.CZAR;
import static com.faforever.client.stats.domain.Unit.DRAGON_FLY;
import static com.faforever.client.stats.domain.Unit.FATBOY;
import static com.faforever.client.stats.domain.Unit.FIRE_BEETLE;
import static com.faforever.client.stats.domain.Unit.GALACTIC_COLOSSUS;
import static com.faforever.client.stats.domain.Unit.GEMINI;
import static com.faforever.client.stats.domain.Unit.IAZYNE;
import static com.faforever.client.stats.domain.Unit.MAVOR;
import static com.faforever.client.stats.domain.Unit.MEGALITH;
import static com.faforever.client.stats.domain.Unit.MERCY;
import static com.faforever.client.stats.domain.Unit.MONKEYLORD;
import static com.faforever.client.stats.domain.Unit.NOVAX_CENTER;
import static com.faforever.client.stats.domain.Unit.PARAGON;
import static com.faforever.client.stats.domain.Unit.SALVATION;
import static com.faforever.client.stats.domain.Unit.SCATHIS;
import static com.faforever.client.stats.domain.Unit.SERAPHIM_ACU;
import static com.faforever.client.stats.domain.Unit.SERAPHIM_SACU;
import static com.faforever.client.stats.domain.Unit.SERAPHIM_T1_ENGINEER;
import static com.faforever.client.stats.domain.Unit.SERAPHIM_T2_ENGINEER;
import static com.faforever.client.stats.domain.Unit.SERAPHIM_T3_ENGINEER;
import static com.faforever.client.stats.domain.Unit.SKYHOOK;
import static com.faforever.client.stats.domain.Unit.SOUL_RIPPER;
import static com.faforever.client.stats.domain.Unit.TEMPEST;
import static com.faforever.client.stats.domain.Unit.UEF_ACU;
import static com.faforever.client.stats.domain.Unit.UEF_SACU;
import static com.faforever.client.stats.domain.Unit.UEF_T1_ENGINEER;
import static com.faforever.client.stats.domain.Unit.UEF_T2_ENGINEER;
import static com.faforever.client.stats.domain.Unit.UEF_T2_FIELD_ENGINEER;
import static com.faforever.client.stats.domain.Unit.UEF_T3_ENGINEER;
import static com.faforever.client.stats.domain.Unit.UNKNOWN;
import static com.faforever.client.stats.domain.Unit.VISH;
import static com.faforever.client.stats.domain.Unit.VISHALA;
import static com.faforever.client.stats.domain.Unit.WASP;
import static com.faforever.client.stats.domain.Unit.YOLONA_OSS;
import static com.faforever.client.stats.domain.Unit.YTHOTHA;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class GameServiceImpl implements GameService, OnGameTypeInfoListener, OnGameInfoListener {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String CIVILIAN = "civilian";
  private static final String AI_INDICATOR = "AI: ";
  private static final Unit[] ACUS = {AEON_ACU, CYBRAN_ACU, UEF_ACU, SERAPHIM_ACU};
  private static final Unit[] SACUS = {AEON_SACU, CYBRAN_SACU, UEF_SACU, SERAPHIM_SACU};
  private static final Unit[] TRANSPORTS = {CHARIOT, ALUMINAR, SKYHOOK, DRAGON_FLY, C6_COURIER, C14_STAR_LIFTER, CONTINENTAL, VISH, VISHALA};
  private static final Unit[] EXPERIMENTALS = {YOLONA_OSS, PARAGON, ATLANTIS, TEMPEST, SCATHIS, MAVOR, CZAR, AHWASSA, YTHOTHA, FATBOY, MONKEYLORD, GALACTIC_COLOSSUS, SOUL_RIPPER, MEGALITH, NOVAX_CENTER};
  private static final Unit[] ASFS = {CORONA, GEMINI, WASP, IAZYNE};
  private static final Unit[] ENGINEERS = {AEON_T1_ENGINEER, AEON_T2_ENGINEER, AEON_T3_ENGINEER, CYBRAN_T1_ENGINEER, CYBRAN_T2_ENGINEER, CYBRAN_T3_ENGINEER, UEF_T1_ENGINEER,
      UEF_T2_ENGINEER, UEF_T2_FIELD_ENGINEER, UEF_T3_ENGINEER, SERAPHIM_T1_ENGINEER, SERAPHIM_T2_ENGINEER, SERAPHIM_T3_ENGINEER};

  private final Collection<OnGameStartedListener> onGameLaunchingListeners;
  private final ObservableMap<String, GameTypeBean> gameTypeBeans;
  // It is indeed ugly to keep references in both, a list and a map, however I don't see how I can populate the map
  // values as an observable list (in order to display it in the games table)
  private final ObservableList<GameInfoBean> gameInfoBeans;
  private final Map<Integer, GameInfoBean> uidToGameInfoBean;

  @Autowired
  LobbyServerAccessor lobbyServerAccessor;
  @Autowired
  ForgedAllianceService forgedAllianceService;
  @Autowired
  MapService mapService;
  @Autowired
  Proxy proxy;
  @Autowired
  PreferencesService preferencesService;
  @Autowired
  GameUpdateService gameUpdateService;
  @Autowired
  NotificationService notificationService;
  @Autowired
  I18n i18n;
  @Autowired
  Environment environment;
  @Autowired
  ApplicationContext applicationContext;
  @Autowired
  ScheduledExecutorService scheduledExecutorService;
  @Autowired
  PlayerService playerService;
  @Autowired
  PlayServices playServices;
  @Autowired
  LocalRelayServer localRelayServer;
  @VisibleForTesting
  RatingMode ratingMode;
  @VisibleForTesting
  GameStats gameStats;
  private Process process;
  private BooleanProperty searching1v1;
  private Instant gameStartedTime;
  private ScheduledFuture<?> searchExpansionFuture;
  private Map<String, Object> gameOptions;

  public GameServiceImpl() {
    gameTypeBeans = FXCollections.observableHashMap();
    onGameLaunchingListeners = new HashSet<>();
    uidToGameInfoBean = new HashMap<>();
    gameOptions = new HashMap<>();
    searching1v1 = new SimpleBooleanProperty();

    gameInfoBeans = FXCollections.observableArrayList(
        item -> new Observable[]{item.statusProperty()}
    );
  }

  @Override
  public void addOnGameInfoBeanListener(ListChangeListener<GameInfoBean> listener) {
    gameInfoBeans.addListener(listener);
  }

  @Override
  public CompletableFuture<Void> hostGame(NewGameInfo newGameInfo) {
    if (isRunning()) {
      logger.debug("Game is running, ignoring host request");
      return CompletableFuture.completedFuture(null);
    }

    stopSearchRanked1v1();

    return updateGameIfNecessary(newGameInfo.getGameType(), newGameInfo.getVersion(), emptyMap(), newGameInfo.getSimModUidsToVersions())
        .thenRun(() -> lobbyServerAccessor.requestNewGame(newGameInfo)
            .thenAccept((gameLaunchInfo) -> startGame(gameLaunchInfo, null, RatingMode.GLOBAL))
            .exceptionally(throwable -> {
              logger.warn("Could request new game", throwable);
              return null;
            }))
        .exceptionally(throwable -> {
          logger.warn("Game could not be updated", throwable);
          return null;
        });
  }

  @Override
  public CompletableFuture<Void> joinGame(GameInfoBean gameInfoBean, String password) {
    if (isRunning()) {
      logger.debug("Game is running, ignoring join request");
      return CompletableFuture.completedFuture(null);
    }

    logger.info("Joining game {} ({})", gameInfoBean.getTitle(), gameInfoBean.getUid());

    stopSearchRanked1v1();

    Map<String, Integer> simModVersions = gameInfoBean.getFeaturedModVersions();
    Set<String> simModUIds = gameInfoBean.getSimMods().keySet();

    return updateGameIfNecessary(gameInfoBean.getFeaturedMod(), null, simModVersions, simModUIds)
        .thenRun(() -> downloadMapIfNecessary(gameInfoBean.getTechnicalName())
            .thenRun(() -> lobbyServerAccessor.requestJoinGame(gameInfoBean, password)
                .thenAccept(gameLaunchInfo -> startGame(gameLaunchInfo, null, RatingMode.GLOBAL))))
        .exceptionally(throwable -> {
          logger.warn("Game could not be started", throwable);
          return null;
        });
  }

  private CompletionStage<Void> downloadMapIfNecessary(String mapName) {
    CompletableFuture<Void> future = new CompletableFuture<>();

    if (mapService.isAvailable(mapName)) {
      future.complete(null);
    } else {
      return mapService.download(mapName);
    }

    return future;
  }

  @Override
  public List<GameTypeBean> getGameTypes() {
    return new ArrayList<>(gameTypeBeans.values());
  }

  @Override
  public void addOnGameTypeInfoListener(MapChangeListener<String, GameTypeBean> changeListener) {
    gameTypeBeans.addListener(changeListener);
  }

  @Override
  public void addOnGameStartedListener(OnGameStartedListener listener) {
    onGameLaunchingListeners.add(listener);
  }

  @Override
  public void runWithReplay(Path path, @Nullable Integer replayId, String gameType, Integer version, Map<String, Integer> modVersions, Set<String> simMods) {
    updateGameIfNecessary(gameType, version, modVersions, simMods)
        .thenRun(() -> {
          try {
            Process process = forgedAllianceService.startReplay(path, replayId, gameType);
            onGameLaunchingListeners.forEach(onGameStartedListener -> onGameStartedListener.onGameStarted(null));
            this.ratingMode = RatingMode.NONE;
            spawnTerminationListener(process);
          } catch (IOException e) {
            notifyCantPlayReplay(replayId, e);
          }
        })
        .exceptionally(throwable -> {
          notifyCantPlayReplay(replayId, throwable);
          return null;
        });
  }

  private void notifyCantPlayReplay(@Nullable Integer replayId, Throwable throwable) {
    notificationService.addNotification(new ImmediateNotification(
            i18n.get("replayCouldNotBeStarted.title"),
            i18n.get("replayCouldNotBeStarted.text", replayId),
            Severity.ERROR, throwable,
            singletonList(new Action(i18n.get("report"))))
    );
  }

  @Override
  public void runWithReplay(URL replayUrl, Integer replayId) throws IOException {
    Process process = forgedAllianceService.startReplay(replayUrl, replayId);
    onGameLaunchingListeners.forEach(onGameStartedListener -> onGameStartedListener.onGameStarted(null));
    lobbyServerAccessor.notifyGameStarted();

    this.ratingMode = RatingMode.NONE;
    spawnTerminationListener(process);
  }

  @Override
  public ObservableList<GameInfoBean> getGameInfoBeans() {
    return FXCollections.unmodifiableObservableList(gameInfoBeans);
  }

  @Override
  public GameTypeBean getGameTypeByString(String gameTypeName) {
    return gameTypeBeans.get(gameTypeName);
  }

  @Override
  public GameInfoBean getByUid(int uid) {
    return uidToGameInfoBean.get(uid);
  }

  @Override
  public void addOnRankedMatchNotificationListener(OnRankedMatchNotificationListener listener) {
    lobbyServerAccessor.addOnRankedMatchNotificationListener(listener);
  }

  @Override
  public CompletableFuture<Void> startSearchRanked1v1(Faction faction) {
    if (isRunning()) {
      logger.debug("Game is running, ignoring 1v1 search request");
      return CompletableFuture.completedFuture(null);
    }

    searching1v1.set(true);

    searchExpansionFuture = scheduleSearchExpansionTask();

    return updateGameIfNecessary(GameType.LADDER_1V1.getString(), null, emptyMap(), emptySet())
        .thenRun(() -> lobbyServerAccessor.startSearchRanked1v1(faction, preferencesService.getPreferences().getForgedAlliance().getPort())
            .thenAccept((gameLaunchInfo) -> {
              searchExpansionFuture.cancel(true);
              startGame(gameLaunchInfo, faction, RatingMode.RANKED_1V1);
            })
            .exceptionally(throwable -> {
              logger.warn("Could not start game", throwable);
              searchExpansionFuture.cancel(true);
              return null;
            }))
        .exceptionally(throwable -> {
          logger.warn("Game could not be updated", throwable);
          searchExpansionFuture.cancel(true);
          return null;
        });
  }

  @NotNull
  private ScheduledFuture<?> scheduleSearchExpansionTask() {
    SearchExpansionTask expansionTask = applicationContext.getBean(SearchExpansionTask.class);
    expansionTask.setMaxRadius(environment.getProperty("ranked1v1.search.maxRadius", float.class));
    expansionTask.setRadiusIncrement(environment.getProperty("ranked1v1.search.radiusIncrement", float.class));

    Integer delay = environment.getProperty("ranked1v1.search.expansionDelay", int.class);
    return scheduledExecutorService.scheduleWithFixedDelay(expansionTask, delay, delay, MILLISECONDS);
  }

  @Override
  public void stopSearchRanked1v1() {
    if (searchExpansionFuture != null) {
      searchExpansionFuture.cancel(true);
    }
    if (searching1v1.get()) {
      lobbyServerAccessor.stopSearchingRanked();
      searching1v1.set(false);
    }
  }

  @Override
  public BooleanProperty searching1v1Property() {
    return searching1v1;
  }

  private boolean isRunning() {
    return process != null && process.isAlive();
  }

  private CompletableFuture<Void> updateGameIfNecessary(@NotNull String gameType, @Nullable Integer version, @NotNull Map<String, Integer> modVersions, @NotNull Set<String> simModUIds) {
    return gameUpdateService.updateInBackground(gameType, version, modVersions, simModUIds);
  }

  private void startGame(GameLaunchInfo gameLaunchInfo, Faction faction, RatingMode ratingMode) {
    stopSearchRanked1v1();
    List<String> args = fixMalformedArgs(gameLaunchInfo.getArgs());
    try {
      process = forgedAllianceService.startGame(gameLaunchInfo.getUid(), gameLaunchInfo.getMod(), faction, args, ratingMode);
      onGameLaunchingListeners.forEach(onGameStartedListener -> onGameStartedListener.onGameStarted(gameLaunchInfo.getUid()));
      lobbyServerAccessor.notifyGameStarted();

      this.ratingMode = ratingMode;
      spawnTerminationListener(process);
    } catch (IOException e) {
      logger.warn("Game could not be started", e);
      notificationService.addNotification(
          new PersistentNotification(i18n.get("gameCouldNotBeStarted", e.getLocalizedMessage()), Severity.ERROR)
      );
    }
  }

  /**
   * A correct argument list looks like ["/ratingcolor", "d8d8d8d8", "/numgames", "236"]. However, the FAF server sends
   * it as ["/ratingcolor d8d8d8d8", "/numgames 236"]. This method fixes this.
   */
  private List<String> fixMalformedArgs(List<String> gameLaunchMessage) {
    ArrayList<String> fixedArgs = new ArrayList<>();

    for (String combinedArg : gameLaunchMessage) {
      String[] split = combinedArg.split(" ");

      Collections.addAll(fixedArgs, split);
    }
    return fixedArgs;
  }

  @VisibleForTesting
  void spawnTerminationListener(Process process) {
    CompletableFuture.runAsync(() -> {
      try {
        int exitCode = process.waitFor();
        logger.info("Forged Alliance terminated with exit code {}", exitCode);
        lobbyServerAccessor.notifyGameTerminated();

        proxy.close();
      } catch (InterruptedException | IOException e) {
        logger.warn("Error during post-game processing", e);
      }
    }, scheduledExecutorService);
  }

  @PostConstruct
  void postConstruct() {
    lobbyServerAccessor.addOnGameTypeInfoListener(this);
    lobbyServerAccessor.addOnGameInfoListener(this);
    localRelayServer.setGameStatsListener(this::onGameStats);
    localRelayServer.setGameOptionListener(this::onGameOption);
    localRelayServer.setGameLaunchedListener(aVoid -> {
      gameStats = null;
      gameStartedTime = Instant.now();
      logger.debug("Game started at: {}", gameStartedTime);
    });
  }

  private void onGameOption(List<Object> option) {
    gameOptions.put((String) option.get(0), option.get(1));
  }

  @Override
  public void onGameTypeInfo(GameTypeInfo gameTypeInfo) {
    if (!gameTypeInfo.isHost() || !gameTypeInfo.isLive() || gameTypeBeans.containsKey(gameTypeInfo.getName())) {
      return;
    }

    gameTypeBeans.put(gameTypeInfo.getName(), new GameTypeBean(gameTypeInfo));
  }

  @Override
  public void onGameInfo(GameInfo gameInfo) {
    if (GameState.CLOSED.equals(gameInfo.getState())) {
      gameInfoBeans.remove(uidToGameInfoBean.remove(gameInfo.getUid()));
      return;
    }

    if (!uidToGameInfoBean.containsKey(gameInfo.getUid())) {
      GameInfoBean gameInfoBean = new GameInfoBean(gameInfo);

      gameInfoBeans.add(gameInfoBean);
      uidToGameInfoBean.put(gameInfo.getUid(), gameInfoBean);
    } else {
      uidToGameInfoBean.get(gameInfo.getUid()).updateFromGameInfo(gameInfo);
    }
  }

  private void onGameStats(GameStats gameStats) {
    // CAVEAT: Game stats are received twice; when the player is defeated and when the game ends.
    logger.debug("Received game stats");
    if (this.gameStats != null) {
      logger.debug("Ignoring game stats since they've already been received");
      return;
    }
    this.gameStats = gameStats;

    Duration gameDuration = Duration.between(gameStartedTime, Instant.now());
    logger.debug("Game duration was: {}min {}s",
        gameDuration.toMinutes(),
        gameDuration.minusMinutes(gameDuration.toMinutes()).getSeconds()
    );

    try {
      updatePlayServicesIfApplicable(ratingMode, gameDuration);
    } catch (IOException e) {
      logger.warn("Error during post-game processing", e);
    }
  }

  private void updatePlayServicesIfApplicable(RatingMode ratingMode, Duration gameDuration) throws IOException {
    if (!isApplicableForPlayServices(ratingMode, gameDuration)) {
      return;
    }

    Duration minDuration = Duration.of(environment.getProperty("playServices.minGameTime", int.class), MILLIS);
    if (gameDuration.compareTo(minDuration) <= 0) {
      logger.debug("Not updating play services since game time was too short ({}s)", gameDuration.getSeconds());
      return;
    }

    try {
      playServices.startBatchUpdate();
      updatePlayServices(ratingMode, gameStats, gameDuration);
      playServices.executeBatchUpdate();
    } finally {
      playServices.resetBatchUpdate();
    }
  }

  private boolean isApplicableForPlayServices(RatingMode ratingMode, Duration gameDuration) {
    return ratingMode != null
        && ratingMode != RatingMode.NONE
        && gameStartedTime != null
        && !isCheatsEnabled()
        && isHumanOnlyMultiplayerGame(gameStats.getArmies());
  }

  private void updatePlayServices(RatingMode ratingMode, GameStats gameStats, Duration gameDuration) throws IOException {
    int numberOfActualPlayers = 0;
    int highScore = 0;
    boolean isTopScoringPlayer = false;

    String currentPlayerName = playerService.getCurrentPlayer().getUsername();
    List<Army> armies = gameStats.getArmies();
    for (Army army : armies) {
      if (CIVILIAN.equals(army.getName())) {
        continue;
      }
      numberOfActualPlayers++;
      boolean isCurrentPlayer = currentPlayerName.equals(army.getName());

      int score = calculateScore(army);
      if (score > highScore) {
        highScore = score;
        isTopScoringPlayer = isCurrentPlayer;
      }

      if (isCurrentPlayer) {
        processCurrentPlayerStats(army, ratingMode, gameDuration);
      }
    }

    if (isTopScoringPlayer) {
      playServices.topScoringPlayer(numberOfActualPlayers);
    }

    switch (ratingMode) {
      case GLOBAL:
        playServices.customGamePlayed();
        break;
      case RANKED_1V1:
        playServices.ranked1v1GamePlayed();
        break;
    }
  }

  private boolean isCheatsEnabled() {
    return Boolean.parseBoolean((String) gameOptions.get("CheatsEnabled"));
  }

  private boolean isHumanOnlyMultiplayerGame(List<Army> armies) {
    for (Army army : armies) {
      if (army.getName().contains(AI_INDICATOR)) {
        return false;
      }
    }
    return armies.size() > 1;
  }

  private static int calculateScore(Army army) {
    EconomyStats economyStats = army.getEconomyStats();
    EconomyStat energyStats = economyStats.getEnergy();
    EconomyStat massStats = economyStats.getMass();

    int commanderKills = 0;
    double massValueDestroyed = 0;
    double massValueLost = 0;
    double energyValueDestroyed = 0;
    double energyValueLost = 0;

    for (UnitStat unitStat : army.getUnitStats()) {
      if (unitStat.getType() == UnitType.ACU) {
        commanderKills += unitStat.getKilled();
      }
      massValueDestroyed += unitStat.getKilled() * unitStat.getMasscost();
      massValueLost += unitStat.getLost() * unitStat.getMasscost();
      energyValueDestroyed += unitStat.getKilled() * unitStat.getEnergycost();
      energyValueLost += unitStat.getLost() * unitStat.getEnergycost();
    }

    double energyValueCoefficient = 20;
    double resourceProduction = ((massStats.getConsumed()) + (energyStats.getConsumed() / energyValueCoefficient)) / 2;
    double battleResults = (((massValueDestroyed - massValueLost - (commanderKills * 18000)) + ((energyValueDestroyed - energyValueLost - (commanderKills * 5000000)) / energyValueCoefficient)) / 2);
    battleResults = Math.max(0, battleResults);

    return (int) Math.floor(resourceProduction + battleResults + (commanderKills * 5000));
  }

  private void processCurrentPlayerStats(Army army, RatingMode ratingMode, Duration gameDuration) throws IOException {
    Map<UnitCategory, SummaryStat> categories = army.getSummaryStats().stream()
        .collect(Collectors.toMap(SummaryStat::getType, Function.identity()));

    SummaryStat airUnitStats = categories.get(UnitCategory.AIR);
    SummaryStat landUnitStats = categories.get(UnitCategory.LAND);
    SummaryStat navalUnitStats = categories.get(UnitCategory.NAVAL);
    SummaryStat engineerStats = categories.get(UnitCategory.ENGINEER);
    SummaryStat tech1UnitStats = categories.get(UnitCategory.TECH1);
    SummaryStat tech2UnitStats = categories.get(UnitCategory.TECH2);
    SummaryStat tech3UnitStats = categories.get(UnitCategory.TECH3);

    // Since the summary stats send by FA miss the very important "lost" value, add it manually
    for (UnitStat unitStat : army.getUnitStats()) {
      switch (unitStat.getId().charAt(2)) {
        case 'a':
          airUnitStats.addLost(unitStat.getLost());
          break;
        case 'l':
          landUnitStats.addLost(unitStat.getLost());
          break;
        case 's':
          navalUnitStats.addLost(unitStat.getLost());
          break;
      }
    }

    Map<Unit, UnitStat> unitStatsByUnit = army.getUnitStats().stream()
        .filter(unitStat -> Unit.fromId(unitStat.getId()) != UNKNOWN)
        .collect(Collectors.toMap(unitStat -> Unit.fromId(unitStat.getId()), Function.identity()));

    // Since the summary stats send by FA miss the very important "lost" value, add it manually
    engineerStats.addLost((int) measure(unitStatsByUnit, UnitStat::getLost, ENGINEERS));

    Faction faction = measure(unitStatsByUnit, UnitStat::getBuildtime, AEON_ACU) > 0 ? Faction.AEON :
        measure(unitStatsByUnit, UnitStat::getBuildtime, CYBRAN_ACU) > 0 ? Faction.CYBRAN :
            measure(unitStatsByUnit, UnitStat::getBuildtime, UEF_ACU) > 0 ? Faction.UEF :
                measure(unitStatsByUnit, UnitStat::getBuildtime, SERAPHIM_ACU) > 0 ? Faction.SERAPHIM : null;

    boolean survived = measure(unitStatsByUnit, UnitStat::getLost, AEON_ACU) == 0
        && measure(unitStatsByUnit, UnitStat::getLost, CYBRAN_ACU) == 0
        && measure(unitStatsByUnit, UnitStat::getLost, UEF_ACU) == 0
        && measure(unitStatsByUnit, UnitStat::getLost, SERAPHIM_ACU) == 0;

    int commanderKills = (int) measure(unitStatsByUnit, UnitStat::getKilled, ACUS);
    double acuDamageReceived = measure(unitStatsByUnit, UnitStat::getDamagereceived, ACUS);
    int builtTransports = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, TRANSPORTS);

    int builtMercies = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, MERCY);
    int builtFireBeetles = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, FIRE_BEETLE);
    int builtSalvations = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, SALVATION);

    // Experimentals
    int builtYolonaOss = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, YOLONA_OSS);
    int builtParagons = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, PARAGON);
    int builtAtlantis = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, ATLANTIS);
    int builtTempest = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, TEMPEST);
    int builtScathis = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, SCATHIS);
    int builtMavors = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, MAVOR);
    int builtCzars = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, CZAR);
    int builtAhwasshas = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, AHWASSA);
    int builtYthothas = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, YTHOTHA);
    int builtFatBoys = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, FATBOY);
    int builtMonkeyLords = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, MONKEYLORD);
    int builtGalacticColossus = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, GALACTIC_COLOSSUS);
    int builtSoulRippers = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, SOUL_RIPPER);
    int builtMegaliths = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, MEGALITH);

    int builtExperimentals = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, EXPERIMENTALS);
    int killedExperimentals = (int) measure(unitStatsByUnit, UnitStat::getKilled, EXPERIMENTALS);
    int builtSupportCommanders = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, SACUS);
    int asfBuilt = (int) measure(unitStatsByUnit, UnitStat::getRealBuilt, ASFS);

    if (survived && ratingMode == RatingMode.RANKED_1V1) {
      playServices.ranked1v1GameWon();
      playServices.wonWithinDuration(gameDuration);
    }

    playServices.timePlayed(gameDuration, survived);
    playServices.builtMegaliths(builtMegaliths);
    playServices.builtCzars(builtCzars);
    playServices.builtAhwasshas(builtAhwasshas);
    playServices.builtYthothas(builtYthothas);
    playServices.builtFatboys(builtFatBoys);
    playServices.builtMonkeylords(builtMonkeyLords);
    playServices.builtGalacticColossus(builtGalacticColossus);
    playServices.builtSoulRippers(builtSoulRippers);
    playServices.builtMercies(builtMercies);
    playServices.builtFireBeetles(builtFireBeetles);
    playServices.builtSupportCommanders(builtSupportCommanders);
    playServices.builtTempests(builtTempest);
    playServices.builtAtlantis(builtAtlantis);
    playServices.builtParagons(builtParagons, survived);
    playServices.builtYolonaOss(builtYolonaOss, survived);
    playServices.builtScathis(builtScathis, survived);
    playServices.builtSalvations(builtSalvations, survived);
    playServices.builtMavors(builtMavors, survived);
    playServices.asfBuilt(asfBuilt);
    playServices.builtTransports(builtTransports);
    playServices.factionPlayed(faction, survived);
    playServices.killedCommanders(commanderKills, survived);
    playServices.acuDamageReceived(acuDamageReceived, survived);
    playServices.unitStats(
        airUnitStats.getBuilt(), airUnitStats.getKilled(),
        landUnitStats.getBuilt(), landUnitStats.getKilled(),
        navalUnitStats.getBuilt(), navalUnitStats.getKilled(),
        tech1UnitStats.getBuilt(), tech1UnitStats.getKilled(),
        tech2UnitStats.getBuilt(), tech2UnitStats.getKilled(),
        tech3UnitStats.getBuilt(), tech3UnitStats.getKilled(),
        builtExperimentals, killedExperimentals,
        engineerStats.getBuilt(), engineerStats.getKilled(),
        survived);
  }

  private double measure(Map<Unit, UnitStat> unitStatsByUnit, Function<UnitStat, Number> function, Unit... units) {
    double result = 0;
    for (Unit unit : units) {
      UnitStat unitStat = unitStatsByUnit.get(unit);
      if (unitStat != null) {
        result += function.apply(unitStat).doubleValue();
      }
    }
    return result;
  }
}
