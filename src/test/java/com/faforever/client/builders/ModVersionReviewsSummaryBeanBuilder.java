package com.faforever.client.builders;

import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.domain.ModVersionReviewsSummaryBean;


public class ModVersionReviewsSummaryBeanBuilder {
  public static ModVersionReviewsSummaryBeanBuilder create() {
    return new ModVersionReviewsSummaryBeanBuilder();
  }

  private final ModVersionReviewsSummaryBean modVersionReviewsSummaryBean = new ModVersionReviewsSummaryBean();

  public ModVersionReviewsSummaryBeanBuilder defaultValues(){
    modVersion(ModVersionBeanBuilder.create().defaultValues().get());
    id(0);
    positive(10);
    negative(2);
    score(1);
    reviews(10);
    lowerBound(4);
    return this;
  }

  public ModVersionReviewsSummaryBeanBuilder modVersion(ModVersionBean modVersion) {
    modVersionReviewsSummaryBean.setModVersion(modVersion);
    return this;
  }

  public ModVersionReviewsSummaryBeanBuilder id(Integer id) {
    modVersionReviewsSummaryBean.setId(id);
    return this;
  }

  public ModVersionReviewsSummaryBeanBuilder positive(float positive) {
    modVersionReviewsSummaryBean.setPositive(positive);
    return this;
  }

  public ModVersionReviewsSummaryBeanBuilder negative(float negative) {
    modVersionReviewsSummaryBean.setNegative(negative);
    return this;
  }

  public ModVersionReviewsSummaryBeanBuilder score(float score) {
    modVersionReviewsSummaryBean.setScore(score);
    return this;
  }

  public ModVersionReviewsSummaryBeanBuilder reviews(int reviews) {
    modVersionReviewsSummaryBean.setReviews(reviews);
    return this;
  }

  public ModVersionReviewsSummaryBeanBuilder lowerBound(float lowerBound) {
    modVersionReviewsSummaryBean.setLowerBound(lowerBound);
    return this;
  }

  public ModVersionReviewsSummaryBean get() {
    return modVersionReviewsSummaryBean;
  }

}

