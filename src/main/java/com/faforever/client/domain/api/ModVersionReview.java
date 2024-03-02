package com.faforever.client.domain.api;

import com.faforever.client.domain.server.PlayerInfo;
import org.apache.maven.artifact.versioning.ComparableVersion;

public record ModVersionReview(
    Integer id, String text, PlayerInfo player, Integer score, ModVersion subject
) implements ReviewBean<ModVersionReview> {

  @Override
  public ComparableVersion version() {
    return subject().version();
  }
  @Override
  public ModVersionReview withScoreAndText(int score, String text) {
    return new ModVersionReview(id(), text, player(), score, subject());
  }

}
