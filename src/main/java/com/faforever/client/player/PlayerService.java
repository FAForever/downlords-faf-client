package com.faforever.client.player;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.domain.api.NameRecord;
import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.mapstruct.PlayerMapper;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.user.LoginService;
import com.faforever.client.util.RatingUtil;
import com.faforever.commons.api.dto.Player;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.util.Subscription;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlayerService implements InitializingBean {

  private final Map<String, PlayerInfo> playersByName = new ConcurrentHashMap<>();
  private final Map<Integer, PlayerInfo> playersById = new ConcurrentHashMap<>();
  private final Map<PlayerInfo, Set<Subscription>> playerSubscriptions = new ConcurrentHashMap<>();
  private final ReadOnlyObjectWrapper<PlayerInfo> currentPlayer = new ReadOnlyObjectWrapper<>();
  private final List<Consumer<PlayerInfo>> playerOnlineListeners = new ArrayList<>();
  private final List<Consumer<PlayerInfo>> playerOfflineListeners = new ArrayList<>();

  private final FafServerAccessor fafServerAccessor;
  private final FafApiAccessor fafApiAccessor;
  private final LoginService loginService;
  private final PlayerMapper playerMapper;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  @Override
  public void afterPropertiesSet() {
    fafServerAccessor.getEvents(com.faforever.commons.lobby.PlayerInfo.class)
                     .map(com.faforever.commons.lobby.PlayerInfo::getPlayers)
                     .flatMap(Flux::fromIterable)
                     .flatMap(player -> Mono.zip(Mono.just(player), Mono.justOrEmpty(playersById.get(player.getId()))
                                                                        .switchIfEmpty(initializePlayer(player))))
                     .publishOn(fxApplicationThreadExecutor.asScheduler())
                     .map(TupleUtils.function(playerMapper::update))
                     .doOnError(throwable -> log.error("Error processing player", throwable))
                     .retry()
                     .subscribe();

    currentPlayer.bind(loginService.ownPlayerProperty().map(this::createOrUpdateFromOwnPlayer));
  }

  public ObservableValue<Double> getAverageRatingPropertyForGame(GameInfo gameInfo) {
    return gameInfo.activePlayersInGameProperty()
                   .map(ids -> ids.stream()
                                  .map(this::getPlayerByIdIfOnline)
                                  .flatMap(Optional::stream)
                                  .mapToInt(
                                      player -> RatingUtil.getLeaderboardRating(player, gameInfo.getLeaderboard()))
                                  .average()
                                  .orElse(0));
  }

  public double getAverageRatingForGame(GameInfo gameInfo) {
    return getAverageRatingPropertyForGame(gameInfo).getValue();
  }

  public boolean isCurrentPlayerInGame(GameInfo game) {
    // TODO the following can be removed as soon as the server tells us which game a player is in.
    return game.getAllPlayersInGame().contains(loginService.getUserId());
  }

  public boolean isOnline(Integer playerId) {
    return playersById.containsKey(playerId);
  }

  /**
   * Gets a player for the given username. A new player is created and registered if it does not yet exist.
   */
  private PlayerInfo createOrUpdateFromOwnPlayer(@NonNull com.faforever.commons.lobby.Player playerInfo) {
    return playersById.compute(playerInfo.getId(), (id, knownPlayer) -> {
      if (knownPlayer == null) {
        PlayerInfo newPlayer = new PlayerInfo();
        newPlayer.setId(id);
        newPlayer.setUsername(playerInfo.getLogin());
        newPlayer.setSocialStatus(SocialStatus.SELF);
        Subscription removeSubscription = newPlayer.serverStatusProperty().subscribe(serverStatus -> {
          if (serverStatus == ServerStatus.OFFLINE) {
            removePlayer(newPlayer);
          }
        });
        playerSubscriptions.computeIfAbsent(newPlayer, _ -> ConcurrentHashMap.newKeySet())
                           .add(removeSubscription);
        playersByName.put(newPlayer.getUsername(), newPlayer);
        return playerMapper.update(playerInfo, newPlayer);
      } else {
        return playerMapper.update(playerInfo, knownPlayer);
      }
    });
  }

  private Mono<PlayerInfo> initializePlayer(com.faforever.commons.lobby.Player player) {
    return Mono.fromCallable(() -> {
                 PlayerInfo newPlayer = new PlayerInfo();
                 newPlayer.setId(player.getId());
                 newPlayer.setUsername(player.getLogin());
                 Subscription removeSubscription = newPlayer.serverStatusProperty().subscribe(serverStatus -> {
                   if (serverStatus == ServerStatus.OFFLINE) {
                     removePlayer(newPlayer);
                   }
                 });
                 playerSubscriptions.computeIfAbsent(newPlayer, _ -> ConcurrentHashMap.newKeySet()).add(removeSubscription);
                 return newPlayer;
               })
               .doOnNext(playerBean -> {
                 playersById.put(playerBean.getId(), playerBean);
                 playersByName.put(playerBean.getUsername(), playerBean);
               })
               .doOnNext(playerBean -> playerOnlineListeners.forEach(listener -> listener.accept(playerBean)));
  }

  public Set<String> getPlayerNames() {
    return new HashSet<>(playersByName.keySet());
  }

  public PlayerInfo getCurrentPlayer() {
    return currentPlayer.get();
  }

  public ReadOnlyObjectProperty<PlayerInfo> currentPlayerProperty() {
    return currentPlayer.getReadOnlyProperty();
  }

  public Optional<PlayerInfo> getPlayerByIdIfOnline(int playerId) {
    return Optional.ofNullable(playersById.get(playerId));
  }

  public Optional<PlayerInfo> getPlayerByNameIfOnline(String playerName) {
    return Optional.ofNullable(playersByName.get(playerName));
  }

  public Flux<PlayerInfo> getPlayersByIds(Collection<Integer> playerIds) {
    List<PlayerInfo> onlinePlayers = playerIds.stream()
                                              .map(this::getPlayerByIdIfOnline)
                                              .flatMap(Optional::stream)
                                              .toList();

    if (onlinePlayers.size() == playerIds.size()) {
      return Flux.fromIterable(onlinePlayers);
    }

    Set<Integer> onlineIds = onlinePlayers.stream().map(PlayerInfo::getId).collect(Collectors.toSet());

    return Flux.fromIterable(playerIds)
               .filter(playerId -> !onlineIds.contains(playerId))
               .window(100)
               .flatMap(Flux::collectList)
               .flatMap(offlineIds -> {
                 ElideNavigatorOnCollection<Player> navigator = ElideNavigator.of(Player.class)
                                                                              .collection()
                                                                              .setFilter(qBuilder().intNum("id")
                                                                                                   .in(offlineIds));
                 return fafApiAccessor.getMany(navigator).map(playerMapper::map);
               }).concatWithValues(onlinePlayers.toArray(new PlayerInfo[0]));
  }

  public Mono<PlayerInfo> getPlayerByName(String playerName) {
    ElideNavigatorOnCollection<Player> navigator = ElideNavigator.of(Player.class)
                                                                 .collection()
                                                                 .setFilter(qBuilder().string("login").eq(playerName));

    Mono<PlayerInfo> apiPlayer = fafApiAccessor.getMany(navigator)
                                               .next().map(playerMapper::map);

    return Mono.justOrEmpty(getPlayerByNameIfOnline(playerName)).switchIfEmpty(apiPlayer);
  }

  public Flux<NameRecord> getPlayerNames(PlayerInfo player) {
    ElideNavigatorOnCollection<com.faforever.commons.api.dto.NameRecord> navigator = ElideNavigator.of(
        com.faforever.commons.api.dto.NameRecord.class).collection().setFilter(qBuilder().intNum("player.id")
                                                                                          .eq(player.getId()));

    return fafApiAccessor.getMany(navigator).map(playerMapper::map)
                         .sort(Comparator.comparing(NameRecord::changeTime));
  }

  private void removePlayer(PlayerInfo player) {
    PlayerInfo removedPlayer = playersById.remove(player.getId());
    if (removedPlayer != null) {
      playersByName.remove(removedPlayer.getUsername());
      Set<Subscription> subscriptions = playerSubscriptions.remove(player);
      if (subscriptions != null) {
        subscriptions.forEach(Subscription::unsubscribe);
      }
      playerOfflineListeners.forEach(listener -> listener.accept(removedPlayer));
    }
  }

  public void addPlayerOnlineListener(Consumer<PlayerInfo> listener) {
    playerOnlineListeners.add(listener);
  }

  public void addPlayerOfflineListener(Consumer<PlayerInfo> listener) {
    playerOfflineListeners.add(listener);
  }
}
