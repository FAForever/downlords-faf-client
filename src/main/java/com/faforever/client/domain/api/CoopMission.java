package com.faforever.client.domain.api;

import com.faforever.client.coop.CoopFaction;
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
) {
  public String concatName() {
    return name + " - V" + version;
  }
}