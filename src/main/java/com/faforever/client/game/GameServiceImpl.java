package com.faforever.client.game;

import com.faforever.client.fa.ForgedAllianceService;
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
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.patch.GameUpdateService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.ConcurrentUtil;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Path;
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

import static java.util.Collections.emptyMap;

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
  GameUpdateService gameUpdateService;
  @Autowired
  NotificationService notificationService;
  @Autowired
  I18n i18n;

  public GameServiceImpl() {
    gameTypeBeans = FXCollections.observableHashMap();
    onGameLaunchingListeners = new HashSet<>();
    uidToGameInfoBean = new HashMap<>();

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
  public CompletionStage<Void> hostGame(NewGameInfo newGameInfo) {
    cancelLadderSearch();


    CompletableFuture<Void> future = new CompletableFuture<>();

    updateGameIfNecessary(newGameInfo.getGameType(), newGameInfo.getVersion(), emptyMap(), newGameInfo.getSimModUidsToVersions())
        .thenRun(() -> lobbyServerAccessor.requestNewGame(newGameInfo)
            .thenAccept((gameLaunchInfo) -> startGame(gameLaunchInfo, future))
            .exceptionally(throwable -> {
              logger.warn("Could not start game", throwable);
              return null;
            }))
        .exceptionally(throwable -> {
          logger.warn("Game could not be updated", throwable);
          return null;
        });

    return future;
  }

  private CompletableFuture<Void> updateGameIfNecessary(String gameType, Integer version, Map<String, Integer> modVersions, Set<String> simModUIds) {
    return gameUpdateService.updateInBackground(gameType, version, modVersions, simModUIds);
  }

  private void startGame(GameLaunchInfo gameLaunchInfo, CompletableFuture<Void> future) {
    List<String> args = fixMalformedArgs(gameLaunchInfo.getArgs());
    try {
      Process process = forgedAllianceService.startGame(gameLaunchInfo.getUid(), gameLaunchInfo.getMod(), args);
      onGameLaunchingListeners.forEach(onGameStartedListener -> onGameStartedListener.onGameStarted(gameLaunchInfo.getUid()));
      lobbyServerAccessor.notifyGameStarted();

      waitForProcessTerminationInBackground(process);
      future.complete(null);
    } catch (Exception e) {
      future.completeExceptionally(e);
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
  void waitForProcessTerminationInBackground(Process process) {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        int exitCode = process.waitFor();
        logger.info("Forged Alliance terminated with exit code {}", exitCode);
        proxy.close();
        lobbyServerAccessor.notifyGameTerminated();
        return null;
      }
    });
  }

  @Override
  public void cancelLadderSearch() {
    logger.warn("Cancelling ladder search has not yet been implemented");
  }

  @Override
  public CompletableFuture<Void> joinGame(GameInfoBean gameInfoBean, String password) {
    logger.info("Joining game {} ({})", gameInfoBean.getTitle(), gameInfoBean.getUid());

    cancelLadderSearch();

    Map<String, Integer> simModVersions = gameInfoBean.getFeaturedModVersions();
    Set<String> simModUIds = gameInfoBean.getSimMods().keySet();

    CompletableFuture<Void> future = new CompletableFuture<>();
    return updateGameIfNecessary(gameInfoBean.getFeaturedMod(), null, simModVersions, simModUIds)
        .thenRun(() -> downloadMapIfNecessary(gameInfoBean.getTechnicalName())
            .thenRun(() -> lobbyServerAccessor.requestJoinGame(gameInfoBean, password)
                .thenAccept(gameLaunchInfo -> startGame(gameLaunchInfo, future))));
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
            Process process = forgedAllianceService.startReplay(path, replayId);
            onGameLaunchingListeners.forEach(onGameStartedListener -> onGameStartedListener.onGameStarted(null));
            waitForProcessTerminationInBackground(process);
          } catch (IOException e) {
            notificationService.addNotification(new ImmediateNotification(
                i18n.get("replayCouldNotBeStarted.title", path),
                i18n.get("replayCouldNotBeStarted.text"),
                Severity.ERROR
                // TODO add detail
            ));
          }
        });
  }

  @Override
  public void runWithReplay(URL replayUrl, Integer replayId) throws IOException {
    Process process = forgedAllianceService.startReplay(replayUrl, replayId);
    onGameLaunchingListeners.forEach(onGameStartedListener -> onGameStartedListener.onGameStarted(null));
    // TODO is this needed when watching a replay?
    lobbyServerAccessor.notifyGameStarted();

    waitForProcessTerminationInBackground(process);
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
    return uidToGameInfoBean.get(uid);
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
    logger.debug("Received game info from server: {}", gameInfo);
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
