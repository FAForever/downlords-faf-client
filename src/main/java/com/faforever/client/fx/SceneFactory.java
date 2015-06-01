package com.faforever.client.fx;

import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public interface SceneFactory {

  Scene createScene(Stage stage, Region mainRoot, boolean resizable, WindowDecorator.WindowButtonType... buttons);
}
