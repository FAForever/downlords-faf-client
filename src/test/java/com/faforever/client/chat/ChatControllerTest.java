package com.faforever.client.chat;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.user.UserService;
import javafx.scene.control.Tab;
import javafx.stage.Stage;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TODO those unit tests need to be improved (missing verifications)
public class ChatControllerTest extends AbstractPlainJavaFxTest {

  private static final String TEST_CHANNEL_NAME = "#testChannel";
  private static final long TIMEOUT = 1000;
  private static final TimeUnit TIMEOUT_UNITS = TimeUnit.MILLISECONDS;
  @Mock
  ChannelTabController channelTabController;
  @Mock
  PrivateChatTabController privateChatTabController;
  @Mock
  UserService userService;
  @Mock
  ApplicationContext applicationContext;
  @Mock
  ChatService chatService;
  private ChatController instance;

  @Override
  public void start(Stage stage) throws Exception {
    super.start(stage);

    instance = loadController("chat.fxml");
    instance.userService = userService;
    instance.chatService = chatService;
    instance.applicationContext = applicationContext;

    when(applicationContext.getBean(PrivateChatTabController.class)).thenReturn(privateChatTabController);
    when(applicationContext.getBean(ChannelTabController.class)).thenReturn(channelTabController);
    when(userService.getUsername()).thenReturn("junit");
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

    instance.onMessage(TEST_CHANNEL_NAME, chatMessage);
    chatMessageCompletableFuture.get(TIMEOUT, TIMEOUT_UNITS);

    verify(channelTabController).onChatMessage(chatMessage);
  }

  @Test
  public void testOnDisconnected() throws Exception {
    instance.onDisconnected(new Exception());
  }

  @Test
  public void testOnUserJoinedChannel() throws Exception {
    ChatUser chatUser = mock(ChatUser.class);
    instance.onUserJoinedChannel(TEST_CHANNEL_NAME, chatUser);
  }

  @Test
  public void testOnPrivateMessage() throws Exception {
    ChatMessage chatMessage = mock(ChatMessage.class);
    instance.onPrivateMessage("testSender", chatMessage);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.chatRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test(expected = IllegalStateException.class)
  public void testOpenPrivateMessageTabForUserNotOnApplicationThread() throws Exception {
    instance.openPrivateMessageTabForUser("user");
  }

  @Test
  public void testOpenPrivateMessageTabForUser() throws Exception {
    when(privateChatTabController.getRoot()).thenReturn(new Tab());
    WaitForAsyncUtils.waitForAsyncFx(TIMEOUT, () -> instance.openPrivateMessageTabForUser("user"));
  }

  @Test
  public void testOnChatUserLeftChannel() throws Exception {
    instance.onChatUserLeftChannel("testUser", TEST_CHANNEL_NAME);
  }

  @Test
  public void testOnJoinChannelsRequest() throws Exception {
    when(channelTabController.getRoot()).thenReturn(new Tab());
    instance.onJoinChannelsRequest(Arrays.asList(TEST_CHANNEL_NAME, TEST_CHANNEL_NAME));
  }
}
