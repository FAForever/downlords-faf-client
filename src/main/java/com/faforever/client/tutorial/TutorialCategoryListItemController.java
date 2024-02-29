package com.faforever.client.tutorial;


import com.faforever.client.domain.TutorialCategoryBean;
import com.faforever.client.fx.NodeController;
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
public class TutorialCategoryListItemController extends NodeController<Node> {
  public HBox root;
  public Label titleLabel;

  @Override
  public Node getRoot() {
    return root;
  }

  public void setCategory(TutorialCategoryBean category) {
    titleLabel.setText(category.category());
  }
}
