package com.faforever.client.domain.api;

import com.faforever.client.domain.server.PlayerInfo;

public record Mod(
    Integer id,
    String displayName,
    boolean recommended,
    String author, PlayerInfo uploader, ModReviewsSummary modReviewsSummary
) {}
