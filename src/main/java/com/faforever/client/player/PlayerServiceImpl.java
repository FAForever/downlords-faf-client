package com.faforever.client.player;

import com.faforever.client.chat.SocialStatus;
import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.chat.avatar.event.AvatarChangedEvent;
import com.faforever.client.chat.event.ChatMessageEvent;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameService;
import com.faforever.client.player.event.FriendJoinedGameEvent;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.GameState;
import com.faforever.client.remote.domain.PlayersMessage;
import com.faforever.client.remote.domain.SocialMessage;
import com.faforever.client.user.UserService;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.faforever.client.util.Assert;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.faforever.client.chat.SocialStatus.FOE;
import static com.faforever.client.chat.SocialStatus.FRIEND;
import static com.faforever.client.chat.SocialStatus.OTHER;
import static com.faforever.client.chat.SocialStatus.SELF;

@Lazy
@Service
public class PlayerServiceImpl implements PlayerService {

  private final ObservableMap<String, Player> playersByName;
  private final ObservableMap<Integer, Player> playersById;
  private final List<Integer> foeList;
  private final List<Integer> friendList;
  private final ObjectProperty<Player> currentPlayer;

  private final FafService fafService;
  private final UserService userService;
  private final GameService gameService;
  private final EventBus eventBus;

  @Inject
  public PlayerServiceImpl(FafService fafService, UserService userService, GameService gameService, EventBus eventBus) {
    this.fafService = fafService;
    this.userService = userService;
    this.gameService = gameService;
    this.eventBus = eventBus;


    playersByName = FXCollections.observableHashMap();
    playersById = FXCollections.observableHashMap();
    friendList = new ArrayList<>();
    foeList = new ArrayList<>();
    currentPlayer = new SimpleObjectProperty<>();
  }

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
    fafService.addOnMessageListener(PlayersMessage.class, this::onPlayersInfo);
    fafService.addOnMessageListener(SocialMessage.class, this::onFoeList);

    gameService.getGames().addListener((ListChangeListener<? super Game>) listChange -> {
      while (listChange.next()) {
        listChange.getRemoved().forEach(this::updateGameForPlayersInGame);

        if (listChange.wasUpdated()) {
          for (int i = listChange.getFrom(); i < listChange.getTo(); i++) {
            updateGameForPlayersInGame(listChange.getList().get(i));
          }
        }

        listChange.getAddedSubList().forEach(this::updateGameForPlayersInGame);
      }
    });
  }

  @Subscribe
  public void onLoginSuccess(LoginSuccessEvent event) {
    Player player = createAndGetPlayerForUsername(event.getUsername());
    player.setId(event.getUserId());
    currentPlayer.set(player);
    player.setIdleSince(Instant.now());
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
    resetIdleTime(getPlayerForUsername(event.getMessage().getUsername()));
  }

  private void resetIdleTime(Player playerForUsername) {
    Optional.ofNullable(playerForUsername).ifPresent(player -> player.setIdleSince(Instant.now()));
  }

  private void updateGameForPlayersInGame(Game game) {
    ObservableMap<String, List<String>> teams = game.getTeams();
    synchronized (teams) {
      teams.forEach((team, players) -> updateGamePlayers(players, game));
    }
  }

  private void updateGamePlayers(List<String> players, Game game) {
    players.stream()
        .map(this::getPlayerForUsername)
        .filter(Objects::nonNull)
        .forEach(player -> {
          resetIdleTime(player);
          player.setGame(game);
          if (game == null) {
            return;
          }
          GameState gameState = game.getStatus();
          if (player.getSocialStatus() == FRIEND) {
            if (gameState == GameState.OPEN) {
              eventBus.post(new FriendJoinedGameEvent(player));
            } else if (gameState == GameState.PLAYING) {
//              eventBus.post(new FriendPlaysGameEvent(player));
            }
          }
        });
  }

  @Override
  public Player getPlayerForUsername(String username) {
    return playersByName.get(username);
  }

  @Override
  public Player createAndGetPlayerForUsername(@NotNull String username) {
    Assert.checkNullArgument(username, "username must not be null");

    synchronized (playersByName) {
      if (!playersByName.containsKey(username)) {
        Player player = new Player(username);
        player.idProperty().addListener((observable, oldValue, newValue) -> {
          synchronized (playersById) {
            playersById.remove(oldValue.intValue());
            playersById.put(newValue.intValue(), player);
          }
        });
        playersByName.put(username, player);
      }
    }

    return playersByName.get(username);
  }

  @Override
  public Set<String> getPlayerNames() {
    return playersByName.keySet();
  }

  @Override
  public void addFriend(Player player) {
    playersByName.get(player.getUsername()).setSocialStatus(FRIEND);
    friendList.add(player.getId());
    foeList.remove((Integer) player.getId());

    fafService.addFriend(player);
  }

  @Override
  public void removeFriend(Player player) {
    playersByName.get(player.getUsername()).setSocialStatus(OTHER);
    friendList.remove((Integer) player.getId());

    fafService.removeFriend(player);
  }

  @Override
  public void addFoe(Player player) {
    playersByName.get(player.getUsername()).setSocialStatus(FOE);
    foeList.add(player.getId());
    friendList.remove((Integer) player.getId());

    fafService.addFoe(player);
  }

  @Override
  public void removeFoe(Player player) {
    playersByName.get(player.getUsername()).setSocialStatus(OTHER);
    foeList.remove((Integer) player.getId());

    fafService.removeFoe(player);
  }

  @Override
  public Player getCurrentPlayer() {
    Assert.checkNullIllegalState(currentPlayer.get(), "currentPlayer has not yet been set");
    return currentPlayer.get();
  }

  @Override
  public ReadOnlyObjectProperty<Player> currentPlayerProperty() {
    return currentPlayer;
  }

  private void onPlayersInfo(PlayersMessage playersMessage) {
    playersMessage.getPlayers().forEach(this::onPlayerInfo);
  }

  private void onFoeList(SocialMessage socialMessage) {
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

    synchronized (playersById) {
      for (Integer userId : socialList) {
        Player player = playersById.get(userId);
        if (player != null) {
          player.setSocialStatus(socialStatus);
        }
      }
    }
  }

  private void onPlayerInfo(com.faforever.client.remote.domain.Player player) {
    if (player.getLogin().equalsIgnoreCase(userService.getUsername())) {
      Player playerInfoBean = getCurrentPlayer();
      playerInfoBean.updateFromPlayerInfo(player);
      playerInfoBean.setSocialStatus(SELF);
    } else {
      Player playerInfoBean = createAndGetPlayerForUsername(player.getLogin());

      if (friendList.contains(player.getId())) {
        playerInfoBean.setSocialStatus(FRIEND);
      } else if (foeList.contains(player.getId())) {
        playerInfoBean.setSocialStatus(FOE);
      } else {
        playerInfoBean.setSocialStatus(OTHER);
      }

      playerInfoBean.updateFromPlayerInfo(player);
    }
  }
}
