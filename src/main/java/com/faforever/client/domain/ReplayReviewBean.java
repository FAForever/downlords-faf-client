package com.faforever.client.domain;

import org.apache.maven.artifact.versioning.ComparableVersion;

public record ReplayReviewBean(
    Integer id, String text, PlayerBean player, Integer score, ReplayBean subject
) implements ReviewBean<ReplayReviewBean, ReplayBean> {

  @Override
  public ComparableVersion version() {
    return null;
  }

  @Override
  public ReplayReviewBean withScoreAndText(int score, String text) {
    return new ReplayReviewBean(id(), text, player(), score, subject());
  }
}
