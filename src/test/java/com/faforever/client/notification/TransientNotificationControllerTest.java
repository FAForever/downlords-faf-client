package com.faforever.client.notification;

import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.notification.Action.ActionCallback;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.UITest;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static com.faforever.client.fx.MouseEvents.generateClick;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransientNotificationControllerTest extends UITest {

  @Mock
  PreferencesService preferencesService;

  private TransientNotificationController instance;

  @BeforeEach
  public void setUp() throws Exception {
    when(preferencesService.getPreferences()).thenReturn(PreferencesBuilder.create().defaultValues().get());
    instance = new TransientNotificationController(preferencesService);

    loadFxml("theme/transient_notification.fxml", clazz -> instance);
  }

  @Test
  public void testSetNotificationWithoutActions() throws Exception {
    Image image = new Image(getClass().getResource("/theme/images/default_achievement.png").toExternalForm());

    TransientNotification notification = new TransientNotification("title", "text", image);
    instance.setNotification(notification);

    assertEquals("title", instance.titleLabel.getText());
    assertEquals("text", instance.messageLabel.getText());
    assertEquals(image, instance.imageView.getImage());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.transientNotificationRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testOnRightClick() throws Exception {
    assertEquals("timeline has not been set", assertThrows(Exception.class, () -> instance.onClicked(generateClick(MouseButton.SECONDARY, 1))).getMessage());
  }

  @Test
  public void testOnLeftClick() throws Exception {
    TransientNotification notificationMock = mock(TransientNotification.class);
    ActionCallback actionMock = mock(ActionCallback.class);
    when(notificationMock.getActionCallback()).thenReturn(actionMock);
    instance.setNotification(notificationMock);
    MouseEvent mouseEvent = generateClick(MouseButton.PRIMARY, 1);
    instance.onClicked(mouseEvent);
    verify(actionMock).call(mouseEvent);
  }
}
