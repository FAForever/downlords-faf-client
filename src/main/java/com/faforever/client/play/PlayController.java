package com.faforever.client.play;

import com.faforever.client.chat.event.UnreadPartyMessageEvent;
import com.faforever.client.coop.CoopController;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.game.CustomGamesController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenCoopEvent;
import com.faforever.client.main.event.OpenCustomGamesEvent;
import com.faforever.client.main.event.OpenTeamMatchmakingEvent;
import com.faforever.client.teammatchmaking.TeamMatchmakingController;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.css.PseudoClass;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.skin.TabPaneSkin;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PlayController extends AbstractViewController<Node> {
  private static final PseudoClass UNREAD_PSEUDO_STATE = PseudoClass.getPseudoClass("unread");

  private final EventBus eventBus;
  public Node playRoot;
  public Tab teamMatchmakingTab;
  public Tab customGamesTab;
  public Tab coopTab;
  public TabPane playRootTabPane;
  public TeamMatchmakingController teamMatchmakingController;
  public CustomGamesController customGamesController;
  public CoopController coopController;
  private boolean isHandlingEvent;
  private AbstractViewController<?> lastTabController;
  private Tab lastTab;

  public PlayController(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public void initialize() {
    eventBus.register(this);
    lastTab = teamMatchmakingTab;
    lastTabController = teamMatchmakingController;
    playRootTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (isHandlingEvent) {
        return;
      }

      if (newValue == teamMatchmakingTab) {
        setMatchmakingTabUnread(false);
        eventBus.post(new OpenTeamMatchmakingEvent());
      } else if (newValue == customGamesTab) {
        eventBus.post(new OpenCustomGamesEvent());
      } else if (newValue == coopTab) {
        eventBus.post(new OpenCoopEvent());
      }
    });
  }

  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    isHandlingEvent = true;

    try {
      if (navigateEvent instanceof OpenTeamMatchmakingEvent) {
        lastTab = teamMatchmakingTab;
        lastTabController = teamMatchmakingController;
      } else if (navigateEvent instanceof OpenCustomGamesEvent) {
        lastTab = customGamesTab;
        lastTabController = customGamesController;
      } else if (navigateEvent instanceof OpenCoopEvent) {
        lastTab = coopTab;
        lastTabController = coopController;
      }
      playRootTabPane.getSelectionModel().select(lastTab);
      lastTabController.display(navigateEvent);
    } finally {
      isHandlingEvent = false;
    }
  }

  @Override
  public void onHide() {
    teamMatchmakingController.onHide();
    customGamesController.onHide();
    coopController.onHide();
  }

  @Override
  public Node getRoot() {
    return playRoot;
  }

  protected void setMatchmakingTabUnread(boolean unread) {
    TabPaneSkin skin = (TabPaneSkin) playRootTabPane.getSkin();
    if (skin == null) {
      return;
    }
    Node tab = (Node) skin.queryAccessibleAttribute(AccessibleAttribute.ITEM_AT_INDEX, 0);
    tab.pseudoClassStateChanged(UNREAD_PSEUDO_STATE, unread);
  }

  @Subscribe
  public void onUnreadPartyMessage(UnreadPartyMessageEvent event) {
    setMatchmakingTabUnread(true);
  }
}
