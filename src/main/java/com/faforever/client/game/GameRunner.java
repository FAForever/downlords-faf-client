package com.faforever.client.game;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.api.Division;
import com.faforever.client.domain.api.LeagueEntry;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.exception.NotifiableException;
import com.faforever.client.fa.ForgedAllianceLaunchService;
import com.faforever.client.fa.GameParameters;
import com.faforever.client.fa.GameParameters.League;
import com.faforever.client.fa.relay.ice.CoturnService;
import com.faforever.client.fa.relay.ice.IceAdapter;
import com.faforever.client.featuredmod.FeaturedModService;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.PlatformService;
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
import com.faforever.client.notification.ServerNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.replay.ReplayServer;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.MaskPatternLayout;
import com.faforever.client.util.RatingUtil;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.NoticeInfo;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Nullable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.faforever.client.game.KnownFeaturedMod.FAF;
import static com.faforever.client.game.KnownFeaturedMod.TUTORIALS;
import static com.faforever.client.notification.Severity.WARN;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Downloads necessary maps, mods and updates before starting
 */
@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class GameRunner implements InitializingBean {

  private final FafServerAccessor fafServerAccessor;
  private final ForgedAllianceLaunchService forgedAllianceLaunchService;
  private final CoturnService coturnService;
  private final MapService mapService;
  private final PreferencesService preferencesService;
  private final LoggingService loggingService;
  private final LeaderboardService leaderboardService;
  private final NotificationService notificationService;
  private final UiService uiService;
  private final I18n i18n;
  private final PlayerService playerService;
  private final IceAdapter iceAdapter;
  private final ModService modService;
  private final FeaturedModService featuredModService;
  private final PlatformService platformService;
  private final GameService gameService;
  private final ReplayServer replayServer;
  private final OperatingSystem operatingSystem;
  private final ClientProperties clientProperties;
  private final GameMapper gameMapper;
  private final GamePathHandler gamePathHandler;
  private final NavigationHandler navigationHandler;
  private final NotificationPrefs notificationPrefs;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final MaskPatternLayout logMasker = new MaskPatternLayout();
  private final SimpleObjectProperty<Integer> runningGameId = new SimpleObjectProperty<>();
  private final ObjectProperty<Process> process = new SimpleObjectProperty<>();

  private final ReadOnlyObjectWrapper<GameInfo> runningGame = new ReadOnlyObjectWrapper<>();
  private final ReadOnlyBooleanWrapper running = new ReadOnlyBooleanWrapper();
  private final ReadOnlyObjectWrapper<Long> pid = new ReadOnlyObjectWrapper<>();

  private CompletableFuture<Void> matchmakerFuture;
  private boolean gameKilled;

  @Override
  public void afterPropertiesSet() {
    runningGame.bind(runningGameId.flatMap(gameService::observeByUid));

    running.bind(process.flatMap(process -> {
      BooleanProperty isAlive = new SimpleBooleanProperty(process.isAlive());
      process.onExit().thenRunAsync(() -> isAlive.set(false), fxApplicationThreadExecutor);
      return isAlive;
    }));

    pid.bind(process.flatMap(process -> {
      ObjectProperty<Long> pid = new SimpleObjectProperty<>(process.isAlive() ? process.pid() : null);
      process.onExit().thenRunAsync(() -> pid.set(null), fxApplicationThreadExecutor);
      return pid;
    }));

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
      if (isRunning() && newValue == ConnectionState.CONNECTED && oldValue != ConnectionState.CONNECTED) {
        fafServerAccessor.restoreGameSession(runningGameId.get());
      }
    });
  }

  public ReadOnlyBooleanProperty runningProperty() {
    return running.getReadOnlyProperty();
  }

  @VisibleForTesting
  CompletableFuture<Void> startOnlineGame(GameLaunchResponse gameLaunchResponse) {
    int uid = gameLaunchResponse.getUid();
    String leaderboard = gameLaunchResponse.getLeaderboard();
    boolean hasLeague = leaderboard == null || "global".equals(leaderboard);

    String mapFolderName = gameLaunchResponse.getMapName();
    CompletableFuture<Void> downloadMapFuture = mapFolderName == null ? completedFuture(
        null) : mapService.downloadIfNecessary(mapFolderName).toFuture();
    CompletableFuture<League> leagueFuture = hasLeague ? completedFuture(null) : getDivisionInfo(
        leaderboard).toFuture();
    CompletableFuture<Integer> startReplayServerFuture = replayServer.start(uid);
    CompletableFuture<Integer> startIceAdapterFuture = startIceAdapter(uid);

    return CompletableFuture.allOf(downloadMapFuture, leagueFuture, startIceAdapterFuture, startReplayServerFuture)
                            .thenApply(ignored -> gameMapper.map(gameLaunchResponse, leagueFuture.join()))
                            .thenApply(parameters -> launchOnlineGame(parameters, startIceAdapterFuture.join(),
                                                                      startReplayServerFuture.join()))
                            .whenCompleteAsync((process, throwable) -> {
                              if (process != null) {
                                this.process.set(process);
                                runningGameId.set(uid);
                              }
                            }, fxApplicationThreadExecutor)
                            .thenCompose(Process::onExit)
                            .thenAccept(this::handleTermination)
                            .whenComplete((ignored, throwable) -> {
                              iceAdapter.stop();
                              replayServer.stop();
                              fafServerAccessor.notifyGameEnded();
                            })
                            .whenCompleteAsync((ignored, throwable) -> {
                              process.set(null);
                              runningGameId.set(null);
                            }, fxApplicationThreadExecutor);
  }

  @VisibleForTesting
  CompletableFuture<Void> prepareAndLaunchGameWhenReady(String featuredModName, Set<String> simModUids,
                                                                @Nullable String mapFolderName,
                                                                Supplier<CompletableFuture<GameLaunchResponse>> gameLaunchSupplier) {
    CompletableFuture<Void> updateFeaturedModFuture = featuredModService.updateFeaturedModToLatest(featuredModName,
                                                                                                   false);

    CompletableFuture<Void> installSimModsFuture = simModUids.isEmpty() ? completedFuture(
        null) : modService.downloadAndEnableMods(simModUids).toFuture();
    CompletableFuture<Void> downloadMapFuture = mapFolderName == null || mapFolderName.isBlank() ? completedFuture(
        null) : mapService.downloadIfNecessary(mapFolderName).toFuture();
    return CompletableFuture.allOf(updateFeaturedModFuture, installSimModsFuture, downloadMapFuture)
                            .thenCompose(ignored -> gameLaunchSupplier.get())
                            .thenCompose(this::startOnlineGame);
  }

  public void host(NewGameInfo newGameInfo) {
    if (isRunning()) {
      log.info("Game is running, ignoring host request");
      notificationService.addImmediateWarnNotification("game.gameRunning");
      return;
    }

    if (!preferencesService.hasValidGamePath()) {
      gamePathHandler.chooseAndValidateGameDirectory().thenRun(() -> host(newGameInfo));
      return;
    }

    if (waitingForMatchMakerGame()) {
      notificationService.addImmediateWarnNotification("teammatchmaking.notification.customAlreadyInQueue.message");
      return;
    }

    prepareAndLaunchGameWhenReady(newGameInfo.featuredModName(), newGameInfo.simMods(), newGameInfo.map(),
                                  () -> fafServerAccessor.requestHostGame(newGameInfo)).exceptionally(throwable -> {
      throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
      log.error("Game could not be hosted", throwable);
      if (throwable instanceof NotifiableException notifiableException) {
        notificationService.addErrorNotification(notifiableException);
      } else {
        notificationService.addImmediateErrorNotification(throwable, "game.create.failed");
      }
      return null;
    });
  }

  public void join(GameInfo gameInfo) {
    join(gameInfo, null, false);
  }

  private void join(GameInfo game, String password, boolean ignoreRating) {
    if (isRunning()) {
      log.info("Game is running, ignoring join request");
      notificationService.addImmediateWarnNotification("game.gameRunning");
      return;
    }

    if (!preferencesService.hasValidGamePath()) {
      gamePathHandler.chooseAndValidateGameDirectory().thenRun(() -> join(game, password, ignoreRating));
      return;
    }

    if (waitingForMatchMakerGame()) {
      notificationService.addImmediateWarnNotification("teammatchmaking.notification.customAlreadyInQueue.message");
      return;
    }

    PlayerInfo currentPlayer = playerService.getCurrentPlayer();
    int playerRating = RatingUtil.getRoundedLeaderboardRating(currentPlayer, game.getLeaderboard());

    boolean minRatingViolated = game.getRatingMin() != null && playerRating < game.getRatingMin();
    boolean maxRatingViolated = game.getRatingMax() != null && playerRating > game.getRatingMax();

    if (!ignoreRating && (minRatingViolated || maxRatingViolated)) {
      showRatingOutOfBoundsConfirmation(playerRating, game, password);
      return;
    }

    if (game.isPasswordProtected() && password == null) {
      showEnterPasswordDialog(game, ignoreRating);
      return;
    }

    log.info("Joining game: '{}' ({})", game.getTitle(), game.getId());

    Set<String> simModUIds = game.getSimMods().keySet();
    prepareAndLaunchGameWhenReady(game.getFeaturedMod(), simModUIds, game.getMapFolderName(),
                                  () -> fafServerAccessor.requestJoinGame(game.getId(), password)).exceptionally(
        throwable -> {
          log.error("Game could not be joined", throwable);
          notificationService.addImmediateErrorNotification(throwable, "games.couldNotJoin");
          return null;
        });
  }

  private void showEnterPasswordDialog(GameInfo game, boolean ignoreRating) {
    EnterPasswordController enterPasswordController = uiService.loadFxml("theme/enter_password.fxml");
    enterPasswordController.setPasswordEnteredListener(this::join);
    enterPasswordController.setGame(game);
    enterPasswordController.setIgnoreRating(ignoreRating);
    enterPasswordController.showPasswordDialog(StageHolder.getStage());
  }

  private void showRatingOutOfBoundsConfirmation(int playerRating, GameInfo game, String password) {
    notificationService.addNotification(new ImmediateNotification(i18n.get("game.joinGameRatingConfirmation.title"),
                                                                  i18n.get("game.joinGameRatingConfirmation.text",
                                                                           game.getRatingMin(), game.getRatingMax(),
                                                                           playerRating), Severity.INFO, List.of(
        new Action(i18n.get("game.join"), () -> join(game, password, true)), new Action(i18n.get("game.cancel")))));
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

    if (!preferencesService.hasValidGamePath()) {
      gamePathHandler.chooseAndValidateGameDirectory().thenRun(this::startSearchMatchmaker);
      return;
    }

    log.info("Matchmaking search has been started");

    matchmakerFuture = prepareAndLaunchGameWhenReady(FAF.getTechnicalName(), Set.of(), null,
                                                     fafServerAccessor::startSearchMatchmaker);

    matchmakerFuture.whenComplete((ignored, throwable) -> {
      if (throwable != null) {
        throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
        if (throwable instanceof CancellationException) {
          log.info("Matchmaking search has been cancelled");
          if (isRunning()) {
            notificationService.addNotification(
                new ServerNotification(i18n.get("matchmaker.cancelled.title"), i18n.get("matchmaker.cancelled"),
                                       Severity.INFO));
            killGame();
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

  public boolean isRunning() {
    return running.get();
  }

  private boolean waitingForMatchMakerGame() {
    return matchmakerFuture != null && !matchmakerFuture.isDone();
  }

  private Process launchOnlineGame(GameParameters gameParameters, Integer gpgPort, Integer replayPort) {
    fafServerAccessor.setPingIntervalSeconds(5);
    fafServerAccessor.setTimeoutLoginReconnectSeconds(5);
    gameKilled = false;
    return forgedAllianceLaunchService.launchOnlineGame(gameParameters, gpgPort, replayPort);
  }

  private CompletableFuture<Integer> startIceAdapter(int uid) {
    return iceAdapter.start(uid)
                     .thenCompose(icePort -> coturnService.getSelectedCoturns(uid)
                                                          .collectList()
                                                          .doOnNext(iceAdapter::setIceServers)
                                                          .thenReturn(icePort)
                                                          .toFuture());
  }

  private Mono<League> getDivisionInfo(String leaderboard) {
    return leaderboardService.getActiveLeagueEntryForPlayer(playerService.getCurrentPlayer(), leaderboard)
                             .map(LeagueEntry::subdivision)
                             .map(subdivision -> {
                               Division divisionBean = subdivision.division();
                               String division = divisionBean == null ? null : divisionBean.nameKey();
                               String subDivision = subdivision.nameKey();
                               return new League(division, subDivision);
                             })
                             .defaultIfEmpty(new League("unlisted", null));
  }

  private void handleTermination(Process finishedProcess) {
    fafServerAccessor.setPingIntervalSeconds(25);
    fafServerAccessor.setTimeoutLoginReconnectSeconds(30);
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

    if (!gameKilled) {
      if (exitCode != 0) {
        alertOnBadExit(exitCode, logFile);
      } else if (notificationPrefs.isAfterGameReviewEnabled()) {
        askForGameRate();
      }
    }
  }

  private void askForGameRate() {
    GameInfo game = getRunningGame();
    if (game == null) {
      return;
    }

    notificationService.addNotification(
        new PersistentNotification(i18n.get("game.ended", game.getTitle()), Severity.INFO, List.of(
            new Action(i18n.get("game.rate"), () -> navigationHandler.navigateTo(new ShowReplayEvent(game.getId()))))));
  }

  private void alertOnBadExit(int exitCode, Optional<Path> logFile) {
    if (exitCode == -1073741515) {
      notificationService.addImmediateWarnNotification("game.crash.notInitialized");
    } else {
      notificationService.addNotification(new ImmediateNotification(i18n.get("errorTitle"),
                                                                    i18n.get("game.crash", exitCode,
                                                                             logFile.map(Path::toString).orElse("")),
                                                                    WARN, List.of(new Action(i18n.get("game.open.log"),
                                                                                             () -> platformService.reveal(
                                                                                                 logFile.orElse(
                                                                                                     operatingSystem.getLoggingDirectory()))),
                                                                                  new DismissAction(i18n))));
    }
  }

  private void killGame() {
    if (isRunning()) {
      gameKilled = true;
      log.info("ForgedAlliance still running, destroying process");
      process.get().destroy();
    }
  }

  public void launchTutorial(MapVersion mapVersion, String technicalMapName) {
    if (isRunning()) {
      log.info("Game is running, ignoring tutorial launch");
      notificationService.addImmediateWarnNotification("game.gameRunning");
      return;
    }

    if (!preferencesService.hasValidGamePath()) {
      gamePathHandler.chooseAndValidateGameDirectory().thenAccept(path -> launchTutorial(mapVersion, technicalMapName));
      return;
    }

    String mapFolderName = mapVersion.folderName();
    CompletableFuture<Void> downloadMapFuture = mapService.downloadIfNecessary(mapFolderName).toFuture();
    CompletableFuture<Void> updateTutorialFuture = featuredModService.updateFeaturedModToLatest(
        TUTORIALS.getTechnicalName(), false);

    CompletableFuture.allOf(updateTutorialFuture, downloadMapFuture)
                     .thenApply(ignored -> forgedAllianceLaunchService.launchOfflineGame(technicalMapName))
                     .whenCompleteAsync((process, throwable) -> {
                       if (process != null) {
                         this.process.set(process);
                       }
                     }, fxApplicationThreadExecutor)
                     .thenCompose(Process::onExit)
                     .thenAccept(this::handleTermination)
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

  public void startOffline() {
    if (isRunning()) {
      log.info("Game is running, ignoring start offline request");
      notificationService.addImmediateWarnNotification("game.gameRunning");
      return;
    }

    if (!preferencesService.hasValidGamePath()) {
      gamePathHandler.chooseAndValidateGameDirectory().thenAccept(path -> startOffline());
      return;
    }

    CompletableFuture.supplyAsync(() -> forgedAllianceLaunchService.launchOfflineGame(null))
                     .whenCompleteAsync((process, throwable) -> {
                       if (process != null) {
                         this.process.set(process);
                       }
                     }, fxApplicationThreadExecutor)
                     .thenCompose(Process::onExit)
                     .thenAccept(this::handleTermination)
                     .exceptionally(throwable -> {
                       throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
                       log.error("Launching offline game failed", throwable);
                       if (throwable instanceof NotifiableException notifiableException) {
                         notificationService.addErrorNotification(notifiableException);
                       } else {
                         notificationService.addImmediateErrorNotification(throwable, "tutorial.launchFailed");
                       }
                       return null;
                     });
  }

  public Long getRunningProcessId() {
    return this.pid.getValue();
  }

  public GameInfo getRunningGame() {
    return runningGame.getValue();
  }

  public ReadOnlyObjectProperty<GameInfo> runningGameProperty() {
    return runningGame.getReadOnlyProperty();
  }
}
