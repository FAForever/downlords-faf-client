package com.faforever.client.domain;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.maven.artifact.versioning.ComparableVersion;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Value
public class ReplayReviewBean extends ReviewBean {
  ObjectProperty<ReplayBean> replay = new SimpleObjectProperty<>();

  public ReplayBean getReplay() {
    return replay.get();
  }

  public ObjectProperty<ReplayBean> replayProperty() {
    return replay;
  }

  public void setReplay(ReplayBean replay) {
    this.replay.set(replay);
  }

  @Override
  public ComparableVersion getVersion() {
    return null;
  }

  @Override
  public ComparableVersion getLatestVersion() {
    return new ComparableVersion("0");
  }
}
