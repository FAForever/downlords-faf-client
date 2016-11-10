package com.faforever.client.game;

import com.faforever.client.fa.ForgedAllianceService;
import com.faforever.client.fa.RatingMode;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.ice.IceAdapter;
import com.faforever.client.map.MapService;
import com.faforever.client.net.ConnectionState;
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
import com.faforever.client.relay.event.RehostRequestEvent;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.GameInfoMessage;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.GameState;
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
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import static com.faforever.client.fa.RatingMode.NONE;
import static com.faforever.client.game.KnownFeaturedMod.LADDER_1V1;
import static com.github.nocatch.NoCatch.noCatch;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class GameServiceImpl implements GameService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  @VisibleForTesting
  final BooleanProperty gameRunning;
  @VisibleForTesting
  final SimpleObjectProperty<GameInfoBean> currentGame;

  /**
   * An observable copy of {@link #uidToGameInfoBean}. <strong>Do not modify its content directly</strong>.
   */
  private final ObservableList<GameInfoBean> gameInfoBeans;
  private final ObservableMap<Integer, GameInfoBean> uidToGameInfoBean;

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
  ReportingService reportingService;
  @Resource
  ReplayService replayService;
  @Resource
  EventBus eventBus;
  @Resource
  IceAdapter iceAdapter;

  @VisibleForTesting
  RatingMode ratingMode;

  private Process process;
  private BooleanProperty searching1v1;
  private boolean rehostRequested;

  public GameServiceImpl() {
    uidToGameInfoBean = FXCollections.observableHashMap();
    searching1v1 = new SimpleBooleanProperty();
    gameRunning = new SimpleBooleanProperty();
    currentGame = new SimpleObjectProperty<>();
    gameInfoBeans = FXCollections.observableList(new ArrayList<>(),
        item -> new Observable[]{item.statusProperty()}
    );
    JavaFxUtil.attachListToMap(gameInfoBeans, uidToGameInfoBean);
  }

  @Override
  public ReadOnlyBooleanProperty gameRunningProperty() {
    return gameRunning;
  }

  @Override
  public CompletionStage<Void> hostGame(NewGameInfo newGameInfo) {
    if (isRunning()) {
      logger.debug("Game is running, ignoring host request");
      return completedFuture(null);
    }

    stopSearchRanked1v1();

    return updateGameIfNecessary(newGameInfo.getFeaturedMod(), null, emptyMap(), newGameInfo.getSimMods())
        .thenCompose(aVoid -> fafService.requestHostGame(newGameInfo))
        .thenAccept(gameLaunchMessage -> startGame(gameLaunchMessage, null, RatingMode.GLOBAL));
  }

  @Override
  public CompletionStage<Void> joinGame(GameInfoBean gameInfoBean, String password) {
    if (isRunning()) {
      logger.debug("Game is running, ignoring join request");
      return completedFuture(null);
    }

    logger.info("Joining game: {} ({})", gameInfoBean.getTitle(), gameInfoBean.getUid());

    stopSearchRanked1v1();

    Map<String, Integer> simModVersions = gameInfoBean.getFeaturedModVersions();
    Set<String> simModUIds = gameInfoBean.getSimMods().keySet();

    return getFeaturedMod(gameInfoBean.getFeaturedMod())
        .thenCompose(featuredModBean -> updateGameIfNecessary(featuredModBean, null, simModVersions, simModUIds))
        .thenCompose(aVoid -> downloadMapIfNecessary(gameInfoBean.getMapFolderName()))
        .thenCompose(aVoid -> fafService.requestJoinGame(gameInfoBean.getUid(), password))
        .thenAccept(gameLaunchMessage -> {
          synchronized (currentGame) {
            // Store password in case we rehost
            gameInfoBean.setPassword(password);
            currentGame.set(gameInfoBean);
          }
          startGame(gameLaunchMessage, null, RatingMode.GLOBAL);
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
  public CompletableFuture<List<FeaturedModBean>> getFeaturedMods() {
    return fafService.getFeaturedMods();
  }

  @Override
  public void runWithReplay(Path path, @Nullable Integer replayId, String featuredMod, Integer version, Map<String, Integer> modVersions, Set<String> simMods, String mapName) {
    if (isRunning()) {
      logger.warn("Forged Alliance is already running, not starting replay");
      return;
    }
    getFeaturedMod(featuredMod)
        .thenCompose(featuredModBean -> updateGameIfNecessary(featuredModBean, version, modVersions, simMods))
        .thenCompose(aVoid -> downloadMapIfNecessary(mapName))
        .thenRun(() -> {
          try {
            process = forgedAllianceService.startReplay(path, replayId);
            setGameRunning(true);
            this.ratingMode = NONE;
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
      return completedFuture(null);
    }

    GameInfoBean gameBean = getByUid(gameId);

    Map<String, Integer> modVersions = gameBean.getFeaturedModVersions();
    Set<String> simModUids = gameBean.getSimMods().keySet();

    return getFeaturedMod(gameType)
        .thenCompose(featuredModBean -> updateGameIfNecessary(featuredModBean, null, modVersions, simModUids))
        .thenCompose(aVoid -> downloadMapIfNecessary(mapName))
        .thenRun(() -> noCatch(() -> {
          process = forgedAllianceService.startReplay(replayUrl, gameId);
          setGameRunning(true);
          this.ratingMode = NONE;
          spawnTerminationListener(process);
        }));
  }

  @Override
  public ObservableList<GameInfoBean> getGameInfoBeans() {
    return FXCollections.unmodifiableObservableList(gameInfoBeans);
  }

  @Override
  public CompletableFuture<FeaturedModBean> getFeaturedMod(String featuredMod) {
    return getFeaturedMods().thenCompose(featuredModBeans -> completedFuture(featuredModBeans.stream()
        .filter(featuredModBean -> featuredMod.equals(featuredModBean.getTechnicalName()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Not a valid featured mod: " + featuredMod))
    ));
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
      return completedFuture(null);
    }

    searching1v1.set(true);

    int port = preferencesService.getPreferences().getForgedAlliance().getPort();

    return getFeaturedMod(LADDER_1V1.getString())
        .thenAccept(featuredModBean -> updateGameIfNecessary(featuredModBean, null, emptyMap(), emptySet()))
        .thenCompose(aVoid -> fafService.startSearchRanked1v1(faction, port))
        .thenAccept((gameLaunchMessage) -> downloadMapIfNecessary(gameLaunchMessage.getMapname())
            .thenRun(() -> {
              // TODO this should be sent by the server!
              gameLaunchMessage.setArgs(new ArrayList<>(gameLaunchMessage.getArgs()));
              gameLaunchMessage.getArgs().add("/team 1");
              gameLaunchMessage.getArgs().add("/players 2");

              startGame(gameLaunchMessage, faction, RatingMode.RANKED_1V1);
            }))
        .exceptionally(throwable -> {
          if (throwable instanceof CancellationException) {
            logger.info("Ranked1v1 search has been cancelled");
          } else {
            logger.warn("Ranked1v1 could not be started", throwable);
          }
          return null;
        });
  }

  @Override
  public void stopSearchRanked1v1() {
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

  private CompletionStage<Void> updateGameIfNecessary(FeaturedModBean featuredMod, @Nullable Integer version, @NotNull Map<String, Integer> modVersions, @NotNull Set<String> simModUids) {
    return gameUpdateService.updateInBackground(featuredMod, version, modVersions, simModUids);
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
   * Actually starts the game, including relay and replay server. Call this method when everything else is prepared
   * (mod/map download, connectivity check etc.)
   */
  private void startGame(GameLaunchMessage gameLaunchMessage, Faction faction, RatingMode ratingMode) {
    if (isRunning()) {
      logger.warn("Forged Alliance is already running, not starting game");
      return;
    }

    stopSearchRanked1v1();
    replayService.startReplayServer(gameLaunchMessage.getUid())
        .thenCompose(aVoid -> iceAdapter.start())
        .thenAccept(adapterPort -> {
          List<String> args = fixMalformedArgs(gameLaunchMessage.getArgs());
          process = noCatch(() -> forgedAllianceService.startGame(gameLaunchMessage.getUid(), faction, args, ratingMode, adapterPort, rehostRequested));
          setGameRunning(true);

          this.ratingMode = ratingMode;
          spawnTerminationListener(process);
        })
        .exceptionally(throwable -> {
          logger.warn("Game could not be started", throwable);
          notificationService.addNotification(
              new ImmediateNotification(i18n.get("errorTitle"),
                  i18n.get("game.start.couldNotStart"), Severity.ERROR, throwable, Arrays.asList(
                  new ReportAction(i18n, reportingService, throwable), new DismissAction(i18n)))
          );
          setGameRunning(false);
          return null;
        });
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
          fafService.notifyGameEnded();
          replayService.stopReplayServer();
          iceAdapter.stop();

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

    getFeaturedMod(gameInfoBean.getFeaturedMod())
        .thenAccept(featuredModBean -> hostGame(new NewGameInfo(
            gameInfoBean.getTitle(),
            gameInfoBean.getPassword(),
            featuredModBean,
            gameInfoBean.getMapFolderName(),
            new HashSet<>(gameInfoBean.getSimMods().values())
        )));
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
    fafService.addOnMessageListener(GameInfoMessage.class, this::onGameInfo);
    fafService.connectionStateProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == ConnectionState.DISCONNECTED) {
        uidToGameInfoBean.clear();
      }
    });
  }

  private void onGameInfo(GameInfoMessage gameInfoMessage) {
    if (gameInfoMessage.getGames() != null) {
      gameInfoMessage.getGames().forEach(this::onGameInfo);
      return;
    }

    if (GameState.CLOSED == gameInfoMessage.getState()) {
      synchronized (uidToGameInfoBean) {
        uidToGameInfoBean.remove(gameInfoMessage.getUid());
      }
      return;
    }

    final GameInfoBean gameInfoBean;
    if (!uidToGameInfoBean.containsKey(gameInfoMessage.getUid())) {
      gameInfoBean = new GameInfoBean(gameInfoMessage);
      uidToGameInfoBean.put(gameInfoMessage.getUid(), gameInfoBean);
    } else {
      gameInfoBean = uidToGameInfoBean.get(gameInfoMessage.getUid());
      Platform.runLater(() -> gameInfoBean.updateFromGameInfo(gameInfoMessage));
    }

    boolean currentPlayerInGame = gameInfoMessage.getTeams().values().stream()
        .anyMatch(team -> team.contains(playerService.getCurrentPlayer().getUsername()));

    if (currentPlayerInGame && GameState.OPEN == gameInfoMessage.getState()) {
      synchronized (currentGame) {
        currentGame.set(gameInfoBean);
      }
    }
  }
}
