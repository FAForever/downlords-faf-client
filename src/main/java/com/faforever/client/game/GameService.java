package com.faforever.client.game;

import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.rankedmatch.OnRankedMatchNotificationListener;
import com.faforever.client.util.Callback;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

/**
 * Downloads necessary maps, mods and updates before starting
 */
public interface GameService {

  void addOnGameInfoBeanListener(ListChangeListener<GameInfoBean> listener);

  void publishPotentialPlayer();

  void hostGame(NewGameInfo name, Callback<Void> callback);

  void cancelLadderSearch();

  void joinGame(GameInfoBean gameInfoBean, String password, Callback<Void> callback);

  List<GameTypeBean> getGameTypes();

  void addOnGameTypeInfoListener(MapChangeListener<String, GameTypeBean> changeListener);

  void addOnGameStartedListener(OnGameStartedListener listener);

  /**
   * @param path a replay file that is readable by the game without any further conversion
   */
  void runWithReplay(Path path, @Nullable Integer replayId) throws IOException;

  void runWithReplay(URL url, Integer replayId) throws IOException;

  ObservableList<GameInfoBean> getGameInfoBeans();

  @Nullable
  GameTypeBean getGameTypeByString(String gameTypeBeanName);

  GameInfoBean getByUid(int uid);

  void accept1v1Match(Faction faction);

  void addOnRankedMatchNotificationListener(OnRankedMatchNotificationListener listener);

  void startSearchRanked1v1(Faction faction, Callback<GameLaunchInfo> callback);
}
