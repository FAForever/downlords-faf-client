package com.faforever.client.builders;

import com.faforever.client.domain.api.Avatar;
import com.faforever.client.domain.api.LeaderboardRating;
import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.player.SocialStatus;
import org.instancio.Instancio;

import java.util.HashMap;
import java.util.Map;


public class PlayerInfoBuilder {
  public static PlayerInfoBuilder create() {
    return new PlayerInfoBuilder();
  }

  private final PlayerInfo playerInfo = new PlayerInfo();

  public PlayerInfoBuilder defaultValues() {
    username("junit");
    id(1);
    leaderboardRatings(new HashMap<>());
    socialStatus(SocialStatus.OTHER);
    clan("tst");
    country("US");
    note("");
    avatar(Instancio.create(Avatar.class));
    return this;
  }

  public PlayerInfoBuilder username(String username) {
    playerInfo.setUsername(username);
    return this;
  }

  public PlayerInfoBuilder clan(String clan) {
    playerInfo.setClan(clan);
    return this;
  }

  public PlayerInfoBuilder country(String country) {
    playerInfo.setCountry(country);
    return this;
  }

  public PlayerInfoBuilder avatar(Avatar avatar) {
    playerInfo.setAvatar(avatar);
    return this;
  }

  public PlayerInfoBuilder socialStatus(SocialStatus socialStatus) {
    playerInfo.setSocialStatus(socialStatus);
    return this;
  }

  public PlayerInfoBuilder leaderboardRatings(Map<String, LeaderboardRating> leaderboardRatings) {
    playerInfo.setLeaderboardRatings(leaderboardRatings);
    return this;
  }

  public PlayerInfoBuilder game(GameInfo game) {
    playerInfo.setGame(game);
    return this;
  }

  public PlayerInfoBuilder id(Integer id) {
    playerInfo.setId(id);
    return this;
  }

  public PlayerInfoBuilder note(String text) {
    playerInfo.setNote(text);
    return this;
  }

  public PlayerInfo get() {
    return playerInfo;
  }

}

