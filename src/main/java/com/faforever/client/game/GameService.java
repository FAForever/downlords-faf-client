package com.faforever.client.game;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.discord.DiscordRichPresenceService;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.fa.ForgedAllianceService;
import com.faforever.client.fa.relay.event.CloseGameEvent;
import com.faforever.client.fa.relay.event.RehostRequestEvent;
import com.faforever.client.fa.relay.ice.IceAdapter;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.error.GameCleanupException;
import com.faforever.client.game.error.GameLaunchException;
import com.faforever.client.i18n.I18n;
import com.faforever.client.logging.LoggingService;
import com.faforever.client.main.event.ShowReplayEvent;
import com.faforever.client.map.MapService;
import com.faforever.client.mapstruct.GameMapper;
import com.faforever.client.mod.ModService;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.patch.GameUpdater;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.remote.ReconnectTimerService;
import com.faforever.client.replay.ReplayServer;
import com.faforever.client.teammatchmaking.event.PartyOwnerChangedEvent;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.MaskPatternLayout;
import com.faforever.client.util.RatingUtil;
import com.faforever.commons.lobby.GameInfo;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameStatus;
import com.faforever.commons.lobby.GameVisibility;
import com.faforever.commons.lobby.LoginSuccessResponse;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static com.faforever.client.game.KnownFeaturedMod.FAF;
import static com.faforever.client.game.KnownFeaturedMod.TUTORIALS;
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
public class GameService implements InitializingBean {

  private static final Pattern GAME_PREFS_ALLOW_MULTI_LAUNCH_PATTERN = Pattern.compile("debug\\s*=(\\s)*[{][^}]*enable_debug_facilities\\s*=\\s*true");
  private static final String GAME_PREFS_ALLOW_MULTI_LAUNCH_STRING = "\ndebug = {\n" +
      "    enable_debug_facilities = true\n" +
      "}";

  @VisibleForTesting
  final BooleanProperty gameRunning;

  /** TODO: Explain why access needs to be synchronized. */
  @VisibleForTesting
  final SimpleObjectProperty<GameBean> currentGame;

  private final ObservableMap<Integer, GameBean> gameIdToGame;

  private final FafServerAccessor fafServerAccessor;
  private final ForgedAllianceService forgedAllianceService;
  private final MapService mapService;
  private final PreferencesService preferencesService;
  private final LoggingService loggingService;
  private final GameUpdater gameUpdater;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final PlayerService playerService;
  private final EventBus eventBus;
  private final IceAdapter iceAdapter;
  private final ModService modService;
  private final PlatformService platformService;
  private final DiscordRichPresenceService discordRichPresenceService;
  private final ReplayServer replayServer;
  private final ReconnectTimerService reconnectTimerService;
  private final ObservableList<GameBean> games;
  private final String faWindowTitle;
  private final MaskPatternLayout logMasker;
  private final GameMapper gameMapper;
  private final ForgedAlliancePrefs forgedAlliancePrefs;

  private Process process;
  private CompletableFuture<Void> matchmakerFuture;
  private boolean gameKilled;
  private boolean rehostRequested;
  private int localReplayPort;
  private boolean inOthersParty;

  public GameService(ClientProperties clientProperties,
                     FafServerAccessor fafServerAccessor,
                     ForgedAllianceService forgedAllianceService,
                     MapService mapService,
                     PreferencesService preferencesService,
                     LoggingService loggingService,
                     GameUpdater gameUpdater,
                     NotificationService notificationService,
                     I18n i18n,
                     PlayerService playerService,
                     EventBus eventBus,
                     IceAdapter iceAdapter,
                     ModService modService,
                     PlatformService platformService,
                     DiscordRichPresenceService discordRichPresenceService,
                     ReplayServer replayServer,
                     ReconnectTimerService reconnectTimerService,
                     GameMapper gameMapper) {
    this.fafServerAccessor = fafServerAccessor;
    this.forgedAllianceService = forgedAllianceService;
    this.mapService = mapService;
    this.preferencesService = preferencesService;
    this.loggingService = loggingService;
    this.gameUpdater = gameUpdater;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.playerService = playerService;
    this.eventBus = eventBus;
    this.iceAdapter = iceAdapter;
    this.modService = modService;
    this.platformService = platformService;
    this.discordRichPresenceService = discordRichPresenceService;
    this.replayServer = replayServer;
    this.reconnectTimerService = reconnectTimerService;
    this.gameMapper = gameMapper;

    logMasker = new MaskPatternLayout();
    faWindowTitle = clientProperties.getForgedAlliance().getWindowTitle();
    gameIdToGame = FXCollections.observableMap(new ConcurrentHashMap<>());
    gameRunning = new SimpleBooleanProperty();
    currentGame = new SimpleObjectProperty<>();
    games = FXCollections.synchronizedObservableList(FXCollections.observableList(new ArrayList<>(),
        item -> new Observable[]{item.statusProperty(), item.teamsProperty()}
    ));
    forgedAlliancePrefs = preferencesService.getPreferences().getForgedAlliance();
    inOthersParty = false;
  }

  @Override
  public void afterPropertiesSet() {
    currentGame.addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        discordRichPresenceService.clearGameInfo();
        return;
      }

      InvalidationListener listener = generateNumberOfPlayersChangeListener(newValue);
      JavaFxUtil.addAndTriggerListener(newValue.numPlayersProperty(), listener);

      ChangeListener<GameStatus> statusChangeListener = generateGameStatusListener(newValue);
      JavaFxUtil.addAndTriggerListener(newValue.statusProperty(), statusChangeListener);
    });

    JavaFxUtil.attachListToMap(games, gameIdToGame);
    JavaFxUtil.addListener(
        gameRunning,
        (observable, oldValue, newValue) -> reconnectTimerService.setGameRunning(newValue)
    );

    eventBus.register(this);

    fafServerAccessor.addEventListener(GameInfo.class, this::onGameInfo);
    fafServerAccessor.addEventListener(LoginSuccessResponse.class, message -> onLoggedIn());

    JavaFxUtil.addListener(
        fafServerAccessor.connectionStateProperty(),
        (observable, oldValue, newValue) -> {
          if (newValue == ConnectionState.DISCONNECTED) {
            synchronized (gameIdToGame) {
              gameIdToGame.clear();
            }
          }
        }
    );
  }

  @NotNull
  private InvalidationListener generateNumberOfPlayersChangeListener(GameBean game) {
    return new InvalidationListener() {
      @Override
      public void invalidated(Observable observable) {
        if (currentGame.get() == null || !Objects.equals(game, currentGame.get())) {
          observable.removeListener(this);
          return;
        }
        discordRichPresenceService.updatePlayedGameTo(currentGame.get());
      }
    };
  }

  @NotNull
  private ChangeListener<GameStatus> generateGameStatusListener(GameBean game) {
    return new ChangeListener<>() {
      @Override
      public void changed(ObservableValue<? extends GameStatus> observable, GameStatus oldStatus, GameStatus newStatus) {
        if (!playerService.isCurrentPlayerInGame(game)) {
          observable.removeListener(this);
          return;
        }

        if (Objects.equals(currentGame.get(), game)) {
          discordRichPresenceService.updatePlayedGameTo(currentGame.get());
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
      log.warn("Game is running, ignoring host request");
      notificationService.addImmediateWarnNotification("game.gameRunning");
      return completedFuture(null);
    }

    if (!preferencesService.isGamePathValid()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      return gameDirectoryFuture.thenCompose(path -> hostGame(newGameInfo));
    }

    if (isInMatchmakerQueue()) {
      addAlreadyInQueueNotification();
      return completedFuture(null);
    }

    return updateGameIfNecessary(newGameInfo.getFeaturedMod(), null, newGameInfo.getSimMods())
        .thenCompose(aVoid -> downloadMapIfNecessary(newGameInfo.getMap()))
        .thenCompose(aVoid -> fafServerAccessor.requestHostGame(newGameInfo))
        .thenAccept(this::startGame);
  }

  private void addAlreadyInQueueNotification() {
    notificationService.addImmediateWarnNotification("teammatchmaking.notification.customAlreadyInQueue.message");
  }

  public CompletableFuture<Void> joinGame(GameBean game, String password) {
    if (isRunning()) {
      log.warn("Game is running, ignoring join request");
      notificationService.addImmediateWarnNotification("game.gameRunning");
      return completedFuture(null);
    }

    if (!preferencesService.isGamePathValid()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      return gameDirectoryFuture.thenCompose(path -> joinGame(game, password));
    }

    if (isInMatchmakerQueue()) {
      addAlreadyInQueueNotification();
      return completedFuture(null);
    }

    log.info("Joining game: '{}' ({})", game.getTitle(), game.getId());

    Set<String> simModUIds = game.getSimMods().keySet();
    return modService.getFeaturedMod(game.getFeaturedMod())
        .thenCompose(featuredModBean -> updateGameIfNecessary(featuredModBean, null, simModUIds))
        .thenAccept(aVoid -> {
          try {
            modService.enableSimMods(simModUIds);
          } catch (IOException e) {
            log.warn("SimMods could not be enabled", e);
          }
        })
        .thenCompose(aVoid -> downloadMapIfNecessary(game.getMapFolderName()))
        .thenCompose(aVoid -> fafServerAccessor.requestJoinGame(game.getId(), password))
        .thenAccept(gameLaunchMessage -> {
          synchronized (currentGame) {
            // Store password in case we rehost
            game.setPassword(password);
            currentGame.set(game);
          }

          startGame(gameLaunchMessage);
        })
        .exceptionally(throwable -> {
          log.warn("Game could not be joined", throwable);
          notificationService.addImmediateErrorNotification(throwable, "games.couldNotJoin");
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
  public CompletableFuture<Void> runWithReplay(Path path, @Nullable Integer replayId, String featuredMod, Integer version, Map<String, Integer> modVersions, Set<String> simMods, String mapName) {
    if (!canStartReplay()) {
      return completedFuture(null);
    }

    if (!preferencesService.isGamePathValid()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      gameDirectoryFuture.thenAccept(pathSet -> runWithReplay(path, replayId, featuredMod, version, modVersions, simMods, mapName));
      return completedFuture(null);
    }

    return modService.getFeaturedMod(featuredMod)
        .thenCompose(featuredModBean -> updateGameIfNecessary(featuredModBean, version, simMods))
        .thenCompose(aVoid -> downloadMapIfNecessary(mapName)
            .handleAsync((ignoredResult, throwable) -> {
              try {
                return askWhetherToStartWithOutMap(throwable);
              } catch (Throwable e) {
                throw new CompletionException(e);
              }
            }))
        .thenRun(() -> {
          try {
            Process processForReplay = forgedAllianceService.startReplay(path, replayId);
            if (forgedAlliancePrefs.isAllowReplaysWhileInGame() && isRunning()) {
              return;
            }
            this.process = processForReplay;
            setGameRunning(true);
            spawnTerminationListener(this.process);
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
    if (isRunning() && !forgedAlliancePrefs.isAllowReplaysWhileInGame()) {
      log.warn("Forged Alliance is already running and experimental concurrent game feature not turned on, not starting replay");
      notificationService.addImmediateWarnNotification("replay.gameRunning");
      return false;
    } else if (isInMatchmakerQueue()) {
      log.warn("In matchmaker queue, not starting replay");
      notificationService.addImmediateWarnNotification("replay.inQueue");
      return false;
    } else if (inOthersParty) {
      log.info("In party, not starting replay");
      notificationService.addImmediateWarnNotification("replay.inParty");
      return false;
    }
    return true;
  }

  @NotNull
  public CompletableFuture<Path> postGameDirectoryChooseEvent() {
    CompletableFuture<Path> gameDirectoryFuture = new CompletableFuture<>();
    eventBus.post(new GameDirectoryChooseEvent(gameDirectoryFuture));
    return gameDirectoryFuture;
  }

  private Void askWhetherToStartWithOutMap(Throwable throwable) throws Throwable {
    if (throwable == null) {
      return null;
    }
    JavaFxUtil.assertBackgroundThread();
    log.warn("Something went wrong loading map for replay", throwable);

    CountDownLatch userAnswered = new CountDownLatch(1);
    AtomicReference<Boolean> proceed = new AtomicReference<>(false);
    List<Action> actions = Arrays.asList(new Action(i18n.get("replay.ignoreMapNotFound"), event -> {
          proceed.set(true);
          userAnswered.countDown();
        }),
        new Action(i18n.get("replay.abortAfterMapNotFound"), event -> userAnswered.countDown()));
    notificationService.addNotification(new ImmediateNotification(i18n.get("replay.mapDownloadFailed"), i18n.get("replay.mapDownloadFailed.wannaContinue"), Severity.WARN, actions));
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
      log.error("Could not play replay '" + replayId + "'", throwable);
      notificationService.addImmediateErrorNotification(throwable, "replayCouldNotBeStarted");
    }
  }

  public CompletableFuture<Void> runWithLiveReplay(URI replayUrl, Integer gameId, String gameType, String mapName) {
    if (!canStartReplay()) {
      return completedFuture(null);
    }

    if (!preferencesService.isGamePathValid()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      return gameDirectoryFuture.thenCompose(path -> runWithLiveReplay(replayUrl, gameId, gameType, mapName));
    }

    GameBean game = getByUid(gameId);

    Set<String> simModUids = game.getSimMods().keySet();

    return modService.getFeaturedMod(gameType)
        .thenCompose(featuredModBean -> updateGameIfNecessary(featuredModBean, null, simModUids))
        .thenCompose(aVoid -> downloadMapIfNecessary(mapName))
        .thenRun(() -> {
              Process processCreated;
              try {
                processCreated = forgedAllianceService.startReplay(replayUrl, gameId, playerService.getCurrentPlayer());
              } catch (IOException e) {
                throw new GameLaunchException("Live replay could not be started", e, "replay.live.startError");
              }
              if (forgedAlliancePrefs.isAllowReplaysWhileInGame() && isRunning()) {
                return;
              }
              this.process = processCreated;
              setGameRunning(true);
              spawnTerminationListener(this.process);
            }
        ).exceptionally(throwable -> {
          throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
          notifyCantPlayReplay(gameId, throwable);
          return null;
        });
  }

  public ObservableList<GameBean> getGames() {
    return games;
  }

  public GameBean getByUid(int uid) {
    GameBean game = gameIdToGame.get(uid);
    if (game == null) {
      log.warn("Can't find {} in gameInfoBean map", uid);
    }
    return game;
  }

  public CompletableFuture<Void> startSearchMatchmaker() {
    if (isRunning()) {
      log.warn("Game is running, ignoring matchmaking search request");
      notificationService.addImmediateWarnNotification("game.gameRunning");
      return completedFuture(null);
    }

    if (isInMatchmakerQueue()) {
      log.debug("Matchmaker search has already been started, ignoring call");
      return matchmakerFuture;
    }

    if (!preferencesService.isGamePathValid()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      return gameDirectoryFuture.thenCompose(path -> startSearchMatchmaker());
    }

    log.info("Matchmaking search has been started");

    matchmakerFuture = modService.getFeaturedMod(FAF.getTechnicalName())
        .thenAccept(featuredModBean -> updateGameIfNecessary(featuredModBean, null, Set.of()))
        .thenCompose(aVoid -> fafServerAccessor.startSearchMatchmaker())
        .thenCompose((gameLaunchMessage) -> downloadMapIfNecessary(gameLaunchMessage.getMapName())
            .thenCompose(aVoid -> startGame(gameLaunchMessage)));

    matchmakerFuture.whenComplete((aVoid, throwable) -> {
      if (throwable != null) {
        throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
        if (throwable instanceof CancellationException) {
          log.info("Matchmaking search has been cancelled");
          if (isRunning()) {
            notificationService.addServerNotification(new ImmediateNotification(i18n.get("matchmaker.cancelled.title"), i18n.get("matchmaker.cancelled"), Severity.INFO));
            gameKilled = true;
            process.destroy();
          }
        } else {
          log.warn("Matchmade game could not be started", throwable);
        }
      } else {
        log.debug("Matchmaker queue exited");
      }
    });

    return matchmakerFuture;
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

  private boolean isRunning() {
    return process != null && process.isAlive();
  }

  private CompletableFuture<Void> updateGameIfNecessary(FeaturedModBean featuredModBean, @Nullable Integer version, @NotNull Set<String> simModUids) {
    return gameUpdater.update(featuredModBean, version, simModUids);
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

  public boolean isInMatchmakerQueue() {
    return matchmakerFuture != null && !matchmakerFuture.isDone();
  }

  /**
   * Actually starts the game, including relay and replay server. Call this method when everything else is prepared
   * (mod/map download, connectivity check etc.)
   */
  private CompletableFuture<Void> startGame(GameLaunchResponse gameLaunchMessage) {
    if (isRunning()) {
      log.warn("Forged Alliance is already running, not starting game");
      CompletableFuture.completedFuture(null);
    }

    int uid = gameLaunchMessage.getUid();
    return replayServer.start(uid, () -> getByUid(uid))
        .thenCompose(port -> {
          localReplayPort = port;
          return iceAdapter.start();
        })
        .thenApply(adapterPort -> {
          gameKilled = false;
          try {
            process = forgedAllianceService.startGameOnline(gameLaunchMessage,
                adapterPort, localReplayPort, rehostRequested);
          } catch (IOException e) {
            throw new GameLaunchException("Could not start game", e, "game.start.couldNotStart");
          }
          setGameRunning(true);
          return process;
        })
        .exceptionally(throwable -> {
          throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
          log.warn("Game could not be started", throwable);
          notificationService.addImmediateErrorNotification(throwable, "game.start.couldNotStart");
          iceAdapter.stop();
          setGameRunning(false);
          return null;
        })
        .thenCompose(this::spawnTerminationListener);
  }

  private void onRecentlyPlayedGameEnded(GameBean game) {
    NotificationsPrefs notification = preferencesService.getPreferences().getNotification();
    if (!notification.isAfterGameReviewEnabled() || !notification.isTransientNotificationsEnabled()) {
      return;
    }

    notificationService.addNotification(new PersistentNotification(i18n.get("game.ended", game.getTitle()),
        Severity.INFO,
        singletonList(new Action(i18n.get("game.rate"), actionEvent -> eventBus.post(new ShowReplayEvent(game.getId()))))));
  }

  @VisibleForTesting
  CompletableFuture<Void> spawnTerminationListener(Process process) {
    return spawnTerminationListener(process, true);
  }

  @VisibleForTesting
  CompletableFuture<Void> spawnTerminationListener(Process process, Boolean forOnlineGame) {
    rehostRequested = false;
    return process.onExit().thenAccept(finishedProcess -> {
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

      if (exitCode != 0 && !gameKilled) {
        notificationService.addImmediateWarnNotification("game.crash", exitCode, logFile.map(Path::toString).orElse(""));
      }

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

        if (rehostRequested) {
          rehost();
        }
      }
    });
  }

  private void rehost() {
    synchronized (currentGame) {
      GameBean game = currentGame.get();

      modService.getFeaturedMod(game.getFeaturedMod())
          .thenAccept(featuredModBean -> hostGame(new NewGameInfo(
              game.getTitle(),
              game.getPassword(),
              featuredModBean,
              game.getMapFolderName(),
              new HashSet<>(game.getSimMods().values()),
              GameVisibility.PUBLIC,
              game.getRatingMin(), game.getRatingMax(), game.getEnforceRating())));
    }
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


  private void onLoggedIn() {
    if (isGameRunning()) {
      fafServerAccessor.restoreGameSession(currentGame.get().getId());
    }
  }

  private void onGameInfo(GameInfo gameInfoMessage) {
    if (gameInfoMessage.getGames() != null) {
      gameInfoMessage.getGames().forEach(this::onGameInfo);
      return;
    }

    Integer gameId = gameInfoMessage.getUid();
    GameBean game = gameIdToGame.computeIfAbsent(gameId, integer -> {
      GameBean newGame = new GameBean();
      newGame.setTeamsListener(observable -> newGame.setAverageRating(calcAverageRating(newGame)));
      return newGame;
    });
    gameMapper.update(gameInfoMessage, game);
    playerService.updatePlayersInGame(game);
    if (game.getStatus() == GameStatus.CLOSED) {
      boolean removed = gameIdToGame.remove(gameInfoMessage.getUid(), game);
      if (!removed) {
        log.debug("Could not remove game, unexpected game mapping: '{}'", gameId);
      }
    }

    synchronized (currentGame) {
      if (playerService.isCurrentPlayerInGame(game)) {
        if (GameStatus.OPEN == game.getStatus()) {
          currentGame.set(enhanceWithLastPasswordIfPasswordProtected(game));
        } else if (GameStatus.CLOSED == game.getStatus()) {
          currentGame.set(null);
        }
      } else if (Objects.equals(currentGame.get(), game)) {
        currentGame.set(null);
      }
    }

    JavaFxUtil.addListener(game.statusProperty(), (observable, oldValue, newValue) -> {
      if (oldValue == GameStatus.OPEN
          && newValue == GameStatus.PLAYING
          && playerService.isCurrentPlayerInGame(game)
          && !platformService.isWindowFocused(faWindowTitle)) {
        platformService.focusWindow(faWindowTitle);
      }
    });
  }

  private GameBean enhanceWithLastPasswordIfPasswordProtected(GameBean game) {
    if (!game.isPasswordProtected()) {
      return game;
    }
    String lastGamePassword = preferencesService.getPreferences().getLastGame().getLastGamePassword();
    game.setPassword(lastGamePassword);
    return game;
  }

  private double calcAverageRating(GameBean game) {
    return playerService.getAllPlayersInGame(game).stream()
        .mapToInt(player -> RatingUtil.getLeaderboardRating(player, game.getLeaderboard()))
        .average()
        .orElse(0.0);
  }

  public void killGame() {
    if (process != null && process.isAlive()) {
      log.info("ForgedAlliance still running, destroying process");
      process.destroy();
    }
  }

  @Subscribe
  public void onGameCloseRequested(CloseGameEvent event) {
    killGame();
  }

  @Subscribe
  public void onPartyOwnerChangedEvent(PartyOwnerChangedEvent event) {
    inOthersParty = !Objects.equals(playerService.getCurrentPlayer(), event.getNewOwner());
  }

  public void launchTutorial(MapVersionBean mapVersion, String technicalMapName) {

    if (!preferencesService.isGamePathValid()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      gameDirectoryFuture.thenAccept(path -> launchTutorial(mapVersion, technicalMapName));
      return;
    }

    modService.getFeaturedMod(TUTORIALS.getTechnicalName())
        .thenCompose(featuredModBean -> updateGameIfNecessary(featuredModBean, null, emptySet()))
        .thenCompose(aVoid -> downloadMapIfNecessary(mapVersion.getFolderName()))
        .thenAccept(aVoid -> {
          try {
            process = forgedAllianceService.startGameOffline(technicalMapName);
            setGameRunning(true);
            spawnTerminationListener(process, false);
          } catch (IOException e) {
            throw new CompletionException(e);
          }
        })
        .exceptionally(throwable -> {
          throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
          log.error("Launching tutorials failed", throwable);
          notificationService.addImmediateErrorNotification(throwable, "tutorial.launchFailed");
          return null;
        });
  }

  public void startGameOffline() throws IOException {
    if (!preferencesService.isGamePathValid()) {
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
    Path preferencesFile = preferencesService.getPreferences().getForgedAlliance().getPreferencesFile();
    Files.writeString(preferencesFile, GAME_PREFS_ALLOW_MULTI_LAUNCH_STRING, US_ASCII, StandardOpenOption.APPEND);
    return completedFuture(null);
  }

  private String getGamePrefsContent() throws IOException {
    Path preferencesFile = preferencesService.getPreferences().getForgedAlliance().getPreferencesFile();
    return Files.readString(preferencesFile, US_ASCII);
  }

  @Async
  public CompletableFuture<Boolean> isGamePrefsPatchedToAllowMultiInstances() throws IOException {
    String gamePrefsContent = getGamePrefsContent();
    return completedFuture(GAME_PREFS_ALLOW_MULTI_LAUNCH_PATTERN.matcher(gamePrefsContent).find());
  }
}
