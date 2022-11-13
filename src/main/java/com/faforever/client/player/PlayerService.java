package com.faforever.client.player;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.avatar.AvatarService;
import com.faforever.client.avatar.event.AvatarChangedEvent;
import com.faforever.client.chat.event.ChatMessageEvent;
import com.faforever.client.chat.event.ChatUserCategoryChangeEvent;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.NameRecordBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.PlayerMapper;
import com.faforever.client.player.event.FriendJoinedGameEvent;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Assert;
import com.faforever.commons.api.dto.NameRecord;
import com.faforever.commons.api.dto.Player;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import com.faforever.commons.lobby.GameStatus;
import com.faforever.commons.lobby.GameType;
import com.faforever.commons.lobby.SocialInfo;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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

  private final ObservableMap<String, PlayerBean> playersByName = FXCollections.observableMap(new ConcurrentHashMap<>());
  private final ObservableMap<Integer, PlayerBean> playersById = FXCollections.observableMap(new ConcurrentHashMap<>());
  private final List<Integer> foeList = new ArrayList<>();
  private final List<Integer> friendList = new ArrayList<>();
  private final Map<Integer, List<PlayerBean>> playersByGame = new HashMap<>();
  private ObservableMap<Integer, String> notesByPlayerId;

  private final FafServerAccessor fafServerAccessor;
  private final FafApiAccessor fafApiAccessor;
  private final UserService userService;
  private final AvatarService avatarService;
  private final EventBus eventBus;
  private final PlayerMapper playerMapper;
  private final PreferencesService preferencesService;

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
    fafServerAccessor.addEventListener(com.faforever.commons.lobby.PlayerInfo.class, this::onPlayersInfo);
    fafServerAccessor.addEventListener(SocialInfo.class, this::onSocialMessage);
    notesByPlayerId = preferencesService.getPreferences().getUser().getNotesByPlayerId();
    JavaFxUtil.addListener(notesByPlayerId, (MapChangeListener<Integer, String>) change -> {
      PlayerBean player = playersById.get(change.getKey());
      if (change.wasAdded()) {
        player.setNote(change.getValueAdded());
      } else if (change.wasRemoved()) {
        player.setNote("");
      }
      preferencesService.storeInBackground();
    });
  }

  public void updatePlayersInGame(GameBean game) {
    int gameId = game.getId();
    playersByGame.putIfAbsent(gameId, new ArrayList<>());
    synchronized (playersByGame.get(gameId)) {
      List<PlayerBean> currentPlayersInGame = playersByGame.get(gameId);
      List<PlayerBean> playersInGameToRemove = new ArrayList<>(currentPlayersInGame);

      currentPlayersInGame.clear();
      currentPlayersInGame.addAll(getAllPlayersInGame(game));

      playersInGameToRemove.removeAll(currentPlayersInGame);
      playersInGameToRemove.forEach(player -> removeGameFromPlayer(game, player));
      currentPlayersInGame.forEach(player -> addGameToPlayer(game, player));

      if (game.getStatus() == GameStatus.CLOSED) {
        boolean removed = playersByGame.remove(gameId, currentPlayersInGame);
        if (!removed) {
          log.warn("Could not remove players list for game due to list mismatch: '{}'", gameId);
        }
      }
    }
  }

  @Subscribe
  public void onAvatarChanged(AvatarChangedEvent event) {
    PlayerBean player = getCurrentPlayer();

    player.setAvatar(event.getAvatar());
  }

  @Subscribe
  public void onChatMessage(ChatMessageEvent event) {
    getPlayerByNameIfOnline(event.getMessage().getUsername()).ifPresent(this::resetIdleTime);
  }

  private void resetIdleTime(PlayerBean playerForUsername) {
    Optional.ofNullable(playerForUsername).ifPresent(player -> player.setIdleSince(Instant.now()));
  }

  private void addGameToPlayer(GameBean game, PlayerBean player) {
    if (player.getGame() != game) {
      player.setGame(game);
      if (player.getSocialStatus() == FRIEND
          && game.getStatus() == GameStatus.OPEN
          && game.getGameType() != GameType.MATCHMAKER) {
        eventBus.post(new FriendJoinedGameEvent(player, game));
      }
    }
  }

  private void removeGameFromPlayer(GameBean game, PlayerBean player) {
    if (player.getGame() == game) {
      player.setGame(null);
    }
  }

  public boolean areFriendsInGame(GameBean game) {
    if (game == null) {
      return false;
    }
    return getAllPlayersInGame(game).stream()
        .anyMatch(player -> friendList.contains(player.getId()));
  }

  public List<PlayerBean> getAllPlayersInGame(GameBean game) {
    return game.getTeams().values().stream()
        .flatMap(Collection::stream)
        .flatMap(playerId -> getPlayerByIdIfOnline(playerId).stream())
        .collect(Collectors.toList());
  }

  public Optional<Image> getCurrentAvatarByPlayerName(String name) {
    return Optional.ofNullable(playersByName.get(name)).map(PlayerBean::getAvatar).map(avatarService::loadAvatar);
  }

  public boolean isCurrentPlayerInGame(GameBean game) {
    // TODO the following can be removed as soon as the server tells us which game a player is in.
    PlayerBean player = getCurrentPlayer();
    return getAllPlayersInGame(game).stream().anyMatch(player::equals);
  }

  public boolean isOnline(Integer playerId) {
    return playersById.containsKey(playerId);
  }

  /**
   * Gets a player for the given username. A new player is created and registered if it does not yet exist.
   */
  @VisibleForTesting
  void createOrUpdatePlayerForPlayerInfo(@NotNull com.faforever.commons.lobby.Player playerInfo) {
    Assert.checkNullArgument(playerInfo, "playerInfo must not be null");

    PlayerBean player;
    synchronized (playersById) {
      player = playersById.computeIfAbsent(playerInfo.getId(), id -> {
        PlayerBean newPlayer = new PlayerBean();
        newPlayer.setUsername(playerInfo.getLogin());
        JavaFxUtil.addAndTriggerListener(newPlayer.usernameProperty(), observable -> playersByName.put(newPlayer.getUsername(), newPlayer));
        setPlayerSocialStatus(id, newPlayer);
        newPlayer.setNote(notesByPlayerId.getOrDefault(id, ""));
        return newPlayer;
      });
    }

    synchronized (player) {
      playerMapper.update(playerInfo, player);
    }

    if (player.getIdleSince() == null) {
      eventBus.post(new PlayerOnlineEvent(player));
    }

    resetIdleTime(player);
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

  private void setPlayerSocialStatus(Integer id, PlayerBean player) {
    if (userService.getUserId().equals(id)) {
      player.setSocialStatus(SELF);
    } else {
      if (friendList.contains(id)) {
        player.setSocialStatus(FRIEND);
      } else if (foeList.contains(id)) {
        player.setSocialStatus(FOE);
      } else {
        player.setSocialStatus(OTHER);
      }
    }
  }

  public Set<String> getPlayerNames() {
    return new HashSet<>(playersByName.keySet());
  }

  public void addFriend(PlayerBean player) {
    player.setSocialStatus(FRIEND);
    friendList.add(player.getId());
    foeList.remove(player.getId());

    player.getChatChannelUsers().forEach(chatUser -> eventBus.post(new ChatUserCategoryChangeEvent(chatUser)));
    fafServerAccessor.addFriend(player.getId());
  }

  public void removeFriend(PlayerBean player) {
    player.setSocialStatus(OTHER);
    friendList.remove(player.getId());

    player.getChatChannelUsers().forEach(chatUser -> eventBus.post(new ChatUserCategoryChangeEvent(chatUser)));
    fafServerAccessor.removeFriend(player.getId());
  }

  public void addFoe(PlayerBean player) {
    player.setSocialStatus(FOE);
    foeList.add(player.getId());
    friendList.remove(player.getId());

    player.getChatChannelUsers().forEach(chatUser -> eventBus.post(new ChatUserCategoryChangeEvent(chatUser)));
    fafServerAccessor.addFoe(player.getId());
  }

  public void removeFoe(PlayerBean player) {
    player.setSocialStatus(OTHER);
    foeList.remove(player.getId());

    player.getChatChannelUsers().forEach(chatUser -> eventBus.post(new ChatUserCategoryChangeEvent(chatUser)));
    fafServerAccessor.removeFoe(player.getId());
  }

  public PlayerBean getCurrentPlayer() {
    Assert.checkNullIllegalState(userService.getOwnPlayer(), "Own player not set");
    if (!playersByName.containsKey(userService.getUsername())) {
      createOrUpdatePlayerForPlayerInfo(userService.getOwnPlayer());
    }
    return playersByName.get(userService.getUsername());
  }

  public Optional<PlayerBean> getPlayerByIdIfOnline(int playerId) {
    return Optional.ofNullable(playersById.get(playerId));
  }

  public Optional<PlayerBean> getPlayerByNameIfOnline(String playerName) {
    return Optional.ofNullable(playersByName.get(playerName));
  }

  private void onPlayersInfo(com.faforever.commons.lobby.PlayerInfo playerInfoMessage) {
    playerInfoMessage.getPlayers().forEach(this::onPlayerInfo);
  }

  private void onSocialMessage(SocialInfo socialMessage) {
    onFoeList(socialMessage.getFoes());
    onFriendList(socialMessage.getFriends());
  }

  private void onFoeList(List<Integer> foes) {
    updateSocialList(foeList, foes, FOE);
  }

  private void onFriendList(List<Integer> friends) {
    updateSocialList(friendList, friends, FRIEND);
  }

  private void updateSocialList(List<Integer> socialList, List<Integer> newValues, SocialStatus socialStatus) {
    socialList.clear();
    socialList.addAll(newValues);

    for (Integer userId : socialList) {
      PlayerBean player = playersById.get(userId);
      if (player != null) {
        player.setSocialStatus(socialStatus);
      }
    }
  }

  private void onPlayerInfo(com.faforever.commons.lobby.Player dto) {
    createOrUpdatePlayerForPlayerInfo(dto);
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

    Set<Integer> offlineIds = playerIds.stream().filter(playerId -> !onlineIds.contains(playerId)).collect(Collectors.toSet());

    ElideNavigatorOnCollection<Player> navigator = ElideNavigator.of(Player.class).collection()
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

    ElideNavigatorOnCollection<Player> navigator = ElideNavigator.of(Player.class).collection()
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
        }).toFuture()
        .thenApply(Optional::ofNullable);
  }

  public CompletableFuture<List<NameRecordBean>> getPlayerNames(PlayerBean player) {
    ElideNavigatorOnCollection<NameRecord> navigator = ElideNavigator.of(NameRecord.class).collection()
        .setFilter(qBuilder().intNum("player.id").eq(player.getId()));

    return fafApiAccessor.getMany(navigator)
        .map(dto -> playerMapper.map(dto, new CycleAvoidingMappingContext()))
        .sort(Comparator.comparing(NameRecordBean::getChangeTime))
        .collectList()
        .toFuture();
  }

  @Subscribe
  public void onPlayerOffline(PlayerOfflineEvent event) {
    PlayerBean player = event.getPlayer();
    synchronized (playersById) {
      playersById.remove(player.getId());
    }

    synchronized (playersByName) {
      playersByName.remove(player.getUsername());
    }
  }
}
