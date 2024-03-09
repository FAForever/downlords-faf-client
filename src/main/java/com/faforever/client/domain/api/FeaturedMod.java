package com.faforever.client.domain.api;

public record FeaturedMod(
    Integer id,
    String technicalName,
    String displayName,
    String description,
    String gitUrl,
    String gitBranch,
    boolean visible
) {}
