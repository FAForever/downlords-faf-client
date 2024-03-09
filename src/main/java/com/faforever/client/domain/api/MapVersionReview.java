package com.faforever.client.domain.api;

import com.faforever.client.domain.server.PlayerInfo;
import org.apache.maven.artifact.versioning.ComparableVersion;

public record MapVersionReview(
    Integer id, String text, PlayerInfo player, Integer score, MapVersion subject
) implements ReviewBean<MapVersionReview> {

  @Override
  public ComparableVersion version() {
    return subject().version();
  }

  @Override
  public MapVersionReview withScoreAndText(int score, String text) {
    return new MapVersionReview(id(), text, player(), score, subject());
  }
}
