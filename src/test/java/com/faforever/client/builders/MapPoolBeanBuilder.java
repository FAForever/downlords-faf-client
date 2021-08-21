package com.faforever.client.builders;

import com.faforever.client.domain.MapPoolAssignmentBean;
import com.faforever.client.domain.MapPoolBean;
import com.faforever.client.domain.MatchmakerQueueMapPoolBean;

import java.time.OffsetDateTime;
import java.util.List;


public class MapPoolBeanBuilder {
  public static MapPoolBeanBuilder create() {
    return new MapPoolBeanBuilder();
  }

  private final MapPoolBean mapPoolBean = new MapPoolBean();

  public MapPoolBeanBuilder defaultValues() {
    poolAssignments(List.of());
    id(0);
    return this;
  }

  public MapPoolBeanBuilder name(String name) {
    mapPoolBean.setName(name);
    return this;
  }

  public MapPoolBeanBuilder mapPool(MatchmakerQueueMapPoolBean mapPool) {
    mapPoolBean.setMapPool(mapPool);
    return this;
  }

  public MapPoolBeanBuilder poolAssignments(List<MapPoolAssignmentBean> poolAssignments) {
    mapPoolBean.setPoolAssignments(poolAssignments);
    return this;
  }

  public MapPoolBeanBuilder id(Integer id) {
    mapPoolBean.setId(id);
    return this;
  }

  public MapPoolBeanBuilder createTime(OffsetDateTime createTime) {
    mapPoolBean.setCreateTime(createTime);
    return this;
  }

  public MapPoolBeanBuilder updateTime(OffsetDateTime updateTime) {
    mapPoolBean.setUpdateTime(updateTime);
    return this;
  }

  public MapPoolBean get() {
    return mapPoolBean;
  }

}

