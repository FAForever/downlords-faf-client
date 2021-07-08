package com.faforever.client.game;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.discord.DiscordRichPresenceService;
import com.faforever.client.fa.CloseGameEvent;
import com.faforever.client.fa.ForgedAllianceService;
import com.faforever.client.fa.relay.event.RehostRequestEvent;
import com.faforever.client.fa.relay.ice.IceAdapter;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowReplayEvent;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.ModService;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.patch.GameUpdater;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.ReconnectTimerService;
import com.faforever.client.remote.domain.GameInfoMessage;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.replay.ReplayServer;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.teammatchmaking.event.PartyOwnerChangedEvent;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.faforever.client.util.RatingUtil;
import com.faforever.commons.api.dto.Faction;
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
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static com.faforever.client.game.KnownFeaturedMod.FAF;
import static com.faforever.client.game.KnownFeaturedMod.TUTORIALS;
import static com.github.nocatch.NoCatch.noCatch;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Collections.emptyMap;
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

  @VisibleForTesting
  static final String DEFAULT_RATING_TYPE = "global";
  private static final Pattern GAME_PREFS_ALLOW_MULTI_LAUNCH_PATTERN = Pattern.compile("debug\\s*=(\\s)*[{][^}]*enable_debug_facilities\\s*=\\s*true");
  private static final String GAME_PREFS_ALLOW_MULTI_LAUNCH_STRING = "\ndebug = {\n" +
      "    enable_debug_facilities = true\n" +
      "}";

  @VisibleForTesting
  final BooleanProperty gameRunning;

  /** TODO: Explain why access needs to be synchronized. */
  @VisibleForTesting
  final SimpleObjectProperty<Game> currentGame;

  private final ObservableMap<Integer, Game> gameIdToGame;

  private final FafService fafService;
  private final ForgedAllianceService forgedAllianceService;
  private final MapService mapService;
  private final PreferencesService preferencesService;
  private final GameUpdater gameUpdater;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final ExecutorService executorService;
  private final PlayerService playerService;
  private final ReportingService reportingService;
  private final EventBus eventBus;
  private final IceAdapter iceAdapter;
  private final ModService modService;
  private final PlatformService platformService;
  private final DiscordRichPresenceService discordRichPresenceService;
  private final ReplayServer replayServer;
  private final ReconnectTimerService reconnectTimerService;
  private final ObservableList<Game> games;
  private final String faWindowTitle;

  private Process process;
  private boolean rehostRequested;
  private int localReplayPort;
  private ForgedAlliancePrefs forgedAlliancePrefs;
  private boolean inOthersParty;
  private boolean inMatchmakerQueue;

  @Inject
  public GameService(ClientProperties clientProperties,
                     FafService fafService,
                     ForgedAllianceService forgedAllianceService,
                     MapService mapService,
                     PreferencesService preferencesService,
                     GameUpdater gameUpdater,
                     NotificationService notificationService,
                     I18n i18n,
                     ExecutorService executorService,
                     PlayerService playerService,
                     ReportingService reportingService,
                     EventBus eventBus,
                     IceAdapter iceAdapter,
                     ModService modService,
                     PlatformService platformService,
                     DiscordRichPresenceService discordRichPresenceService,
                     ReplayServer replayServer,
                     ReconnectTimerService reconnectTimerService) {
    this.fafService = fafService;
    this.forgedAllianceService = forgedAllianceService;
    this.mapService = mapService;
    this.preferencesService = preferencesService;
    this.gameUpdater = gameUpdater;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.executorService = executorService;
    this.playerService = playerService;
    this.reportingService = reportingService;
    this.eventBus = eventBus;
    this.iceAdapter = iceAdapter;
    this.modService = modService;
    this.platformService = platformService;
    this.discordRichPresenceService = discordRichPresenceService;
    this.replayServer = replayServer;
    this.reconnectTimerService = reconnectTimerService;

    faWindowTitle = clientProperties.getForgedAlliance().getWindowTitle();
    gameIdToGame = FXCollections.observableMap(new ConcurrentHashMap<>());
    gameRunning = new SimpleBooleanProperty();
    currentGame = new SimpleObjectProperty<>();
    games = FXCollections.synchronizedObservableList(FXCollections.observableList(new ArrayList<>(),
        item -> new Observable[]{item.statusProperty(), item.teamsProperty()}
    ));
    forgedAlliancePrefs = preferencesService.getPreferences().getForgedAlliance();
    inMatchmakerQueue = false;
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

    fafService.addOnMessageListener(GameInfoMessage.class, this::onGameInfo);
    fafService.addOnMessageListener(LoginMessage.class, message -> onLoggedIn());

    JavaFxUtil.addListener(
        fafService.connectionStateProperty(),
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
  private InvalidationListener generateNumberOfPlayersChangeListener(Game game) {
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
  private ChangeListener<GameStatus> generateGameStatusListener(Game game) {
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

    if (inMatchmakerQueue) {
      addAlreadyInQueueNotification();
      return completedFuture(null);
    }

    return updateGameIfNecessary(newGameInfo.getFeaturedMod(), null, Map.of(), newGameInfo.getSimMods())
        .thenCompose(aVoid -> downloadMapIfNecessary(newGameInfo.getMap()))
        .thenCompose(aVoid -> fafService.requestHostGame(newGameInfo))
        .thenAccept(gameLaunchMessage -> {

          String ratingType = gameLaunchMessage.getRatingType();
          if (ratingType == null) {
            log.warn("Rating type not in gameLaunchMessage using default");
            ratingType = DEFAULT_RATING_TYPE;
          }

          startGame(gameLaunchMessage, gameLaunchMessage.getFaction(), ratingType);
        });
  }

  private void addAlreadyInQueueNotification() {
    notificationService.addImmediateWarnNotification("teammatchmaking.notification.customAlreadyInQueue.message");
  }

  public CompletableFuture<Void> joinGame(Game game, String password) {
    if (isRunning()) {
      log.warn("Game is running, ignoring join request");
      notificationService.addImmediateWarnNotification("game.gameRunning");
      return completedFuture(null);
    }

    if (!preferencesService.isGamePathValid()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      return gameDirectoryFuture.thenCompose(path -> joinGame(game, password));
    }

    if (inMatchmakerQueue) {
      addAlreadyInQueueNotification();
      return completedFuture(null);
    }

    log.info("Joining game: '{}' ({})", game.getTitle(), game.getId());

    Map<String, Integer> featuredModVersions = game.getFeaturedModVersions();
    Set<String> simModUIds = game.getSimMods().keySet();
    return modService.getFeaturedMod(game.getFeaturedMod())
        .thenCompose(featuredModBean -> updateGameIfNecessary(featuredModBean, null, featuredModVersions, simModUIds))
        .thenAccept(aVoid -> {
          try {
            modService.enableSimMods(simModUIds);
          } catch (IOException e) {
            log.warn("SimMods could not be enabled", e);
          }
        })
        .thenCompose(aVoid -> downloadMapIfNecessary(game.getMapFolderName()))
        .thenCompose(aVoid -> fafService.requestJoinGame(game.getId(), password))
        .thenAccept(gameLaunchMessage -> {
          synchronized (currentGame) {
            // Store password in case we rehost
            game.setPassword(password);
            currentGame.set(game);
          }

          String ratingType = gameLaunchMessage.getRatingType();
          if (ratingType == null) {
            log.warn("Rating type not in gameLaunchMessage using game rating type");
            ratingType = game.getRatingType();
          }

          if (ratingType == null) {
            log.warn("Rating type not in game using default");
            ratingType = DEFAULT_RATING_TYPE;
          }

          startGame(gameLaunchMessage, null, ratingType);
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

    onMatchmakerSearchStopped();

    return modService.getFeaturedMod(featuredMod)
        .thenCompose(featuredModBean -> updateGameIfNecessary(featuredModBean, version, modVersions, simMods))
        .thenCompose(aVoid -> downloadMapIfNecessary(mapName).handleAsync((ignoredResult, throwable) -> askWhetherToStartWithOutMap(throwable)))
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
    } else if (inMatchmakerQueue) {
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

  @SneakyThrows
  private Void askWhetherToStartWithOutMap(Throwable throwable) {
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
    if (throwable.getCause() instanceof UnsupportedOperationException) {
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

    onMatchmakerSearchStopped();

    Game game = getByUid(gameId);

    Map<String, Integer> modVersions = game.getFeaturedModVersions();
    Set<String> simModUids = game.getSimMods().keySet();

    return modService.getFeaturedMod(gameType)
        .thenCompose(featuredModBean -> updateGameIfNecessary(featuredModBean, null, modVersions, simModUids))
        .thenCompose(aVoid -> downloadMapIfNecessary(mapName))
        .thenRun(() -> noCatch(() -> {
          Process processCreated = forgedAllianceService.startReplay(replayUrl, gameId, getCurrentPlayer());
          if (forgedAlliancePrefs.isAllowReplaysWhileInGame() && isRunning()) {
            return;
          }
          this.process = processCreated;
          setGameRunning(true);
          spawnTerminationListener(this.process);
        }))
        .exceptionally(throwable -> {
          notifyCantPlayReplay(gameId, throwable);
          return null;
        });
  }

  @NotNull
  private Player getCurrentPlayer() {
    return playerService.getCurrentPlayer();
  }

  public ObservableList<Game> getGames() {
    return games;
  }

  public Game getByUid(int uid) {
    Game game = gameIdToGame.get(uid);
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

    if (inMatchmakerQueue) {
      log.debug("Matchmaker search has already been started, ignoring call");
      return completedFuture(null);
    }

    if (!preferencesService.isGamePathValid()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      return gameDirectoryFuture.thenCompose(path -> startSearchMatchmaker());
    }

    log.info("Matchmaking search has been started");
    inMatchmakerQueue = true;

    return modService.getFeaturedMod(FAF.getTechnicalName())
        .thenAccept(featuredModBean -> updateGameIfNecessary(featuredModBean, null, emptyMap(), emptySet()))
        .thenCompose(aVoid -> fafService.startSearchMatchmaker())
        .thenAccept((gameLaunchMessage) -> downloadMapIfNecessary(gameLaunchMessage.getMapname())
            .thenRun(() -> {
              gameLaunchMessage.setArgs(new ArrayList<>(gameLaunchMessage.getArgs()));

              gameLaunchMessage.getArgs().add("/team " + gameLaunchMessage.getTeam());
              gameLaunchMessage.getArgs().add("/players " + gameLaunchMessage.getExpectedPlayers());
              gameLaunchMessage.getArgs().add("/startspot " + gameLaunchMessage.getMapPosition());

              String ratingType = gameLaunchMessage.getRatingType();

              if (ratingType == null) {
                log.warn("Rating type not in game launch message using default");
                ratingType = DEFAULT_RATING_TYPE;
              }

              startGame(gameLaunchMessage, gameLaunchMessage.getFaction(), ratingType);
            }))
        .exceptionally(throwable -> {
          if (throwable.getCause() instanceof CancellationException) {
            log.info("Matchmaking search has been cancelled");
          } else {
            log.warn("Matchmade game could not be started", throwable);
          }
          return null;
        });
  }

  public void onMatchmakerSearchStopped() {
    if (inMatchmakerQueue) {
      fafService.stopSearchMatchmaker();
      inMatchmakerQueue = false;
      log.debug("Matchmaker search stopped");
    } else {
      log.debug("Matchmaker search has already been stopped, ignoring call");
    }
  }

  /**
   * Returns the preferences the player is currently in. Returns {@code null} if not in a preferences.
   */
  @Nullable
  public Game getCurrentGame() {
    synchronized (currentGame) {
      return currentGame.get();
    }
  }

  private boolean isRunning() {
    return process != null && process.isAlive();
  }

  private CompletableFuture<Void> updateGameIfNecessary(FeaturedMod featuredMod, @Nullable Integer version, @NotNull Map<String, Integer> featuredModVersions, @NotNull Set<String> simModUids) {
    return gameUpdater.update(featuredMod, version, featuredModVersions, simModUids);
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

  /**
   * Actually starts the game, including relay and replay server. Call this method when everything else is prepared
   * (mod/map download, connectivity check etc.)
   */
  private void startGame(GameLaunchMessage gameLaunchMessage, Faction faction, String ratingType) {
    if (isRunning()) {
      log.warn("Forged Alliance is already running, not starting game");
      return;
    }

    int uid = gameLaunchMessage.getUid();
    replayServer.start(uid, () -> getByUid(uid))
        .thenCompose(port -> {
          localReplayPort = port;
          return iceAdapter.start();
        })
        .thenAccept(adapterPort -> {
          List<String> args = fixMalformedArgs(gameLaunchMessage.getArgs());
          process = noCatch(() -> forgedAllianceService.startGame(gameLaunchMessage.getUid(), faction, args, ratingType,
              adapterPort, localReplayPort, rehostRequested, getCurrentPlayer()));
          setGameRunning(true);

          spawnTerminationListener(process);
        })
        .exceptionally(throwable -> {
          log.warn("Game could not be started", throwable);
          notificationService.addImmediateErrorNotification(throwable, "game.start.couldNotStart");
          iceAdapter.stop();
          setGameRunning(false);
          return null;
        });
  }

  private void onRecentlyPlayedGameEnded(Game game) {
    NotificationsPrefs notification = preferencesService.getPreferences().getNotification();
    if (!notification.isAfterGameReviewEnabled() || !notification.isTransientNotificationsEnabled()) {
      return;
    }

    notificationService.addNotification(new PersistentNotification(i18n.get("game.ended", game.getTitle()),
        Severity.INFO,
        singletonList(new Action(i18n.get("game.rate"), actionEvent -> eventBus.post(new ShowReplayEvent(game.getId()))))));
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
    spawnTerminationListener(process, true);
  }

  @VisibleForTesting
  void spawnTerminationListener(Process process, Boolean forOnlineGame) {
    executorService.execute(() -> {
      try {
        rehostRequested = false;
        int exitCode = process.waitFor();
        log.info("Forged Alliance terminated with exit code {}", exitCode);
        if (exitCode != 0) {
          Optional<Path> logFile = preferencesService.getMostRecentGameLogFile();
          notificationService.addImmediateErrorNotification(new RuntimeException(String.format("Forged Alliance Crashed with exit code %d. " +
                  "See %s for more information", exitCode, logFile.map(Path::getFileName).map(Path::toString).orElse(""))),
              "game.crash", logFile.map(Path::toString).orElse(""));
        }

        synchronized (gameRunning) {
          gameRunning.set(false);
          if (forOnlineGame) {
            fafService.notifyGameEnded();
            replayServer.stop();
            iceAdapter.stop();
          }

          if (rehostRequested) {
            rehost();
          }
        }
      } catch (InterruptedException e) {
        log.warn("Error during post-game processing", e);
      }
    });
  }

  private void rehost() {
    synchronized (currentGame) {
      Game game = currentGame.get();

      modService.getFeaturedMod(game.getFeaturedMod())
          .thenAccept(featuredModBean -> hostGame(new NewGameInfo(
              game.getTitle(),
              game.getPassword(),
              featuredModBean,
              game.getMapFolderName(),
              new HashSet<>(game.getSimMods().values()),
              GameVisibility.PUBLIC,
              game.getMinRating(), game.getMaxRating(), game.getEnforceRating())));
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
      fafService.restoreGameSession(currentGame.get().getId());
    }
  }

  private void onGameInfo(GameInfoMessage gameInfoMessage) {
    if (gameInfoMessage.getGames() != null) {
      gameInfoMessage.getGames().forEach(this::onGameInfo);
      return;
    }

    Integer gameId = gameInfoMessage.getUid();
    gameIdToGame.computeIfAbsent(gameId, integer -> {
      Game game = new Game();
      game.setTeamsListener(observable -> game.setAverageRating(calcAverageRating(game)));
      return game;
    });
    Game game = gameIdToGame.get(gameId);
    game.updateFromLobbyServer(gameInfoMessage);
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

  private Game enhanceWithLastPasswordIfPasswordProtected(Game game) {
    if (!game.isPasswordProtected()) {
      return game;
    }
    String lastGamePassword = preferencesService.getPreferences().getLastGame().getLastGamePassword();
    game.setPassword(lastGamePassword);
    return game;
  }

  private double calcAverageRating(Game game) {
    return playerService.getAllPlayersInGame(game).stream()
        .mapToInt(player -> RatingUtil.getLeaderboardRating(player, game.getRatingType()))
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

  public void launchTutorial(MapBean mapVersion, String technicalMapName) {

    if (!preferencesService.isGamePathValid()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      gameDirectoryFuture.thenAccept(path -> launchTutorial(mapVersion, technicalMapName));
      return;
    }

    modService.getFeaturedMod(TUTORIALS.getTechnicalName())
        .thenCompose(featuredModBean -> updateGameIfNecessary(featuredModBean, null, emptyMap(), emptySet()))
        .thenCompose(aVoid -> downloadMapIfNecessary(mapVersion.getFolderName()))
        .thenAccept(aVoid -> {
          List<String> args = Arrays.asList("/map", technicalMapName);
          process = noCatch(() -> forgedAllianceService.startGameOffline(args));
          setGameRunning(true);
          spawnTerminationListener(process, false);
        })
        .exceptionally(throwable -> {
          log.error("Launching tutorials failed", throwable);
          notificationService.addImmediateErrorNotification(throwable, "tutorial.launchFailed");
          return null;
        });

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
