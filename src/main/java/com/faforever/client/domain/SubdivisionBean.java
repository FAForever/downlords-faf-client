package com.faforever.client.domain;

import java.net.URL;

public record SubdivisionBean(
    Integer id,
    String nameKey,
    String descriptionKey,
    int index,
    int highestScore,
    int maxRating,
    int minRating,
    DivisionBean division,
    URL imageUrl,
    URL mediumImageUrl,
    URL smallImageUrl
) {}

