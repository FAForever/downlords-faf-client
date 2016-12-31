package com.faforever.client.notification;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.scene.control.Button;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;

public class ImmediateNotificationControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private WebViewConfigurer webViewConfigurer;

  private ImmediateNotificationController instance;

  @Before
  public void setUp() throws Exception {
    instance = new ImmediateNotificationController(webViewConfigurer);
    loadFxml("theme/immediate_notification.fxml", clazz -> instance);
  }

  @Test
  public void testSetNotificationWithoutActions() throws Exception {
    ImmediateNotification notification = new ImmediateNotification("title", "text", Severity.INFO);
    instance.setNotification(notification);

    WaitForAsyncUtils.waitForFxEvents();

    assertEquals("title", instance.titleLabel.getText());
    assertEquals("text", instance.errorMessageView.getEngine().getDocument().getDocumentElement().getTextContent());
    assertThat(instance.buttonBar.getButtons(), empty());
  }

  @Test
  public void testSetNotificationWithActions() throws Exception {
    ImmediateNotification notification = new ImmediateNotification("title", "text", Severity.INFO,
        Collections.singletonList(
            new Action("actionTitle")
        ));
    instance.setNotification(notification);

    WaitForAsyncUtils.waitForFxEvents();

    assertEquals("title", instance.titleLabel.getText());
    assertEquals("text", instance.errorMessageView.getEngine().getDocument().getDocumentElement().getTextContent());
    assertThat(instance.buttonBar.getButtons(), hasSize(1));
    assertEquals("actionTitle", ((Button) instance.buttonBar.getButtons().get(0)).getText());
  }

  @Test
  public void testGetRoot() throws Exception {
    Assert.assertThat(instance.getRoot(), is(instance.notificationRoot));
    Assert.assertThat(instance.getRoot().getParent(), is(nullValue()));
  }
}
