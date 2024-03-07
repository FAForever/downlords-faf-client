package com.faforever.client.domain.api;

import com.faforever.client.domain.server.PlayerInfo;

public record Map(
    Integer id,
    String displayName,
    int gamesPlayed, PlayerInfo author,
    boolean recommended, MapType mapType, ReviewsSummary reviewsSummary
) {}
