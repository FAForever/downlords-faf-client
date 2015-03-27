package com.faforever.client.main;

import com.faforever.client.fxml.FxmlLoader;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class MainController {

  @FXML
  Node whatsNew;

  @FXML
  Node chat;

  @FXML
  Parent mainRoot;

  @Autowired
  private FxmlLoader fxmlLoader;

  @Autowired
  private Environment environment;

  public Node getWhatsNew() {
    return whatsNew;
  }

  public Node getChat() {
    return chat;
  }

  public void display(Stage stage) {
    Scene scene = new Scene(mainRoot, 800, 600);
    scene.getStylesheets().add(environment.getProperty("style"));

    stage.setScene(scene);
    stage.setTitle("FA Forever");
    stage.show();
  }
}
