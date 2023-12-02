package com.faforever.client.domain;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ReplayReviewsSummaryBean extends ReviewsSummaryBean {
  private final ObjectProperty<ReplayBean> replay = new SimpleObjectProperty<>();

  public ReplayBean getReplay() {
    return replay.get();
  }

  public ObjectProperty<ReplayBean> replayProperty() {
    return replay;
  }

  public void setReplay(ReplayBean replay) {
    this.replay.set(replay);
  }
}
