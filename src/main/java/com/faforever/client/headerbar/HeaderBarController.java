package com.faforever.client.headerbar;

import com.faforever.client.chat.event.UnreadPartyMessageEvent;
import com.faforever.client.chat.event.UnreadPrivateMessageEvent;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.news.UnreadNewsEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class HeaderBarController implements Controller<HBox> {

  private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");

  private final EventBus eventBus;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public MenuButton mainMenuButton;
  public HBox mainHeader;
  public ToggleButton newsButton;
  public ToggleButton chatButton;
  public ToggleButton playButton;
  public ToggleButton replayButton;
  public ToggleButton mapButton;
  public ToggleButton modButton;
  public ToggleButton leaderboardsButton;
  public ToggleButton unitsButton;
  public ToggleGroup mainNavigation;
  public Pane leftMenuPane;
  public Pane rightMenuPane;
  /** Dropdown for when there is not enough room for all navigation buttons to be displayed. */
  public MenuButton navigationDropdown;

  private final Map<ToggleButton, NavigationItem> navigationItemMap = new HashMap<>();
  private final Map<NavigationItem, ToggleButton> toggleButtonMap = new HashMap<>();

  public void initialize() {
    navigationItemMap.put(newsButton, NavigationItem.NEWS);
    navigationItemMap.put(chatButton, NavigationItem.CHAT);
    navigationItemMap.put(playButton, NavigationItem.PLAY);
    navigationItemMap.put(replayButton, NavigationItem.REPLAY);
    navigationItemMap.put(mapButton, NavigationItem.MAP);
    navigationItemMap.put(modButton, NavigationItem.MOD);
    navigationItemMap.put(leaderboardsButton, NavigationItem.LEADERBOARD);
    navigationItemMap.put(unitsButton, NavigationItem.UNITS);

    navigationItemMap.forEach(((toggleButton, navigationItem) -> toggleButtonMap.put(navigationItem, toggleButton)));

    navigationDropdown.getItems().setAll(createMenuItemsFromNavigation());
    navigationDropdown.managedProperty().bind(navigationDropdown.visibleProperty());

    leftMenuPane.layoutBoundsProperty().addListener(observable -> {
      navigationDropdown.setVisible(false);
      leftMenuPane.getChildrenUnmodifiable().forEach(node -> {
        Bounds boundsInParent = node.getBoundsInParent();
        // First time this is called, minY is negative. This is hacky but better than wasting time investigating.
        boolean hasSpace = boundsInParent.getMinY() < 0
            || leftMenuPane.getLayoutBounds().contains(boundsInParent.getCenterX(), boundsInParent.getCenterY());
        if (!hasSpace) {
          navigationDropdown.setVisible(true);
        }
        node.setVisible(hasSpace);
      });
    });

    eventBus.register(this);
  }

  private List<MenuItem> createMenuItemsFromNavigation() {
    return leftMenuPane.getChildrenUnmodifiable().stream()
        .filter(ToggleButton.class::isInstance)
        .map(ToggleButton.class::cast)
        .map(menuButton -> {
          MenuItem menuItem = new MenuItem(menuButton.getText());
          menuItem.setOnAction(event -> eventBus.post(new NavigateEvent(navigationItemMap.get(menuButton))));
          return menuItem;
        })
        .toList();
  }

  @Subscribe
  public void onUnreadNews(UnreadNewsEvent event) {
    fxApplicationThreadExecutor.execute(() -> newsButton.pseudoClassStateChanged(HIGHLIGHTED, event.hasUnreadNews()));
  }

  @Subscribe
  public void onUnreadPartyMessage(UnreadPartyMessageEvent event) {
    fxApplicationThreadExecutor.execute(() -> playButton.pseudoClassStateChanged(HIGHLIGHTED, !Objects.equals(getSelectedNavigationItem(), NavigationItem.PLAY)));
  }

  @Subscribe
  public void onUnreadPrivateMessage(UnreadPrivateMessageEvent event) {
    fxApplicationThreadExecutor.execute(() -> chatButton.pseudoClassStateChanged(HIGHLIGHTED, !Objects.equals(getSelectedNavigationItem(), NavigationItem.CHAT)));
  }

  @Subscribe
  public void onNavigateEvent(NavigateEvent navigateEvent) {
    ToggleButton toggleButton = toggleButtonMap.get(navigateEvent.getItem());
    if (toggleButton != null) {
      toggleButton.setSelected(true);
    }
  }

  public HBox getRoot() {
    return mainHeader;
  }

  public void onNavigateButtonClicked(ActionEvent event) {
    ToggleButton source = (ToggleButton) event.getSource();
    source.pseudoClassStateChanged(HIGHLIGHTED, false);
    NavigateEvent navigateEvent = new NavigateEvent(navigationItemMap.get(source));
    log.trace("Navigating to {}", navigateEvent.getItem().toString());
    eventBus.post(navigateEvent);
  }

  private NavigationItem getSelectedNavigationItem() {
    return navigationItemMap.get((ToggleButton) mainNavigation.getSelectedToggle());
  }

  public List<Node> getNonCaptionNodes() {
    return List.of(leftMenuPane, navigationDropdown, rightMenuPane, mainMenuButton);
  }
}
