package com.faforever.client.vault.review;


import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.sql.Timestamp;

public class ReviewBuilder {

  private final Review review;

  public ReviewBuilder() {
    review = new Review();
  }

  public static ReviewBuilder create() {
    return new ReviewBuilder();
  }

  public ReviewBuilder defaultValues() {
    player(PlayerBuilder.create("junit").defaultValues().get())
        .id("test").text("test review").score(3).version(new ComparableVersion("1"))
        .latestVersion(new ComparableVersion("1"));
    return this;
  }

  public ReviewBuilder id(String id) {
    review.setId(id);
    return this;
  }

  public ReviewBuilder text(String text) {
    review.setId(text);
    return this;
  }

  public Review get() {
    return review;
  }

  public ReviewBuilder version(ComparableVersion version) {
    review.setVersion(version);
    return this;
  }

  public ReviewBuilder latestVersion(ComparableVersion latestVersion) {
    review.setLatestVersion(latestVersion);
    return this;
  }

  public ReviewBuilder player(Player player) {
    review.setPlayer(player);
    return this;
  }

  public ReviewBuilder score(int score) {
    review.setScore(score);
    return this;
  }

  public ReviewBuilder updateTime(Timestamp updateTime) {
    review.setUpdateTime(updateTime);
    return this;
  }
}
