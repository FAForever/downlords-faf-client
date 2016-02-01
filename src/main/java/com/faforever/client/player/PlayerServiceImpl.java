package com.faforever.client.player;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.GameService;
import com.faforever.client.legacy.GameStatus;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.Player;
import com.faforever.client.legacy.domain.PlayersMessage;
import com.faforever.client.legacy.domain.SocialMessage;
import com.faforever.client.remote.FafService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Assert;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.faforever.client.chat.SocialStatus.FOE;
import static com.faforever.client.chat.SocialStatus.FRIEND;
import static com.faforever.client.chat.SocialStatus.OTHER;
import static com.faforever.client.chat.SocialStatus.SELF;

public class PlayerServiceImpl implements PlayerService {

  private static final Lock CURRENT_PLAYER_LOCK = new ReentrantLock();

  /**
   * Maps usernames to players.
   */
  private final ObservableMap<String, PlayerInfoBean> players;

  @Resource
  FafService fafService;
  @Resource
  UserService userService;
  @Resource
  GameService gameService;

  private List<String> foeList;
  private List<String> friendList;
  private ObjectProperty<PlayerInfoBean> currentPlayer;

  public PlayerServiceImpl() {
    players = FXCollections.observableHashMap();
    friendList = new ArrayList<>();
    foeList = new ArrayList<>();
    currentPlayer = new SimpleObjectProperty<>();
  }

  @PostConstruct
  void init() {
    fafService.addOnMessageListener(PlayersMessage.class, this::onPlayersInfo);
    fafService.addOnMessageListener(SocialMessage.class, this::onFoeList);
    gameService.addOnGameInfoBeanListener(change -> {
      while (change.next()) {
        for (GameInfoBean gameInfoBean : change.getRemoved()) {
          gameInfoBean.setStatus(GameState.CLOSED);
          gameInfoBean.getTeams().forEach((team, players) -> updatePlayerInfoBean(players, gameInfoBean));
        }
        for (GameInfoBean gameInfoBean : change.getAddedSubList()) {
          gameInfoBean.getTeams().forEach((team, players) -> updatePlayerInfoBean(players, gameInfoBean));
          gameInfoBean.statusProperty().addListener(change2 -> {
            gameInfoBean.getTeams().forEach((team, updatedPlayer) -> updatePlayerInfoBean(updatedPlayer, gameInfoBean));
          });
        }
      }
    });
  }

  //FIXME ugly fix until host can be resolved from gamestate
  private void updatePlayerInfoBean(List<String> players, GameInfoBean gameInfoBean) {
    for (String player : players) {
      PlayerInfoBean playerInfoBean = getPlayerForUsername(player);
      if (playerInfoBean == null) {
        continue;
      }
      updatePlayerGameStatus(playerInfoBean, GameStatus.getFromGameState(gameInfoBean.getStatus()));
      playerInfoBean.setGameUid(gameInfoBean.getUid());
    }
    if (GameStatus.getFromGameState(gameInfoBean.getStatus()) == GameStatus.LOBBY) {
      PlayerInfoBean host = getPlayerForUsername(gameInfoBean.getHost());
      updatePlayerGameStatus(host, GameStatus.HOST);
    }
  }

  @Override
  public void updatePlayerGameStatus(PlayerInfoBean playerInfoBean, GameStatus gameStatus) {
    if (playerInfoBean != null && playerInfoBean.getGameStatus() != gameStatus) {
      //FIXME until api, host is set twice or ugly code, I chose to set twice
      playerInfoBean.setGameStatus(gameStatus);
    }
  }

  @Override
  public PlayerInfoBean getPlayerForUsername(String username) {
    return players.get(username);
  }

  @Override
  public PlayerInfoBean registerAndGetPlayerForUsername(@NotNull String username) {
    Assert.checkNull(username, "username must not be null");

    if (!players.containsKey(username)) {
      players.put(username, new PlayerInfoBean(username));
    }

    return players.get(username);
  }

  @Override
  public Set<String> getPlayerNames() {
    return players.keySet();
  }

  @Override
  public void addFriend(PlayerInfoBean player) {
    players.get(player.getUsername()).setSocialStatus(FRIEND);
    friendList.add(player.getUsername());
    foeList.remove(player.getUsername());

    fafService.addFriend(player);
  }

  @Override
  public void removeFriend(PlayerInfoBean player) {
    players.get(player.getUsername()).setSocialStatus(OTHER);
    friendList.remove(player.getUsername());

    fafService.removeFriend(player);
  }

  @Override
  public void addFoe(PlayerInfoBean player) {
    players.get(player.getUsername()).setSocialStatus(FOE);
    foeList.add(player.getUsername());
    friendList.remove(player.getUsername());

    fafService.addFoe(player);
  }

  @Override
  public void removeFoe(PlayerInfoBean player) {
    players.get(player.getUsername()).setSocialStatus(OTHER);
    foeList.remove(player.getUsername());

    fafService.removeFoe(player);
  }

  @Override
  public PlayerInfoBean getCurrentPlayer() {
    CURRENT_PLAYER_LOCK.lock();
    try {
      if (currentPlayer.get() == null) {
        currentPlayer.set(registerAndGetPlayerForUsername(userService.getUsername()));
      }
    } finally {
      CURRENT_PLAYER_LOCK.unlock();
    }
    return currentPlayer.get();
  }

  @Override
  public ReadOnlyObjectProperty<PlayerInfoBean> currentPlayerProperty() {
    return currentPlayer;
  }

  private void onPlayersInfo(PlayersMessage playersMessage) {
    playersMessage.getPlayers().forEach(this::onPlayerInfo);
  }

  private void onFoeList(SocialMessage socialMessage) {
    onFoeList(socialMessage.getFoes());
    onFriendList(socialMessage.getFriends());
  }

  private void onFoeList(List<String> foes) {
    this.foeList = foes;

    for (String foe : foes) {
      PlayerInfoBean playerInfoBean = players.get(foe);
      if (playerInfoBean != null) {
        playerInfoBean.setSocialStatus(FOE);
      }
    }
  }

  private void onFriendList(List<String> friends) {
    this.friendList = friends;

    for (String friend : friendList) {
      PlayerInfoBean playerInfoBean = players.get(friend);
      if (playerInfoBean != null) {
        playerInfoBean.setSocialStatus(FRIEND);
      }
    }
  }

  private void onPlayerInfo(Player player) {
    if (player.getLogin().equals(userService.getUsername())) {
      PlayerInfoBean playerInfoBean = getCurrentPlayer();
      playerInfoBean.updateFromPlayerInfo(player);
      playerInfoBean.setSocialStatus(SELF);
    } else {
      PlayerInfoBean playerInfoBean = registerAndGetPlayerForUsername(player.getLogin());

      if (friendList.contains(player.getLogin())) {
        playerInfoBean.setSocialStatus(FRIEND);
      } else if (foeList.contains(player.getLogin())) {
        playerInfoBean.setSocialStatus(FOE);
      } else {
        playerInfoBean.setSocialStatus(OTHER);
      }

      playerInfoBean.updateFromPlayerInfo(player);
    }
  }
}
