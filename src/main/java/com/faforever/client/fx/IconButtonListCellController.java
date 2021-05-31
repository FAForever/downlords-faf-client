package com.faforever.client.fx;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class IconButtonListCellController implements Controller<Node> {
  public HBox root;
  public Label label;
  public Button iconButton;
  public Region iconRegion;

  public Label getLabel() {
    return label;
  }

  public Button getIconButton() {
    return iconButton;
  }

  public Region getIconRegion() {
    return iconRegion;
  }

  public Node getRoot() {
    return root;
  }
}
