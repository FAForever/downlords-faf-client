package com.faforever.client.builders;

import com.faforever.client.domain.MapPoolAssignmentBean;
import com.faforever.client.domain.MapPoolBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.commons.api.dto.MapParams;

import java.time.OffsetDateTime;


public class MapPoolAssignmentBeanBuilder {
  public static MapPoolAssignmentBeanBuilder create() {
    return new MapPoolAssignmentBeanBuilder();
  }

  private final MapPoolAssignmentBean mapPoolAssignmentBean = new MapPoolAssignmentBean();

  public MapPoolAssignmentBeanBuilder defaultValues() {
    mapVersion(MapVersionBeanBuilder.create().defaultValues().get());
    weight(1);
    id(0);
    return this;
  }

  public MapPoolAssignmentBeanBuilder mapParams(MapParams mapParams) {
    mapPoolAssignmentBean.setMapParams(mapParams);
    return this;
  }

  public MapPoolAssignmentBeanBuilder mapPool(MapPoolBean mapPool) {
    mapPoolAssignmentBean.setMapPool(mapPool);
    return this;
  }

  public MapPoolAssignmentBeanBuilder mapVersion(MapVersionBean mapVersion) {
    mapPoolAssignmentBean.setMapVersion(mapVersion);
    return this;
  }

  public MapPoolAssignmentBeanBuilder weight(int weight) {
    mapPoolAssignmentBean.setWeight(weight);
    return this;
  }

  public MapPoolAssignmentBeanBuilder id(Integer id) {
    mapPoolAssignmentBean.setId(id);
    return this;
  }

  public MapPoolAssignmentBeanBuilder createTime(OffsetDateTime createTime) {
    mapPoolAssignmentBean.setCreateTime(createTime);
    return this;
  }

  public MapPoolAssignmentBeanBuilder updateTime(OffsetDateTime updateTime) {
    mapPoolAssignmentBean.setUpdateTime(updateTime);
    return this;
  }

  public MapPoolAssignmentBean get() {
    return mapPoolAssignmentBean;
  }

}

