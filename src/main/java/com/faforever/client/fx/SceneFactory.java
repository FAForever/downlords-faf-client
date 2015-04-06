package com.faforever.client.fx;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public interface SceneFactory {

  Scene createScene(Stage stage, Parent mainRoot, boolean resizable, WindowDecorator.WindowButtonType... buttons);
}
