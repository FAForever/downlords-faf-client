package com.faforever.client.domain;

public record TutorialBean(
    Integer id,
    String title,
    String description,
    String image,
    String imageUrl,
    int ordinal,
    boolean launchable,
    MapVersionBean mapVersion,
    String technicalName
) {}
