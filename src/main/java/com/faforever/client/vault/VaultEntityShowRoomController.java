package com.faforever.client.vault;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
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
public class VaultEntityShowRoomController extends NodeController<Node> {

  public Label label;
  public VBox root;
  public Button moreButton;
  public FlowPane pane;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(root);
    label.prefWidthProperty().bind(root.widthProperty());
  }

  public void setChildren(List<Node> children) {
    pane.getChildren().setAll(children);
    pane.getChildren().add(moreButton);
  }
}
