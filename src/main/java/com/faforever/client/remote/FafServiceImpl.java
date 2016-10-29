package com.faforever.client.remote;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.Ranked1v1Stats;
import com.faforever.client.api.RatingType;
import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.chat.avatar.event.AvatarChangedEvent;
import com.faforever.client.config.CacheNames;
import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.game.Faction;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.leaderboard.Ranked1v1EntryBean;
import com.faforever.client.map.MapBean;
import com.faforever.client.mod.ModInfoBean;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.relay.GpgGameMessage;
import com.faforever.client.remote.domain.GameEndedMessage;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.ServerMessage;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.ReadOnlyObjectProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;
import static java.time.LocalDateTime.ofEpochSecond;
import static java.time.ZoneOffset.UTC;

public class FafServiceImpl implements FafService {

  @Resource
  FafServerAccessor fafServerAccessor;
  @Resource
  FafApiAccessor fafApiAccessor;
  @Resource
  ThreadPoolExecutor threadPoolExecutor;
  @Resource
  EventBus eventBus;

  @Override
  public <T extends ServerMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener) {
    fafServerAccessor.addOnMessageListener(type, listener);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends ServerMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener) {
    fafServerAccessor.removeOnMessageListener(type, listener);
  }

  @Override
  public CompletionStage<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo) {
    return fafServerAccessor.requestHostGame(newGameInfo);
  }

  @Override
  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return fafServerAccessor.connectionStateProperty();
  }

  @Override
  public CompletionStage<GameLaunchMessage> requestJoinGame(int gameId, String password) {
    return fafServerAccessor.requestJoinGame(gameId, password
    );
  }

  @Override
  public CompletionStage<GameLaunchMessage> startSearchRanked1v1(Faction faction, int port) {
    return fafServerAccessor.startSearchRanked1v1(faction);
  }

  @Override
  public void stopSearchingRanked() {
    fafServerAccessor.stopSearchingRanked();
  }

  @Override
  public void initConnectivityTest(int port) {
    fafServerAccessor.initConnectivityTest(port);
  }

  @Override
  public void sendGpgGameMessage(GpgGameMessage message) {
    fafServerAccessor.sendGpgMessage(message);
  }

  @Override
  public CompletionStage<LoginMessage> connectAndLogIn(String username, String password) {
    return fafServerAccessor.connectAndLogIn(username, password);
  }

  @Override
  public void disconnect() {
    fafServerAccessor.disconnect();
  }

  @Override
  public void addFriend(PlayerInfoBean player) {
    fafServerAccessor.addFriend(player.getId());
  }

  @Override
  public void addFoe(PlayerInfoBean player) {
    fafServerAccessor.addFoe(player.getId());
  }

  @Override
  public void removeFriend(PlayerInfoBean player) {
    fafServerAccessor.removeFriend(player.getId());
  }

  @Override
  public void removeFoe(PlayerInfoBean player) {
    fafServerAccessor.removeFoe(player.getId());
  }

  @Override
  public Long getSessionId() {
    return fafServerAccessor.getSessionId();
  }

  @Override
  public CompletionStage<List<Ranked1v1EntryBean>> getRanked1v1Entries() {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getRanked1v1Entries(), threadPoolExecutor);
  }

  @Override
  public CompletionStage<Ranked1v1Stats> getRanked1v1Stats() {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getRanked1v1Stats(), threadPoolExecutor);
  }

  @Override
  public CompletionStage<Ranked1v1EntryBean> getRanked1v1EntryForPlayer(int playerId) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getRanked1v1EntryForPlayer(playerId), threadPoolExecutor);
  }

  @Override
  public void notifyGameEnded() {
    fafServerAccessor.sendGpgMessage(new GameEndedMessage());
  }

  @Override
  @Cacheable(CacheNames.MAPS)
  public List<MapBean> getMaps() {
    return fafApiAccessor.getMaps();
  }

  @Override
  public MapBean findMapByName(String mapName) {
    return fafApiAccessor.findMapByName(mapName);
  }

  @Override
  @Cacheable(CacheNames.MODS)
  public List<ModInfoBean> getMods() {
    return fafApiAccessor.getMods();
  }

  @Override
  public void reconnect() {
    fafServerAccessor.reconnect();
  }

  @Override
  public CompletionStage<List<MapBean>> getMostDownloadedMaps(int count) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getMostDownloadedMaps(count), threadPoolExecutor);
  }

  @Override
  public CompletionStage<List<MapBean>> getMostPlayedMaps(int count) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getMostPlayedMaps(count), threadPoolExecutor);
  }

  @Override
  public CompletionStage<List<MapBean>> getMostLikedMaps(int count) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getBestRatedMaps(count), threadPoolExecutor);
  }

  @Override
  public CompletionStage<List<MapBean>> getNewestMaps(int count) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getNewestMaps(count), threadPoolExecutor);
  }

  @Override
  public CompletionStage<List<AvatarBean>> getAvailableAvatars() {
    return CompletableFuture.supplyAsync(() -> fafServerAccessor.getAvailableAvatars())
        .thenApply(avatars -> avatars.stream()
            .map(AvatarBean::fromAvatar)
            .collect(Collectors.toList()));
  }

  @Override
  public void selectAvatar(AvatarBean avatar) {
    fafServerAccessor.selectAvatar(avatar == null ? null : avatar.getUrl());
    eventBus.post(new AvatarChangedEvent(avatar));
  }

  @Override
  @CacheEvict(CacheNames.MODS)
  public void evictModsCache() {
    // Nothing to see, please move along
  }

  @Cacheable(CacheNames.RATING_HISTORY)
  @Override
  public CompletableFuture<List<RatingHistoryDataPoint>> getRatingHistory(RatingType ratingType, int playerId) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getRatingHistory(ratingType, playerId)
        .getData().entrySet()
        .stream()
        .sorted((o1, o2) -> Long.compare(parseLong(o1.getKey()), parseLong(o2.getKey())))
        .map(entry -> new RatingHistoryDataPoint(ofEpochSecond(parseLong(entry.getKey()), 0, UTC), entry.getValue().get(0), entry.getValue().get(1)))
        .collect(Collectors.toList())
    );
  }

  @Override
  public void sendSdp(int localPlayerId, int remotePlayerId, String sdp) {
    fafServerAccessor.sendSdp(localPlayerId, remotePlayerId, sdp);
  }
}
