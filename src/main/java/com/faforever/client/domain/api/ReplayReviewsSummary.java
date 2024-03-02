package com.faforever.client.domain.api;

public record ReplayReviewsSummary(
    Integer id,
    float positive,
    float negative,
    float score,
    float averageScore,
    int numReviews,
    float lowerBound, Replay subject
) implements ReviewsSummary {}
