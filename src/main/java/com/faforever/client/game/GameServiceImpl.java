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
import com.faforever.client.user.UserService;
import com.faforever.client.util.ConcurrentUtil;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
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
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class GameServiceImpl implements GameService, OnGameTypeInfoListener, OnGameInfoListener {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Collection<OnGameStartedListener> onGameLaunchingListeners;
  private final ObservableMap<String, GameTypeBean> gameTypeBeans;
  // It is indeed ugly to keep references in both, a list and a map, however I don't see how I can populate the map
  // values as an observable list (in order to display it in the games table)
  private final ObservableList<GameInfoBean> gameInfoBeans;
  private final Map<Integer, GameInfoBean> uidToGameInfoBean;

  @Autowired
  LobbyServerAccessor lobbyServerAccessor;
  @Autowired
  UserService userService;
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

  private Process process;
  private BooleanProperty searching1v1;
  private Instant gameStartedTime;
  private ScheduledFuture<?> searchExpansionFuture;

  public GameServiceImpl() {
    gameTypeBeans = FXCollections.observableHashMap();
    onGameLaunchingListeners = new HashSet<>();
    uidToGameInfoBean = new HashMap<>();
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
  public void publishPotentialPlayer() {
    String username = userService.getUsername();
    // FIXME implement
//    serverAccessor.publishPotentialPlayer(username);
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
        .thenRun(() -> downloadMapIfNecessary(gameInfoBean.getMapTechnicalName())
            .thenRun(() -> lobbyServerAccessor.requestJoinGame(gameInfoBean, password)
                .thenAccept(gameLaunchInfo -> startGame(gameLaunchInfo, null, RatingMode.GLOBAL))));
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
            spawnTerminationListener(process, RatingMode.NONE);
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
  public void runWithReplay(String replayUrl, Integer replayId, String gameType, String mapName) throws IOException {
    //FIXME needs to update
    //downloadMapIfNecessary(map);
    Process process = forgedAllianceService.startReplay(replayUrl, replayId, gameType);
    onGameLaunchingListeners.forEach(onGameStartedListener -> onGameStartedListener.onGameStarted(null));
    // TODO is this needed when watching a replay?
    lobbyServerAccessor.notifyGameStarted();

    spawnTerminationListener(process, RatingMode.NONE);
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
    GameInfoBean gameInfoBean = uidToGameInfoBean.get(uid);
    if (gameInfoBean == null) {
      logger.warn("Can't find {} in gameInfoBean map", uid);
    }
    return gameInfoBean;
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
    gameStartedTime = Instant.now();

    stopSearchRanked1v1();
    List<String> args = fixMalformedArgs(gameLaunchInfo.getArgs());
    try {
      process = forgedAllianceService.startGame(gameLaunchInfo.getUid(), gameLaunchInfo.getMod(), faction, args, ratingMode);
      onGameLaunchingListeners.forEach(onGameStartedListener -> onGameStartedListener.onGameStarted(gameLaunchInfo.getUid()));
      lobbyServerAccessor.notifyGameStarted();

      spawnTerminationListener(process, ratingMode);
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
  void spawnTerminationListener(Process process, RatingMode ratingMode) {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        int exitCode = process.waitFor();
        logger.info("Forged Alliance terminated with exit code {}", exitCode);
        proxy.close();

        recordGamePlayedIfApplicable(ratingMode);

        lobbyServerAccessor.notifyGameTerminated();
        return null;
      }
    });
  }

  private void recordGamePlayedIfApplicable(RatingMode ratingMode) throws IOException {
    if (ratingMode != null && gameStartedTime != null) {
      Duration gameDuration = Duration.between(gameStartedTime, Instant.now());
      // FIXME MINUTES
      if (gameDuration.compareTo(Duration.of(5, ChronoUnit.SECONDS)) > 0) {
        switch (ratingMode) {
          case GLOBAL:
            playServices.incrementPlayedCustomGames();
            break;
          case RANKED_1V1:
            playServices.incrementPlayedRanked1v1Games();
            break;
        }
      }
    }
  }

  @PostConstruct
  void postConstruct() {
    lobbyServerAccessor.addOnGameTypeInfoListener(this);
    lobbyServerAccessor.addOnGameInfoListener(this);
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
}
