package com.faforever.client.domain.api;

import java.net.URL;

public record Subdivision(
    Integer id,
    String nameKey,
    String descriptionKey,
    int index,
    int highestScore,
    int maxRating,
    int minRating, Division division,
    URL imageUrl,
    URL mediumImageUrl,
    URL smallImageUrl
) {}

