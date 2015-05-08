package com.faforever.client.player;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.legacy.OnFoeListListener;
import com.faforever.client.legacy.OnFriendListListener;
import com.faforever.client.legacy.OnPlayerInfoListener;
import com.faforever.client.legacy.ServerAccessor;
import com.faforever.client.legacy.domain.PlayerInfo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

public class PlayerServiceImpl implements PlayerService, OnPlayerInfoListener, OnFoeListListener, OnFriendListListener {

  @Autowired
  ServerAccessor serverAccessor;

  private ObservableMap<String, PlayerInfoBean> knownPlayers;
  private List<String> foeList;
  private List<String> friendList;

  public PlayerServiceImpl() {
    knownPlayers = FXCollections.observableHashMap();
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
  public void addOnPlayerInfoListener(OnPlayerInfoListener listener) {

  }

  @Override
  public ObservableMap<String, PlayerInfoBean> getKnownPlayers() {
    return knownPlayers;
  }

  @Override
  public void onPlayerInfo(PlayerInfo playerInfo) {
    if (!knownPlayers.containsKey(playerInfo.login)) {
      knownPlayers.put(playerInfo.login, new PlayerInfoBean(playerInfo));
    }

    knownPlayers.get(playerInfo.login).setFriend(friendList.contains(playerInfo.login));
    knownPlayers.get(playerInfo.login).setFoe(foeList.contains(playerInfo.login));
  }

  @Override
  public void onFoeList(List<String> foes) {
    this.foeList = foes;

    for (String foe : foes) {
      PlayerInfoBean playerInfoBean = knownPlayers.get(foe);
      if (playerInfoBean != null) {
        playerInfoBean.setFoe(true);
      }
    }
  }

  @Override
  public void onFriendList(List<String> friends) {
    this.friendList = friends;

    for (String friend : friendList) {
      PlayerInfoBean playerInfoBean = knownPlayers.get(friend);
      if (playerInfoBean != null) {
        playerInfoBean.setFriend(true);
      }
    }
  }
}
