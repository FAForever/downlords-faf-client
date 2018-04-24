package com.faforever.client.game;

import com.faforever.client.rankedmatch.MatchmakerMessage;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Downloads necessary maps, mods and updates before starting
 */
public interface GameService {

  ReadOnlyBooleanProperty gameRunningProperty();

  CompletableFuture<Void> hostGame(NewGameInfo name);

  CompletableFuture<Void> joinGame(Game game, String password);

  /**
   * @param path a replay file that is readable by the preferences without any further conversion
   */
  void runWithReplay(Path path, @Nullable Integer replayId, String featuredMod, Integer version, Map<String, Integer> modVersions, Set<String> simMods, String mapName);

  CompletableFuture<Void> runWithLiveReplay(URI replayUri, Integer gameId, String gameType, String mapName);

  ObservableList<Game> getGames();

  Game getByUid(int uid);

  void addOnRankedMatchNotificationListener(Consumer<MatchmakerMessage> listener);

  CompletableFuture<Void> startSearchLadder1v1(Faction faction);

  void stopSearchLadder1v1();

  BooleanProperty searching1v1Property();

  /**
   * Returns the preferences the player is currently in. Returns {@code null} if not in a preferences.
   */
  @Nullable
  Game getCurrentGame();

  boolean isGameRunning();

  public boolean isForgedAllianceProcessRunning();
}
