package com.faforever.client.domain.api;

import java.net.URL;

public record CoopMission(
    Integer id,
    String name,
    String description,
    int version,
    URL downloadUrl,
    URL thumbnailUrlSmall,
    URL thumbnailUrlLarge,
    String mapFolderName
) {}