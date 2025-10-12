package com.faforever.client.domain.api;

import com.faforever.client.coop.CoopCategoryEnum;

import java.net.URL;

public record CoopMission(
    Integer id,
    String name,
    String description,
    int version,
    CoopCategoryEnum category,
    URL downloadUrl,
    URL thumbnailUrlSmall,
    URL thumbnailUrlLarge,
    String mapFolderName
) {
  public String ConcatName() {
    return name + " - V" + version;
  }
}