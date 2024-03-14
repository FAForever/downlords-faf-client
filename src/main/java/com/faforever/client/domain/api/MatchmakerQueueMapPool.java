package com.faforever.client.domain.api;

import com.faforever.client.domain.server.MatchmakerQueueInfo;

public record MatchmakerQueueMapPool(
    Integer id,
    double minRating,
    double maxRating, MatchmakerQueueInfo matchmakerQueue
) {}
