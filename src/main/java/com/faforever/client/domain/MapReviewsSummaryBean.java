package com.faforever.client.domain;

public record MapReviewsSummaryBean(
    Integer id,
    float positive,
    float negative,
    float score,
    float averageScore,
    int numReviews,
    float lowerBound,
    MapBean subject
) implements ReviewsSummaryBean {}
