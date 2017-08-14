package com.faforever.client.play;

import com.faforever.client.fx.AbstractViewController;
import javafx.scene.Node;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PlayController extends AbstractViewController<Node> {
  public Node playRoot;

  @Override
  public Node getRoot() {
    return playRoot;
  }
}
