package com.faforever.client.remote;

import com.faforever.client.api.AchievementDefinition;
import com.faforever.client.api.CoopLeaderboardEntry;
import com.faforever.client.api.FeaturedMod;
import com.faforever.client.api.FeaturedModFile;
import com.faforever.client.api.PlayerAchievement;
import com.faforever.client.api.Ranked1v1Stats;
import com.faforever.client.api.RatingType;
import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.coop.CoopMission;
import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.fa.relay.GpgGameMessage;
import com.faforever.client.game.Faction;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.io.ByteCountListener;
import com.faforever.client.leaderboard.Ranked1v1EntryBean;
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
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

// TODO divide and conquer
public interface FafService {

  @SuppressWarnings("unchecked")
  <T extends ServerMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener);

  @SuppressWarnings("unchecked")
  <T extends ServerMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener);

  CompletionStage<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo);

  ReadOnlyObjectProperty<ConnectionState> connectionStateProperty();

  CompletionStage<GameLaunchMessage> requestJoinGame(int gameId, String password);

  CompletionStage<GameLaunchMessage> startSearchRanked1v1(Faction faction, int port);

  void stopSearchingRanked();

  void sendGpgGameMessage(GpgGameMessage message);

  CompletionStage<LoginMessage> connectAndLogIn(String username, String password);

  void disconnect();

  void addFriend(Player friendId);

  void addFoe(Player foeId);

  void removeFriend(Player friendId);

  void removeFoe(Player foeId);

  CompletionStage<Ranked1v1Stats> getRanked1v1Stats();

  CompletionStage<Ranked1v1EntryBean> getRanked1v1EntryForPlayer(int playerId);

  void notifyGameEnded();

  List<MapBean> getMaps();

  MapBean findMapByName(String mapName);

  List<Mod> getMods();

  Mod getMod(String uid);

  void reconnect();

  CompletionStage<List<MapBean>> getMostDownloadedMaps(int count);

  CompletionStage<List<MapBean>> getMostPlayedMaps(int count);

  CompletionStage<List<MapBean>> getMostLikedMaps(int count);

  CompletionStage<List<MapBean>> getNewestMaps(int count);

  CompletableFuture<List<CoopMission>> getCoopMaps();

  CompletionStage<List<AvatarBean>> getAvailableAvatars();

  void selectAvatar(AvatarBean avatar);

  void evictModsCache();

  CompletableFuture<List<CoopLeaderboardEntry>> getCoopLeaderboard(CoopMission mission, int numberOfPlayers);

  CompletableFuture<List<RatingHistoryDataPoint>> getRatingHistory(RatingType ratingType, int playerId);

  void sendSdp(int remotePlayerId, String sdp);

  CompletableFuture<List<FeaturedModBean>> getFeaturedMods();

  CompletableFuture<List<FeaturedModFile>> getFeaturedModFiles(FeaturedModBean featuredMod, Integer version);

  CompletionStage<List<Ranked1v1EntryBean>> getLeaderboardEntries(KnownFeaturedMod mod);

  CompletableFuture<List<Replay>> searchReplayByMap(String mapName);

  CompletableFuture<List<Replay>> searchReplayByMod(FeaturedMod featuredMod);

  CompletableFuture<List<Replay>> searchReplayByPlayer(String playerName);

  CompletionStage<List<Replay>> getNewestReplays(int topElementCount);

  CompletionStage<List<Replay>> getHighestRatedReplays(int topElementCount);

  CompletionStage<List<Replay>> getMostWatchedReplays(int topElementCount);

  void uploadMod(Path modFile, ByteCountListener byteListener);

  CompletionStage<List<PlayerAchievement>> getPlayerAchievements(int playerId);

  CompletionStage<List<AchievementDefinition>> getAchievementDefinitions();

  CompletionStage<AchievementDefinition> getAchievementDefinition(String achievementId);
}
