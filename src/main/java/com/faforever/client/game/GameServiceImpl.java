package com.faforever.client.game;

import com.faforever.client.connectivity.ConnectivityService;
import com.faforever.client.fa.ForgedAllianceService;
import com.faforever.client.fa.RatingMode;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.patch.GameUpdateService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.rankedmatch.MatchmakerMessage;
import com.faforever.client.relay.LocalRelayServer;
import com.faforever.client.relay.event.RehostRequestEvent;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.GameInfoMessage;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.GameState;
import com.faforever.client.remote.domain.GameTypeMessage;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.reporting.ReportingService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.synchronizedList;
import static java.util.Collections.synchronizedMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class GameServiceImpl implements GameService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  @VisibleForTesting
  final BooleanProperty gameRunning;
  @VisibleForTesting
  final SimpleObjectProperty<GameInfoBean> currentGame;
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
  @Resource
  ReportingService reportingService;
  @Resource
  ReplayService replayService;
  @Resource
  EventBus eventBus;

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
  private boolean rehostRequested;

  public GameServiceImpl() {
    gameTypeBeans = FXCollections.observableHashMap();
    uidToGameInfoBean = synchronizedMap(new HashMap<>());
    searching1v1 = new SimpleBooleanProperty();
    gameRunning = new SimpleBooleanProperty();
    currentGame = new SimpleObjectProperty<>();
    gameInfoBeans = FXCollections.observableList(synchronizedList(new ArrayList<>()),
        item -> new Observable[]{item.statusProperty()}
    );
  }

  @Override
  public ReadOnlyBooleanProperty gameRunningProperty() {
    return gameRunning;
  }

  @Override
  public void addOnGameInfoBeansChangeListener(ListChangeListener<GameInfoBean> listener) {
    gameInfoBeans.addListener(listener);
  }

  @Override
  public CompletionStage<Void> hostGame(NewGameInfo newGameInfo) {
    if (isRunning()) {
      logger.debug("Game is running, ignoring host request");
      return CompletableFuture.completedFuture(null);
    }

    stopSearchRanked1v1();

    return updateGameIfNecessary(newGameInfo.getGameType(), null, emptyMap(), newGameInfo.getSimMods())
        .thenRun(() -> connectivityService.connect())
        .thenRun(() -> localRelayServer.start(connectivityService))
        .thenCompose(aVoid -> fafService.requestHostGame(newGameInfo))
        .thenAccept(gameLaunchInfo -> {
          replayService.startReplayServer(gameLaunchInfo.getUid());
          startGame(gameLaunchInfo, null, RatingMode.GLOBAL, localRelayServer.getPort());
        });
  }

  @Override
  public CompletionStage<Void> joinGame(GameInfoBean gameInfoBean, String password) {
    if (isRunning()) {
      logger.debug("Game is running, ignoring join request");
      return CompletableFuture.completedFuture(null);
    }

    logger.info("Joining game: {} ({})", gameInfoBean.getTitle(), gameInfoBean.getUid());

    stopSearchRanked1v1();

    Map<String, Integer> simModVersions = gameInfoBean.getFeaturedModVersions();
    Set<String> simModUIds = gameInfoBean.getSimMods().keySet();

    return updateGameIfNecessary(gameInfoBean.getFeaturedMod(), null, simModVersions, simModUIds)
        .thenCompose(aVoid -> downloadMapIfNecessary(gameInfoBean.getMapFolderName()))
        .thenRun(() -> connectivityService.connect())
        .thenRun(() -> localRelayServer.start(connectivityService))
        .thenCompose(aVoid -> fafService.requestJoinGame(gameInfoBean.getUid(), password))
        .thenAccept(gameLaunchInfo -> {
          synchronized (currentGame) {
            // Store password in case we rehost
            gameInfoBean.setPassword(password);
            currentGame.set(gameInfoBean);
          }
          replayService.startReplayServer(gameLaunchInfo.getUid());
          startGame(gameLaunchInfo, null, RatingMode.GLOBAL, localRelayServer.getPort());
        });
  }

  private CompletionStage<Void> downloadMapIfNecessary(String mapFolderName) {
    CompletableFuture<Void> future = new CompletableFuture<>();

    if (mapService.isInstalled(mapFolderName)) {
      future.complete(null);
      return future;
    }
    return mapService.download(mapFolderName);
  }

  @Override
  public List<GameTypeBean> getGameTypes() {
    return new ArrayList<>(gameTypeBeans.values());
  }

  @Override
  public void addOnGameTypesChangeListener(MapChangeListener<String, GameTypeBean> changeListener) {
    gameTypeBeans.addListener(changeListener);
  }

  @Override
  public void runWithReplay(Path path, @Nullable Integer replayId, String gameType, Integer version, Map<String, Integer> modVersions, Set<String> simMods) {
    if (isRunning()) {
      logger.warn("Forged Alliance is already running, not starting replay");
      return;
    }

    updateGameIfNecessary(gameType, version, modVersions, simMods)
        .thenRun(() -> {
          try {
            process = forgedAllianceService.startReplay(path, replayId, gameType);
            setGameRunning(true);
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
  public CompletionStage<Void> runWithLiveReplay(URI replayUrl, Integer gameId, String gameType, String mapName) throws IOException {
    if (isRunning()) {
      logger.warn("Forged Alliance is already running, not starting live replay");
      return CompletableFuture.completedFuture(null);
    }

    GameInfoBean gameBean = getByUid(gameId);

    Map<String, Integer> modVersions = gameBean.getFeaturedModVersions();
    Set<String> simModUids = gameBean.getSimMods().keySet();

    return updateGameIfNecessary(gameType, null, modVersions, simModUids)
        .thenCompose(aVoid -> downloadMapIfNecessary(mapName))
        .thenRun(() -> {
          try {
            process = forgedAllianceService.startReplay(replayUrl, gameId, gameType);
            setGameRunning(true);
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
  public CompletionStage<Void> startSearchRanked1v1(Faction faction) {
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
        .thenAccept((gameLaunchInfo) -> downloadMapIfNecessary(gameLaunchInfo.getMapname())
            .thenRun(() -> {
              // TODO this should be sent by the server!
              gameLaunchInfo.setArgs(new ArrayList<>(gameLaunchInfo.getArgs()));
              gameLaunchInfo.getArgs().add("/team 1");
              gameLaunchInfo.getArgs().add("/players 2");

              searchExpansionFuture.cancel(true);
              startGame(gameLaunchInfo, faction, RatingMode.RANKED_1V1, localRelayServer.getPort());
            }))
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

  @Nullable
  @Override
  public GameInfoBean getCurrentGame() {
    synchronized (currentGame) {
      return currentGame.get();
    }
  }

  private boolean isRunning() {
    return process != null && process.isAlive();
  }

  private CompletionStage<Void> updateGameIfNecessary(@NotNull String gameType, @Nullable Integer version, @NotNull Map<String, Integer> modVersions, @NotNull Set<String> simModUids) {
    return gameUpdateService.updateInBackground(gameType, version, modVersions, simModUids);
  }

  @Override
  public boolean isGameRunning() {
    synchronized (gameRunning) {
      return gameRunning.get();
    }
  }

  private void setGameRunning(boolean running) {
    synchronized (gameRunning) {
      gameRunning.set(running);
    }
  }

  /**
   * Actually starts the game. Call this method when everything else is prepared (mod/map download, connectivity check
   * etc.)
   */
  private void startGame(GameLaunchMessage gameLaunchMessage, Faction faction, RatingMode ratingMode, Integer localRelayPort) {
    if (isRunning()) {
      logger.warn("Forged Alliance is already running, not starting game");
      return;
    }

    stopSearchRanked1v1();
    List<String> args = fixMalformedArgs(gameLaunchMessage.getArgs());
    try {
      localRelayServer.getPort();
      process = forgedAllianceService.startGame(gameLaunchMessage.getUid(), gameLaunchMessage.getMod(), faction, args, ratingMode, localRelayPort, rehostRequested);
      setGameRunning(true);

      this.ratingMode = ratingMode;
      spawnTerminationListener(process);
    } catch (IOException e) {
      logger.warn("Game could not be started", e);
      notificationService.addNotification(
          new ImmediateNotification(i18n.get("errorTitle"),
              i18n.get("game.start.couldNotStart"), Severity.ERROR, e, Arrays.asList(
              new ReportAction(i18n, reportingService, e), new DismissAction(i18n)))
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
        rehostRequested = false;
        int exitCode = process.waitFor();
        logger.info("Forged Alliance terminated with exit code {}", exitCode);

        synchronized (gameRunning) {
          gameRunning.set(false);
          localRelayServer.close();
          fafService.notifyGameEnded();
          replayService.stopReplayServer();

          if (rehostRequested) {
            rehost();
          }
        }
      } catch (InterruptedException e) {
        logger.warn("Error during post-game processing", e);
      }
    }, scheduledExecutorService);
  }

  private void rehost() {
    GameInfoBean gameInfoBean = currentGame.get();

    hostGame(new NewGameInfo(
        gameInfoBean.getTitle(),
        gameInfoBean.getPassword(),
        gameInfoBean.getFeaturedMod(),
        gameInfoBean.getMapFolderName(),
        new HashSet<>(gameInfoBean.getSimMods().values())));
  }

  @Subscribe
  public void onRehostRequest(RehostRequestEvent event) {
    this.rehostRequested = true;
    synchronized (gameRunning) {
      if (!gameRunning.get()) {
        // If the game already has terminated, the rehost is issued here. Otherwise it will be issued after termination
        rehost();
      }
    }
  }

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
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

    if (GameState.CLOSED == gameInfoMessage.getState()) {
      gameInfoBeans.remove(uidToGameInfoBean.remove(gameInfoMessage.getUid()));
      return;
    }

    final GameInfoBean gameInfoBean;
    if (!uidToGameInfoBean.containsKey(gameInfoMessage.getUid())) {
      gameInfoBean = new GameInfoBean(gameInfoMessage);

      gameInfoBeans.add(gameInfoBean);
      uidToGameInfoBean.put(gameInfoMessage.getUid(), gameInfoBean);
    } else {
      gameInfoBean = uidToGameInfoBean.get(gameInfoMessage.getUid());
      Platform.runLater(() -> gameInfoBean.updateFromGameInfo(gameInfoMessage));
    }

    boolean currentPlayerInGame = gameInfoMessage.getTeams().values().stream()
        .anyMatch(team -> team.contains(playerService.getCurrentPlayer().getUsername()));

    if (currentPlayerInGame && GameState.PLAYING == gameInfoMessage.getState()) {
      synchronized (currentGame) {
        currentGame.set(gameInfoBean);
      }
    }
  }
}
