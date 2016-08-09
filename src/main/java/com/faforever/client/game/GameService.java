package com.faforever.client.game;

import com.faforever.client.rankedmatch.MatchmakerMessage;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Downloads necessary maps, mods and updates before starting
 */
public interface GameService {

  ReadOnlyBooleanProperty gameRunningProperty();

  void addOnGameInfoBeansChangeListener(ListChangeListener<GameInfoBean> listener);

  CompletionStage<Void> hostGame(NewGameInfo name);

  CompletionStage<Void> joinGame(GameInfoBean gameInfoBean, String password);

  List<GameTypeBean> getGameTypes();

  void addOnGameTypesChangeListener(MapChangeListener<String, GameTypeBean> changeListener);

  /**
   * @param path a replay file that is readable by the game without any further conversion
   */
  void runWithReplay(Path path, @Nullable Integer replayId, String gameType, Integer version, Map<String, Integer> modVersions, Set<String> simMods);

  CompletionStage<Void> runWithLiveReplay(URI replayUri, Integer gameId, String gameType, String mapName) throws IOException;

  ObservableList<GameInfoBean> getGameInfoBeans();

  @Nullable
  GameTypeBean getGameTypeByString(String gameTypeBeanName);

  GameInfoBean getByUid(int uid);

  void addOnRankedMatchNotificationListener(Consumer<MatchmakerMessage> listener);

  CompletionStage<Void> startSearchRanked1v1(Faction faction);

  void stopSearchRanked1v1();

  BooleanProperty searching1v1Property();

  CompletionStage<Void> prepareForRehost();

  /**
   * Returns the game the player is currently in. Returns {@code null} if not in a game.
   */
  @Nullable
  GameInfoBean getCurrentGame();

  boolean isGameRunning();
}
