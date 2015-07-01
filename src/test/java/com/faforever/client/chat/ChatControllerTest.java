package com.faforever.client.chat;

import com.faforever.client.test.AbstractSpringJavaFxTest;
import javafx.stage.Stage;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChatControllerTest extends AbstractSpringJavaFxTest {

  @Autowired
  ChatTabFactory chatTabFactory;

  private ChatController chatController;

  @Override
  public void start(Stage stage) throws Exception {
    chatController = loadController("chat.fxml");

    super.start(stage);
  }

  @Test
  public void testOnMessage() throws Exception {
    ChatMessage chatMessage = mock(ChatMessage.class);
    chatController.onMessage("#testChannel", chatMessage);
  }

  @Test
  public void testOnDisconnected() throws Exception {
    chatController.onDisconnected(new Exception());
  }

  @Test
  public void testOnUserJoinedChannel() throws Exception {
    ChatUser chatUser = mock(ChatUser.class);
    chatController.onUserJoinedChannel("#testChannel", chatUser);
  }

  @Test
  public void testOnPrivateMessage() throws Exception {
    ChatMessage chatMessage = mock(ChatMessage.class);
    chatController.onPrivateMessage("testSender", chatMessage);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertNotNull(chatController.getRoot());
  }

  @Test(expected = IllegalStateException.class)
  public void testOpenPrivateMessageTabForUserNotOnApplicationThread() throws Exception {
    chatController.openPrivateMessageTabForUser("user");
  }

  @Test
  public void testOpenPrivateMessageTabForUserOnApplicationThread() throws Exception {
    when(chatTabFactory.createPrivateMessageTab("user")).thenReturn(new PrivateChatTab("user"));
    WaitForAsyncUtils.waitForAsyncFx(10, () -> chatController.openPrivateMessageTabForUser("user"));
  }

  @Test
  public void testOnChatUserLeftChannel() throws Exception {
    chatController.onChatUserLeftChannel("testUser", "#testChannel");
  }

  @Test
  public void testOnJoinChannelsRequest() throws Exception {
    chatController.onJoinChannelsRequest(Arrays.asList("#testChannel", "#clanChannel"));
  }
}
