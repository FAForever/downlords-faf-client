package com.faforever.client.player;

import com.faforever.client.chat.SocialStatus;
import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.game.Game;

public class PlayerInfoBeanBuilder {

  private final Player player;

  private PlayerInfoBeanBuilder(String username) {
    player = new Player(username);
  }

  public static PlayerInfoBeanBuilder create(String username) {
    return new PlayerInfoBeanBuilder(username);
  }

  public PlayerInfoBeanBuilder id(int id) {
    player.setId(id);
    return this;
  }

  public Player get() {
    return player;
  }

  public PlayerInfoBeanBuilder chatOnly(boolean chatOnly) {
    player.setChatOnly(chatOnly);
    return this;
  }

  public PlayerInfoBeanBuilder socialStatus(SocialStatus socialStatus) {
    player.setSocialStatus(socialStatus);
    return this;
  }

  public PlayerInfoBeanBuilder leaderboardRatingMean(float mean) {
    player.setLeaderboardRatingMean(mean);
    return this;
  }

  public PlayerInfoBeanBuilder leaderboardRatingDeviation(float deviation) {
    player.setLeaderboardRatingDeviation(deviation);
    return this;
  }

  public PlayerInfoBeanBuilder game(Game game) {
    player.setGame(game);
    return this;
  }

  public PlayerInfoBeanBuilder avatar(AvatarBean avatar) {
    player.setAvatarUrl(avatar == null ? null : avatar.getUrl().toExternalForm());
    player.setAvatarTooltip(avatar == null ? null : avatar.getDescription());
    return this;
  }
}
