package com.faforever.client.vault;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
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

  public Label label;
  public VBox root;
  public Button moreButton;
  public FlowPane pane;

  @Override
  public void initialize() {
    JavaFxUtil.bindManagedToVisible(root);
  }

  public void setChildren(List<Node> children) {
    pane.getChildren().clear();
    pane.getChildren().addAll(children);
    pane.getChildren().add(moreButton);
  }
}
