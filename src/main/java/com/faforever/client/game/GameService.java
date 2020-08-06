package com.faforever.client.game;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.discord.DiscordRichPresenceService;
import com.faforever.client.fa.CloseGameEvent;
import com.faforever.client.fa.ForgedAllianceService;
import com.faforever.client.fa.RatingMode;
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
import com.faforever.client.notification.ImmediateErrorNotification;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.patch.GameUpdater;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.rankedmatch.MatchmakerInfoMessage;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.ReconnectTimerService;
import com.faforever.client.remote.domain.GameInfoMessage;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.replay.ReplayServer;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.faforever.client.util.RatingUtil;
import com.faforever.client.util.TimeUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
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
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.faforever.client.fa.RatingMode.NONE;
import static com.faforever.client.game.KnownFeaturedMod.LADDER_1V1;
import static com.faforever.client.game.KnownFeaturedMod.TUTORIALS;
import static com.github.nocatch.NoCatch.noCatch;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Downloads necessary maps, mods and updates before starting
 */
@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class GameService implements InitializingBean {

  private static final String RATING_NUMBER = "\\d+(?:\\.\\d+)?k?";
  private static final Pattern MIN_RATING_PATTERN = Pattern.compile(">\\s*(" + RATING_NUMBER + ")|(" + RATING_NUMBER + ")\\s*\\+");
  private static final Pattern MAX_RATING_PATTERN = Pattern.compile("<\\s*(" + RATING_NUMBER + ")");
  private static final Pattern ABOUT_RATING_PATTERN = Pattern.compile("~\\s*(" + RATING_NUMBER + ")");
  private static final Pattern BETWEEN_RATING_PATTERN = Pattern.compile("(" + RATING_NUMBER + ")\\s*-\\s*(" + RATING_NUMBER + ")");

  @VisibleForTesting
  final BooleanProperty gameRunning;

  /** TODO: Explain why access needs to be synchronized. */
  @VisibleForTesting
  final SimpleObjectProperty<Game> currentGame;

  /**
   * An observable copy of {@link #uidToGameInfoBean}. <strong>Do not modify its content directly</strong>.
   */
  private final ObservableMap<Integer, Game> uidToGameInfoBean;

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

  @VisibleForTesting
  RatingMode ratingMode;

  private final ObservableList<Game> games;
  private final String faWindowTitle;
  private final BooleanProperty searching1v1;

  private Process process;
  private boolean rehostRequested;
  private int localReplayPort;

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
    uidToGameInfoBean = FXCollections.observableMap(new ConcurrentHashMap<>());
    searching1v1 = new SimpleBooleanProperty();
    gameRunning = new SimpleBooleanProperty();
    currentGame = new SimpleObjectProperty<>();
    games = FXCollections.observableList(new ArrayList<>(),
        item -> new Observable[]{item.statusProperty(), item.getTeams()}
    );
  }

  @Override
  public void afterPropertiesSet() {
    currentGame.addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        discordRichPresenceService.clearGameInfo();
        return;
      }

      InvalidationListener listener = generateNumberOfPlayersChangeListener(newValue);
      JavaFxUtil.addListener(newValue.numPlayersProperty(), listener);
      listener.invalidated(newValue.numPlayersProperty());

      ChangeListener<GameStatus> statusChangeListener = generateGameStatusListener(newValue);
      JavaFxUtil.addListener(newValue.statusProperty(), statusChangeListener);
      statusChangeListener.changed(newValue.statusProperty(), newValue.getStatus(), newValue.getStatus());
    });

    JavaFxUtil.attachListToMap(games, uidToGameInfoBean);
    JavaFxUtil.addListener(
        gameRunning,
        (observable, oldValue, newValue) -> reconnectTimerService.setGameRunning(newValue)
    );

    eventBus.register(this);

    fafService.addOnMessageListener(GameInfoMessage.class, message -> Platform.runLater(() -> onGameInfo(message)));
    fafService.addOnMessageListener(LoginMessage.class, message -> onLoggedIn());

    JavaFxUtil.addListener(
        fafService.connectionStateProperty(),
        (observable, oldValue, newValue) -> {
          if (newValue == ConnectionState.DISCONNECTED) {
            synchronized (uidToGameInfoBean) {
              uidToGameInfoBean.clear();
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
        final Player currentPlayer = playerService.getCurrentPlayer().orElseThrow(() -> new IllegalStateException("Player must be set"));
        discordRichPresenceService.updatePlayedGameTo(currentGame.get(), currentPlayer.getId(), currentPlayer.getUsername());
      }
    };
  }

  @NotNull
  private ChangeListener<GameStatus> generateGameStatusListener(Game game) {
    return new ChangeListener<>() {
      @Override
      public void changed(ObservableValue<? extends GameStatus> observable, GameStatus oldStatus, GameStatus newStatus) {
        if (observable.getValue() == GameStatus.CLOSED) {
          observable.removeListener(this);
        }

        Player currentPlayer = getCurrentPlayer();
        boolean playerStillInGame = game.getTeams().entrySet().stream()
            .flatMap(stringListEntry -> stringListEntry.getValue().stream())
            .anyMatch(playerName -> playerName.equals(currentPlayer.getUsername()));

        /*
          Check if player left the game while it was open, in this case we don't care any longer
         */
        if (newStatus == GameStatus.PLAYING && oldStatus == GameStatus.OPEN && !playerStillInGame) {
          observable.removeListener(this);
          return;
        }

        if (oldStatus == GameStatus.PLAYING && newStatus == GameStatus.CLOSED) {
          GameService.this.onRecentlyPlayedGameEnded(game);
          return;
        }

        if (Objects.equals(currentGame.get(), game)) {
          discordRichPresenceService.updatePlayedGameTo(currentGame.get(), currentPlayer.getId(), currentPlayer.getUsername());
        }
      }
    };
  }

  public ReadOnlyBooleanProperty gameRunningProperty() {
    return gameRunning;
  }

  public CompletableFuture<Void> hostGame(NewGameInfo newGameInfo) {
    if (isRunning()) {
      log.debug("Game is running, ignoring host request");
      return completedFuture(null);
    }

    if (!preferencesService.isGamePathValid()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      return gameDirectoryFuture.thenCompose(path -> hostGame(newGameInfo));
    }

    stopSearchLadder1v1();

    return updateGameIfNecessary(newGameInfo.getFeaturedMod(), null, emptyMap(), newGameInfo.getSimMods())
        .thenCompose(aVoid -> downloadMapIfNecessary(newGameInfo.getMap()))
        .thenCompose(aVoid -> fafService.requestHostGame(newGameInfo))
        .thenAccept(gameLaunchMessage -> startGame(gameLaunchMessage, gameLaunchMessage.getFaction(), RatingMode.GLOBAL));
  }

  public CompletableFuture<Void> joinGame(Game game, String password) {
    if (isRunning()) {
      log.debug("Game is running, ignoring join request");
      return completedFuture(null);
    }

    if (!preferencesService.isGamePathValid()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      return gameDirectoryFuture.thenCompose(path -> joinGame(game, password));
    }

    log.info("Joining game: '{}' ({})", game.getTitle(), game.getId());

    stopSearchLadder1v1();

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
          startGame(gameLaunchMessage, null, RatingMode.GLOBAL);
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
  public void runWithReplay(Path path, @Nullable Integer replayId, String featuredMod, Integer version, Map<String, Integer> modVersions, Set<String> simMods, String mapName) {
    if (isRunning()) {
      log.warn("Forged Alliance is already running, not starting replay");
      return;
    }

    if (!preferencesService.isGamePathValid()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      gameDirectoryFuture.thenAccept(pathSet -> runWithReplay(path, replayId, featuredMod, version, modVersions, simMods, mapName));
      return;
    }

    modService.getFeaturedMod(featuredMod)
        .thenCompose(featuredModBean -> updateGameIfNecessary(featuredModBean, version, modVersions, simMods))
        .thenCompose(aVoid -> downloadMapIfNecessary(mapName).handleAsync((ignoredResult, throwable) -> askWhetherToStartWithOutMap(throwable)))
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

  @NotNull
  private CompletableFuture<Path> postGameDirectoryChooseEvent() {
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
    log.error("Could not play replay '" + replayId + "'", throwable);
    notificationService.addNotification(new ImmediateErrorNotification(
        i18n.get("errorTitle"),
        i18n.get("replayCouldNotBeStarted", replayId),
        throwable,
        i18n, reportingService
    ));
  }

  public CompletableFuture<Void> runWithLiveReplay(URI replayUrl, Integer gameId, String gameType, String mapName) {
    if (isRunning()) {
      log.warn("Forged Alliance is already running, not starting live replay");
      return completedFuture(null);
    }

    if (!preferencesService.isGamePathValid()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      return gameDirectoryFuture.thenCompose(path -> runWithLiveReplay(replayUrl, gameId, gameType, mapName));
    }

    Game gameBean = getByUid(gameId);

    Map<String, Integer> modVersions = gameBean.getFeaturedModVersions();
    Set<String> simModUids = gameBean.getSimMods().keySet();

    return modService.getFeaturedMod(gameType)
        .thenCompose(featuredModBean -> updateGameIfNecessary(featuredModBean, null, modVersions, simModUids))
        .thenCompose(aVoid -> downloadMapIfNecessary(mapName))
        .thenRun(() -> noCatch(() -> {
          process = forgedAllianceService.startReplay(replayUrl, gameId, getCurrentPlayer());
          setGameRunning(true);
          this.ratingMode = NONE;
          spawnTerminationListener(process);
        }));
  }

  private Player getCurrentPlayer() {
    return playerService.getCurrentPlayer().orElseThrow(() -> new IllegalStateException("Player has not been set"));
  }

  public ObservableList<Game> getGames() {
    return games;
  }

  public Game getByUid(int uid) {
    Game game = uidToGameInfoBean.get(uid);
    if (game == null) {
      log.warn("Can't find {} in gameInfoBean map", uid);
    }
    return game;
  }

  public void addOnRankedMatchNotificationListener(Consumer<MatchmakerInfoMessage> listener) {
    fafService.addOnMessageListener(MatchmakerInfoMessage.class, listener);
  }

  public CompletableFuture<Void> startSearchLadder1v1(Faction faction) {
    if (isRunning()) {
      log.debug("Game is running, ignoring 1v1 search request");
      return completedFuture(null);
    }

    if (!preferencesService.isGamePathValid()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      return gameDirectoryFuture.thenCompose(path -> startSearchLadder1v1(faction));
    }

    searching1v1.set(true);

    return modService.getFeaturedMod(LADDER_1V1.getTechnicalName())
        .thenAccept(featuredModBean -> updateGameIfNecessary(featuredModBean, null, emptyMap(), emptySet()))
        .thenCompose(aVoid -> fafService.startSearchLadder1v1(faction))
        .thenAccept((gameLaunchMessage) -> downloadMapIfNecessary(gameLaunchMessage.getMapname())
            .thenRun(() -> {
              gameLaunchMessage.setArgs(new ArrayList<>(gameLaunchMessage.getArgs()));

              gameLaunchMessage.getArgs().add("/team " + gameLaunchMessage.getTeam());
              gameLaunchMessage.getArgs().add("/players " + gameLaunchMessage.getExpectedPlayers());
              gameLaunchMessage.getArgs().add("/startspot " + gameLaunchMessage.getMapPosition());

              startGame(gameLaunchMessage, faction, RatingMode.LADDER_1V1);
            }))
        .exceptionally(throwable -> {
          if (throwable instanceof CancellationException) {
            log.info("Ranked1v1 search has been cancelled");
          } else {
            log.warn("Ranked1v1 could not be started", throwable);
          }
          return null;
        });
  }

  public void stopSearchLadder1v1() {
    if (searching1v1.get()) {
      fafService.stopSearchingRanked();
      searching1v1.set(false);
    }
  }

  public BooleanProperty searching1v1Property() {
    return searching1v1;
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
  private void startGame(GameLaunchMessage gameLaunchMessage, Faction faction, RatingMode ratingMode) {
    if (isRunning()) {
      log.warn("Forged Alliance is already running, not starting game");
      return;
    }

    stopSearchLadder1v1();
    int uid = gameLaunchMessage.getUid();
    replayServer.start(uid, () -> getByUid(uid))
        .thenCompose(port -> {
          localReplayPort = port;
          return iceAdapter.start();
        })
        .thenAccept(adapterPort -> {
          List<String> args = fixMalformedArgs(gameLaunchMessage.getArgs());
          process = noCatch(() -> forgedAllianceService.startGame(gameLaunchMessage.getUid(), faction, args, ratingMode,
              adapterPort, localReplayPort, rehostRequested, getCurrentPlayer()));
          setGameRunning(true);

          this.ratingMode = ratingMode;
          spawnTerminationListener(process);
        })
        .exceptionally(throwable -> {
          log.warn("Game could not be started", throwable);
          notificationService.addNotification(
              new ImmediateErrorNotification(i18n.get("errorTitle"), i18n.get("game.start.couldNotStart"), throwable, i18n, reportingService)
          );
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
              new HashSet<>(game.getSimMods().values())
          )));
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
    // Since all game updates are usually reflected on the UI and to prevent deadlocks
    JavaFxUtil.assertApplicationThread();

    if (gameInfoMessage.getGames() != null) {
      gameInfoMessage.getGames().forEach(this::onGameInfo);
      return;
    }

    // We may receive game info before we receive our player info
    Optional<Player> currentPlayerOptional = playerService.getCurrentPlayer();

    Game game = createOrUpdateGame(gameInfoMessage);
    if (GameStatus.CLOSED == game.getStatus()) {
      removeGame(gameInfoMessage);
      if (!currentPlayerOptional.isPresent() || !Objects.equals(currentGame.get(), game)) {
        return;
      }
      synchronized (currentGame) {
        currentGame.set(null);
      }

    }

    if (currentPlayerOptional.isPresent()) {
      // TODO the following can be removed as soon as the server tells us which game a player is in.
      boolean currentPlayerInGame = gameInfoMessage.getTeams().values().stream()
          .anyMatch(team -> team.contains(currentPlayerOptional.get().getUsername()));

      if (currentPlayerInGame && GameStatus.OPEN == gameInfoMessage.getState()) {
        synchronized (currentGame) {
          currentGame.set(game);
        }
      } else if (Objects.equals(currentGame.get(), game) && !currentPlayerInGame) {
        synchronized (currentGame) {
          currentGame.set(null);
        }
      }
    }


    JavaFxUtil.addListener(game.statusProperty(), (observable, oldValue, newValue) -> {
      if (oldValue == GameStatus.OPEN
          && newValue == GameStatus.PLAYING
          && game.getTeams().values().stream().anyMatch(team -> playerService.getCurrentPlayer().isPresent() && team.contains(playerService.getCurrentPlayer().get().getUsername()))
          && !platformService.isWindowFocused(faWindowTitle)) {
        platformService.focusWindow(faWindowTitle);
      }
    });
  }

  private Game createOrUpdateGame(GameInfoMessage gameInfoMessage) {
    Integer gameId = gameInfoMessage.getUid();
    final Game game;
    synchronized (uidToGameInfoBean) {
      if (!uidToGameInfoBean.containsKey(gameId)) {
        game = new Game();
        uidToGameInfoBean.put(gameId, game);
        updateFromGameInfo(gameInfoMessage, game);
        eventBus.post(new GameAddedEvent(game));
      } else {
        game = uidToGameInfoBean.get(gameId);

        /* Since this method synchronizes on and updates members of "game", deadlocks can happen easily (updates can
         fire events on the event bus, and each event subscriber is synchronized as well). By ensuring that we run all
         updates in the application thread, we eliminate this risk. This is not required during construction of the
         game however, since members are not yet accessible from outside. */
        JavaFxUtil.assertApplicationThread();

        updateFromGameInfo(gameInfoMessage, game);
        eventBus.post(new GameUpdatedEvent(game));
      }
    }
    return game;
  }

  public int parseRating(String string) {
    try {
      return Integer.parseInt(string);
    } catch (NumberFormatException e) {
      int rating;
      String[] split = string.replace("k", "").split("\\.");
      try {
        rating = Integer.parseInt(split[0]) * 1000;
        if (split.length == 2) {
          rating += Integer.parseInt(split[1]) * 100;
        }
        return rating;
      } catch (NumberFormatException e1) {
        return Integer.MAX_VALUE;
      }
    }
  }

  private double calcAverageRating(GameInfoMessage gameInfoMessage) {
    return gameInfoMessage.getTeams().values().stream()
        .flatMap(Collection::stream)
        .map(playerService::getPlayerForUsername)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .mapToInt(RatingUtil::getGlobalRating)
        .average()
        .orElse(0.0);
  }

  private void updateFromGameInfo(GameInfoMessage gameInfoMessage, Game game) {
    game.setId(gameInfoMessage.getUid());
    game.setHost(gameInfoMessage.getHost());
    game.setTitle(StringEscapeUtils.unescapeHtml4(gameInfoMessage.getTitle()));
    game.setMapFolderName(gameInfoMessage.getMapname());
    game.setFeaturedMod(gameInfoMessage.getFeaturedMod());
    game.setNumPlayers(gameInfoMessage.getNumPlayers());
    game.setMaxPlayers(gameInfoMessage.getMaxPlayers());
    Optional.ofNullable(gameInfoMessage.getLaunchedAt()).ifPresent(aDouble -> game.setStartTime(
        TimeUtil.fromPythonTime(aDouble.longValue()).toInstant()
    ));
    game.setStatus(gameInfoMessage.getState());
    game.setPasswordProtected(gameInfoMessage.getPasswordProtected());

    game.setAverageRating(calcAverageRating(gameInfoMessage));

    synchronized (game.getSimMods()) {
      game.getSimMods().clear();
      if (gameInfoMessage.getSimMods() != null) {
        game.getSimMods().putAll(gameInfoMessage.getSimMods());
      }
    }

    synchronized (game.getTeams()) {
      game.getTeams().clear();
      if (gameInfoMessage.getTeams() != null) {
        game.getTeams().putAll(gameInfoMessage.getTeams());
      }
    }

    // TODO this can be removed as soon as we valueOf server side support. Until then, let's be hacky
    String titleString = game.getTitle();
    Matcher matcher = BETWEEN_RATING_PATTERN.matcher(titleString);
    if (matcher.find()) {
      game.setMinRating(parseRating(matcher.group(1)));
      game.setMaxRating(parseRating(matcher.group(2)));
    } else {
      matcher = MIN_RATING_PATTERN.matcher(titleString);
      if (matcher.find()) {
        if (matcher.group(1) != null) {
          game.setMinRating(parseRating(matcher.group(1)));
        }
        if (matcher.group(2) != null) {
          game.setMinRating(parseRating(matcher.group(2)));
        }
        game.setMaxRating(3000);
      } else {
        matcher = MAX_RATING_PATTERN.matcher(titleString);
        if (matcher.find()) {
          game.setMinRating(0);
          game.setMaxRating(parseRating(matcher.group(1)));
        } else {
          matcher = ABOUT_RATING_PATTERN.matcher(titleString);
          if (matcher.find()) {
            int rating = parseRating(matcher.group(1));
            game.setMinRating(rating - 300);
            game.setMaxRating(rating + 300);
          }
        }
      }
    }
  }


  private void removeGame(GameInfoMessage gameInfoMessage) {
    Game game;
    synchronized (uidToGameInfoBean) {
      game = uidToGameInfoBean.remove(gameInfoMessage.getUid());
    }
    eventBus.post(new GameRemovedEvent(game));
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
          notificationService.addImmediateErrorNotification(throwable, "tutorial.launchFailed");
          log.error("Launching tutorials failed", throwable);
          return null;
        });

  }
}
