package com.faforever.client.notification;

import com.faforever.client.test.UITest;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

public class PersistentNotificationControllerTest extends UITest {

  private PersistentNotificationController instance;

  @Mock
  private NotificationService notificationService;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new PersistentNotificationController(notificationService);

    loadFxml("theme/persistent_notification.fxml", clazz -> instance);
  }

  @Test
  public void testSetNotificationInfoNoAction() throws Exception {
    assertThat(instance.actionButtonsContainer.getChildren(), empty());

    PersistentNotification notification = new PersistentNotification("foo", Severity.INFO);
    instance.setNotification(notification);

    assertEquals("foo", instance.messageLabel.getText());
    assertThat(instance.icon.getStyleClass(), CoreMatchers.hasItem("info-icon"));
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
    assertThat(instance.icon.getStyleClass(), CoreMatchers.hasItem("warn-icon"));
    assertThat(instance.actionButtonsContainer.getChildren(), hasSize(1));
  }

  @Test
  public void testSetNotificationErrorNoAction() throws Exception {
    assertThat(instance.actionButtonsContainer.getChildren(), empty());

    PersistentNotification notification = new PersistentNotification("foo", Severity.ERROR);
    instance.setNotification(notification);

    assertEquals("foo", instance.messageLabel.getText());
    assertThat(instance.icon.getStyleClass(), CoreMatchers.hasItem("error-icon"));
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
    verify(notificationService).removeNotification(notification);
  }
}
