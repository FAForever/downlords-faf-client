package com.faforever.client.vault.review;

import com.faforever.client.player.Player;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.sql.Timestamp;
import java.util.Optional;

public class Review {
  private final ObjectProperty<String> id;
  private final StringProperty text;
  private final ObjectProperty<ComparableVersion> version;
  private final ObjectProperty<ComparableVersion> latestVersion;
  private final ObjectProperty<Player> player;
  private final ObjectProperty<Integer> score;
  private final ObjectProperty<Timestamp> updateTime;

  public Review() {
    id = new SimpleObjectProperty<>();
    text = new SimpleStringProperty();
    score = new SimpleObjectProperty<>();
    player = new SimpleObjectProperty<>();
    version = new SimpleObjectProperty<>();
    latestVersion = new SimpleObjectProperty<>();
    updateTime = new SimpleObjectProperty<>();
  }

  public static Review fromDto(com.faforever.client.api.dto.Review dto) {
    Review review = new Review();
    review.setId(dto.getId());
    review.setText(dto.getText());
    review.setScore(Optional.ofNullable(dto.getScore()).map(Byte::intValue).orElse(0));

    if (dto.getPlayer() != null) {
      review.setPlayer(Player.fromDto(dto.getPlayer()));
    }
    return review;
  }

  public String getId() {
    return id.get();
  }

  public void setId(String id) {
    this.id.set(id);
  }

  public ObjectProperty<String> idProperty() {
    return id;
  }

  public String getText() {
    return text.get();
  }

  public void setText(String text) {
    this.text.set(text);
  }

  public StringProperty textProperty() {
    return text;
  }

  public ComparableVersion getVersion() {
    return version.get();
  }

  public void setVersion(ComparableVersion version) {
    this.version.set(version);
  }

  public ObjectProperty<ComparableVersion> versionProperty() {
    return version;
  }

  public ComparableVersion getLatestVersion() {
    return latestVersion.get();
  }

  public void setLatestVersion(ComparableVersion latestVersion) {
    this.latestVersion.set(latestVersion);
  }

  public ObjectProperty<ComparableVersion> latestVersionProperty() {
    return latestVersion;
  }

  public Player getPlayer() {
    return player.get();
  }

  public void setPlayer(Player player) {
    this.player.set(player);
  }

  public ObjectProperty<Player> playerProperty() {
    return player;
  }

  public Integer getScore() {
    return score.get();
  }

  public void setScore(Integer score) {
    this.score.set(score);
  }

  public ObjectProperty<Integer> scoreProperty() {
    return score;
  }

  public Timestamp getUpdateTime() {
    return updateTime.get();
  }

  public void setUpdateTime(Timestamp updateTime) {
    this.updateTime.set(updateTime);
  }

  public ObjectProperty<Timestamp> updateTimeProperty() {
    return updateTime;
  }
}
