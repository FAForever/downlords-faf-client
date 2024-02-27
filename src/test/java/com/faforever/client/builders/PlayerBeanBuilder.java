package com.faforever.client.builders;

import com.faforever.client.domain.AvatarBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.LeaderboardRatingBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.player.SocialStatus;
import org.instancio.Instancio;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;


public class PlayerBeanBuilder {
  public static PlayerBeanBuilder create() {
    return new PlayerBeanBuilder();
  }

  private final PlayerBean playerBean = new PlayerBean();

  public PlayerBeanBuilder defaultValues(){
    username("junit");
    id(1);
    leaderboardRatings(new HashMap<>());
    socialStatus(SocialStatus.OTHER);
    clan("tst");
    country("US");
    note("");
    avatar(Instancio.create(AvatarBean.class));
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

  public PlayerBeanBuilder note(String text) {
    playerBean.setNote(text);
    return this;
  }

  public PlayerBean get() {
    return playerBean;
  }

}

