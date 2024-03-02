package com.faforever.client.domain.api;

public record MapVersionReviewsSummary(
    Integer id,
    float positive,
    float negative,
    float score,
    float averageScore,
    int numReviews,
    float lowerBound, MapVersion subject
) implements ReviewsSummary {}
