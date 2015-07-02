package com.faforever.client.chat;

import com.faforever.client.test.AbstractSpringJavaFxTest;
import javafx.stage.Stage;
import org.junit.Test;

import static org.junit.Assert.*;

// FIXME how to handle concurrency?
public class ChannelTabTest extends AbstractSpringJavaFxTest {

  private ChannelTab channelTab;

  @Override
  public void start(Stage stage) throws Exception {
    channelTab = new ChannelTab("#testChannel");
    initBean(channelTab, "channelTab");

    super.start(stage);
  }

  @Test
  public void testGetMessagesWebView() throws Exception {
    assertNotNull(channelTab.getMessagesWebView());
  }

  @Test
  public void testGetMessageTextField() throws Exception {
    assertNotNull(channelTab.getMessageTextField());
  }
}
