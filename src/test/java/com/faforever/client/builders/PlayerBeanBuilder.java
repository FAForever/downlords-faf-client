package com.faforever.client.builders;

import com.faforever.client.domain.AvatarBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.LeaderboardRatingBean;
import com.faforever.client.domain.NameRecordBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.player.SocialStatus;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;


public class PlayerBeanBuilder {
  public static PlayerBeanBuilder create() {
    return new PlayerBeanBuilder();
  }

  private final PlayerBean playerBean = new PlayerBean();

  public PlayerBeanBuilder defaultValues(){
    username("junit");
    id(1);
    leaderboardRatings(Map.of());
    socialStatus(SocialStatus.OTHER);
    clan("tst");
    country("US");
    avatar(AvatarBeanBuilder.create().defaultValues().get());
    names(List.of());
    return this;
  }

  public PlayerBeanBuilder username(String username) {
    playerBean.setUsername(username);
    return this;
  }

  public PlayerBeanBuilder clan(String clan) {
    playerBean.setClan(clan);
    return this;
  }

  public PlayerBeanBuilder country(String country) {
    playerBean.setCountry(country);
    return this;
  }

  public PlayerBeanBuilder avatar(AvatarBean avatar) {
    playerBean.setAvatar(avatar);
    return this;
  }

  public PlayerBeanBuilder socialStatus(SocialStatus socialStatus) {
    playerBean.setSocialStatus(socialStatus);
    return this;
  }

  public PlayerBeanBuilder leaderboardRatings(Map<String, LeaderboardRatingBean> leaderboardRatings) {
    playerBean.setLeaderboardRatings(leaderboardRatings);
    return this;
  }

  public PlayerBeanBuilder game(GameBean game) {
    playerBean.setGame(game);
    return this;
  }

  public PlayerBeanBuilder idleSince(Instant idleSince) {
    playerBean.setIdleSince(idleSince);
    return this;
  }

  public PlayerBeanBuilder names(List<NameRecordBean> names) {
    playerBean.setNames(names);
    return this;
  }

  public PlayerBeanBuilder id(Integer id) {
    playerBean.setId(id);
    return this;
  }

  public PlayerBeanBuilder createTime(OffsetDateTime createTime) {
    playerBean.setCreateTime(createTime);
    return this;
  }

  public PlayerBeanBuilder updateTime(OffsetDateTime updateTime) {
    playerBean.setUpdateTime(updateTime);
    return this;
  }

  public PlayerBean get() {
    return playerBean;
  }

}

