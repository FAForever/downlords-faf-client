package com.faforever.client.player;

import com.faforever.client.chat.*;
import com.faforever.client.legacy.OnFoeListListener;
import com.faforever.client.legacy.OnFriendListListener;
import com.faforever.client.legacy.OnPlayerInfoListener;
import com.faforever.client.legacy.ServerAccessor;
import com.faforever.client.legacy.domain.PlayerInfo;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

public class PlayerServiceImpl implements PlayerService, OnPlayerInfoListener, OnFoeListListener, OnFriendListListener {

  @Autowired
  ServerAccessor serverAccessor;

  @Autowired
  ChatService chatService;

  private ObservableMap<String, PlayerInfoBean> players;
  private List<String> foeList;
  private List<String> friendList;

  public PlayerServiceImpl() {
    players = FXCollections.observableHashMap();
    friendList = new ArrayList<>();
    foeList = new ArrayList<>();
  }

  @PostConstruct
  void init() {
    serverAccessor.setOnPlayerInfoMessageListener(this);
    serverAccessor.setOnFoeListListener(this);
    serverAccessor.setOnFriendListListener(this);
  }

  @Override
  public PlayerInfoBean getPlayerForUsername(String username) {
    return players.get(username);
  }

  @Override
  public void addPlayerListener(MapChangeListener<String, PlayerInfoBean> listener) {
    players.addListener(listener);
  }

  @Override
  public PlayerInfoBean registerAndGetPlayerForUsername(String username) {
    if (!players.containsKey(username)) {
      players.put(username, new PlayerInfoBean(username));
    }

    return players.get(username);
  }

  @Override
  public void onPlayerInfo(PlayerInfo playerInfo) {
    if (!players.containsKey(playerInfo.login)) {
      players.put(playerInfo.login, new PlayerInfoBean(playerInfo));
    }

    PlayerInfoBean playerInfoBean = players.get(playerInfo.login);
    playerInfoBean.setChatOnly(false);
    playerInfoBean.setFriend(friendList.contains(playerInfo.login));
    playerInfoBean.setFoe(foeList.contains(playerInfo.login));
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
