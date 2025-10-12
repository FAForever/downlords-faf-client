package com.faforever.client.domain.api;

import com.faforever.client.coop.CoopCategoryEnum;

public record CoopCategory(
    String categoryName,
    CoopCategoryEnum coopCategory
) {}
