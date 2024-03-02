package com.faforever.client.domain;

import org.apache.maven.artifact.versioning.ComparableVersion;

public record ModVersionReviewBean(
    Integer id, String text, PlayerBean player, Integer score, ModVersionBean subject
) implements ReviewBean<ModVersionReviewBean> {

  @Override
  public ComparableVersion version() {
    return subject().version();
  }
  @Override
  public ModVersionReviewBean withScoreAndText(int score, String text) {
    return new ModVersionReviewBean(id(), text, player(), score, subject());
  }

}
