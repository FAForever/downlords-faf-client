package com.faforever.client.headerbar;

import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotificationsController;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
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
  private EventBus eventBus;

  @InjectMocks
  private HeaderBarController instance;

  @BeforeEach
  public void setup() throws Exception {
    when(persistentNotificationsController.getRoot()).thenReturn(new Pane());
    when(uiService.loadFxml("theme/persistent_notifications.fxml")).thenReturn(persistentNotificationsController);

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
    assertThat(instance.playButton.getPseudoClassStates().contains(HIGHLIGHTED), is(false));

    verify(eventBus).post(argThat(event -> ((NavigateEvent) event).getItem() == NavigationItem.PLAY));
  }

  @Test
  public void testOnNavigateEvent() {
    instance.onNavigateEvent(new NavigateEvent(NavigationItem.PLAY));

    assertThat(instance.mainNavigation.getSelectedToggle(), is(instance.playButton));

    instance.onNavigateEvent(new NavigateEvent(NavigationItem.CHAT));

    assertThat(instance.mainNavigation.getSelectedToggle(), is(instance.chatButton));
  }
}
