package com.faforever.client.player;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.avatar.AvatarService;
import com.faforever.client.avatar.event.AvatarChangedEvent;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.NameRecordBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.JavaFxService;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.PlayerMapper;
import com.faforever.client.preferences.UserPrefs;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.user.UserService;
import com.faforever.client.util.RatingUtil;
import com.faforever.commons.api.dto.NameRecord;
import com.faforever.commons.api.dto.Player;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import com.faforever.commons.lobby.PlayerInfo;
import com.faforever.commons.lobby.SocialInfo;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.scene.image.Image;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.FRIEND;
import static com.faforever.client.player.SocialStatus.OTHER;
import static com.faforever.client.player.SocialStatus.SELF;
import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlayerService implements InitializingBean {

  private final ObservableMap<String, PlayerBean> playersByName = FXCollections.synchronizedObservableMap(FXCollections.observableHashMap());
  private final ObservableMap<Integer, PlayerBean> playersById = FXCollections.synchronizedObservableMap(FXCollections.observableHashMap());
  private final List<Integer> foeList = new ArrayList<>();
  private final List<Integer> friendList = new ArrayList<>();
  private final ReadOnlyObjectWrapper<PlayerBean> currentPlayer = new ReadOnlyObjectWrapper<>();
  private final Object lock = new Object();

  private final FafServerAccessor fafServerAccessor;
  private final FafApiAccessor fafApiAccessor;
  private final UserService userService;
  private final AvatarService avatarService;
  private final EventBus eventBus;
  private final PlayerMapper playerMapper;
  private final JavaFxService javaFxService;
  private final UserPrefs userPrefs;

  private final MapChangeListener<Integer, PlayerBean> playerByIdChangeListener = change -> {
    if (change.wasAdded()) {
      PlayerBean player = change.getValueAdded();
      playersByName.put(player.getUsername(), player);
    } else if (change.wasRemoved()) {
      PlayerBean player = change.getValueRemoved();
      playersByName.remove(player.getUsername());
    }
  };

  private ObservableMap<Integer, String> notesByPlayerId;

  @Override
  public void afterPropertiesSet() {
    playersById.addListener(playerByIdChangeListener);

    eventBus.register(this);

    fafServerAccessor.getEvents(PlayerInfo.class)
        .publishOn(javaFxService.getFxApplicationScheduler())
        .flatMap(playerInfo -> Flux.fromIterable(playerInfo.getPlayers()))
        .flatMap(player -> Mono.zip(Mono.just(player), Mono.justOrEmpty(playersById.get(player.getId()))
            .switchIfEmpty(initializePlayer(player))))
        .publishOn(javaFxService.getFxApplicationScheduler())
        .map(TupleUtils.function(playerMapper::update))
        .doOnError(throwable -> log.error("Error processing player", throwable))
        .retry()
        .subscribe();

    fafServerAccessor.getEvents(SocialInfo.class)
        .flatMap(socialInfo -> {
          Flux<PlayerBean> friendFlux = Mono.just(socialInfo.getFriends()).doOnNext(ids -> {
            friendList.clear();
            friendList.addAll(ids);
          }).flatMapMany(Flux::fromIterable).mapNotNull(playersById::get);

          Flux<PlayerBean> foeFlux = Mono.just(socialInfo.getFoes()).doOnNext(ids -> {
            foeList.clear();
            foeList.addAll(ids);
          }).flatMapMany(Flux::fromIterable).mapNotNull(playersById::get);

          return Flux.merge(friendFlux, foeFlux);
        })
        .doOnError(throwable -> log.error("Error processing social info", throwable))
        .retry()
        .publishOn(javaFxService.getFxApplicationScheduler())
        .subscribe(this::setPlayerSocialStatus);

    notesByPlayerId = userPrefs.getNotesByPlayerId();
    JavaFxUtil.addListener(notesByPlayerId, (MapChangeListener<Integer, String>) change -> {
      PlayerBean player = playersById.get(change.getKey());
      if (change.wasAdded()) {
        player.setNote(change.getValueAdded());
      } else if (change.wasRemoved()) {
        player.setNote(null);
      }
    });

    currentPlayer.bind(userService.ownPlayerProperty().map(this::createOrUpdatePlayerForPlayerInfo));
  }

  @Subscribe
  public void onAvatarChanged(AvatarChangedEvent event) {
    PlayerBean player = getCurrentPlayer();

    player.setAvatar(event.getAvatar());
  }

  public ObservableValue<Double> getAverageRatingPropertyForGame(GameBean gameBean) {
    return gameBean.activePlayersInGameProperty()
        .map(ids -> ids.stream()
            .map(this::getPlayerByIdIfOnline)
            .flatMap(Optional::stream)
            .mapToInt(player -> RatingUtil.getLeaderboardRating(player, gameBean.getLeaderboard()))
            .average()
            .orElse(0));
  }

  public double getAverageRatingForGame(GameBean gameBean) {
    return getAverageRatingPropertyForGame(gameBean).getValue();
  }

  public boolean areFriendsInGame(GameBean game) {
    if (game == null) {
      return false;
    }
    return game.getAllPlayersInGame().stream().anyMatch(friendList::contains);
  }

  public Optional<Image> getCurrentAvatarByPlayerName(String name) {
    return Optional.ofNullable(playersByName.get(name)).map(PlayerBean::getAvatar).map(avatarService::loadAvatar);
  }

  public boolean isCurrentPlayerInGame(GameBean game) {
    // TODO the following can be removed as soon as the server tells us which game a player is in.
    return game.getAllPlayersInGame().contains(userService.getUserId());
  }

  public boolean isOnline(Integer playerId) {
    return playersById.containsKey(playerId);
  }

  /**
   * Gets a player for the given username. A new player is created and registered if it does not yet exist.
   */
  @VisibleForTesting
  PlayerBean createOrUpdatePlayerForPlayerInfo(@NonNull com.faforever.commons.lobby.Player playerInfo) {
    return playersById.compute(playerInfo.getId(), (id, knownPlayer) -> {
      if (knownPlayer == null) {
        PlayerBean newPlayer = new PlayerBean();
        newPlayer.setId(id);
        newPlayer.setUsername(playerInfo.getLogin());
        newPlayer.setNote(notesByPlayerId.get(id));
        setPlayerSocialStatus(newPlayer);
        return playerMapper.update(playerInfo, newPlayer);
      } else {
        return playerMapper.update(playerInfo, knownPlayer);
      }
    });
  }

  private Mono<PlayerBean> initializePlayer(com.faforever.commons.lobby.Player player) {
    return Mono.fromCallable(() -> {
          PlayerBean newPlayer = new PlayerBean();
          newPlayer.setId(player.getId());
          newPlayer.setUsername(player.getLogin());
          newPlayer.setNote(notesByPlayerId.get(player.getId()));
          setPlayerSocialStatus(newPlayer);
          return newPlayer;
        }).publishOn(javaFxService.getFxApplicationScheduler())
        .doOnNext(playerBean -> playersById.put(playerBean.getId(), playerBean))
        .doOnNext(playerBean -> eventBus.post(new PlayerOnlineEvent(playerBean)));
  }

  public void updateNote(PlayerBean player, String text) {
    if (StringUtils.isBlank(text)) {
      removeNote(player);
    } else {
      String normalizedText = text.replaceAll("\\n\\n+", "\n");
      notesByPlayerId.put(player.getId(), normalizedText);
    }
  }

  public void removeNote(PlayerBean player) {
    notesByPlayerId.remove(player.getId());
  }

  public ObservableMap<Integer, String> getNotesByPlayerId() {
    return notesByPlayerId;
  }

  private void setPlayerSocialStatus(PlayerBean player) {
    Integer id = player.getId();
    if (userService.getUserId().equals(id)) {
      player.setSocialStatus(SELF);
    } else if (friendList.contains(id)) {
      player.setSocialStatus(FRIEND);
    } else if (foeList.contains(id)) {
      player.setSocialStatus(FOE);
    } else {
      player.setSocialStatus(OTHER);
    }
  }

  public Set<String> getPlayerNames() {
    return new HashSet<>(playersByName.keySet());
  }

  public void addFriend(PlayerBean player) {
    player.setSocialStatus(FRIEND);
    friendList.add(player.getId());
    foeList.remove(player.getId());
    fafServerAccessor.addFriend(player.getId());
  }

  public void removeFriend(PlayerBean player) {
    player.setSocialStatus(OTHER);
    friendList.remove(player.getId());
    fafServerAccessor.removeFriend(player.getId());
  }

  public void addFoe(PlayerBean player) {
    player.setSocialStatus(FOE);
    foeList.add(player.getId());
    friendList.remove(player.getId());
    fafServerAccessor.addFoe(player.getId());
  }

  public void removeFoe(PlayerBean player) {
    player.setSocialStatus(OTHER);
    foeList.remove(player.getId());
    fafServerAccessor.removeFoe(player.getId());
  }

  public PlayerBean getCurrentPlayer() {
    return currentPlayer.get();
  }

  public Optional<PlayerBean> getPlayerByIdIfOnline(int playerId) {
    return Optional.ofNullable(playersById.get(playerId));
  }

  public Optional<PlayerBean> getPlayerByNameIfOnline(String playerName) {
    return Optional.ofNullable(playersByName.get(playerName));
  }

  public CompletableFuture<List<PlayerBean>> getPlayersByIds(Collection<Integer> playerIds) {
    Set<Integer> onlineIds = new HashSet<>();

    List<PlayerBean> players = playerIds.stream()
        .map(this::getPlayerByIdIfOnline)
        .flatMap(Optional::stream)
        .peek(playerBean -> onlineIds.add(playerBean.getId()))
        .collect(Collectors.toCollection(ArrayList::new));

    if (players.size() == playerIds.size()) {
      return CompletableFuture.completedFuture(players);
    }

    Set<Integer> offlineIds = playerIds.stream()
        .filter(playerId -> !onlineIds.contains(playerId))
        .collect(Collectors.toSet());

    ElideNavigatorOnCollection<Player> navigator = ElideNavigator.of(Player.class)
        .collection()
        .setFilter(qBuilder().intNum("id").in(offlineIds));
    return fafApiAccessor.getMany(navigator)
        .map(dto -> playerMapper.map(dto, new CycleAvoidingMappingContext()))
        .doOnNext(player -> {
          if (friendList.contains(player.getId())) {
            player.setSocialStatus(FRIEND);
          } else if (foeList.contains(player.getId())) {
            player.setSocialStatus(FOE);
          }
          players.add(player);
        })
        .then(Mono.just(players))
        .toFuture();
  }

  public CompletableFuture<Optional<PlayerBean>> getPlayerByName(String playerName) {
    Optional<PlayerBean> onlinePlayer = getPlayerByNameIfOnline(playerName);
    if (onlinePlayer.isPresent()) {
      return CompletableFuture.completedFuture(onlinePlayer);
    }

    ElideNavigatorOnCollection<Player> navigator = ElideNavigator.of(Player.class)
        .collection()
        .setFilter(qBuilder().string("login").eq(playerName));
    return fafApiAccessor.getMany(navigator)
        .next()
        .map(dto -> playerMapper.map(dto, new CycleAvoidingMappingContext()))
        .doOnNext(player -> {
          if (friendList.contains(player.getId())) {
            player.setSocialStatus(FRIEND);
          } else if (foeList.contains(player.getId())) {
            player.setSocialStatus(FOE);
          }
        })
        .toFuture()
        .thenApply(Optional::ofNullable);
  }

  public CompletableFuture<List<NameRecordBean>> getPlayerNames(PlayerBean player) {
    ElideNavigatorOnCollection<NameRecord> navigator = ElideNavigator.of(NameRecord.class)
        .collection()
        .setFilter(qBuilder().intNum("player.id").eq(player.getId()));

    return fafApiAccessor.getMany(navigator)
        .map(dto -> playerMapper.map(dto, new CycleAvoidingMappingContext()))
        .sort(Comparator.comparing(NameRecordBean::getChangeTime))
        .collectList()
        .toFuture();
  }

  public void removePlayerIfOnline(String username) {
    synchronized (lock) {
      getPlayerByNameIfOnline(username).ifPresent(player -> {
        playersById.remove(player.getId());

        eventBus.post(new PlayerOfflineEvent(player));
      });
    }
  }
}
