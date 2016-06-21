package com.faforever.client.chat;

import com.faforever.client.net.ConnectionState;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.user.UserService;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.scene.control.Tab;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
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
  private ArgumentCaptor<MapChangeListener<String, Channel>> channelsListener;
  @Captor
  private ArgumentCaptor<Consumer<ChatMessage>> onChannelMessageListenerCaptor;
  @Captor
  private ArgumentCaptor<Consumer<ChatMessage>> onPrivateMessageListenerCaptor;
  @Captor
  private ArgumentCaptor<MapChangeListener<String, ChatUser>> onUsersListenerCaptor;

  private ChatController instance;
  private SimpleObjectProperty<ConnectionState> connectionState;

  @Before
  public void setUp() throws Exception {
    instance = loadController("chat.fxml");
    instance.userService = userService;
    instance.chatService = chatService;
    instance.applicationContext = applicationContext;

    connectionState = new SimpleObjectProperty<>();
    BooleanProperty loggedInProperty = new SimpleBooleanProperty();

    when(applicationContext.getBean(PrivateChatTabController.class)).thenReturn(privateChatTabController);
    when(applicationContext.getBean(ChannelTabController.class)).thenReturn(channelTabController);
    when(userService.getUsername()).thenReturn(TEST_USER_NAME);
    when(userService.loggedInProperty()).thenReturn(loggedInProperty);
    when(chatService.connectionStateProperty()).thenReturn(connectionState);

    instance.postConstruct();
    verify(chatService).addChannelsListener(channelsListener.capture());
  }

  @Test
  public void testOnMessageForChannel() throws Exception {
    when(channelTabController.getRoot()).thenReturn(new Tab());
    ChatMessage chatMessage = new ChatMessage(TEST_CHANNEL_NAME, Instant.now(), TEST_USER_NAME, "message");

    CompletableFuture<ChatMessage> chatMessageCompletableFuture = new CompletableFuture<>();
    doAnswer(invocation -> {
      chatMessageCompletableFuture.complete((ChatMessage) invocation.getArguments()[0]);
      return null;
    }).when(channelTabController).onChatMessage(chatMessage);

    verify(chatService).addOnMessageListener(onChannelMessageListenerCaptor.capture());
    onChannelMessageListenerCaptor.getValue().accept(chatMessage);

    chatMessageCompletableFuture.get(TIMEOUT, TIMEOUT_UNITS);

    verify(channelTabController).onChatMessage(chatMessage);
  }

  @Test
  public void testOnDisconnected() throws Exception {
    connectionState.set(ConnectionState.DISCONNECTED);
  }

  @Test
  public void testOnPrivateMessage() throws Exception {
    ChatMessage chatMessage = mock(ChatMessage.class);

    verify(chatService).addOnPrivateChatMessageListener(onPrivateMessageListenerCaptor.capture());

    onPrivateMessageListenerCaptor.getValue().accept(chatMessage);
    // TODO assert something useful
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
  public void testOnChannelsJoinedRequest() throws Exception {
    when(channelTabController.getRoot()).thenReturn(new Tab());

    channelJoined(TEST_CHANNEL_NAME);
    channelJoined(TEST_CHANNEL_NAME);

    connectionState.set(ConnectionState.DISCONNECTED);
  }

  @SuppressWarnings("unchecked")
  private void channelJoined(String channel) {
    MapChangeListener.Change<? extends String, ? extends Channel> testChannelChange = mock(MapChangeListener.Change.class);
    when(testChannelChange.getKey()).thenReturn(channel);
    channelsListener.getValue().onChanged(testChannelChange);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOnJoinChannelButtonClicked() throws Exception {
    assertThat(instance.chatsTabPane.getTabs(), is(empty()));

    Tab tab = new Tab();
    tab.setId(TEST_CHANNEL_NAME);

    when(channelTabController.getRoot()).thenReturn(tab);
    when(userService.getUsername()).thenReturn(TEST_USER_NAME);
    when(chatService.isDefaultChannel(TEST_CHANNEL_NAME)).thenReturn(false);
    doAnswer(invocation -> {
      MapChangeListener.Change<? extends String, ? extends Channel> change = mock(MapChangeListener.Change.class);
      when(change.wasAdded()).thenReturn(true);
      when(change.getValueAdded()).thenReturn(new Channel(invocation.getArgumentAt(0, String.class)));
      channelsListener.getValue().onChanged(change);
      return null;
    }).when(chatService).joinChannel(anyString());

    instance.channelNameTextField.setText(TEST_CHANNEL_NAME);
    instance.onJoinChannelButtonClicked();

    verify(chatService).joinChannel(TEST_CHANNEL_NAME);
    verify(chatService).addUsersListener(eq(TEST_CHANNEL_NAME), onUsersListenerCaptor.capture());

    MapChangeListener.Change<? extends String, ? extends ChatUser> change = mock(MapChangeListener.Change.class);
    when(change.wasAdded()).thenReturn(true);
    when(change.getValueAdded()).thenReturn(new ChatUser(TEST_USER_NAME, null));
    onUsersListenerCaptor.getValue().onChanged(change);

    CountDownLatch tabAddedLatch = new CountDownLatch(1);
    instance.chatsTabPane.getTabs().addListener((InvalidationListener) observable -> tabAddedLatch.countDown());
    tabAddedLatch.await(2, TimeUnit.SECONDS);

    assertThat(instance.chatsTabPane.getTabs(), hasSize(1));
    assertThat(instance.chatsTabPane.getTabs().get(0).getId(), is(TEST_CHANNEL_NAME));
  }
}
