package com.faforever.client.headerbar;

import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.NodeController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.navigation.NavigationHandler;
import javafx.collections.SetChangeListener;
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

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class HeaderBarController extends NodeController<HBox> {

  private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");

  private final NavigationHandler navigationHandler;
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

  @Override
  protected void onInitialize() {
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

    navigationHandler.navigationEventProperty()
                     .map(NavigateEvent::getItem)
                     .when(showing)
                     .subscribe(this::onNavigateEvent);
    navigationHandler.getHighlightedItems().addListener((SetChangeListener<? super NavigationItem>) change -> {
      if (change.wasAdded()) {
        NavigationItem item = change.getElementAdded();
        ToggleButton toggleButton = toggleButtonMap.get(item);
        if (toggleButton != null) {
          toggleButton.pseudoClassStateChanged(HIGHLIGHTED, true);
        }
      } else if (change.wasRemoved()) {
        NavigationItem item = change.getElementAdded();
        ToggleButton toggleButton = toggleButtonMap.get(item);
        if (toggleButton != null) {
          toggleButton.pseudoClassStateChanged(HIGHLIGHTED, false);
        }
      }
    });
  }

  private List<MenuItem> createMenuItemsFromNavigation() {
    return leftMenuPane.getChildrenUnmodifiable().stream()
        .filter(ToggleButton.class::isInstance)
        .map(ToggleButton.class::cast)
        .map(menuButton -> {
          MenuItem menuItem = new MenuItem(menuButton.getText());
          menuItem.setOnAction(
              event -> navigationHandler.navigateTo(new NavigateEvent(navigationItemMap.get(menuButton))));
          return menuItem;
        })
        .toList();
  }

  public void onNavigateEvent(NavigationItem navigationItem) {
    ToggleButton toggleButton = toggleButtonMap.get(navigationItem);
    if (toggleButton != null) {
      fxApplicationThreadExecutor.execute(() -> toggleButton.setSelected(true));
    }
  }

  @Override
  public HBox getRoot() {
    return mainHeader;
  }

  public void onNavigateButtonClicked(ActionEvent event) {
    ToggleButton source = (ToggleButton) event.getSource();
    NavigationItem navigationItem = navigationItemMap.get(source);
    navigationHandler.removeHighlight(navigationItem);
    log.trace("Navigating to {}", navigationItem.toString());
    navigationHandler.navigateTo(new NavigateEvent(navigationItem));
  }

  public List<Node> getNonCaptionNodes() {
    return List.of(leftMenuPane, navigationDropdown, rightMenuPane, mainMenuButton);
  }
}
