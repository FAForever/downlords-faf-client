package com.faforever.client.domain.api;

import com.faforever.client.coop.CoopCategory;

import java.net.URL;

public record CoopMission(
    Integer id,
    String name,
    String description,
    int version,
    CoopCategory category,
    URL downloadUrl,
    URL thumbnailUrlSmall,
    URL thumbnailUrlLarge,
    String mapFolderName
) {}
