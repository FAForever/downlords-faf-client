package com.faforever.client.game;

import com.faforever.client.fa.ForgedAllianceService;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.OnGameInfoListener;
import com.faforever.client.legacy.OnGameTypeInfoListener;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.GameTypeInfo;
import com.faforever.client.legacy.proxy.Proxy;
import com.faforever.client.map.MapService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Callback;
import com.faforever.client.util.ConcurrentUtil;
import com.google.common.annotations.VisibleForTesting;
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

  public GameServiceImpl() {
    gameTypeBeans = FXCollections.observableHashMap();
    onGameLaunchingListeners = new HashSet<>();
    gameInfoBeans = FXCollections.observableArrayList();
    uidToGameInfoBean = new HashMap<>();
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
  public void hostGame(NewGameInfo newGameInfo, Callback<Void> callback) {
    cancelLadderSearch();
    updateGameIfNecessary(newGameInfo.mod, new Callback<Void>() {
      @Override
      public void success(Void result) {
        lobbyServerAccessor.requestNewGame(newGameInfo, gameLaunchCallback(callback));
      }

      @Override
      public void error(Throwable e) {
        callback.error(e);
      }
    });
  }

  private void updateGameIfNecessary(String modName, Callback<Void> callback) {
    callback.success(null);
  }

  private Callback<GameLaunchInfo> gameLaunchCallback(final Callback<Void> callback) {
    return new Callback<GameLaunchInfo>() {
      @Override
      public void success(GameLaunchInfo gameLaunchInfo) {
        List<String> args = fixMalformedArgs(gameLaunchInfo.args);
        try {
          Process process = forgedAllianceService.startGame(gameLaunchInfo.uid, gameLaunchInfo.mod, args);
          onGameLaunchingListeners.forEach(onGameStartedListener -> onGameStartedListener.onGameStarted(gameLaunchInfo.uid));
          lobbyServerAccessor.notifyGameStarted();

          waitForProcessTerminationInBackground(process);
          callback.success(null);
        } catch (Exception e) {
          callback.error(e);
        }
      }

      @Override
      public void error(Throwable e) {
        // FIXME implement
        logger.warn("Could not create game", e);
      }
    };
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
  public void joinGame(GameInfoBean gameInfoBean, String password, Callback<Void> callback) {
    logger.info("Joining game {} ({})", gameInfoBean.getTitle(), gameInfoBean.getUid());

    cancelLadderSearch();

    Callback<Void> mapDownloadCallback = new Callback<Void>() {
      @Override
      public void success(Void result) {
        lobbyServerAccessor.requestJoinGame(gameInfoBean, password, gameLaunchCallback(callback));
      }

      @Override
      public void error(Throwable e) {
        callback.error(e);
      }
    };

    updateGameIfNecessary(gameInfoBean.getFeaturedMod(), new Callback<Void>() {
      @Override
      public void success(Void result) {
        downloadMapIfNecessary(gameInfoBean.getMapName(), mapDownloadCallback);
      }

      @Override
      public void error(Throwable e) {
        callback.error(e);
      }
    });
  }

  private void downloadMapIfNecessary(String mapName, Callback<Void> callback) {
    if (mapService.isAvailable(mapName)) {
      callback.success(null);
      return;
    }

    mapService.download(mapName, callback);
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
  public void runWithReplay(Path path, @Nullable Integer replayId) throws IOException {
    Process process = forgedAllianceService.startReplay(path, replayId);
    onGameLaunchingListeners.forEach(onGameStartedListener -> onGameStartedListener.onGameStarted(null));
    // TODO is this needed when watching a replay?
    lobbyServerAccessor.notifyGameStarted();

    waitForProcessTerminationInBackground(process);
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
    if (!gameTypeInfo.host || !gameTypeInfo.live || gameTypeBeans.containsKey(gameTypeInfo.name)) {
      return;
    }

    gameTypeBeans.put(gameTypeInfo.name, new GameTypeBean(gameTypeInfo));
  }

  @Override
  public void onGameInfo(GameInfo gameInfo) {
    logger.debug("Received game info from server: {}", gameInfo);
    if (GameState.CLOSED.equals(gameInfo.state)) {
      gameInfoBeans.remove(uidToGameInfoBean.remove(gameInfo.uid));
      return;
    }

    if (!uidToGameInfoBean.containsKey(gameInfo.uid)) {
      GameInfoBean gameInfoBean = new GameInfoBean(gameInfo);

      gameInfoBeans.add(gameInfoBean);
      uidToGameInfoBean.put(gameInfo.uid, gameInfoBean);
    } else {
      uidToGameInfoBean.get(gameInfo.uid).updateFromGameInfo(gameInfo);
    }
  }
}
