package com.faforever.client.player;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.OnFoeListListener;
import com.faforever.client.legacy.OnFriendListListener;
import com.faforever.client.legacy.OnPlayerInfoListener;
import com.faforever.client.legacy.domain.Player;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Assert;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PlayerServiceImpl implements PlayerService, OnPlayerInfoListener, OnFoeListListener, OnFriendListListener {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Lock CURRENT_PLAYER_LOCK = new ReentrantLock();

  private final ObservableMap<String, PlayerInfoBean> players;

  @Resource
  LobbyServerAccessor lobbyServerAccessor;
  @Resource
  UserService userService;

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


  @Override
  public void onPlayerInfo(Player player) {
    if (player.getLogin().equals(userService.getUsername())) {
      PlayerInfoBean playerInfoBean = getCurrentPlayer();
      playerInfoBean.updateFromPlayerInfo(player);
    } else {
      PlayerInfoBean playerInfoBean = registerAndGetPlayerForUsername(player.getLogin());
      playerInfoBean.setFriend(friendList.contains(player.getLogin()));
      playerInfoBean.setFoe(foeList.contains(player.getLogin()));
      playerInfoBean.updateFromPlayerInfo(player);
    }
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
