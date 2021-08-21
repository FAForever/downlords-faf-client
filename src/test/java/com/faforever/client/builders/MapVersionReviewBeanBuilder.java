package com.faforever.client.builders;

import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.MapVersionReviewBean;
import com.faforever.client.domain.PlayerBean;

import java.time.OffsetDateTime;


public class MapVersionReviewBeanBuilder {
  public static MapVersionReviewBeanBuilder create() {
    return new MapVersionReviewBeanBuilder();
  }

  private final MapVersionReviewBean mapVersionReviewBean = new MapVersionReviewBean();

  public MapVersionReviewBeanBuilder defaultValues() {
    mapVersion(MapVersionBeanBuilder.create().defaultValues().get());
    text("test");
    player(PlayerBeanBuilder.create().defaultValues().get());
    score(0);
    id(null);
    return this;
  }

  public MapVersionReviewBeanBuilder mapVersion(MapVersionBean mapVersion) {
    mapVersionReviewBean.setMapVersion(mapVersion);
    return this;
  }

  public MapVersionReviewBeanBuilder text(String text) {
    mapVersionReviewBean.setText(text);
    return this;
  }

  public MapVersionReviewBeanBuilder player(PlayerBean player) {
    mapVersionReviewBean.setPlayer(player);
    return this;
  }

  public MapVersionReviewBeanBuilder score(int score) {
    mapVersionReviewBean.setScore(score);
    return this;
  }

  public MapVersionReviewBeanBuilder id(Integer id) {
    mapVersionReviewBean.setId(id);
    return this;
  }

  public MapVersionReviewBeanBuilder createTime(OffsetDateTime createTime) {
    mapVersionReviewBean.setCreateTime(createTime);
    return this;
  }

  public MapVersionReviewBeanBuilder updateTime(OffsetDateTime updateTime) {
    mapVersionReviewBean.setUpdateTime(updateTime);
    return this;
  }

  public MapVersionReviewBean get() {
    return mapVersionReviewBean;
  }

}

