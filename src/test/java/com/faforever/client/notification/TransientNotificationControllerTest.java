package com.faforever.client.notification;

import com.faforever.client.notification.Action.ActionCallback;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.faforever.client.fx.MouseEvents.generateClick;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransientNotificationControllerTest extends AbstractPlainJavaFxTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private TransientNotificationController instance;


  @Before
  public void setUp() throws Exception {
    PreferencesService preferencesService = new PreferencesService();
    preferencesService.postConstruct();
    instance = new TransientNotificationController(preferencesService);

    loadFxml("theme/transient_notification.fxml", clazz -> instance);
  }

  @Test
  public void testSetNotificationWithoutActions() throws Exception {
    Image image = new Image(getClass().getResource("/theme/images/close.png").toExternalForm());

    TransientNotification notification = new TransientNotification("title", "text", image);
    instance.setNotification(notification);

    assertEquals("title", instance.titleLabel.getText());
    assertEquals("text", instance.messageLabel.getText());
    assertEquals(image, instance.imageView.getImage());
  }

  @Test
  public void testGetRoot() throws Exception {
    Assert.assertThat(instance.getRoot(), is(instance.transientNotificationRoot));
    Assert.assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testOnRightClick() throws Exception {
    expectedException.expectMessage("timeline");
    instance.onClicked(generateClick(MouseButton.SECONDARY, 1));
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
