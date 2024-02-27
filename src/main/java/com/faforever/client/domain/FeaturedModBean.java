package com.faforever.client.domain;

public record FeaturedModBean(
    Integer id,
    String technicalName,
    String displayName,
    String description,
    String gitUrl,
    String gitBranch,
    boolean visible
) {}
