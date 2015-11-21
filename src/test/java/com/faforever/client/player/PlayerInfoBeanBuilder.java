package com.faforever.client.player;

import com.faforever.client.chat.PlayerInfoBean;

public class PlayerInfoBeanBuilder {

  private final PlayerInfoBean playerInfoBean;

  private PlayerInfoBeanBuilder(String username) {
    playerInfoBean = new PlayerInfoBean(username);
  }

  public PlayerInfoBeanBuilder id(int id) {
    playerInfoBean.setId(id);
    return this;
  }

  public PlayerInfoBean get() {
    return playerInfoBean;
  }

  public static PlayerInfoBeanBuilder create(String username) {
    return new PlayerInfoBeanBuilder(username);
  }
}
