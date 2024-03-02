package com.faforever.client.domain.api;

import com.faforever.client.domain.server.PlayerInfo;
import org.apache.maven.artifact.versioning.ComparableVersion;

public sealed interface ReviewBean<R extends ReviewBean<R>> permits MapVersionReview, ModVersionReview, ReplayReview {
  Integer id();

  String text();

  PlayerInfo player();

  Integer score();

  ComparableVersion version();

  R withScoreAndText(int score, String text);

}
