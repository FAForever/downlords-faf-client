package com.faforever.client.domain;

public record ModReviewsSummaryBean(
    Integer id,
    float positive,
    float negative,
    float score,
    float averageScore,
    int numReviews,
    float lowerBound,
    ModBean subject
) implements ReviewsSummaryBean {}
