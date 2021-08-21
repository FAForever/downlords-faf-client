package com.faforever.client.player;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.avatar.event.AvatarChangedEvent;
import com.faforever.client.chat.event.ChatMessageEvent;
import com.faforever.client.chat.event.ChatUserCategoryChangeEvent;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.PlayerMapper;
import com.faforever.client.player.event.FriendJoinedGameEvent;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Assert;
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
import javafx.collections.ObservableMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
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

  private final FafServerAccessor fafServerAccessor;
  private final FafApiAccessor fafApiAccessor;
  private final UserService userService;
  private final EventBus eventBus;
  private final PlayerMapper playerMapper;

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
    fafServerAccessor.addEventListener(com.faforever.commons.lobby.PlayerInfo.class, this::onPlayersInfo);
    fafServerAccessor.addEventListener(SocialInfo.class, this::onSocialMessage);
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
      playersInGameToRemove.forEach(player -> removeGameDataForPlayer(game, player));
      currentPlayersInGame.forEach(player -> updateGameDataForPlayer(game, player));

      if (game.getStatus() == GameStatus.CLOSED) {
        boolean removed = playersByGame.remove(gameId, currentPlayersInGame);
        if (!removed) {
          log.debug("Could not remove players list for game due to list mismatch: '{}'", gameId);
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

  private void updateGameDataForPlayer(GameBean game, PlayerBean player) {
    if (player.getGame() != game) {
      player.setGame(game);
      if (player.getSocialStatus() == FRIEND
          && game.getStatus() == GameStatus.OPEN
          && game.getGameType() != GameType.MATCHMAKER) {
        eventBus.post(new FriendJoinedGameEvent(player, game));
      }
    }
  }

  private void removeGameDataForPlayer(GameBean game, PlayerBean player) {
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
        .flatMap(playerName -> getPlayerByNameIfOnline(playerName).stream())
        .collect(Collectors.toList());
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

    PlayerBean player = playersByName.computeIfAbsent(playerInfo.getLogin(), name -> {
      int playerId = playerInfo.getId();
      PlayerBean newPlayer = new PlayerBean();
      JavaFxUtil.addListener(newPlayer.idProperty(), (observable, oldValue, newValue) -> {
        if (oldValue != null) {
          playersById.remove(oldValue);
        }
        playersById.put(newValue, newPlayer);
      });
      if (userService.getUserId().equals(playerId)) {
        newPlayer.setSocialStatus(SELF);
      } else {
        if (friendList.contains(playerId)) {
          newPlayer.setSocialStatus(FRIEND);
        } else if (foeList.contains(playerId)) {
          newPlayer.setSocialStatus(FOE);
        } else {
          newPlayer.setSocialStatus(OTHER);
        }
      }
      eventBus.post(new PlayerOnlineEvent(newPlayer));
      return newPlayer;
    });
    synchronized (player) {
      playerMapper.update(playerInfo, player);
    }
    player.setIdleSince(Instant.now());
  }

  public Set<String> getPlayerNames() {
    return new HashSet<>(playersByName.keySet());
  }

  public void addFriend(PlayerBean player) {
    playersByName.get(player.getUsername()).setSocialStatus(FRIEND);
    friendList.add(player.getId());
    foeList.remove(player.getId());

    player.getChatChannelUsers().forEach(chatUser -> eventBus.post(new ChatUserCategoryChangeEvent(chatUser)));
    fafServerAccessor.addFriend(player.getId());
  }

  public void removeFriend(PlayerBean player) {
    playersByName.get(player.getUsername()).setSocialStatus(OTHER);
    friendList.remove(player.getId());

    player.getChatChannelUsers().forEach(chatUser -> eventBus.post(new ChatUserCategoryChangeEvent(chatUser)));
    fafServerAccessor.removeFriend(player.getId());
  }

  public void addFoe(PlayerBean player) {
    playersByName.get(player.getUsername()).setSocialStatus(FOE);
    foeList.add(player.getId());
    friendList.remove(player.getId());

    player.getChatChannelUsers().forEach(chatUser -> eventBus.post(new ChatUserCategoryChangeEvent(chatUser)));
    fafServerAccessor.addFoe(player.getId());
  }

  public void removeFoe(PlayerBean player) {
    playersByName.get(player.getUsername()).setSocialStatus(OTHER);
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
    ElideNavigatorOnCollection<Player> navigator = ElideNavigator.of(Player.class).collection()
        .setFilter(qBuilder().intNum("id").in(playerIds));
    return fafApiAccessor.getMany(navigator)
        .map(dto -> playerMapper.map(dto, new CycleAvoidingMappingContext()))
        .collectList()
        .toFuture();
  }

  public CompletableFuture<Optional<PlayerBean>> getPlayerByName(String playerName) {
    ElideNavigatorOnCollection<Player> navigator = ElideNavigator.of(Player.class).collection()
        .setFilter(qBuilder().string("login").eq(playerName));
    return fafApiAccessor.getMany(navigator)
        .next()
        .map(dto -> playerMapper.map(dto, new CycleAvoidingMappingContext()))
        .toFuture()
        .thenApply(Optional::ofNullable);
  }
}
