package com.faforever.client.domain;

public record ReplayReviewsSummaryBean(
    Integer id,
    float positive,
    float negative,
    float score,
    float averageScore,
    int numReviews,
    float lowerBound,
    ReplayBean subject
) implements ReviewsSummaryBean<ReplayBean> {}
