package com.faforever.client.domain;

public sealed interface ReviewsSummaryBean<T> permits MapReviewsSummaryBean, MapVersionReviewsSummaryBean, ModReviewsSummaryBean, ModVersionReviewsSummaryBean, ReplayReviewsSummaryBean {
  Integer id();

  float positive();

  float negative();

  float score();

  float averageScore();

  int numReviews();

  float lowerBound();

  T subject();

}
