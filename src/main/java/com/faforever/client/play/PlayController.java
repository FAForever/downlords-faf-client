package com.faforever.client.play;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.game.CustomGamesController;
import com.faforever.client.map.HostMapInCustomGameEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PlayController extends AbstractViewController<Node> {
  public Node playRoot;
  private final EventBus eventBus;
  public Tab customGamesTab;
  public CustomGamesController customGamesController;

  public PlayController(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  public void initialze() {
    eventBus.register(this);
  }

  @Override
  public Node getRoot() {
    return playRoot;
  }

  @Subscribe
  public void onHostMapInCustomGameEvent(HostMapInCustomGameEvent event) {
    customGamesTab.getTabPane().getSelectionModel().select(customGamesTab);
  }
}
