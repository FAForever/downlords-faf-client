package com.faforever.client.builders;

import com.faforever.client.domain.CoopResultBean;
import com.faforever.client.domain.ReplayBean;

import java.time.Duration;


public class CoopResultBeanBuilder {
  public static CoopResultBeanBuilder create() {
    return new CoopResultBeanBuilder();
  }

  private final CoopResultBean coopResultBean = new CoopResultBean();

  public CoopResultBeanBuilder defaultValues() {
    id(0);
    return this;
  }

  public CoopResultBeanBuilder id(Integer id) {
    coopResultBean.setId(id);
    return this;
  }

  public CoopResultBeanBuilder playerNames(String playerNames) {
    coopResultBean.setPlayerNames(playerNames);
    return this;
  }

  public CoopResultBeanBuilder secondaryObjectives(boolean secondaryObjectives) {
    coopResultBean.setSecondaryObjectives(secondaryObjectives);
    return this;
  }

  public CoopResultBeanBuilder duration(Duration duration) {
    coopResultBean.setDuration(duration);
    return this;
  }

  public CoopResultBeanBuilder ranking(int ranking) {
    coopResultBean.setRanking(ranking);
    return this;
  }

  public CoopResultBeanBuilder playerCount(int playerCount) {
    coopResultBean.setPlayerCount(playerCount);
    return this;
  }

  public CoopResultBeanBuilder replay(ReplayBean replay) {
    coopResultBean.setReplay(replay);
    return this;
  }

  public CoopResultBean get() {
    return coopResultBean;
  }

}

