package com.faforever.client.builders;

import com.faforever.client.domain.GamePlayerStatsBean;
import com.faforever.client.domain.LeaderboardRatingJournalBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.commons.api.dto.Faction;

import java.time.OffsetDateTime;
import java.util.List;


public class GamePlayerStatsBeanBuilder {
  public static GamePlayerStatsBeanBuilder create() {
    return new GamePlayerStatsBeanBuilder();
  }

  private final GamePlayerStatsBean gamePlayerStatsBean = new GamePlayerStatsBean();

  public GamePlayerStatsBeanBuilder defaultValues() {
    player(PlayerBeanBuilder.create().defaultValues().get());
    score(0);
    team(0);
    faction(Faction.CYBRAN);
    scoreTime(OffsetDateTime.now().minusDays(1));
    return this;
  }

  public GamePlayerStatsBeanBuilder player(PlayerBean player) {
    gamePlayerStatsBean.setPlayer(player);
    return this;
  }

  public GamePlayerStatsBeanBuilder score(int score) {
    gamePlayerStatsBean.setScore(score);
    return this;
  }

  public GamePlayerStatsBeanBuilder team(int team) {
    gamePlayerStatsBean.setTeam(team);
    return this;
  }

  public GamePlayerStatsBeanBuilder faction(Faction faction) {
    gamePlayerStatsBean.setFaction(faction);
    return this;
  }

  public GamePlayerStatsBeanBuilder scoreTime(OffsetDateTime scoreTime) {
    gamePlayerStatsBean.setScoreTime(scoreTime);
    return this;
  }

  public GamePlayerStatsBeanBuilder game(ReplayBean game) {
    gamePlayerStatsBean.setGame(game);
    return this;
  }

  public GamePlayerStatsBeanBuilder leaderboardRatingJournals(List<LeaderboardRatingJournalBean> leaderboardRatingJournals) {
    gamePlayerStatsBean.setLeaderboardRatingJournals(leaderboardRatingJournals);
    return this;
  }

  public GamePlayerStatsBean get() {
    return gamePlayerStatsBean;
  }

}

