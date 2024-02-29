package com.faforever.client.domain;

public record MatchmakerQueueMapPoolBean(
    Integer id,
    double minRating,
    double maxRating,
    MatchmakerQueueBean matchmakerQueue,
    MapPoolBean mapPool
) {}
