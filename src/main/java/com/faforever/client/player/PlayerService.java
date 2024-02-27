package com.faforever.client.player;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.NameRecordBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.PlayerMapper;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.user.LoginService;
import com.faforever.client.util.RatingUtil;
import com.faforever.commons.api.dto.NameRecord;
import com.faforever.commons.api.dto.Player;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import com.faforever.commons.lobby.PlayerInfo;
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
import reactor.core.scheduler.Schedulers;
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

  private final Map<String, PlayerBean> playersByName = new ConcurrentHashMap<>();
  private final Map<Integer, PlayerBean> playersById = new ConcurrentHashMap<>();
  private final Map<PlayerBean, Set<Subscription>> playerSubscriptions = new ConcurrentHashMap<>();
  private final ReadOnlyObjectWrapper<PlayerBean> currentPlayer = new ReadOnlyObjectWrapper<>();
  private final List<Consumer<PlayerBean>> playerOnlineListeners = new ArrayList<>();
  private final List<Consumer<PlayerBean>> playerOfflineListeners = new ArrayList<>();

  private final FafServerAccessor fafServerAccessor;
  private final FafApiAccessor fafApiAccessor;
  private final LoginService loginService;
  private final PlayerMapper playerMapper;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  @Override
  public void afterPropertiesSet() {
    fafServerAccessor.getEvents(PlayerInfo.class)
                     .map(PlayerInfo::getPlayers)
                     .flatMap(Flux::fromIterable)
                     .flatMap(player -> Mono.zip(Mono.just(player), Mono.justOrEmpty(playersById.get(player.getId()))
                                                                        .switchIfEmpty(initializePlayer(player))))
                     .publishOn(fxApplicationThreadExecutor.asScheduler())
                     .map(TupleUtils.function((player, playerBean) -> playerMapper.update(player, playerBean,
                                                                                          new CycleAvoidingMappingContext())))
                     .publishOn(Schedulers.single())
                     .doOnError(throwable -> log.error("Error processing player", throwable))
                     .retry()
                     .subscribe();

    currentPlayer.bind(loginService.ownPlayerProperty().map(this::createOrUpdateFromOwnPlayer));
  }

  public ObservableValue<Double> getAverageRatingPropertyForGame(GameBean gameBean) {
    return gameBean.activePlayersInGameProperty()
                   .map(ids -> ids.stream()
                                  .map(this::getPlayerByIdIfOnline)
                                  .flatMap(Optional::stream)
                                  .mapToInt(
                                      player -> RatingUtil.getLeaderboardRating(player, gameBean.getLeaderboard()))
                                  .average()
                                  .orElse(0));
  }

  public double getAverageRatingForGame(GameBean gameBean) {
    return getAverageRatingPropertyForGame(gameBean).getValue();
  }

  public boolean isCurrentPlayerInGame(GameBean game) {
    // TODO the following can be removed as soon as the server tells us which game a player is in.
    return game.getAllPlayersInGame().contains(loginService.getUserId());
  }

  public boolean isOnline(Integer playerId) {
    return playersById.containsKey(playerId);
  }

  /**
   * Gets a player for the given username. A new player is created and registered if it does not yet exist.
   */
  private PlayerBean createOrUpdateFromOwnPlayer(@NonNull com.faforever.commons.lobby.Player playerInfo) {
    return playersById.compute(playerInfo.getId(), (id, knownPlayer) -> {
      if (knownPlayer == null) {
        PlayerBean newPlayer = new PlayerBean();
        newPlayer.setId(id);
        newPlayer.setUsername(playerInfo.getLogin());
        newPlayer.setSocialStatus(SocialStatus.SELF);
        Subscription removeSubscription = newPlayer.serverStatusProperty().subscribe(serverStatus -> {
          if (serverStatus == ServerStatus.OFFLINE) {
            removePlayer(newPlayer);
          }
        });
        playerSubscriptions.computeIfAbsent(newPlayer, ignored -> ConcurrentHashMap.newKeySet())
                           .add(removeSubscription);
        playersByName.put(newPlayer.getUsername(), newPlayer);
        return playerMapper.update(playerInfo, newPlayer, new CycleAvoidingMappingContext());
      } else {
        return playerMapper.update(playerInfo, knownPlayer, new CycleAvoidingMappingContext());
      }
    });
  }

  private Mono<PlayerBean> initializePlayer(com.faforever.commons.lobby.Player player) {
    return Mono.fromCallable(() -> {
                 PlayerBean newPlayer = new PlayerBean();
                 newPlayer.setId(player.getId());
                 newPlayer.setUsername(player.getLogin());
                 Subscription removeSubscription = newPlayer.serverStatusProperty().subscribe(serverStatus -> {
                   if (serverStatus == ServerStatus.OFFLINE) {
                     removePlayer(newPlayer);
                   }
                 });
                 playerSubscriptions.computeIfAbsent(newPlayer, ignored -> ConcurrentHashMap.newKeySet()).add(removeSubscription);
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

  public PlayerBean getCurrentPlayer() {
    return currentPlayer.get();
  }

  public ReadOnlyObjectProperty<PlayerBean> currentPlayerProperty() {
    return currentPlayer.getReadOnlyProperty();
  }

  public Optional<PlayerBean> getPlayerByIdIfOnline(int playerId) {
    return Optional.ofNullable(playersById.get(playerId));
  }

  public Optional<PlayerBean> getPlayerByNameIfOnline(String playerName) {
    return Optional.ofNullable(playersByName.get(playerName));
  }

  public Flux<PlayerBean> getPlayersByIds(Collection<Integer> playerIds) {
    List<PlayerBean> onlinePlayers = playerIds.stream()
                                              .map(this::getPlayerByIdIfOnline)
                                              .flatMap(Optional::stream)
                                              .toList();

    if (onlinePlayers.size() == playerIds.size()) {
      return Flux.fromIterable(onlinePlayers);
    }

    Set<Integer> onlineIds = onlinePlayers.stream().map(PlayerBean::getId).collect(Collectors.toSet());

    return Flux.fromIterable(playerIds)
               .filter(playerId -> !onlineIds.contains(playerId))
               .window(100)
               .flatMap(Flux::collectList)
               .flatMap(offlineIds -> {
                 ElideNavigatorOnCollection<Player> navigator = ElideNavigator.of(Player.class)
                                                                              .collection()
                                                                              .setFilter(qBuilder().intNum("id")
                                                                                                   .in(offlineIds));
                 return fafApiAccessor.getMany(navigator)
                                      .map(dto -> playerMapper.map(dto, new CycleAvoidingMappingContext()));
               })
               .concatWithValues(onlinePlayers.toArray(new PlayerBean[0]));
  }

  public Mono<PlayerBean> getPlayerByName(String playerName) {
    ElideNavigatorOnCollection<Player> navigator = ElideNavigator.of(Player.class)
                                                                 .collection()
                                                                 .setFilter(qBuilder().string("login").eq(playerName));

    Mono<PlayerBean> apiPlayer = fafApiAccessor.getMany(navigator)
                                               .next()
                                               .map(dto -> playerMapper.map(dto, new CycleAvoidingMappingContext()));

    return Mono.justOrEmpty(getPlayerByNameIfOnline(playerName)).switchIfEmpty(apiPlayer);
  }

  public Flux<NameRecordBean> getPlayerNames(PlayerBean player) {
    ElideNavigatorOnCollection<NameRecord> navigator = ElideNavigator.of(NameRecord.class)
                                                                     .collection()
                                                                     .setFilter(qBuilder().intNum("player.id")
                                                                                          .eq(player.getId()));

    return fafApiAccessor.getMany(navigator)
                         .map(dto -> playerMapper.map(dto, new CycleAvoidingMappingContext()))
                         .sort(Comparator.comparing(NameRecordBean::getChangeTime));
  }

  private void removePlayer(PlayerBean player) {
    PlayerBean removedPlayer = playersById.remove(player.getId());
    if (removedPlayer != null) {
      playersByName.remove(removedPlayer.getUsername());
      Set<Subscription> subscriptions = playerSubscriptions.remove(player);
      if (subscriptions != null) {
        subscriptions.forEach(Subscription::unsubscribe);
      }
      playerOfflineListeners.forEach(listener -> listener.accept(removedPlayer));
    }
  }

  public void addPlayerOnlineListener(Consumer<PlayerBean> listener) {
    playerOnlineListeners.add(listener);
  }

  public void addPlayerOfflineListener(Consumer<PlayerBean> listener) {
    playerOfflineListeners.add(listener);
  }
}
