package com.faforever.client.domain;

public record MapVersionReviewsSummaryBean(
    Integer id,
    float positive,
    float negative,
    float score,
    float averageScore,
    int numReviews,
    float lowerBound,
    MapVersionBean subject
) implements ReviewsSummaryBean {}
