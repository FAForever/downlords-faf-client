package com.faforever.client.notification;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PersistentNotificationControllerTest extends AbstractPlainJavaFxTest {

  private PersistentNotificationController instance;

  @Before
  public void setUp() throws Exception {
    instance = new PersistentNotificationController();
    instance.notificationService = mock(NotificationService.class);

    loadFxml("theme/persistent_notification.fxml", clazz -> instance);
  }

  @Test
  public void testSetNotificationInfoNoAction() throws Exception {
    assertThat(instance.actionButtonsContainer.getChildren(), empty());

    PersistentNotification notification = new PersistentNotification("foo", Severity.INFO);
    instance.setNotification(notification);

    assertEquals("foo", instance.messageLabel.getText());
    assertEquals("\uE88F", instance.iconLabel.getText());
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
    assertEquals("\uE002", instance.iconLabel.getText());
    assertThat(instance.actionButtonsContainer.getChildren(), hasSize(1));
  }

  @Test
  public void testSetNotificationErrorNoAction() throws Exception {
    assertThat(instance.actionButtonsContainer.getChildren(), empty());

    PersistentNotification notification = new PersistentNotification("foo", Severity.ERROR);
    instance.setNotification(notification);

    assertEquals("foo", instance.messageLabel.getText());
    assertEquals("\uE001", instance.iconLabel.getText());
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
