package com.faforever.client.domain.api;

public sealed interface ReviewsSummary permits MapReviewsSummary, MapVersionReviewsSummary, ModReviewsSummary, ModVersionReviewsSummary, ReplayReviewsSummary {
  Integer id();

  float positive();

  float negative();

  float score();

  float averageScore();

  int numReviews();

  float lowerBound();

}
