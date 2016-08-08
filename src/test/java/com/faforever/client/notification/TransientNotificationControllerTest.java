package com.faforever.client.notification;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.scene.image.Image;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;

public class TransientNotificationControllerTest extends AbstractPlainJavaFxTest {

  private TransientNotificationController instance;

  @Before
  public void setUp() throws Exception {
    instance = loadController("transient_notification.fxml");
  }

  @Test
  public void testSetNotificationWithoutActions() throws Exception {
    Image image = new Image(getClass().getResource("/theme/images/tray_icon.png").toExternalForm());

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
}
