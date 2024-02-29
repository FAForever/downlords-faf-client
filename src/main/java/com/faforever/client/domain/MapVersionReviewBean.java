package com.faforever.client.domain;

import org.apache.maven.artifact.versioning.ComparableVersion;

public record MapVersionReviewBean(
    Integer id, String text, PlayerBean player, Integer score, MapVersionBean subject
) implements ReviewBean<MapVersionReviewBean, MapVersionBean> {

  @Override
  public ComparableVersion version() {
    return subject().getVersion();
  }

  @Override
  public MapVersionReviewBean withScoreAndText(int score, String text) {
    return new MapVersionReviewBean(id(), text, player(), score, subject());
  }
}
