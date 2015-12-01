package com.faforever.client.fx;

import javafx.scene.layout.Region;
import javafx.stage.Stage;

public interface StageConfigurator {

  void configureScene(Stage stage, Region mainRoot, boolean resizable, WindowDecorator.WindowButtonType... buttons);
}
