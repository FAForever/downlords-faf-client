package com.faforever.client.fx;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Preloader extends javafx.application.Preloader {

  @Override
  public void start(Stage primaryStage) throws Exception {
    primaryStage.setScene(new Scene(new Pane(), 100, 100));

  }
}
