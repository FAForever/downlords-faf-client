package com.faforever.client.notification;

import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.ToastPosition;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransientNotificationsControllerTest extends AbstractPlainJavaFxTest {

  private TransientNotificationsController instance;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private Preferences preferences;
  @Mock
  private NotificationsPrefs notificationPrefs;
  @Mock
  private ApplicationContext applicationContext;

  @Before
  public void setUp() throws Exception {
    instance = loadController("transient_notifications.fxml");
    instance.preferencesService = preferencesService;
    instance.applicationContext = applicationContext;

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getNotification()).thenReturn(notificationPrefs);
  }

  @Test
  public void testToastPositionTopLeft() throws Exception {
    when(notificationPrefs.getToastPosition()).thenReturn(ToastPosition.TOP_LEFT);
    instance.postConstruct();
    assertThat(instance.transientNotificationsRoot.getAlignment(), is(Pos.TOP_LEFT));
  }

  @Test
  public void testToastPositionTopRight() throws Exception {
    when(notificationPrefs.getToastPosition()).thenReturn(ToastPosition.TOP_RIGHT);
    instance.postConstruct();
    assertThat(instance.transientNotificationsRoot.getAlignment(), is(Pos.TOP_RIGHT));
  }

  @Test
  public void testToastPositionBottomRight() throws Exception {
    when(notificationPrefs.getToastPosition()).thenReturn(ToastPosition.BOTTOM_RIGHT);
    instance.postConstruct();
    assertThat(instance.transientNotificationsRoot.getAlignment(), is(Pos.BOTTOM_RIGHT));
  }

  @Test
  public void testToastPositionBottomLeft() throws Exception {
    when(notificationPrefs.getToastPosition()).thenReturn(ToastPosition.BOTTOM_LEFT);
    instance.postConstruct();
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
    when(applicationContext.getBean(TransientNotificationController.class)).thenReturn(controller);

    TransientNotification notification1 = new TransientNotification("title1", "text1");
    instance.addNotification(notification1);

    TransientNotification notification2 = new TransientNotification("title2", "text2");
    instance.addNotification(notification2);

    assertThat(instance.transientNotificationsRoot.getChildren(), hasSize(2));
    verify(controller).setNotification(notification1);
    verify(controller).setNotification(notification2);
  }
}
