package com.faforever.client.vault;

import com.faforever.client.fx.Controller;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Getter
public class VaultEntityShowRoomController implements Controller<Node> {

  @FXML
  private Label label;
  @FXML
  private VBox root;
  @FXML
  private Button moreButton;
  @FXML
  private FlowPane pane;

  @Override
  public void initialize() {
    root.managedProperty().bind(root.visibleProperty());
    pane.setUserData(moreButton);
  }

  public void addChildren(List<Node> children) {
    pane.getChildren().addAll(children);
    pane.getChildren().add(moreButton);
  }

}
