package com.faforever.client.builders;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.domain.ReplayReviewBean;

import java.time.OffsetDateTime;


public class ReplayReviewBeanBuilder {
  public static ReplayReviewBeanBuilder create() {
    return new ReplayReviewBeanBuilder();
  }

  private final ReplayReviewBean replayReviewBean = new ReplayReviewBean();

  public ReplayReviewBeanBuilder defaultValues() {
    replay(ReplayBeanBuilder.create().defaultValues().get());
    text("test");
    player(PlayerBeanBuilder.create().defaultValues().get());
    score(5);
    id(null);
    return this;
  }

  public ReplayReviewBeanBuilder replay(ReplayBean replay) {
    replayReviewBean.setReplay(replay);
    return this;
  }

  public ReplayReviewBeanBuilder text(String text) {
    replayReviewBean.setText(text);
    return this;
  }

  public ReplayReviewBeanBuilder player(PlayerBean player) {
    replayReviewBean.setPlayer(player);
    return this;
  }

  public ReplayReviewBeanBuilder score(int score) {
    replayReviewBean.setScore(score);
    return this;
  }

  public ReplayReviewBeanBuilder id(Integer id) {
    replayReviewBean.setId(id);
    return this;
  }

  public ReplayReviewBeanBuilder createTime(OffsetDateTime createTime) {
    replayReviewBean.setCreateTime(createTime);
    return this;
  }

  public ReplayReviewBeanBuilder updateTime(OffsetDateTime updateTime) {
    replayReviewBean.setUpdateTime(updateTime);
    return this;
  }

  public ReplayReviewBean get() {
    return replayReviewBean;
  }

}

