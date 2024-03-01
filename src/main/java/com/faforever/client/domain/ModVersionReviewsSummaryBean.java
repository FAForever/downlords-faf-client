package com.faforever.client.domain;

public record ModVersionReviewsSummaryBean(
    Integer id,
    float positive,
    float negative,
    float score,
    float averageScore,
    int numReviews,
    float lowerBound,
    ModVersionBean subject
) implements ReviewsSummaryBean {}
