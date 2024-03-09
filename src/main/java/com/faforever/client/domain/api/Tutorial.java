package com.faforever.client.domain.api;

public record Tutorial(
    Integer id,
    String title,
    String description,
    String image,
    String imageUrl,
    int ordinal, boolean launchable, MapVersion mapVersion,
    String technicalName
) {}
