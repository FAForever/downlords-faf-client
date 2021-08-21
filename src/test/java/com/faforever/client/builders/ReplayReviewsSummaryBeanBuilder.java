package com.faforever.client.builders;

import com.faforever.client.domain.ReplayBean;
import com.faforever.client.domain.ReplayReviewsSummaryBean;


public class ReplayReviewsSummaryBeanBuilder {
  public static ReplayReviewsSummaryBeanBuilder create() {
    return new ReplayReviewsSummaryBeanBuilder();
  }

  private final ReplayReviewsSummaryBean replayReviewsSummaryBean = new ReplayReviewsSummaryBean();

  public ReplayReviewsSummaryBeanBuilder defaultValues() {
    replay(ReplayBeanBuilder.create().defaultValues().get());
    id(0);
    positive(5);
    negative(1);
    score(3);
    reviews(15);
    lowerBound(4);
    return this;
  }

  public ReplayReviewsSummaryBeanBuilder replay(ReplayBean game) {
    replayReviewsSummaryBean.setReplay(game);
    return this;
  }

  public ReplayReviewsSummaryBeanBuilder id(Integer id) {
    replayReviewsSummaryBean.setId(id);
    return this;
  }

  public ReplayReviewsSummaryBeanBuilder positive(float positive) {
    replayReviewsSummaryBean.setPositive(positive);
    return this;
  }

  public ReplayReviewsSummaryBeanBuilder negative(float negative) {
    replayReviewsSummaryBean.setNegative(negative);
    return this;
  }

  public ReplayReviewsSummaryBeanBuilder score(float score) {
    replayReviewsSummaryBean.setScore(score);
    return this;
  }

  public ReplayReviewsSummaryBeanBuilder reviews(int reviews) {
    replayReviewsSummaryBean.setReviews(reviews);
    return this;
  }

  public ReplayReviewsSummaryBeanBuilder lowerBound(float lowerBound) {
    replayReviewsSummaryBean.setLowerBound(lowerBound);
    return this;
  }

  public ReplayReviewsSummaryBean get() {
    return replayReviewsSummaryBean;
  }

}

