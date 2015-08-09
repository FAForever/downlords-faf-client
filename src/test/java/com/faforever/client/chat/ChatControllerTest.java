package com.faforever.client.chat;

import com.faforever.client.test.AbstractSpringJavaFxTest;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.stage.Stage;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TODO those unit tests need to be improved (missing verifications)
public class ChatControllerTest extends AbstractSpringJavaFxTest {

  public static final String TEST_CHANNEL_NAME = "#testChannel";
  private static final long TIMEOUT = 1000;
  private static final TimeUnit TIMEOUT_UNITS = TimeUnit.MILLISECONDS;
  @Autowired
  ChannelTabController channelTabController;

  @Autowired
  PrivateChatTabController privateChatTabController;

  private ChatController chatController;

  @Override
  public void start(Stage stage) throws Exception {
    chatController = loadController("chat.fxml");

    super.start(stage);
  }

  @Test
  public void testOnMessageForChannel() throws Exception {
    when(channelTabController.getRoot()).thenReturn(new Tab());
    ChatMessage chatMessage = new ChatMessage(Instant.now(), "junit", "message");

    CompletableFuture<ChatMessage> chatMessageCompletableFuture = new CompletableFuture<>();
    doAnswer(invocation -> {
      chatMessageCompletableFuture.complete((ChatMessage) invocation.getArguments()[0]);
      return null;
    }).when(channelTabController).onChatMessage(chatMessage);

    chatController.onMessage(TEST_CHANNEL_NAME, chatMessage);
    chatMessageCompletableFuture.get(TIMEOUT, TIMEOUT_UNITS);

    verify(channelTabController).onChatMessage(chatMessage);
  }

  @Test
  public void testOnDisconnected() throws Exception {
    chatController.onDisconnected(new Exception());
  }

  @Test
  public void testOnUserJoinedChannel() throws Exception {
    ChatUser chatUser = mock(ChatUser.class);
    chatController.onUserJoinedChannel(TEST_CHANNEL_NAME, chatUser);
  }

  @Test
  public void testOnPrivateMessage() throws Exception {
    ChatMessage chatMessage = mock(ChatMessage.class);
    chatController.onPrivateMessage("testSender", chatMessage);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(chatController.getRoot(), instanceOf(Node.class));
  }

  @Test(expected = IllegalStateException.class)
  public void testOpenPrivateMessageTabForUserNotOnApplicationThread() throws Exception {
    chatController.openPrivateMessageTabForUser("user");
  }

  @Test
  public void testOpenPrivateMessageTabForUser() throws Exception {
    when(privateChatTabController.getRoot()).thenReturn(new Tab());
    WaitForAsyncUtils.waitForAsyncFx(10, () -> chatController.openPrivateMessageTabForUser("user"));
  }

  @Test
  public void testOnChatUserLeftChannel() throws Exception {
    chatController.onChatUserLeftChannel("testUser", TEST_CHANNEL_NAME);
  }

  @Test
  public void testOnJoinChannelsRequest() throws Exception {
    when(channelTabController.getRoot()).thenReturn(new Tab());
    chatController.onJoinChannelsRequest(Arrays.asList(TEST_CHANNEL_NAME, TEST_CHANNEL_NAME));
  }
}
