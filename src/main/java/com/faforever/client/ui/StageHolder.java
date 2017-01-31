package com.faforever.client.ui;

import javafx.stage.Stage;
import org.springframework.util.Assert;

public final class StageHolder {
  private static Stage stage;

  private StageHolder() {
    // Static class
  }

  public static Stage getStage() {
    Assert.state(stage != null, "Stage has not yet been set");
    return stage;
  }

  public static void setStage(Stage stage) {
    StageHolder.stage = stage;
  }
}
