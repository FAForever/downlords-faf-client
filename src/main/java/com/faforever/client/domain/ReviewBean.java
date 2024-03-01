package com.faforever.client.domain;

import org.apache.maven.artifact.versioning.ComparableVersion;

public sealed interface ReviewBean<R extends ReviewBean<R>> permits MapVersionReviewBean, ModVersionReviewBean, ReplayReviewBean {
  Integer id();

  String text();

  PlayerBean player();

  Integer score();

  ComparableVersion version();

  R withScoreAndText(int score, String text);

}
