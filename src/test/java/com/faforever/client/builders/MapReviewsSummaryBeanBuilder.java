package com.faforever.client.builders;

import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapReviewsSummaryBean;


public class MapReviewsSummaryBeanBuilder {
  public static MapReviewsSummaryBeanBuilder create() {
    return new MapReviewsSummaryBeanBuilder();
  }

  private final MapReviewsSummaryBean mapReviewsSummaryBean = new MapReviewsSummaryBean();

  public MapReviewsSummaryBeanBuilder defaultValues() {
    map(MapBeanBuilder.create().defaultValues().get());
    id(0);
    positive(10);
    negative(2);
    score(4);
    reviews(10);
    lowerBound(1);
    return this;
  }

  public MapReviewsSummaryBeanBuilder map(MapBean map) {
    mapReviewsSummaryBean.setMap(map);
    return this;
  }

  public MapReviewsSummaryBeanBuilder id(Integer id) {
    mapReviewsSummaryBean.setId(id);
    return this;
  }

  public MapReviewsSummaryBeanBuilder positive(float positive) {
    mapReviewsSummaryBean.setPositive(positive);
    return this;
  }

  public MapReviewsSummaryBeanBuilder negative(float negative) {
    mapReviewsSummaryBean.setNegative(negative);
    return this;
  }

  public MapReviewsSummaryBeanBuilder score(float score) {
    mapReviewsSummaryBean.setScore(score);
    return this;
  }

  public MapReviewsSummaryBeanBuilder reviews(int reviews) {
    mapReviewsSummaryBean.setReviews(reviews);
    return this;
  }

  public MapReviewsSummaryBeanBuilder lowerBound(float lowerBound) {
    mapReviewsSummaryBean.setLowerBound(lowerBound);
    return this;
  }

  public MapReviewsSummaryBean get() {
    return mapReviewsSummaryBean;
  }

}

