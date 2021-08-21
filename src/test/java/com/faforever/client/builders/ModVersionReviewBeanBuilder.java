package com.faforever.client.builders;

import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.domain.ModVersionReviewBean;
import com.faforever.client.domain.PlayerBean;

import java.time.OffsetDateTime;


public class ModVersionReviewBeanBuilder {
  public static ModVersionReviewBeanBuilder create() {
    return new ModVersionReviewBeanBuilder();
  }

  private final ModVersionReviewBean modVersionReviewBean = new ModVersionReviewBean();

  public ModVersionReviewBeanBuilder defaultValues() {
    player(PlayerBeanBuilder.create().defaultValues().get());
    text("test");
    modVersion(ModVersionBeanBuilder.create().defaultValues().get());
    score(4);
    id(null);
    return this;
  }

  public ModVersionReviewBeanBuilder modVersion(ModVersionBean modVersion) {
    modVersionReviewBean.setModVersion(modVersion);
    return this;
  }

  public ModVersionReviewBeanBuilder text(String text) {
    modVersionReviewBean.setText(text);
    return this;
  }

  public ModVersionReviewBeanBuilder player(PlayerBean player) {
    modVersionReviewBean.setPlayer(player);
    return this;
  }

  public ModVersionReviewBeanBuilder score(int score) {
    modVersionReviewBean.setScore(score);
    return this;
  }

  public ModVersionReviewBeanBuilder id(Integer id) {
    modVersionReviewBean.setId(id);
    return this;
  }

  public ModVersionReviewBeanBuilder createTime(OffsetDateTime createTime) {
    modVersionReviewBean.setCreateTime(createTime);
    return this;
  }

  public ModVersionReviewBeanBuilder updateTime(OffsetDateTime updateTime) {
    modVersionReviewBean.setUpdateTime(updateTime);
    return this;
  }

  public ModVersionReviewBean get() {
    return modVersionReviewBean;
  }

}

