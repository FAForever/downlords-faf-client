package com.faforever.client.headerbar;

import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotificationsController;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HeaderBarControllerTest extends PlatformTest {

  private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");

  @Mock
  private PersistentNotificationsController persistentNotificationsController;
  @Mock
  private NotificationService notificationService;
  @Mock
  private UiService uiService;
  @Mock
  private NavigationHandler navigationHandler;

  @InjectMocks
  private HeaderBarController instance;

  @BeforeEach
  public void setup() throws Exception {
    when(persistentNotificationsController.getRoot()).thenReturn(new Pane());
    when(uiService.loadFxml("theme/persistent_notifications.fxml")).thenReturn(persistentNotificationsController);
    when(navigationHandler.navigationEventProperty()).thenReturn(new SimpleObjectProperty<>());
    when(navigationHandler.getHighlightedItems()).thenReturn(FXCollections.observableSet());

    loadFxml("theme/headerbar/header_bar.fxml", clazz -> {
      if (clazz == instance.getClass()) {
        return instance;
      }
      return mock(clazz);
    });
  }

  @Test
  public void testOnNavigateButtonClicked() throws Exception {
    instance.playButton.pseudoClassStateChanged(HIGHLIGHTED, true);
    instance.onNavigateButtonClicked(new ActionEvent(instance.playButton, Event.NULL_SOURCE_TARGET));

    verify(navigationHandler).navigateTo(argThat(event -> event.getItem() == NavigationItem.PLAY));
  }
}
