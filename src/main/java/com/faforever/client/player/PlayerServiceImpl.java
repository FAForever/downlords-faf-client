package com.faforever.client.player;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.OnFoeListListener;
import com.faforever.client.legacy.OnFriendListListener;
import com.faforever.client.legacy.OnPlayerInfoListener;
import com.faforever.client.legacy.domain.PlayerInfo;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Assert;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PlayerServiceImpl implements PlayerService, OnPlayerInfoListener, OnFoeListListener, OnFriendListListener {

  private static final Lock CURRENT_PLAYER_LOCK = new ReentrantLock();

  private final ObservableMap<String, PlayerInfoBean> players;
  @Autowired
  LobbyServerAccessor lobbyServerAccessor;
  @Autowired
  UserService userService;
  private List<String> foeList;
  private List<String> friendList;
  private PlayerInfoBean currentPlayer;

  public PlayerServiceImpl() {
    players = FXCollections.observableHashMap();
    friendList = new ArrayList<>();
    foeList = new ArrayList<>();
  }

  @PostConstruct
  void init() {
    lobbyServerAccessor.setOnPlayerInfoMessageListener(this);
    lobbyServerAccessor.setOnFoeListListener(this);
    lobbyServerAccessor.setOnFriendListListener(this);
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
  public void addFriend(String username) {
    players.get(username).setFriend(true);
    players.get(username).setFoe(false);
    friendList.add(username);
    foeList.remove(username);

    lobbyServerAccessor.setFriends(friendList);
  }

  @Override
  public void removeFriend(String username) {
    players.get(username).setFriend(false);
    friendList.remove(username);

    lobbyServerAccessor.setFriends(friendList);
  }

  @Override
  public void addFoe(String username) {
    players.get(username).setFoe(true);
    players.get(username).setFriend(false);
    foeList.add(username);
    friendList.remove(username);

    lobbyServerAccessor.setFoes(foeList);
  }

  @Override
  public void removeFoe(String username) {
    players.get(username).setFoe(false);
    foeList.remove(username);

    lobbyServerAccessor.setFoes(foeList);
  }

  @Override
  public PlayerInfoBean getCurrentPlayer() {
    CURRENT_PLAYER_LOCK.lock();
    if (currentPlayer == null) {
      currentPlayer = registerAndGetPlayerForUsername(userService.getUsername());
    }
    CURRENT_PLAYER_LOCK.unlock();
    return currentPlayer;
  }

  @Override
  public void onPlayerInfo(PlayerInfo playerInfo) {
    PlayerInfoBean playerInfoBean;
    if (playerInfo.getLogin().equals(userService.getUsername())) {
      playerInfoBean = getCurrentPlayer();
    } else {
      playerInfoBean = registerAndGetPlayerForUsername(playerInfo.getLogin());
      playerInfoBean.setFriend(friendList.contains(playerInfo.getLogin()));
      playerInfoBean.setFoe(foeList.contains(playerInfo.getLogin()));
    }
    playerInfoBean.updateFromPlayerInfo(playerInfo);
  }

  @Override
  public void onFoeList(List<String> foes) {
    this.foeList = foes;

    for (String foe : foes) {
      PlayerInfoBean playerInfoBean = players.get(foe);
      if (playerInfoBean != null) {
        playerInfoBean.setFoe(true);
      }
    }
  }

  @Override
  public void onFriendList(List<String> friends) {
    this.friendList = friends;

    for (String friend : friendList) {
      PlayerInfoBean playerInfoBean = players.get(friend);
      if (playerInfoBean != null) {
        playerInfoBean.setFriend(true);
      }
    }
  }
}
