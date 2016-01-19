package com.faforever.client.chat;

import com.faforever.client.net.ConnectionState;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.user.UserService;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Tab;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TODO those unit tests need to be improved (missing verifications)
public class ChatControllerTest extends AbstractPlainJavaFxTest {

  public static final String TEST_USER_NAME = "junit";
  private static final String TEST_CHANNEL_NAME = "#testChannel";
  private static final long TIMEOUT = 1000;
  private static final TimeUnit TIMEOUT_UNITS = TimeUnit.MILLISECONDS;
  @Mock
  private ChannelTabController channelTabController;
  @Mock
  private PrivateChatTabController privateChatTabController;
  @Mock
  private UserService userService;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private ChatService chatService;
  @Captor
  private ArgumentCaptor<Consumer<List<String>>> joinChannelsRequestListenerCaptor;

  private ChatController instance;
  private SimpleObjectProperty<ConnectionState> connectionState;

  @Before
  public void setUp() throws Exception {
    instance = loadController("chat.fxml");
    instance.userService = userService;
    instance.chatService = chatService;
    instance.applicationContext = applicationContext;

    connectionState = new SimpleObjectProperty<>();

    when(applicationContext.getBean(PrivateChatTabController.class)).thenReturn(privateChatTabController);
    when(applicationContext.getBean(ChannelTabController.class)).thenReturn(channelTabController);
    when(userService.getUsername()).thenReturn(TEST_USER_NAME);
    when(chatService.connectionStateProperty()).thenReturn(connectionState);

    instance.postConstrut();

    verify(chatService).addOnJoinChannelsRequestListener(joinChannelsRequestListenerCaptor.capture());
  }

  @Test
  public void testOnMessageForChannel() throws Exception {
    when(channelTabController.getRoot()).thenReturn(new Tab());
    ChatMessage chatMessage = new ChatMessage(Instant.now(), TEST_USER_NAME, "message");

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
    connectionState.set(ConnectionState.DISCONNECTED);
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
  public void testOpenPrivateMessageTabForSelf() throws Exception {
    when(privateChatTabController.getRoot()).thenReturn(new Tab());
    instance.openPrivateMessageTabForUser(TEST_USER_NAME);
  }

  @Test
  public void testOnChatUserLeftChannel() throws Exception {
    instance.onChatUserLeftChannel("testUser", TEST_CHANNEL_NAME);
  }

  @Test
  public void testOnJoinChannelsRequest() throws Exception {
    when(channelTabController.getRoot()).thenReturn(new Tab());
    joinChannelsRequestListenerCaptor.getValue().accept(Arrays.asList(TEST_CHANNEL_NAME, TEST_CHANNEL_NAME));

    connectionState.set(ConnectionState.DISCONNECTED);
  }

  @Test
  public void onJoinChannel() throws Exception {
    Tab tab = new Tab();
    tab.setId(TEST_CHANNEL_NAME);

    when(channelTabController.getRoot()).thenReturn(tab);
    when(userService.getUsername()).thenReturn(TEST_USER_NAME);
    when(chatService.isDefaultChannel(TEST_CHANNEL_NAME)).thenReturn(false);

    instance.channelNameTextField.setText(TEST_CHANNEL_NAME);
    instance.onJoinChannel();

    verify(chatService).joinChannel(TEST_CHANNEL_NAME);
    assertThat(instance.chatsTabPane.getTabs(), is(empty()));

    instance.onUserJoinedChannel(TEST_CHANNEL_NAME, new ChatUser(TEST_USER_NAME, null));

    CountDownLatch tabAddedLatch = new CountDownLatch(1);
    instance.chatsTabPane.getTabs().addListener((InvalidationListener) observable -> tabAddedLatch.countDown());
    tabAddedLatch.await(2, TimeUnit.SECONDS);

    assertThat(instance.chatsTabPane.getTabs(), hasSize(1));
    assertThat(instance.chatsTabPane.getTabs().get(0).getId(), is(TEST_CHANNEL_NAME));
  }
}
