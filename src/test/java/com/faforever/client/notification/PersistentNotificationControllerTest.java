package com.faforever.client.notification;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PersistentNotificationControllerTest extends AbstractPlainJavaFxTest {

  private PersistentNotificationController instance;

  @Before
  public void setUp() throws Exception {
    instance = loadController("persistent_notification.fxml");
    instance.notificationService = mock(NotificationService.class);
  }

  @Test
  public void testSetNotificationInfoNoAction() throws Exception {
    assertThat(instance.actionButtonsContainer.getChildren(), empty());

    PersistentNotification notification = new PersistentNotification("foo", Severity.INFO);
    instance.setNotification(notification);

    assertEquals("foo", instance.messageLabel.getText());
    assertEquals("\uf05a", instance.iconLabel.getText());
    assertThat(instance.actionButtonsContainer.getChildren(), hasSize(0));
  }

  @Test
  public void testSetNotificationWarningOneAction() throws Exception {
    assertThat(instance.actionButtonsContainer.getChildren(), empty());

    PersistentNotification notification = new PersistentNotification("foo", Severity.WARN,
        Collections.singletonList(
            new Action("title", null)
        )
    );
    instance.setNotification(notification);

    assertEquals("foo", instance.messageLabel.getText());
    assertEquals("\uf071", instance.iconLabel.getText());
    assertThat(instance.actionButtonsContainer.getChildren(), hasSize(1));
  }

  @Test
  public void testSetNotificationErrorNoAction() throws Exception {
    assertThat(instance.actionButtonsContainer.getChildren(), empty());

    PersistentNotification notification = new PersistentNotification("foo", Severity.ERROR);
    instance.setNotification(notification);

    assertEquals("foo", instance.messageLabel.getText());
    assertEquals("\uf06a", instance.iconLabel.getText());
    assertThat(instance.actionButtonsContainer.getChildren(), hasSize(0));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.notificationRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testOnCloseButtonClicked() throws Exception {
    PersistentNotification notification = new PersistentNotification("text", Severity.WARN);
    instance.setNotification(notification);
    instance.onCloseButtonClicked();
    verify(instance.notificationService).removeNotification(notification);
  }
}
