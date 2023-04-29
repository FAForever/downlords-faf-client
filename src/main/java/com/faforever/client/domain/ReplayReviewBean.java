package com.faforever.client.domain;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.maven.artifact.versioning.ComparableVersion;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Value
public class ReplayReviewBean extends ReviewBean {
  ObjectProperty<ReplayBean> replay = new SimpleObjectProperty<>();
  ReadOnlyObjectWrapper<ComparableVersion> stubVersion = new ReadOnlyObjectWrapper<>(new ComparableVersion("0"));

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
  public ObservableValue<ComparableVersion> versionProperty() {
    return stubVersion.getReadOnlyProperty();
  }

  @Override
  public ObservableValue<ComparableVersion> latestVersionProperty() {
    return stubVersion.getReadOnlyProperty();
  }
}
