package com.faforever.client.domain.api;

public record ReviewsSummary(
    Integer id, float positive, float negative, float score, float averageScore, int numReviews, float lowerBound
) {}
