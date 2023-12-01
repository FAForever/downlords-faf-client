package com.faforever.client.game;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.exception.NotifiableException;
import com.faforever.client.fa.ForgedAllianceService;
import com.faforever.client.fa.GameParameters;
import com.faforever.client.fa.relay.ice.CoturnService;
import com.faforever.client.fa.relay.ice.IceAdapter;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.error.GameCleanupException;
import com.faforever.client.game.error.GameLaunchException;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.logging.LoggingService;
import com.faforever.client.main.event.ShowReplayEvent;
import com.faforever.client.map.MapService;
import com.faforever.client.mapstruct.GameMapper;
import com.faforever.client.mod.ModService;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.patch.GameUpdater;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.LastGamePrefs;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.replay.ReplayServer;
import com.faforever.client.ui.preferences.GameDirectoryRequiredHandler;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.MaskPatternLayout;
import com.faforever.commons.lobby.GameInfo;
import com.faforever.commons.lobby.GameStatus;
import com.faforever.commons.lobby.NoticeInfo;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static com.faforever.client.game.KnownFeaturedMod.FAF;
import static com.faforever.client.game.KnownFeaturedMod.TUTORIALS;
import static com.faforever.client.notification.Severity.WARN;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

/**
 * Downloads necessary maps, mods and updates before starting
 */
@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class GameService implements InitializingBean {

  private static final Pattern GAME_PREFS_ALLOW_MULTI_LAUNCH_PATTERN = Pattern.compile(
      "debug\\s*=(\\s)*[{][^}]*enable_debug_facilities\\s*=\\s*true");
  private static final String GAME_PREFS_ALLOW_MULTI_LAUNCH_STRING = """

      debug = {
          enable_debug_facilities = true
      }""".trim();


  private final FafServerAccessor fafServerAccessor;
  private final ForgedAllianceService forgedAllianceService;
  private final CoturnService coturnService;
  private final MapService mapService;
  private final PreferencesService preferencesService;
  private final LoggingService loggingService;
  private final GameUpdater gameUpdater;
  private final LeaderboardService leaderboardService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final PlayerService playerService;
  private final NavigationHandler navigationHandler;
  private final IceAdapter iceAdapter;
  private final ModService modService;
  private final PlatformService platformService;
  private final ReplayServer replayServer;
  private final OperatingSystem operatingSystem;
  private final ClientProperties clientProperties;
  private final GameMapper gameMapper;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final LastGamePrefs lastGamePrefs;
  private final NotificationPrefs notificationPrefs;
  private final ForgedAlliancePrefs forgedAlliancePrefs;
  private final GameDirectoryRequiredHandler gameDirectoryRequiredHandler;

  @VisibleForTesting
  final BooleanProperty gameRunning = new SimpleBooleanProperty();
  final BooleanProperty replayRunning = new SimpleBooleanProperty();
  /** TODO: Explain why access needs to be synchronized. */
  @VisibleForTesting
  final ReadOnlyObjectWrapper<GameBean> currentGame = new ReadOnlyObjectWrapper<>();
  private final MaskPatternLayout logMasker = new MaskPatternLayout();
  private final ObservableMap<Integer, GameBean> gameIdToGame = FXCollections.synchronizedObservableMap(
      FXCollections.observableHashMap());
  @Getter
  private final ObservableList<GameBean> games = JavaFxUtil.attachListToMap(FXCollections.synchronizedObservableList(
                                                                                FXCollections.observableArrayList(
                                                                                    game -> new Observable[]{game.statusProperty(), game.teamsProperty(), game.titleProperty(), game.mapFolderNameProperty(), game.simModsProperty(), game.passwordProtectedProperty()})),
                                                                            gameIdToGame);

  private Process process;
  private Process replayProcess;
  private CompletableFuture<Void> matchmakerFuture;
  private boolean gameKilled;
  private boolean replayKilled;
  private int localReplayPort;

  @Override
  public void afterPropertiesSet() {
    currentGame.addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        return;
      }

      ChangeListener<GameStatus> statusChangeListener = generateGameStatusListener(newValue);
      JavaFxUtil.addAndTriggerListener(newValue.statusProperty(), statusChangeListener);
    });

    currentGame.flatMap(GameBean::statusProperty).addListener((observable, oldValue, newValue) -> {
      String faWindowTitle = clientProperties.getForgedAlliance().getWindowTitle();
      if (oldValue == GameStatus.OPEN && newValue == GameStatus.PLAYING && !platformService.isWindowFocused(
          faWindowTitle)) {
        platformService.focusWindow(faWindowTitle);
      }
    });

    Flux<GameBean> gameUpdateFlux = fafServerAccessor.getEvents(GameInfo.class)
                                                     .flatMap(gameInfo -> gameInfo.getGames() == null ? Flux.just(
                                                         gameInfo) : Flux.fromIterable(gameInfo.getGames()))
                                                     .flatMap(gameInfo -> Mono.zip(Mono.just(gameInfo),
                                                                                   Mono.justOrEmpty(gameIdToGame.get(
                                                                                           gameInfo.getUid()))
                                                                                       .switchIfEmpty(
                                                                                           initializeGameBean(
                                                                                               gameInfo))))
                                                     .publishOn(fxApplicationThreadExecutor.asScheduler())
                                                     .map(TupleUtils.function(gameMapper::update))
                                                     .doOnError(
                                                         throwable -> log.error("Error processing game", throwable))
                                                     .retry()
                                                     .share();

    gameUpdateFlux.filter(game -> game.getStatus() == GameStatus.CLOSED)
                  .doOnNext(GameBean::removeListeners)
                  .map(GameBean::getId)
                  .publishOn(fxApplicationThreadExecutor.asScheduler())
                  .doOnNext(gameIdToGame::remove)
                  .doOnError(throwable -> log.error("Error closing game", throwable))
                  .retry()
                  .subscribe();

    gameUpdateFlux.filter(playerService::isCurrentPlayerInGame).doOnNext(game -> {
      if (GameStatus.OPEN == game.getStatus()) {
        currentGame.set(enhanceWithLastPasswordIfPasswordProtected(game));
      } else if (GameStatus.CLOSED == game.getStatus()) {
        currentGame.set(null);
      }
    }).doOnError(throwable -> log.error("Error setting current game", throwable)).retry().subscribe();

    fafServerAccessor.getEvents(NoticeInfo.class)
                     .filter(notice -> Objects.equals(notice.getStyle(), "kill"))
                     .doOnNext(notice -> {
                       log.info("Game close requested by server");
                       String linksRules = clientProperties.getLinks().get("linksRules");
                       ImmediateNotification notification = new ImmediateNotification(i18n.get("game.kicked.title"),
                                                                                      i18n.get("game.kicked.message",
                                                                                               linksRules), WARN,
                                                                                      List.of(new DismissAction(i18n)));
                       notificationService.addNotification(notification);
                       killGame();
                     })
                     .doOnError(throwable -> log.error("Error processing notice", throwable))
                     .retry()
                     .subscribe();


    fafServerAccessor.connectionStateProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == ConnectionState.DISCONNECTED) {
        fxApplicationThreadExecutor.execute(gameIdToGame::clear);
      } else if (newValue == ConnectionState.CONNECTED && oldValue != ConnectionState.CONNECTED) {
        onLoggedIn();
      }
    });

    try {
      patchGamePrefsForMultiInstances();
    } catch (Exception e) {
      log.error("Game.prefs patch failed", e);
    }
  }

  private Mono<GameBean> initializeGameBean(GameInfo gameInfo) {
    return Mono.fromCallable(() -> {
                 GameBean newGame = new GameBean();
                 newGame.setId(gameInfo.getUid());
                 newGame.addPlayerChangeListener(generatePlayerChangeListener(newGame));
                 return newGame;
               })
               .publishOn(fxApplicationThreadExecutor.asScheduler())
               .doOnNext(game -> gameMapper.update(gameInfo, game))
               .doOnNext(game -> gameIdToGame.put(game.getId(), game));
  }

  private ChangeListener<Set<Integer>> generatePlayerChangeListener(GameBean newGame) {
    return (observable, oldValue, newValue) -> {
      oldValue.stream()
              .filter(player -> !newValue.contains(player))
              .map(playerService::getPlayerByIdIfOnline)
              .flatMap(Optional::stream)
              .filter(player -> newGame.equals(player.getGame()))
              .forEach(player -> player.setGame(null));

      newValue.stream()
              .filter(player -> !oldValue.contains(player))
              .map(playerService::getPlayerByIdIfOnline)
              .flatMap(Optional::stream)
              .forEach(player -> player.setGame(newGame));
    };
  }

  private ChangeListener<GameStatus> generateGameStatusListener(GameBean game) {
    return new ChangeListener<>() {
      @Override
      public void changed(ObservableValue<? extends GameStatus> observable, GameStatus oldStatus,
                          GameStatus newStatus) {
        if (!playerService.isCurrentPlayerInGame(game)) {
          observable.removeListener(this);
          return;
        }

        if (newStatus == GameStatus.CLOSED) {
          if (oldStatus == GameStatus.PLAYING) {
            GameService.this.onRecentlyPlayedGameEnded(game);
          }
          observable.removeListener(this);
        }
      }
    };
  }

  public ReadOnlyBooleanProperty gameRunningProperty() {
    return gameRunning;
  }

  public CompletableFuture<Void> hostGame(NewGameInfo newGameInfo) {
    if (isRunning()) {
      log.info("Game is running, ignoring host request");
      notificationService.addImmediateWarnNotification("game.gameRunning");
      return completedFuture(null);
    }

    if (!preferencesService.isValidGamePath()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      return gameDirectoryFuture.thenCompose(path -> hostGame(newGameInfo));
    }

    if (waitingForMatchMakerGame()) {
      addAlreadyInQueueNotification();
      return completedFuture(null);
    }

    return updateGameIfNecessary(newGameInfo.getFeaturedMod(), newGameInfo.getSimMods()).thenCompose(
                                                                                            aVoid -> downloadMapIfNecessary(newGameInfo.getMap()))
                                                                                        .thenCompose(
                                                                                            aVoid -> fafServerAccessor.requestHostGame(
                                                                                                newGameInfo))
                                                                                        .thenCompose(
                                                                                            gameLaunchResponse -> startGame(
                                                                                                gameMapper.map(
                                                                                                    gameLaunchResponse)));
  }

  private void addAlreadyInQueueNotification() {
    notificationService.addImmediateWarnNotification("teammatchmaking.notification.customAlreadyInQueue.message");
  }

  public CompletableFuture<Void> joinGame(GameBean game, String password) {
    if (isRunning()) {
      log.info("Game is running, ignoring join request");
      notificationService.addImmediateWarnNotification("game.gameRunning");
      return completedFuture(null);
    }

    if (!preferencesService.isValidGamePath()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      return gameDirectoryFuture.thenCompose(path -> joinGame(game, password));
    }

    if (waitingForMatchMakerGame()) {
      addAlreadyInQueueNotification();
      return completedFuture(null);
    }

    log.info("Joining game: '{}' ({})", game.getTitle(), game.getId());

    Set<String> simModUIds = game.getSimMods().keySet();
    return modService.getFeaturedMod(game.getFeaturedMod())
                     .toFuture()
                     .thenCompose(featuredModBean -> updateGameIfNecessary(featuredModBean, simModUIds))
                     .thenRun(() -> {
                       try {
                         modService.enableSimMods(simModUIds);
                       } catch (IOException e) {
                         log.error("SimMods could not be enabled", e);
                       }
                     })
                     .thenCompose(aVoid -> downloadMapIfNecessary(game.getMapFolderName()))
                     .thenCompose(aVoid -> fafServerAccessor.requestJoinGame(game.getId(), password))
                     .thenCompose(gameLaunchResponse -> {
                       synchronized (currentGame) {
                         // Store password in case we rehost
                         game.setPassword(password);
                         currentGame.set(game);
                       }

                       return startGame(gameMapper.map(gameLaunchResponse));
                     })
                     .exceptionally(throwable -> {
                       throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
                       log.error("Game could not be joined", throwable);
                       if (throwable instanceof NotifiableException notifiableException) {
                         notificationService.addErrorNotification(notifiableException);
                       } else {
                         notificationService.addImmediateErrorNotification(throwable, "games.couldNotJoin");
                       }
                       return null;
                     });
  }

  private CompletableFuture<Void> downloadMapIfNecessary(String mapFolderName) {
    if (mapService.isInstalled(mapFolderName)) {
      return completedFuture(null);
    }
    return mapService.download(mapFolderName);
  }

  /**
   * @param path a replay file that is readable by the preferences without any further conversion
   */
  public CompletableFuture<Void> runWithReplay(Path path, @Nullable Integer replayId, String featuredMod,
                                               Integer baseFafVersion, Map<String, Integer> featuredModFileVersions,
                                               Set<String> simMods, String mapFolderName) {
    if (!canStartReplay()) {
      return completedFuture(null);
    }

    if (!preferencesService.isValidGamePath()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      gameDirectoryFuture.thenAccept(
          pathSet -> runWithReplay(path, replayId, featuredMod, baseFafVersion, featuredModFileVersions, simMods,
                                   mapFolderName));
      return completedFuture(null);
    }

    return modService.getFeaturedMod(featuredMod)
                     .toFuture()
                     .thenCompose(featuredModBean -> updateReplayFilesIfNecessary(featuredModBean, simMods,
                                                                                  featuredModFileVersions,
                                                                                  baseFafVersion))
                     .thenCompose(
                         aVoid -> downloadMapIfNecessary(mapFolderName).handleAsync((ignoredResult, throwable) -> {
                           try {
                             return askWhetherToStartWithOutMap(throwable);
                           } catch (Throwable e) {
                             throw new CompletionException(e);
                           }
                         }))
                     .thenRun(() -> {
                       replayKilled = false;
                       try {
                         this.replayProcess = forgedAllianceService.startReplay(path, replayId);
                         setReplayRunning(true);
                         spawnReplayTerminationListener(this.replayProcess);
                       } catch (IOException e) {
                         notifyCantPlayReplay(replayId, e);
                       }
                     })
                     .exceptionally(throwable -> {
                       notifyCantPlayReplay(replayId, throwable);
                       return null;
                     });
  }

  private boolean canStartReplay() {
    if (isReplayRunning()) {
      log.info("Another replay is already running, not starting replay");
      notificationService.addImmediateWarnNotification("replay.replayRunning");
      return false;
    }
    return true;
  }

  public CompletableFuture<Path> postGameDirectoryChooseEvent() {
    CompletableFuture<Path> gameDirectoryFuture = new CompletableFuture<>();
    gameDirectoryRequiredHandler.onChooseGameDirectory(gameDirectoryFuture);
    return gameDirectoryFuture;
  }

  private Void askWhetherToStartWithOutMap(Throwable throwable) throws Throwable {
    if (throwable == null) {
      return null;
    }
    JavaFxUtil.assertBackgroundThread();
    log.error("Error loading map for replay", throwable);

    CountDownLatch userAnswered = new CountDownLatch(1);
    AtomicReference<Boolean> proceed = new AtomicReference<>(false);
    List<Action> actions = Arrays.asList(new Action(i18n.get("replay.ignoreMapNotFound"), event -> {
      proceed.set(true);
      userAnswered.countDown();
    }), new Action(i18n.get("replay.abortAfterMapNotFound"), event -> userAnswered.countDown()));
    notificationService.addNotification(new ImmediateNotification(i18n.get("replay.mapDownloadFailed"),
                                                                  i18n.get("replay.mapDownloadFailed.wannaContinue"),
                                                                  Severity.WARN, actions));
    userAnswered.await();
    if (!proceed.get()) {
      throw throwable;
    }
    return null;
  }

  private void notifyCantPlayReplay(@Nullable Integer replayId, Throwable throwable) {
    if (throwable instanceof UnsupportedOperationException) {
      notificationService.addImmediateErrorNotification(throwable, "gameUpdate.error.gameNotWritableAllowMultiOn");
    } else {
      log.error("Could not play replay `{}`", replayId, throwable);
      notificationService.addImmediateErrorNotification(throwable, "replayCouldNotBeStarted");
    }
  }

  public CompletableFuture<Void> runWithLiveReplay(URI replayUrl, Integer gameId, String gameType, String mapName) {
    if (!canStartReplay()) {
      return completedFuture(null);
    }

    if (!preferencesService.isValidGamePath()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      return gameDirectoryFuture.thenCompose(path -> runWithLiveReplay(replayUrl, gameId, gameType, mapName));
    }

    GameBean game = getByUid(gameId);

    Set<String> simModUids = game.getSimMods().keySet();

    return modService.getFeaturedMod(gameType)
                     .toFuture()
                     .thenCompose(
                         featuredModBean -> updateReplayFilesIfNecessary(featuredModBean, simModUids, null, null))
                     .thenCompose(aVoid -> downloadMapIfNecessary(mapName))
                     .thenRun(() -> {
                       replayKilled = false;
                       try {
                         this.replayProcess = forgedAllianceService.startReplay(replayUrl, gameId);
                         setReplayRunning(true);
                         spawnReplayTerminationListener(this.replayProcess);
                       } catch (IOException e) {
                         throw new GameLaunchException("Live replay could not be started", e, "replay.live.startError");
                       }
                     })
                     .exceptionally(throwable -> {
                       throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
                       notifyCantPlayReplay(gameId, throwable);
                       return null;
                     });
  }

  public GameBean getByUid(int uid) {
    GameBean game = gameIdToGame.get(uid);
    if (game == null) {
      log.warn("Can't find `{}` in known games", uid);
    }
    return game;
  }

  public void startSearchMatchmaker() {
    if (isRunning()) {
      log.info("Game is running, ignoring matchmaking search request");
      notificationService.addImmediateWarnNotification("game.gameRunning");
      return;
    }

    if (waitingForMatchMakerGame()) {
      log.info("Matchmaker search has already been started, ignoring call");
      return;
    }

    if (!preferencesService.isValidGamePath()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      gameDirectoryFuture.thenRun(this::startSearchMatchmaker);
      return;
    }

    log.info("Matchmaking search has been started");

    matchmakerFuture = modService.getFeaturedMod(FAF.getTechnicalName())
                                 .toFuture()
                                 .thenAccept(featuredModBean -> updateGameIfNecessary(featuredModBean, Set.of()))
                                 .thenCompose(aVoid -> fafServerAccessor.startSearchMatchmaker())
                                 .thenCompose(gameLaunchResponse -> downloadMapIfNecessary(
                                     gameLaunchResponse.getMapName()).thenCompose(aVoid -> {
                                   // We need to kill the replay to free the lock on the game.prefs
                                   if (isReplayRunning()) {
                                     replayKilled = true;
                                     replayProcess.destroy();
                                   }
                                   return leaderboardService.getActiveLeagueEntryForPlayer(
                                       playerService.getCurrentPlayer(), gameLaunchResponse.getLeaderboard());
                                 }).thenApply(leagueEntryOptional -> {
                                   GameParameters parameters = gameMapper.map(gameLaunchResponse);
                                   parameters.setDivision(
                                       leagueEntryOptional.map(bean -> bean.getSubdivision().getDivision().getNameKey())
                                                          .orElse("unlisted"));
                                   parameters.setSubdivision(
                                       leagueEntryOptional.map(bean -> bean.getSubdivision().getNameKey())
                                                          .orElse(null));
                                   return parameters;
                                 }).thenCompose(this::startGame));

    matchmakerFuture.whenComplete((aVoid, throwable) -> {
      if (throwable != null) {
        throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
        if (throwable instanceof CancellationException) {
          log.info("Matchmaking search has been cancelled");
          if (isRunning()) {
            notificationService.addServerNotification(
                new ImmediateNotification(i18n.get("matchmaker.cancelled.title"), i18n.get("matchmaker.cancelled"),
                                          Severity.INFO));
            gameKilled = true;
            process.destroy();
          }
        } else {
          log.warn("Matchmade game could not be started", throwable);
        }
      } else {
        log.info("Matchmaker queue exited");
      }
    });
  }

  public void stopSearchMatchmaker() {
    log.info("Stopping matchmaker search");
    if (matchmakerFuture != null) {
      matchmakerFuture.cancel(true);
    }
  }

  /**
   * Returns the preferences the player is currently in. Returns {@code null} if not in a preferences.
   */
  @Nullable
  public GameBean getCurrentGame() {
    synchronized (currentGame) {
      return currentGame.get();
    }
  }

  public ReadOnlyObjectProperty<GameBean> currentGameProperty() {
    return currentGame.getReadOnlyProperty();
  }

  private boolean isRunning() {
    return process != null && process.isAlive();
  }

  public CompletableFuture<Void> updateGameIfNecessary(FeaturedModBean featuredModBean, Set<String> simModUids) {
    return gameUpdater.update(featuredModBean, simModUids, null, null, false);
  }

  private CompletableFuture<Void> updateReplayFilesIfNecessary(FeaturedModBean featuredModBean, Set<String> simModUids,
                                                               @Nullable Map<String, Integer> featuredModFileVersions,
                                                               @Nullable Integer version) {
    return gameUpdater.update(featuredModBean, simModUids, featuredModFileVersions, version, true);
  }

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

  public boolean isReplayRunning() {
    synchronized (replayRunning) {
      return replayRunning.get();
    }
  }

  private void setReplayRunning(boolean running) {
    synchronized (replayRunning) {
      this.replayRunning.set(running);
    }
  }

  private boolean waitingForMatchMakerGame() {
    return matchmakerFuture != null && !matchmakerFuture.isDone();
  }

  /**
   * Actually starts the game, including relay and replay server. Call this method when everything else is prepared
   * (mod/map download, connectivity check etc.)
   */
  private CompletableFuture<Void> startGame(GameParameters gameParameters) {
    if (isRunning()) {
      log.info("Forged Alliance is already running, not starting game");
      CompletableFuture.completedFuture(null);
    }

    int uid = gameParameters.getUid();
    return replayServer.start(uid, () -> getByUid(uid))
                       .thenCompose(port -> {
                         localReplayPort = port;
                         return iceAdapter.start(gameParameters.getUid());
                       })
                       .thenCompose(adapterPort -> coturnService.getSelectedCoturns(uid)
                                                                .thenAccept(iceAdapter::setIceServers)
                                                                .thenApply(aVoid -> adapterPort))
                       .thenApply(adapterPort -> {
                         fafServerAccessor.setPingIntervalSeconds(5);
                         gameKilled = false;
                         gameParameters.setLocalGpgPort(adapterPort);
                         gameParameters.setLocalReplayPort(localReplayPort);
                         try {
                           process = forgedAllianceService.startGameOnline(gameParameters);
                         } catch (IOException e) {
                           throw new GameLaunchException("Could not start game", e, "game.start.couldNotStart");
                         }
                         setGameRunning(true);
                         return process;
                       })
                       .exceptionally(throwable -> {
                         throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
                         log.error("Game could not be started", throwable);
                         if (throwable instanceof NotifiableException notifiableException) {
                           notificationService.addErrorNotification(notifiableException);
                         } else {
                           notificationService.addImmediateErrorNotification(throwable, "games.couldNotStart");
                         }
                         iceAdapter.stop();
                         setGameRunning(false);
                         return null;
                       })
                       .thenCompose(process -> spawnTerminationListener(process, true));
  }

  private void onRecentlyPlayedGameEnded(GameBean game) {
    if (!notificationPrefs.isAfterGameReviewEnabled() || !notificationPrefs.isTransientNotificationsEnabled()) {
      return;
    }

    notificationService.addNotification(
        new PersistentNotification(i18n.get("game.ended", game.getTitle()), Severity.INFO, singletonList(
            new Action(i18n.get("game.rate"),
                       actionEvent -> navigationHandler.navigateTo(new ShowReplayEvent(game.getId()))))));
  }

  @VisibleForTesting
  CompletableFuture<Void> spawnTerminationListener(Process process, Boolean forOnlineGame) {
    return process.onExit().thenAccept(finishedProcess -> {
      handleTermination(finishedProcess, false);

      synchronized (gameRunning) {
        gameRunning.set(false);
        if (forOnlineGame) {
          fafServerAccessor.notifyGameEnded();
          iceAdapter.stop();
          try {
            replayServer.stop();
          } catch (IOException e) {
            throw new GameCleanupException("Error during post-game processing", e, "replayServer.stopError");
          }
        }
      }
    });
  }

  @VisibleForTesting
  void spawnReplayTerminationListener(Process process) {
    process.onExit().thenAccept(finishedProcess -> {
      handleTermination(finishedProcess, true);
      setReplayRunning(false);
    });
  }

  private void handleTermination(Process finishedProcess, boolean isReplay) {
    fafServerAccessor.setPingIntervalSeconds(25);
    int exitCode = finishedProcess.exitValue();
    log.info("Forged Alliance terminated with exit code {}", exitCode);
    Optional<Path> logFile = loggingService.getMostRecentGameLogFile();
    logFile.ifPresent(file -> {
      try {
        Files.writeString(file, logMasker.maskMessage(Files.readString(file)));
      } catch (IOException e) {
        log.warn("Could not open log file", e);
      }
    });

    if (exitCode != 0 && ((!isReplay && !gameKilled) || (isReplay && !replayKilled))) {
      if (exitCode == -1073741515) {
        notificationService.addImmediateWarnNotification("game.crash.notInitialized");
      } else {
        notificationService.addNotification(new ImmediateNotification(i18n.get("errorTitle"),
                                                                      i18n.get("game.crash", exitCode,
                                                                               logFile.map(Path::toString).orElse("")),
                                                                      WARN, List.of(
            new Action(i18n.get("game.open.log"),
                       event -> platformService.reveal(logFile.orElse(operatingSystem.getLoggingDirectory()))),
            new DismissAction(i18n))));
      }
    }
  }

  private void onLoggedIn() {
    if (isGameRunning()) {
      fafServerAccessor.restoreGameSession(currentGame.get().getId());
    }
  }

  private GameBean enhanceWithLastPasswordIfPasswordProtected(GameBean game) {
    if (!game.isPasswordProtected()) {
      return game;
    }
    String lastGamePassword = lastGamePrefs.getLastGamePassword();
    game.setPassword(lastGamePassword);
    return game;
  }

  private void killGame() {
    if (isRunning()) {
      log.info("ForgedAlliance still running, destroying process");
      iceAdapter.onGameCloseRequested();
      process.destroy();
    }
  }

  public void launchTutorial(MapVersionBean mapVersion, String technicalMapName) {

    if (!preferencesService.isValidGamePath()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      gameDirectoryFuture.thenAccept(path -> launchTutorial(mapVersion, technicalMapName));
      return;
    }

    modService.getFeaturedMod(TUTORIALS.getTechnicalName())
              .toFuture()
              .thenCompose(featuredModBean -> updateGameIfNecessary(featuredModBean, emptySet()))
              .thenCompose(aVoid -> downloadMapIfNecessary(mapVersion.getFolderName()))
              .thenCompose(aVoid -> {
                try {
                  process = forgedAllianceService.startGameOffline(technicalMapName);
                  setGameRunning(true);
                  return spawnTerminationListener(process, false);
                } catch (IOException e) {
                  throw new CompletionException(e);
                }
              })
              .exceptionally(throwable -> {
                throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
                log.error("Launching tutorials failed", throwable);
                if (throwable instanceof NotifiableException notifiableException) {
                  notificationService.addErrorNotification(notifiableException);
                } else {
                  notificationService.addImmediateErrorNotification(throwable, "tutorial.launchFailed");
                }
                return null;
              });
  }

  public void startGameOffline() throws IOException {
    if (!preferencesService.isValidGamePath()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      gameDirectoryFuture.thenAccept(path -> {
        try {
          startGameOffline();
        } catch (IOException e) {
          throw new CompletionException(e);
        }
      });
      return;
    }

    process = forgedAllianceService.startGameOffline(null);
    setGameRunning(true);
    spawnTerminationListener(process, false);
  }

  @Async
  public CompletableFuture<Void> patchGamePrefsForMultiInstances() throws IOException, ExecutionException, InterruptedException {
    if (isGamePrefsPatchedToAllowMultiInstances().get()) {
      return failedFuture(new IllegalStateException("Can not patch game.prefs file cause it already is patched"));
    }
    Path preferencesFile = forgedAlliancePrefs.getPreferencesFile();
    Files.writeString(preferencesFile, GAME_PREFS_ALLOW_MULTI_LAUNCH_STRING, US_ASCII, StandardOpenOption.APPEND);
    return completedFuture(null);
  }

  private String getGamePrefsContent() throws IOException {
    Path preferencesFile = forgedAlliancePrefs.getPreferencesFile();
    return Files.readString(preferencesFile, US_ASCII);
  }

  @Async
  public CompletableFuture<Boolean> isGamePrefsPatchedToAllowMultiInstances() throws IOException {
    String gamePrefsContent = getGamePrefsContent();
    return completedFuture(GAME_PREFS_ALLOW_MULTI_LAUNCH_PATTERN.matcher(gamePrefsContent).find());
  }

  public long getRunningProcessId() {
    return isRunning() ? process.pid() : -1;
  }
}
