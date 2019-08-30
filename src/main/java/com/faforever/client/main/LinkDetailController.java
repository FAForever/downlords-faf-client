package com.faforever.client.main;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.PlatformService;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import lombok.Setter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LinkDetailController extends AbstractViewController<Node> {
  private final PlatformService platformService;
  public Label name;
  public HBox root;
  @Setter
  private String url;

  public LinkDetailController(PlatformService platformService) {
    this.platformService = platformService;
  }

  @Override
  public Node getRoot() {
    return root;
  }

  public void open() {
    platformService.showDocument(url);
  }

  public void setName(String name) {
    this.name.setText(name);
  }
}
