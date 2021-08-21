package com.faforever.client.builders;

import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.MapVersionReviewsSummaryBean;


public class MapVersionReviewsSummaryBeanBuilder {
  public static MapVersionReviewsSummaryBeanBuilder create() {
    return new MapVersionReviewsSummaryBeanBuilder();
  }

  private final MapVersionReviewsSummaryBean mapVersionReviewsSummaryBean = new MapVersionReviewsSummaryBean();

  public MapVersionReviewsSummaryBeanBuilder defaultValues() {
    id(0);
    mapVersion(MapVersionBeanBuilder.create().defaultValues().get());
    positive(10);
    negative(5);
    score(2);
    reviews(10);
    lowerBound(2);
    return this;
  }

  public MapVersionReviewsSummaryBeanBuilder mapVersion(MapVersionBean mapVersion) {
    mapVersionReviewsSummaryBean.setMapVersion(mapVersion);
    return this;
  }

  public MapVersionReviewsSummaryBeanBuilder id(Integer id) {
    mapVersionReviewsSummaryBean.setId(id);
    return this;
  }

  public MapVersionReviewsSummaryBeanBuilder positive(float positive) {
    mapVersionReviewsSummaryBean.setPositive(positive);
    return this;
  }

  public MapVersionReviewsSummaryBeanBuilder negative(float negative) {
    mapVersionReviewsSummaryBean.setNegative(negative);
    return this;
  }

  public MapVersionReviewsSummaryBeanBuilder score(float score) {
    mapVersionReviewsSummaryBean.setScore(score);
    return this;
  }

  public MapVersionReviewsSummaryBeanBuilder reviews(int reviews) {
    mapVersionReviewsSummaryBean.setReviews(reviews);
    return this;
  }

  public MapVersionReviewsSummaryBeanBuilder lowerBound(float lowerBound) {
    mapVersionReviewsSummaryBean.setLowerBound(lowerBound);
    return this;
  }

  public MapVersionReviewsSummaryBean get() {
    return mapVersionReviewsSummaryBean;
  }

}

