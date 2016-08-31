package com.faforever.client.player;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.chat.SocialStatus;

public class PlayerInfoBeanBuilder {

  private final PlayerInfoBean playerInfoBean;

  private PlayerInfoBeanBuilder(String username) {
    playerInfoBean = new PlayerInfoBean(username);
  }

  public static PlayerInfoBeanBuilder create(String username) {
    return new PlayerInfoBeanBuilder(username);
  }

  public PlayerInfoBeanBuilder id(int id) {
    playerInfoBean.setId(id);
    return this;
  }

  public PlayerInfoBean get() {
    return playerInfoBean;
  }

  public PlayerInfoBeanBuilder chatOnly(boolean chatOnly) {
    playerInfoBean.setChatOnly(chatOnly);
    return this;
  }

  public PlayerInfoBeanBuilder socialStatus(SocialStatus socialStatus) {
    playerInfoBean.setSocialStatus(socialStatus);
    return this;
  }

  public PlayerInfoBeanBuilder leaderboardRatingMean(float mean) {
    playerInfoBean.setLeaderboardRatingMean(mean);
    return this;
  }

  public PlayerInfoBeanBuilder leaderboardRatingDeviation(float deviation) {
    playerInfoBean.setLeaderboardRatingDeviation(deviation);
    return this;
  }

  public PlayerInfoBeanBuilder gameUid(int gameUid) {
    playerInfoBean.setGameUid(gameUid);
    return this;
  }
}
