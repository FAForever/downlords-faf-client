package com.faforever.client.builders;

import com.faforever.client.domain.ModBean;
import com.faforever.client.domain.ModReviewsSummaryBean;


public class ModReviewsSummaryBeanBuilder {
  public static ModReviewsSummaryBeanBuilder create() {
    return new ModReviewsSummaryBeanBuilder();
  }

  private final ModReviewsSummaryBean modReviewsSummaryBean = new ModReviewsSummaryBean();

  public ModReviewsSummaryBeanBuilder defaultValues() {
    mod(ModBeanBuilder.create().defaultValues().get());
    id(0);
    positive(10);
    negative(5);
    score(2);
    lowerBound(2);
    reviews(5);
    return this;
  }

  public ModReviewsSummaryBeanBuilder mod(ModBean mod) {
    modReviewsSummaryBean.setMod(mod);
    return this;
  }

  public ModReviewsSummaryBeanBuilder id(Integer id) {
    modReviewsSummaryBean.setId(id);
    return this;
  }

  public ModReviewsSummaryBeanBuilder positive(float positive) {
    modReviewsSummaryBean.setPositive(positive);
    return this;
  }

  public ModReviewsSummaryBeanBuilder negative(float negative) {
    modReviewsSummaryBean.setNegative(negative);
    return this;
  }

  public ModReviewsSummaryBeanBuilder score(float score) {
    modReviewsSummaryBean.setScore(score);
    return this;
  }

  public ModReviewsSummaryBeanBuilder reviews(int reviews) {
    modReviewsSummaryBean.setReviews(reviews);
    return this;
  }

  public ModReviewsSummaryBeanBuilder lowerBound(float lowerBound) {
    modReviewsSummaryBean.setLowerBound(lowerBound);
    return this;
  }

  public ModReviewsSummaryBean get() {
    return modReviewsSummaryBean;
  }

}

