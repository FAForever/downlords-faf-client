package com.faforever.client.player.event;

import com.faforever.client.chat.PlayerInfoBean;

public class FriendJoinedGameEvent {
  private PlayerInfoBean playerInfoBean;

  public FriendJoinedGameEvent(PlayerInfoBean playerInfoBean) {
    this.playerInfoBean = playerInfoBean;
  }

  public PlayerInfoBean getPlayerInfoBean() {
    return playerInfoBean;
  }
}
