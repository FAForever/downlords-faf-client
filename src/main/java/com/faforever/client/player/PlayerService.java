package com.faforever.client.player;

import com.faforever.client.avatar.AvatarBean;
import com.faforever.client.avatar.event.AvatarChangedEvent;
import com.faforever.client.chat.event.ChatMessageEvent;
import com.faforever.client.chat.event.ChatUserCategoryChangeEvent;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.Game;
import com.faforever.client.player.event.FriendJoinedGameEvent;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.remote.domain.GameType;
import com.faforever.client.remote.domain.PlayerInfo;
import com.faforever.client.remote.domain.inbound.faf.PlayerInfoMessage;
import com.faforever.client.remote.domain.inbound.faf.SocialMessage;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Assert;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.FRIEND;
import static com.faforever.client.player.SocialStatus.OTHER;
import static com.faforever.client.player.SocialStatus.SELF;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlayerService implements InitializingBean {

  private final ObservableMap<String, Player> playersByName = FXCollections.observableMap(new ConcurrentHashMap<>());
  private final ObservableMap<Integer, Player> playersById = FXCollections.observableMap(new ConcurrentHashMap<>());
  private final List<Integer> foeList = new ArrayList<>();
  private final List<Integer> friendList = new ArrayList<>();
  private final Map<Integer, List<Player>> playersByGame = new HashMap<>();

  private final FafService fafService;
  private final UserService userService;
  private final EventBus eventBus;

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
    fafService.addOnMessageListener(PlayerInfoMessage.class, this::onPlayersInfo);
    fafService.addOnMessageListener(SocialMessage.class, this::onFoeList);
  }

  public void updatePlayersInGame(Game game) {
    int gameId = game.getId();
    playersByGame.putIfAbsent(gameId, new ArrayList<>());
    synchronized (playersByGame.get(gameId)) {
      List<Player> currentPlayersInGame = playersByGame.get(gameId);
      List<Player> playersInGameToRemove = new ArrayList<>(currentPlayersInGame);

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
    Player player = getCurrentPlayer();

    AvatarBean avatar = event.getAvatar();
    if (avatar == null) {
      player.setAvatarTooltip(null);
      player.setAvatarUrl(null);
    } else {
      player.setAvatarTooltip(avatar.getDescription());
      player.setAvatarUrl(Objects.toString(avatar.getUrl(), null));
    }
  }

  @Subscribe
  public void onChatMessage(ChatMessageEvent event) {
    getPlayerByNameIfOnline(event.getMessage().getUsername()).ifPresent(this::resetIdleTime);
  }

  private void resetIdleTime(Player playerForUsername) {
    Optional.ofNullable(playerForUsername).ifPresent(player -> player.setIdleSince(Instant.now()));
  }

  private void updateGameDataForPlayer(Game game, Player player) {
    if (player.getGame() != game) {
      player.setGame(game);
      if (player.getSocialStatus() == FRIEND
          && game.getStatus() == GameStatus.OPEN
          && game.getGameType() != GameType.MATCHMAKER) {
        eventBus.post(new FriendJoinedGameEvent(player, game));
      }
    }
  }

  private void removeGameDataForPlayer(Game game, Player player) {
    if (player.getGame() == game) {
      player.setGame(null);
    }
  }

  public boolean areFriendsInGame(Game game) {
    if (game == null) {
      return false;
    }
    return getAllPlayersInGame(game).stream()
        .anyMatch(player -> friendList.contains(player.getId()));
  }

  public List<Player> getAllPlayersInGame(Game game) {
    return game.getTeams().values().stream()
        .flatMap(Collection::stream)
        .flatMap(playerName -> getPlayerByNameIfOnline(playerName).stream())
        .collect(Collectors.toList());
  }

  public boolean isCurrentPlayerInGame(Game game) {
    // TODO the following can be removed as soon as the server tells us which game a player is in.
    Player player = getCurrentPlayer();
    return getAllPlayersInGame(game).stream().anyMatch(player::equals);
  }

  public boolean isOnline(Integer playerId) {
    return playersById.containsKey(playerId);
  }

  /**
   * Gets a player for the given username. A new player is created and registered if it does not yet exist.
   */
  @VisibleForTesting
  void createOrUpdatePlayerForPlayerInfo(@NotNull PlayerInfo playerInfo) {
    Assert.checkNullArgument(playerInfo, "playerInfo must not be null");

    Player player;
    if (!playersByName.containsKey(playerInfo.getLogin())) {
      player = Player.fromPlayerInfo(playerInfo);
      int playerId = player.getId();
      playersById.put(playerId, player);
      JavaFxUtil.addListener(player.idProperty(), (observable, oldValue, newValue) -> {
        playersById.remove(oldValue.intValue());
        playersById.put(newValue.intValue(), player);
      });
      if (userService.getUsername().equals(player.getUsername())) {
        player.setSocialStatus(SELF);
      } else {
        if (friendList.contains(playerId)) {
          player.setSocialStatus(FRIEND);
        } else if (foeList.contains(playerId)) {
          player.setSocialStatus(FOE);
        } else {
          player.setSocialStatus(OTHER);
        }
      }
      playersByName.put(player.getUsername(), player);
      eventBus.post(new PlayerOnlineEvent(player));
    } else {
      player = playersByName.get(playerInfo.getLogin());
      synchronized (player) {
        player.updateFromPlayerInfo(playerInfo);
      }
    }

    player.setIdleSince(Instant.now());
  }

  public Set<String> getPlayerNames() {
    return new HashSet<>(playersByName.keySet());
  }

  public void addFriend(Player player) {
    playersByName.get(player.getUsername()).setSocialStatus(FRIEND);
    friendList.add(player.getId());
    foeList.remove((Integer) player.getId());

    player.getChatChannelUsers().forEach(chatUser -> eventBus.post(new ChatUserCategoryChangeEvent(chatUser)));
    fafService.addFriend(player);
  }

  public void removeFriend(Player player) {
    playersByName.get(player.getUsername()).setSocialStatus(OTHER);
    friendList.remove((Integer) player.getId());

    player.getChatChannelUsers().forEach(chatUser -> eventBus.post(new ChatUserCategoryChangeEvent(chatUser)));
    fafService.removeFriend(player);
  }

  public void addFoe(Player player) {
    playersByName.get(player.getUsername()).setSocialStatus(FOE);
    foeList.add(player.getId());
    friendList.remove((Integer) player.getId());

    player.getChatChannelUsers().forEach(chatUser -> eventBus.post(new ChatUserCategoryChangeEvent(chatUser)));
    fafService.addFoe(player);
  }

  public void removeFoe(Player player) {
    playersByName.get(player.getUsername()).setSocialStatus(OTHER);
    foeList.remove((Integer) player.getId());

    player.getChatChannelUsers().forEach(chatUser -> eventBus.post(new ChatUserCategoryChangeEvent(chatUser)));
    fafService.removeFoe(player);
  }

  public Player getCurrentPlayer() {
    Assert.checkNullIllegalState(userService.getOwnPlayerInfo(), "Own player not set");
    if (!playersByName.containsKey(userService.getUsername())) {
      createOrUpdatePlayerForPlayerInfo(userService.getOwnPlayerInfo());
    }
    return playersByName.get(userService.getUsername());
  }

  public CompletableFuture<List<Player>> getPlayersByIds(Collection<Integer> playerIds) {
    return fafService.getPlayersByIds(playerIds);
  }

  public CompletableFuture<Optional<Player>> getPlayerByName(String playerName) {
    return fafService.queryPlayerByName(playerName);
  }

  public Optional<Player> getPlayerByIdIfOnline(int playerId) {
    return Optional.ofNullable(playersById.get(playerId));
  }

  public Optional<Player> getPlayerByNameIfOnline(String playerName) {
    return Optional.ofNullable(playersByName.get(playerName));
  }

  private void onPlayersInfo(PlayerInfoMessage playerInfoMessage) {
    playerInfoMessage.getPlayers().forEach(this::onPlayerInfo);
  }

  private void onFoeList(SocialMessage socialMessage) {
    Optional.ofNullable(socialMessage.getFoes()).ifPresent(this::onFoeList);
    Optional.ofNullable(socialMessage.getFriends()).ifPresent(this::onFriendList);
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
      Player player = playersById.get(userId);
      if (player != null) {
        player.setSocialStatus(socialStatus);
      }
    }
  }

  private void onPlayerInfo(PlayerInfo dto) {
    createOrUpdatePlayerForPlayerInfo(dto);
  }
}
