package com.faforever.client.notification;

import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.scene.image.Image;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class TransientNotificationControllerTest extends AbstractPlainJavaFxTest {

  private TransientNotificationController instance;

  @Mock
  private PreferencesService preferencesService;

  @Before
  public void setUp() throws Exception {
    instance = new TransientNotificationController(preferencesService);

    when(preferencesService.getPreferences()).thenReturn(new Preferences());

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
}
