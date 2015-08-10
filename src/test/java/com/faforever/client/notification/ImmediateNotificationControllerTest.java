package com.faforever.client.notification;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.*;

public class ImmediateNotificationControllerTest extends AbstractPlainJavaFxTest {

  private ImmediateNotificationController instance;

  @Before
  public void setUp() throws Exception {
    instance = new ImmediateNotificationController();
    instance.titleLabel = new Label();
    instance.messageLabel = new Label();
    instance.buttonBar = new ButtonBar();
    instance.notificationRoot = new Pane();
  }

  @Test
  public void testSetNotificationWithoutActions() throws Exception {
    ImmediateNotification notification = new ImmediateNotification("title", "text", Severity.INFO);
    instance.setNotification(notification);

    assertEquals("title", instance.titleLabel.getText());
    assertEquals("text", instance.messageLabel.getText());
    assertThat(instance.buttonBar.getButtons(), empty());
  }

  @Test
  public void testSetNotificationWithActions() throws Exception {
    ImmediateNotification notification = new ImmediateNotification("title", "text", Severity.INFO,
        Collections.singletonList(
            new Action("actionTitle")
        ));
    instance.setNotification(notification);

    assertEquals("title", instance.titleLabel.getText());
    assertEquals("text", instance.messageLabel.getText());
    assertThat(instance.buttonBar.getButtons(), hasSize(1));
    assertEquals("actionTitle", ((Button) instance.buttonBar.getButtons().get(0)).getText());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertEquals(instance.notificationRoot, instance.getRoot());
  }
}
