package com.faforever.client.chat;

import com.faforever.client.test.AbstractSpringJavaFxTest;
import javafx.stage.Stage;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

public class ChannelTabControllerTest extends AbstractSpringJavaFxTest {

  private static final String CHANNEL_NAME = "#testChannel";
  private ChannelTabController instance;

  @Autowired
  ChatService chatService;

  @Override
  public void start(Stage stage) throws Exception {
    super.start(stage);

    instance = loadController("channel_tab.fxml");
  }

  @Test
  public void testGetMessagesWebView() throws Exception {
    assertNotNull(instance.getMessagesWebView());
  }

  @Test
  public void testGetMessageTextField() throws Exception {
    assertNotNull(instance.getMessageTextField());
  }

  @Test
  public void testSetChannelName() throws Exception {
    instance.setChannelName(CHANNEL_NAME);

    verify(chatService).addChannelUserListListener(eq(CHANNEL_NAME), any());
  }
}
