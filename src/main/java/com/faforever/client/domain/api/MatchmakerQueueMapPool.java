package com.faforever.client.domain.api;

import com.faforever.client.domain.server.MatchmakerQueueInfo;

public record MatchmakerQueueMapPool(
    Integer id,
    Double minRating,
    Double maxRating, MatchmakerQueueInfo matchmakerQueue,
    Integer vetoTokensPerPlayer,
    Integer maxTokensPerMap,
    Float minimalMapsAllowed
) {}
