package com.faforever.client.game;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fa.ForgedAllianceService;
import com.faforever.client.fa.RatingMode;
import com.faforever.client.fa.relay.event.RehostRequestEvent;
import com.faforever.client.fa.relay.ice.IceAdapter;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.ModService;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.patch.GameUpdater;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.rankedmatch.MatchmakerMessage;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.GameInfoMessage;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.replay.ExternalReplayInfoGenerator;
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
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
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
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static com.faforever.client.fa.RatingMode.NONE;
import static com.faforever.client.game.KnownFeaturedMod.LADDER_1V1;
import static com.faforever.client.notification.Severity.ERROR;
import static com.github.nocatch.NoCatch.noCatch;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;

@Lazy
@Service
public class GameServiceImpl implements GameService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @VisibleForTesting
  final BooleanProperty gameRunning;
  @VisibleForTesting
  final SimpleObjectProperty<Game> currentGame;

  /**
   * An observable copy of {@link #uidToGameInfoBean}. <strong>Do not modify its content directly</strong>.
   */
  private final ObservableList<Game> games;
  private final ObservableMap<Integer, Game> uidToGameInfoBean;

  private final FafService fafService;
  private final ForgedAllianceService forgedAllianceService;
  private final MapService mapService;
  private final PreferencesService preferencesService;
  private final GameUpdater gameUpdater;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final Executor executor;
  private final PlayerService playerService;
  private final ReportingService reportingService;
  private final EventBus eventBus;
  private final IceAdapter iceAdapter;
  private final ModService modService;
  private final PlatformService platformService;
  private final String faWindowTitle;
  private final ClientProperties clientProperties;
  private final ExternalReplayInfoGenerator externalReplayInfoGenerator;

  //TODO: circular reference
  @Inject
  ReplayService replayService;
  @VisibleForTesting
  RatingMode ratingMode;

  private Process process;
  private BooleanProperty searching1v1;
  private boolean rehostRequested;

  @Inject
  public GameServiceImpl(ClientProperties clientProperties, FafService fafService,
                         ForgedAllianceService forgedAllianceService, MapService mapService,
                         PreferencesService preferencesService, GameUpdater gameUpdater,
                         NotificationService notificationService, I18n i18n, Executor executor,
                         PlayerService playerService, ReportingService reportingService, EventBus eventBus,
                         IceAdapter iceAdapter, ModService modService, PlatformService platformService,
                         ExternalReplayInfoGenerator externalReplayInfoGenerator1) {
    this.clientProperties = clientProperties;
    this.fafService = fafService;
    this.forgedAllianceService = forgedAllianceService;
    this.mapService = mapService;
    this.preferencesService = preferencesService;
    this.gameUpdater = gameUpdater;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.executor = executor;
    this.playerService = playerService;
    this.reportingService = reportingService;
    this.eventBus = eventBus;
    this.iceAdapter = iceAdapter;
    this.modService = modService;
    this.platformService = platformService;

    faWindowTitle = clientProperties.getForgedAlliance().getWindowTitle();
    this.externalReplayInfoGenerator = externalReplayInfoGenerator1;
    uidToGameInfoBean = FXCollections.observableHashMap();
    searching1v1 = new SimpleBooleanProperty();
    gameRunning = new SimpleBooleanProperty();

    currentGame = new SimpleObjectProperty<>();
    currentGame.addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        return;
      }

      newValue.statusProperty().addListener(new WeakChangeListener<>((observable1, oldValue1, newValue1) -> {
        if (newValue1 == GameStatus.CLOSED) {
          onCurrentGameEnded(currentGame);
        }
      }));
    });

    games = FXCollections.observableList(new ArrayList<>(),
        item -> new Observable[]{item.statusProperty(), item.getTeams()}
    );
    games.addListener((ListChangeListener<Game>) change -> {
      while (change.next()) {
        change.getRemoved().forEach(game -> eventBus.post(new GameRemovedEvent(game)));

        if (change.wasUpdated()) {
          for (int i = change.getFrom(); i < change.getTo(); i++) {
            eventBus.post(new GameUpdatedEvent(change.getList().get(i)));
          }
        }

        change.getAddedSubList().forEach(game -> eventBus.post(new GameAddedEvent(game)));
      }
    });
    JavaFxUtil.attachListToMap(games, uidToGameInfoBean);
  }

  @Override
  public ReadOnlyBooleanProperty gameRunningProperty() {
    return gameRunning;
  }

  @Override
  public CompletableFuture<Void> hostGame(NewGameInfo newGameInfo) {
    if (isRunning()) {
      logger.debug("Game is running, ignoring host request");
      return completedFuture(null);
    }

    stopSearchLadder1v1();

    return updateGameIfNecessary(newGameInfo.getFeaturedMod(), null, emptyMap(), newGameInfo.getSimMods())
        .thenCompose(aVoid -> downloadMapIfNecessary(newGameInfo.getMap()))
        .thenCompose(aVoid -> fafService.requestHostGame(newGameInfo))
        .thenAccept(gameLaunchMessage -> startGame(gameLaunchMessage, null, RatingMode.GLOBAL));
  }

  @Override
  public CompletableFuture<Void> joinGame(Game game, String password) {
    if (isRunning()) {
      logger.debug("Game is running, ignoring join request");
      return completedFuture(null);
    }

    logger.info("Joining game: '{}' ({})", game.getTitle(), game.getId());

    stopSearchLadder1v1();

    Map<String, Integer> featuredModVersions = game.getFeaturedModVersions();
    Set<String> simModUIds = game.getSimMods().keySet();

    return modService.getFeaturedMod(game.getFeaturedMod())
        .thenCompose(featuredModBean -> updateGameIfNecessary(featuredModBean, null, featuredModVersions, simModUIds))
        .thenAccept(aVoid -> {
          try {
            modService.enableSimMods(simModUIds);
          } catch (IOException e) {
            logger.warn("SimMods were not enabled", e);
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
        });
  }

  private CompletableFuture<Void> downloadMapIfNecessary(String mapFolderName) {
    if (mapService.isInstalled(mapFolderName)) {
      return completedFuture(null);
    }
    return mapService.download(mapFolderName);
  }

  @Override
  public void runWithReplay(Path path, @Nullable Integer replayId, String featuredMod, Integer version, Map<String, Integer> modVersions, Set<String> simMods, String mapName) {
    if (isRunning()) {
      logger.warn("Forged Alliance is already running, not starting replay");
      return;
    }
    modService.getFeaturedMod(featuredMod)
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
    logger.error("Could not play replay '" + replayId + "'", throwable);
    notificationService.addNotification(new ImmediateNotification(
        i18n.get("errorTitle"),
        i18n.get("replayCouldNotBeStarted", replayId),
        ERROR, throwable,
        singletonList(new Action(i18n.get("report"))))
    );
  }

  @Override
  public CompletableFuture<Void> runWithLiveReplay(URI replayUrl, Integer gameId, String gameType, String mapName) {
    if (isRunning()) {
      logger.warn("Forged Alliance is already running, not starting live replay");
      return completedFuture(null);
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

  @Override
  public ObservableList<Game> getGames() {
    return games;
  }

  @Override
  public Game getByUid(int uid) {
    Game game = uidToGameInfoBean.get(uid);
    if (game == null) {
      logger.warn("Can't find {} in gameInfoBean map", uid);
    }
    return game;
  }

  @Override
  public void addOnRankedMatchNotificationListener(Consumer<MatchmakerMessage> listener) {
    fafService.addOnMessageListener(MatchmakerMessage.class, listener);
  }

  @Override
  public CompletableFuture<Void> startSearchLadder1v1(Faction faction) {
    if (isRunning()) {
      logger.debug("Game is running, ignoring 1v1 search request");
      return completedFuture(null);
    }

    searching1v1.set(true);

    int port = preferencesService.getPreferences().getForgedAlliance().getPort();

    return modService.getFeaturedMod(LADDER_1V1.getTechnicalName())
        .thenAccept(featuredModBean -> updateGameIfNecessary(featuredModBean, null, emptyMap(), emptySet()))
        .thenCompose(aVoid -> fafService.startSearchLadder1v1(faction, port))
        .thenAccept((gameLaunchMessage) -> downloadMapIfNecessary(gameLaunchMessage.getMapname())
            .thenRun(() -> {
              // TODO this should be sent by the server!
              gameLaunchMessage.setArgs(new ArrayList<>(gameLaunchMessage.getArgs()));
              gameLaunchMessage.getArgs().add("/team 1");
              gameLaunchMessage.getArgs().add("/players 2");

              startGame(gameLaunchMessage, faction, RatingMode.LADDER_1V1);
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
  public void stopSearchLadder1v1() {
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
   * Actually starts the game, including relay and replay server. Call this method when everything else is
   * prepared (mod/map download, connectivity check etc.)
   */
  private void startGame(GameLaunchMessage gameLaunchMessage, Faction faction, RatingMode ratingMode) {
    if (isRunning()) {
      logger.warn("Forged Alliance is already running, not starting game");
      return;
    }

    stopSearchLadder1v1();
    replayService.startReplayServer(gameLaunchMessage.getUid())
        .thenCompose(aVoid -> iceAdapter.start())
        .thenAccept(adapterPort -> {
          List<String> args = fixMalformedArgs(gameLaunchMessage.getArgs());
          process = noCatch(() -> forgedAllianceService.startGame(gameLaunchMessage.getUid(), faction, args, ratingMode,
              adapterPort, clientProperties.getReplay().getLocalServerPort(), rehostRequested, getCurrentPlayer()));
          setGameRunning(true);

          this.ratingMode = ratingMode;
          spawnTerminationListener(process);
        })
        .exceptionally(throwable -> {
          logger.warn("Game could not be started", throwable);
          notificationService.addNotification(
              new ImmediateNotification(i18n.get("errorTitle"),
                  i18n.get("game.start.couldNotStart"), ERROR, throwable, Arrays.asList(
                  new ReportAction(i18n, reportingService, throwable), new DismissAction(i18n)))
          );
          setGameRunning(false);
          return null;
        });
  }

  private void onCurrentGameEnded(SimpleObjectProperty<Game> game) {
    notificationService.addNotification(new PersistentNotification(i18n.get("game.ended", game.get().getTitle()),
        Severity.INFO,
        singletonList(new Action(i18n.get("game.rate"), actionEvent -> {
          replayService.findById(game.get().getId())
              .thenAccept(replay -> externalReplayInfoGenerator.showExternalReplayInfo(replay, String.valueOf(game.get().getId())));
        }))));
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
    executor.execute(() -> {
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
    });
  }

  private void rehost() {
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

    final Game game;
    Integer gameId = gameInfoMessage.getUid();
    Player currentPlayer = playerService.getCurrentPlayer().orElseThrow(() -> new IllegalStateException("Player has not been set"));
    if (!uidToGameInfoBean.containsKey(gameId)) {
      game = new Game(gameInfoMessage);
      uidToGameInfoBean.put(gameId, game);
    } else {
      game = uidToGameInfoBean.get(gameId);
      Platform.runLater(() -> game.updateFromGameInfo(gameInfoMessage));

      if (GameStatus.CLOSED == gameInfoMessage.getState()) {
        if (currentPlayer.getGame() == game) {
          // Don't remove the game until the current player closed it
          currentPlayer.gameProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null && oldValue.getStatus() == GameStatus.CLOSED) {
              removeGame(gameInfoMessage);
            }
          });
        } else {
          removeGame(gameInfoMessage);
        }
        return;
      }
    }

    boolean currentPlayerInGame = gameInfoMessage.getTeams().values().stream()
        .anyMatch(team -> team.contains(currentPlayer.getUsername()));

    if (currentPlayerInGame && GameStatus.OPEN == gameInfoMessage.getState()) {
      synchronized (currentGame) {
        currentGame.set(game);
      }
    }

    game.statusProperty().addListener((observable, oldValue, newValue) -> {
      if (oldValue == GameStatus.OPEN
          && newValue == GameStatus.PLAYING
          && game.getTeams().values().stream().anyMatch(team -> team.contains(currentPlayer.getUsername()))
          && !platformService.isWindowFocused(faWindowTitle)) {
        platformService.focusWindow(faWindowTitle);
      }
    });
  }

  private void removeGame(GameInfoMessage gameInfoMessage) {
    Platform.runLater(() -> {
      // This needs to run in the application thread since otherwise code triggered by the above updateFromGameInfo
      // operation, which runs in the application thread, too, may try to access the removed element.
      synchronized (uidToGameInfoBean) {
        uidToGameInfoBean.remove(gameInfoMessage.getUid());
      }
    });
  }
}
