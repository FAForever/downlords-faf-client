package com.faforever.client.domain.api;

import com.faforever.client.domain.server.PlayerInfo;
import org.apache.maven.artifact.versioning.ComparableVersion;

public record ReplayReview(
    Integer id, String text, PlayerInfo player, Integer score, Replay subject
) implements ReviewBean<ReplayReview> {

  @Override
  public ComparableVersion version() {
    return null;
  }

  @Override
  public ReplayReview withScoreAndText(int score, String text) {
    return new ReplayReview(id(), text, player(), score, subject());
  }
}
