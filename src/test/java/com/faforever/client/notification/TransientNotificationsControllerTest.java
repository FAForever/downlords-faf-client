package com.faforever.client.notification;

import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.preferences.ToastPosition;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransientNotificationsControllerTest extends PlatformTest {

  @InjectMocks
  private TransientNotificationsController instance;

  @Mock
  private UiService uiService;
  @Spy
  private NotificationPrefs notificationPrefs;

  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/transient_notifications.fxml", clazz -> instance);
  }

  @Test
  public void testToastPositionTopLeft() throws Exception {
    notificationPrefs.setToastPosition(ToastPosition.TOP_LEFT);
    reinitialize(instance);
    assertThat(instance.transientNotificationsRoot.getAlignment(), is(Pos.TOP_LEFT));
  }

  @Test
  public void testToastPositionTopRight() throws Exception {
    notificationPrefs.setToastPosition(ToastPosition.TOP_RIGHT);
    reinitialize(instance);
    assertThat(instance.transientNotificationsRoot.getAlignment(), is(Pos.TOP_RIGHT));
  }

  @Test
  public void testToastPositionBottomRight() throws Exception {
    notificationPrefs.setToastPosition(ToastPosition.BOTTOM_RIGHT);
    reinitialize(instance);
    assertThat(instance.transientNotificationsRoot.getAlignment(), is(Pos.BOTTOM_RIGHT));
  }

  @Test
  public void testToastPositionBottomLeft() throws Exception {
    notificationPrefs.setToastPosition(ToastPosition.BOTTOM_LEFT);
    reinitialize(instance);
    assertThat(instance.transientNotificationsRoot.getAlignment(), is(Pos.BOTTOM_LEFT));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.transientNotificationsRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testAddNotification() throws Exception {
    TransientNotificationController controller = mock(TransientNotificationController.class);
    when(controller.getRoot()).thenReturn(new Pane()).thenReturn(new Pane());
    when(uiService.loadFxml("theme/transient_notification.fxml")).thenReturn(controller);

    TransientNotification notification1 = new TransientNotification("title1", "text1");
    instance.addNotification(notification1);

    TransientNotification notification2 = new TransientNotification("title2", "text2");
    instance.addNotification(notification2);

    assertThat(instance.transientNotificationsRoot.getChildren(), hasSize(2));
    verify(controller).setNotification(notification1);
    verify(controller).setNotification(notification2);
  }
}
