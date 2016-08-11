package com.faforever.client.remote;

import com.faforever.client.api.Ranked1v1Stats;
import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.config.CacheNames;
import com.faforever.client.game.Faction;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.leaderboard.Ranked1v1EntryBean;
import com.faforever.client.map.MapBean;
import com.faforever.client.mod.ModInfoBean;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.relay.GpgClientMessage;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.ServerMessage;
import javafx.beans.property.ReadOnlyObjectProperty;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

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

  void initConnectivityTest(int port);

  void sendGpgMessage(GpgClientMessage message);

  void expand1v1Search(float radius);

  CompletionStage<LoginMessage> connectAndLogIn(String username, String password);

  void disconnect();

  void addFriend(PlayerInfoBean friendId);

  void addFoe(PlayerInfoBean foeId);

  void removeFriend(PlayerInfoBean friendId);

  void removeFoe(PlayerInfoBean foeId);

  Long getSessionId();

  CompletionStage<List<Ranked1v1EntryBean>> getRanked1v1Entries();

  CompletionStage<Ranked1v1Stats> getRanked1v1Stats();

  CompletionStage<Ranked1v1EntryBean> getRanked1v1EntryForPlayer(int playerId);

  void notifyGameEnded();

  @Cacheable(CacheNames.MAPS)
  List<MapBean> getMaps();

  MapBean findMapByName(String mapName);

  List<ModInfoBean> getMods();

  CompletionStage<GameLaunchMessage> expectRehostCommand();

  void reconnect();

  CompletionStage<List<MapBean>> getMostDownloadedMaps(int count);

  CompletionStage<List<MapBean>> getMostPlayedMaps(int count);

  CompletionStage<List<MapBean>> getMostLikedMaps(int count);

  CompletionStage<List<MapBean>> getNewestMaps(int count);
}
