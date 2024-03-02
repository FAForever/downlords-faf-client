package com.faforever.client.domain.api;

public record ModReviewsSummary(
    Integer id,
    float positive,
    float negative,
    float score,
    float averageScore,
    int numReviews,
    float lowerBound, Mod subject
) implements ReviewsSummary {}
