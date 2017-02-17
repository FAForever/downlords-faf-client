package com.faforever.client.remote;

import com.faforever.client.api.dto.AchievementDefinition;
import com.faforever.client.api.dto.CoopResult;
import com.faforever.client.api.dto.FeaturedMod;
import com.faforever.client.api.dto.FeaturedModFile;
import com.faforever.client.api.dto.PlayerAchievement;
import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.coop.CoopMission;
import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.fa.relay.GpgGameMessage;
import com.faforever.client.game.Faction;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.io.ProgressListener;
import com.faforever.client.leaderboard.LeaderboardEntry;
import com.faforever.client.map.MapBean;
import com.faforever.client.mod.FeaturedModBean;
import com.faforever.client.mod.Mod;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.Player;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.ServerMessage;
import com.faforever.client.replay.Replay;
import javafx.beans.property.ReadOnlyObjectProperty;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

// TODO divide and conquer
public interface FafService {

  @SuppressWarnings("unchecked")
  <T extends ServerMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener);

  @SuppressWarnings("unchecked")
  <T extends ServerMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener);

  CompletableFuture<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo);

  ReadOnlyObjectProperty<ConnectionState> connectionStateProperty();

  CompletableFuture<GameLaunchMessage> requestJoinGame(int gameId, String password);

  CompletableFuture<GameLaunchMessage> startSearchLadder1v1(Faction faction, int port);

  void stopSearchingRanked();

  void sendGpgGameMessage(GpgGameMessage message);

  CompletableFuture<LoginMessage> connectAndLogIn(String username, String password);

  void disconnect();

  void addFriend(Player friendId);

  void addFoe(Player foeId);

  void removeFriend(Player friendId);

  void removeFoe(Player foeId);

  CompletableFuture<LeaderboardEntry> getLadder1v1EntryForPlayer(int playerId);

  void notifyGameEnded();

  CompletableFuture<List<MapBean>> getMaps();

  CompletableFuture<List<Mod>> getMods();

  CompletableFuture<Mod> getMod(String uid);

  void reconnect();

  CompletableFuture<List<MapBean>> getMostDownloadedMaps(int count);

  CompletableFuture<List<MapBean>> getMostPlayedMaps(int count);

  CompletableFuture<List<MapBean>> getMostLikedMaps(int count);

  CompletableFuture<List<MapBean>> getNewestMaps(int count);

  CompletableFuture<List<CoopMission>> getCoopMaps();

  CompletableFuture<List<AvatarBean>> getAvailableAvatars();

  void selectAvatar(AvatarBean avatar);

  void evictModsCache();

  CompletableFuture<List<CoopResult>> getCoopLeaderboard(CoopMission mission, int numberOfPlayers);

  CompletableFuture<List<RatingHistoryDataPoint>> getRatingHistory(int playerId, KnownFeaturedMod knownFeaturedMod);

  void sendSdp(int remotePlayerId, String sdp);

  CompletableFuture<List<FeaturedModBean>> getFeaturedMods();

  CompletableFuture<List<FeaturedModFile>> getFeaturedModFiles(FeaturedModBean featuredMod, Integer version);

  CompletableFuture<List<LeaderboardEntry>> getLadder1v1Leaderboard();

  CompletableFuture<List<LeaderboardEntry>> getGlobalLeaderboard();

  CompletableFuture<List<Replay>> searchReplayByMap(String mapName);

  CompletableFuture<List<Replay>> searchReplayByMod(FeaturedMod featuredMod);

  CompletableFuture<List<Replay>> searchReplayByPlayer(String playerName);

  CompletableFuture<List<Replay>> getNewestReplays(int topElementCount);

  CompletableFuture<List<Replay>> getHighestRatedReplays(int topElementCount);

  CompletableFuture<List<Replay>> getMostWatchedReplays(int topElementCount);

  void uploadMod(Path modFile, ProgressListener byteListener);

  CompletableFuture<List<PlayerAchievement>> getPlayerAchievements(int playerId);

  CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions();

  CompletableFuture<AchievementDefinition> getAchievementDefinition(String achievementId);
}
