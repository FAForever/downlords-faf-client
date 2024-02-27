package com.faforever.client.domain;

import com.faforever.client.coop.CoopCategory;

import java.net.URL;

public record CoopMissionBean(
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
