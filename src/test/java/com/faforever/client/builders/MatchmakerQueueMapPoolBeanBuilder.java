package com.faforever.client.builders;

import com.faforever.client.domain.MapPoolBean;
import com.faforever.client.domain.MatchmakerQueueBean;
import com.faforever.client.domain.MatchmakerQueueMapPoolBean;

import java.time.OffsetDateTime;


public class MatchmakerQueueMapPoolBeanBuilder {
  public static MatchmakerQueueMapPoolBeanBuilder create() {
    return new MatchmakerQueueMapPoolBeanBuilder();
  }

  private final MatchmakerQueueMapPoolBean matchmakerQueueMapPoolBean = new MatchmakerQueueMapPoolBean();

  public MatchmakerQueueMapPoolBeanBuilder defaultValues() {
    minRating(500);
    maxRating(1000);
    id(0);
    return this;
  }

  public MatchmakerQueueMapPoolBeanBuilder minRating(double minRating) {
    matchmakerQueueMapPoolBean.setMinRating(minRating);
    return this;
  }

  public MatchmakerQueueMapPoolBeanBuilder maxRating(double maxRating) {
    matchmakerQueueMapPoolBean.setMaxRating(maxRating);
    return this;
  }

  public MatchmakerQueueMapPoolBeanBuilder matchmakerQueue(MatchmakerQueueBean matchmakerQueue) {
    matchmakerQueueMapPoolBean.setMatchmakerQueue(matchmakerQueue);
    return this;
  }

  public MatchmakerQueueMapPoolBeanBuilder mapPool(MapPoolBean mapPool) {
    matchmakerQueueMapPoolBean.setMapPool(mapPool);
    return this;
  }

  public MatchmakerQueueMapPoolBeanBuilder id(Integer id) {
    matchmakerQueueMapPoolBean.setId(id);
    return this;
  }

  public MatchmakerQueueMapPoolBeanBuilder createTime(OffsetDateTime createTime) {
    matchmakerQueueMapPoolBean.setCreateTime(createTime);
    return this;
  }

  public MatchmakerQueueMapPoolBeanBuilder updateTime(OffsetDateTime updateTime) {
    matchmakerQueueMapPoolBean.setUpdateTime(updateTime);
    return this;
  }

  public MatchmakerQueueMapPoolBean get() {
    return matchmakerQueueMapPoolBean;
  }

}

