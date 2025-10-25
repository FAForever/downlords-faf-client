package com.faforever.client.domain.api;

import com.faforever.client.coop.CoopFaction;

public record CoopCategory(
    String categoryName,
    CoopFaction coopCategory
) {}
