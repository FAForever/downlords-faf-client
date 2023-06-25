package com.faforever.client.headerbar;

import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotificationsController;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import javafx.css.PseudoClass;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NavigationButtonControllerTest extends UITest {

  private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");

  @Mock
  private PersistentNotificationsController persistentNotificationsController;
  @Mock
  private NotificationService notificationService;
  @Mock
  private UiService uiService;

  @InjectMocks
  private NotificationButtonController instance;

  @BeforeEach
  public void setup() throws Exception {
    when(persistentNotificationsController.getRoot()).thenReturn(new Pane());
    when(uiService.loadFxml("theme/persistent_notifications.fxml")).thenReturn(persistentNotificationsController);

    loadFxml("theme/headerbar/notification_button.fxml", clazz -> {
      if (clazz == instance.getClass()) {
        return instance;
      }
      return mock(clazz);
    });

    runOnFxThreadAndWait(() -> getRoot().getChildren().add(instance.getRoot()));
  }

  @Test
  public void testOnNotificationsButtonClicked() throws Exception {
    WaitForAsyncUtils.asyncFx(instance::onNotificationsButtonClicked);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.persistentNotificationsPopup.isShowing(), is(true));
  }

}
