package com.faforever.client.domain.api;

public record MapReviewsSummary(
    Integer id,
    float positive,
    float negative,
    float score,
    float averageScore,
    int numReviews,
    float lowerBound, Map subject
) implements ReviewsSummary {}
