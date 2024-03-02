package com.faforever.client.domain.api;

public record ModVersionReviewsSummary(
    Integer id,
    float positive,
    float negative,
    float score,
    float averageScore,
    int numReviews,
    float lowerBound, ModVersion subject
) implements ReviewsSummary {}
