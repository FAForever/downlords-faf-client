package com.faforever.client.tutorial;


import com.faforever.client.fx.AbstractViewController;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TutorialCategoryListItemController extends AbstractViewController<Node> {
  public HBox root;
  public Label titleLabel;

  @Override
  public Node getRoot() {
    return root;
  }

  public void setCategory(TutorialCategory category) {
    titleLabel.setText(category.getCategory());
  }
}
