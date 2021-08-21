package com.faforever.client.domain;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Value
public class ModReviewsSummaryBean extends ReviewsSummaryBean {
  ObjectProperty<ModBean> mod = new SimpleObjectProperty<>();

  public ModBean getMod() {
    return mod.get();
  }

  public ObjectProperty<ModBean> modProperty() {
    return mod;
  }

  public void setMod(ModBean mod) {
    this.mod.set(mod);
  }
}
