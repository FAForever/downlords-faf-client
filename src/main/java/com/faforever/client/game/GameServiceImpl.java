package com.faforever.client.game;

import com.faforever.client.connectivity.ConnectivityService;
import com.faforever.client.fa.ForgedAllianceService;
import com.faforever.client.fa.RatingMode;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.patch.GameUpdateService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.rankedmatch.MatchmakerMessage;
import com.faforever.client.relay.LocalRelayServer;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.GameInfoMessage;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.GameState;
import com.faforever.client.remote.domain.GameTypeMessage;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class GameServiceImpl implements GameService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final ObservableMap<String, GameTypeBean> gameTypeBeans;
  // It is indeed ugly to keep references in both, a list and a map, however I don't see how I can populate the map
  // values as an observable list (in order to display it in the games table)
  private final ObservableList<GameInfoBean> gameInfoBeans;
  private final Map<Integer, GameInfoBean> uidToGameInfoBean;

  @Resource
  FafService fafService;
  @Resource
  ForgedAllianceService forgedAllianceService;
  @Resource
  MapService mapService;
  @Resource
  PreferencesService preferencesService;
  @Resource
  GameUpdateService gameUpdateService;
  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  ScheduledExecutorService scheduledExecutorService;
  @Resource
  PlayerService playerService;
  @Resource
  ConnectivityService connectivityService;
  @Resource
  LocalRelayServer localRelayServer;

  @Value("${ranked1v1.search.maxRadius}")
  float ranked1v1SearchMaxRadius;
  @Value("${ranked1v1.search.radiusIncrement}")
  float ranked1v1SearchRadiusIncrement;
  @Value("${ranked1v1.search.expansionDelay}")
  int ranked1v1SearchExpansionDelay;

  @VisibleForTesting
  RatingMode ratingMode;
  private Process process;
  private BooleanProperty searching1v1;
  private ScheduledFuture<?> searchExpansionFuture;
  private BooleanProperty gameRunning;

  public GameServiceImpl() {
    gameTypeBeans = FXCollections.observableHashMap();
    uidToGameInfoBean = new HashMap<>();
    searching1v1 = new SimpleBooleanProperty();
    gameRunning = new SimpleBooleanProperty();

    gameInfoBeans = FXCollections.observableArrayList(
        item -> new Observable[]{item.statusProperty()}
    );
  }

  @Override
  public BooleanProperty gameRunningProperty() {
    return gameRunning;
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

    return updateGameIfNecessary(
        newGameInfo.getGameType(),
        newGameInfo.getVersion(), emptyMap(),
        newGameInfo.getSimModUidsToVersions()
    )
        .thenRun(() -> connectivityService.connect())
        .thenRun(() -> localRelayServer.start(connectivityService))
        .thenCompose(aVoid -> fafService.requestHostGame(newGameInfo))
        .thenAccept(gameLaunchInfo -> startGame(gameLaunchInfo, null, RatingMode.GLOBAL, localRelayServer.getPort()));
  }

  @Override
  public CompletableFuture<Void> joinGame(GameInfoBean gameInfoBean, String password) {
    if (isRunning()) {
      logger.debug("Game is running, ignoring join request");
      return CompletableFuture.completedFuture(null);
    }

    logger.info("Joining game: {} ({})", gameInfoBean.getTitle(), gameInfoBean.getUid());

    stopSearchRanked1v1();

    Map<String, Integer> simModVersions = gameInfoBean.getFeaturedModVersions();
    Set<String> simModUIds = gameInfoBean.getSimMods().keySet();

    return updateGameIfNecessary(gameInfoBean.getFeaturedMod(), null, simModVersions, simModUIds)
        .thenCompose(aVoid -> downloadMapIfNecessary(gameInfoBean.getMapTechnicalName()))
        .thenRun(() -> connectivityService.connect())
        .thenRun(() -> localRelayServer.start(connectivityService))
        .thenCompose(aVoid -> fafService.requestJoinGame(gameInfoBean.getUid(), password))
        .thenAccept(gameLaunchInfo -> startGame(gameLaunchInfo, null, RatingMode.GLOBAL, localRelayServer.getPort()));
  }

  private CompletableFuture<Void> downloadMapIfNecessary(String mapName) {
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
  public void runWithReplay(Path path, @Nullable Integer replayId, String gameType, Integer version, Map<String, Integer> modVersions, Set<String> simMods) {
    updateGameIfNecessary(gameType, version, modVersions, simMods)
        .thenRun(() -> {
          try {
            process = forgedAllianceService.startReplay(path, replayId, gameType);
            gameRunning.set(true);
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
        i18n.get("errorTitle"),
        i18n.get("replayCouldNotBeStarted", replayId),
        Severity.ERROR, throwable,
        singletonList(new Action(i18n.get("report"))))
    );
  }

  @Override
  public CompletableFuture<Void> runWithLiveReplay(URI replayUrl, Integer gameId, String gameType, String mapName) throws IOException {
    GameInfoBean gameBean = getByUid(gameId);

    Map<String, Integer> modVersions = gameBean.getFeaturedModVersions();
    Set<String> simModUids = gameBean.getSimMods().keySet();

    return updateGameIfNecessary(gameType, null, modVersions, simModUids)
        .thenCompose(aVoid -> downloadMapIfNecessary(mapName))
        .thenRun(() -> {
          try {
            process = forgedAllianceService.startReplay(replayUrl, gameId, gameType);
            gameRunning.set(true);
            this.ratingMode = RatingMode.NONE;
            spawnTerminationListener(process);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
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
  public void addOnRankedMatchNotificationListener(Consumer<MatchmakerMessage> listener) {
    fafService.addOnMessageListener(MatchmakerMessage.class, listener);
  }

  @Override
  public CompletableFuture<Void> startSearchRanked1v1(Faction faction) {
    if (isRunning()) {
      logger.debug("Game is running, ignoring 1v1 search request");
      return CompletableFuture.completedFuture(null);
    }

    searching1v1.set(true);

    searchExpansionFuture = scheduleSearchExpansionTask();

    int port = preferencesService.getPreferences().getForgedAlliance().getPort();

    return updateGameIfNecessary(GameType.LADDER_1V1.getString(), null, emptyMap(), emptySet())
        .thenRun(() -> localRelayServer.start(connectivityService))
        .thenCompose(aVoid -> fafService.startSearchRanked1v1(faction, port))
        .thenAccept((gameLaunchInfo) -> {
          searchExpansionFuture.cancel(true);
          startGame(gameLaunchInfo, faction, RatingMode.RANKED_1V1, localRelayServer.getPort());
        })
        .exceptionally(throwable -> {
          if (throwable instanceof CancellationException) {
            logger.info("Ranked1v1 search has been cancelled");
          } else {
            logger.warn("Ranked1v1 could not be started", throwable);
          }
          searchExpansionFuture.cancel(true);
          return null;
        });
  }

  @NotNull
  private ScheduledFuture<?> scheduleSearchExpansionTask() {
    SearchExpansionTask expansionTask = applicationContext.getBean(SearchExpansionTask.class);
    expansionTask.setMaxRadius(ranked1v1SearchMaxRadius);
    expansionTask.setRadiusIncrement(ranked1v1SearchRadiusIncrement);

    Integer delay = ranked1v1SearchExpansionDelay;
    return scheduledExecutorService.scheduleWithFixedDelay(expansionTask, delay, delay, MILLISECONDS);
  }

  @Override
  public void stopSearchRanked1v1() {
    if (searchExpansionFuture != null) {
      searchExpansionFuture.cancel(true);
    }
    if (searching1v1.get()) {
      fafService.stopSearchingRanked();
      searching1v1.set(false);
    }
  }

  @Override
  public BooleanProperty searching1v1Property() {
    return searching1v1;
  }

  @Override
  public CompletableFuture<Void> prepareForRehost() {
    return fafService.expectRehostCommand().thenAccept(gameLaunchMessage -> {
      logger.debug("Received game launch command, waiting for FA to terminate");
      try {
        process.waitFor();
        localRelayServer.close();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      localRelayServer.start(connectivityService);
      connectivityService.connect();
      startGame(gameLaunchMessage, null, RatingMode.GLOBAL, localRelayServer.getPort());
    });
  }

  private boolean isRunning() {
    return process != null && process.isAlive();
  }

  private CompletableFuture<Void> updateGameIfNecessary(@NotNull String gameType, @Nullable Integer version, @NotNull Map<String, Integer> modVersions, @NotNull Set<String> simModUids) {
    return gameUpdateService.updateInBackground(gameType, version, modVersions, simModUids);
  }

  /**
   * Actually starts the game. Call this method when everything else is prepared (mod/map download, connectivity check
   * etc.)
   */
  private void startGame(GameLaunchMessage gameLaunchMessage, Faction faction, RatingMode ratingMode, Integer localRelayPort) {
    stopSearchRanked1v1();
    List<String> args = fixMalformedArgs(gameLaunchMessage.getArgs());
    try {
      localRelayServer.getPort();
      process = forgedAllianceService.startGame(gameLaunchMessage.getUid(), gameLaunchMessage.getMod(), faction, args, ratingMode, localRelayPort);
      gameRunning.set(true);

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
        gameRunning.set(false);
        localRelayServer.close();
        fafService.notifyGameEnded();
        logger.info("Forged Alliance terminated with exit code {}", exitCode);
      } catch (InterruptedException e) {
        logger.warn("Error during post-game processing", e);
      }
    }, scheduledExecutorService);
  }

  @PostConstruct
  void postConstruct() {
    fafService.addOnMessageListener(GameTypeMessage.class, this::onGameTypeInfo);
    fafService.addOnMessageListener(GameInfoMessage.class, this::onGameInfo);
  }

  private void onGameTypeInfo(GameTypeMessage gameTypeMessage) {
    if (!gameTypeMessage.isPublish() || gameTypeBeans.containsKey(gameTypeMessage.getName())) {
      return;
    }

    gameTypeBeans.put(gameTypeMessage.getName(), new GameTypeBean(gameTypeMessage));
  }

  private void onGameInfo(GameInfoMessage gameInfoMessage) {
    if (gameInfoMessage.getGames() != null) {
      gameInfoMessage.getGames().forEach(this::onGameInfo);
      return;
    }

    if (GameState.CLOSED.equals(gameInfoMessage.getState())) {
      gameInfoBeans.remove(uidToGameInfoBean.remove(gameInfoMessage.getUid()));
      return;
    }

    if (!uidToGameInfoBean.containsKey(gameInfoMessage.getUid())) {
      GameInfoBean gameInfoBean = new GameInfoBean(gameInfoMessage);

      gameInfoBeans.add(gameInfoBean);
      uidToGameInfoBean.put(gameInfoMessage.getUid(), gameInfoBean);
    } else {
      uidToGameInfoBean.get(gameInfoMessage.getUid()).updateFromGameInfo(gameInfoMessage);
    }
  }
}
