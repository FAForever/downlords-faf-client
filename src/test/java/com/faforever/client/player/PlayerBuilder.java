package com.faforever.client.player;

import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.chat.avatar.AvatarBeanBuilder;
import com.faforever.client.clan.ClanBuilder;
import com.faforever.client.game.Game;
import com.faforever.client.leaderboard.LeaderboardRating;
import com.faforever.client.leaderboard.LeaderboardRatingMapBuilder;

import java.util.Map;

public class PlayerBuilder {

  private final Player player;

  private PlayerBuilder(String username) {
    player = new Player(username);
  }

  public static PlayerBuilder create(String username) {
    return new PlayerBuilder(username);
  }

  public PlayerBuilder defaultValues() {
    id(1);
    leaderboardRatings(LeaderboardRatingMapBuilder.create().defaultValues().get());
    socialStatus(SocialStatus.OTHER);
    clan(ClanBuilder.TEST_CLAN_TAG);
    country("US");
    avatar(AvatarBeanBuilder.create().defaultValues().get());
    return this;
  }

  public PlayerBuilder clan(String clan) {
    player.setClan(clan);
    return this;
  }

  public PlayerBuilder id(int id) {
    player.setId(id);
    return this;
  }

  public Player get() {
    return player;
  }

  public PlayerBuilder socialStatus(SocialStatus socialStatus) {
    player.setSocialStatus(socialStatus);
    return this;
  }

  public PlayerBuilder leaderboardRatings(Map<String, LeaderboardRating> leaderboardRatingMap) {
    player.setLeaderboardRatings(leaderboardRatingMap);
    return this;
  }

  public PlayerBuilder game(Game game) {
    player.setGame(game);
    return this;
  }

  public PlayerBuilder country(String country) {
    player.setCountry(country);
    return this;
  }

  public PlayerBuilder avatar(AvatarBean avatar) {
    player.setAvatarUrl(avatar == null ? null : avatar.getUrl().toExternalForm());
    player.setAvatarTooltip(avatar == null ? null : avatar.getDescription());
    return this;
  }
}
