package com.faforever.client.chat;

import com.faforever.client.chat.event.ChatMessageEvent;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.scene.control.Tab;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.util.ReflectionUtils;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.natpryce.hamcrest.reflection.HasAnnotationMatcher.hasAnnotation;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TODO those unit tests need to be improved (missing verifications)
public class ChatControllerTest extends AbstractPlainJavaFxTest {

  public static final String TEST_USER_NAME = "junit";
  private static final String TEST_CHANNEL_NAME = "#testChannel";
  private static final long TIMEOUT = 1000;

  @Mock
  private ChannelTabController channelTabController;
  @Mock
  private PrivateChatTabController privateChatTabController;
  @Mock
  private UserService userService;
  @Mock
  private UiService uiService;
  @Mock
  private ChatService chatService;
  @Mock
  private EventBus eventBus;
  @Captor
  private ArgumentCaptor<MapChangeListener<String, Channel>> channelsListener;
  @Captor
  private ArgumentCaptor<MapChangeListener<String, ChatChannelUser>> onUsersListenerCaptor;

  private ChatController instance;
  private SimpleObjectProperty<ConnectionState> connectionState;

  @Before
  public void setUp() throws Exception {
    instance = new ChatController(chatService, uiService, userService, eventBus);

    connectionState = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);

    when(uiService.loadFxml("theme/chat/private_chat_tab.fxml")).thenReturn(privateChatTabController);
    when(uiService.loadFxml("theme/chat/channel_tab.fxml")).thenReturn(channelTabController);
    when(userService.getUsername()).thenReturn(TEST_USER_NAME);
    when(chatService.connectionStateProperty()).thenReturn(connectionState);

    loadFxml("theme/chat/chat.fxml", clazz -> instance);

    verify(chatService).addChannelsListener(channelsListener.capture());
  }

  @Test
  public void testOnMessageForChannel() throws Exception {
    when(channelTabController.getRoot()).thenReturn(new Tab());

    ChatMessage chatMessage = new ChatMessage(TEST_CHANNEL_NAME, Instant.now(), TEST_USER_NAME, "message");
    instance.onChatMessage(new ChatMessageEvent(chatMessage));
    WaitForAsyncUtils.waitForFxEvents();

    verify(channelTabController).onChatMessage(chatMessage);
    verify(privateChatTabController, never()).onChatMessage(chatMessage);
  }

  @Test
  public void testOnDisconnected() throws Exception {
    connectionState.set(ConnectionState.DISCONNECTED);
  }

  @Test
  public void testOnPrivateMessage() throws Exception {
    when(privateChatTabController.getRoot()).thenReturn(new Tab());
    ChatMessage chatMessage = new ChatMessage(null, Instant.now(), TEST_USER_NAME, "message");
    instance.onChatMessage(new ChatMessageEvent(chatMessage));
    WaitForAsyncUtils.waitForFxEvents();

    verify(privateChatTabController).onChatMessage(chatMessage);
    verify(channelTabController, never()).onChatMessage(chatMessage);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.chatRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testOpenPrivateMessageTabForUser() throws Exception {
    when(privateChatTabController.getRoot()).thenReturn(new Tab());
    WaitForAsyncUtils.waitForAsyncFx(TIMEOUT, () ->
        instance.onInitiatePrivateChatEvent(new InitiatePrivateChatEvent("user")));
  }

  @Test
  public void testOpenPrivateMessageTabForSelf() throws Exception {
    instance.onInitiatePrivateChatEvent(new InitiatePrivateChatEvent(TEST_USER_NAME));
  }

  @Test
  public void testOnChannelsJoinedRequest() throws Exception {
    channelJoined(TEST_CHANNEL_NAME);
    channelJoined(TEST_CHANNEL_NAME);

    connectionState.set(ConnectionState.DISCONNECTED);
  }

  @SuppressWarnings("unchecked")
  private void channelJoined(String channel) {
    MapChangeListener.Change<? extends String, ? extends Channel> testChannelChange = mock(MapChangeListener.Change.class);
    channelsListener.getValue().onChanged(testChannelChange);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOnJoinChannelButtonClicked() throws Exception {
    assertThat(instance.tabPane.getTabs(), is(empty()));

    Tab tab = new Tab();
    tab.setId(TEST_CHANNEL_NAME);

    when(channelTabController.getRoot()).thenReturn(tab);
    when(userService.getUsername()).thenReturn(TEST_USER_NAME);
    doAnswer(invocation -> {
      MapChangeListener.Change<? extends String, ? extends Channel> change = mock(MapChangeListener.Change.class);
      when(change.wasAdded()).thenReturn(true);
      // Error here is caused by a bug in IntelliJ
      when(change.getValueAdded()).thenReturn(new Channel(invocation.getArgument(0)));
      channelsListener.getValue().onChanged(change);
      return null;
    }).when(chatService).joinChannel(anyString());

    instance.channelNameTextField.setText(TEST_CHANNEL_NAME);
    instance.onJoinChannelButtonClicked();

    verify(chatService).joinChannel(TEST_CHANNEL_NAME);
    verify(chatService).addUsersListener(eq(TEST_CHANNEL_NAME), onUsersListenerCaptor.capture());

    MapChangeListener.Change<? extends String, ? extends ChatChannelUser> change = mock(MapChangeListener.Change.class);
    when(change.wasAdded()).thenReturn(true);
    // Error here is caused by a bug in IntelliJ
    when(change.getValueAdded()).thenReturn(new ChatChannelUser(TEST_USER_NAME, null, false));
    onUsersListenerCaptor.getValue().onChanged(change);

    CountDownLatch tabAddedLatch = new CountDownLatch(1);
    instance.tabPane.getTabs().addListener((InvalidationListener) observable -> tabAddedLatch.countDown());
    tabAddedLatch.await(2, TimeUnit.SECONDS);

    assertThat(instance.tabPane.getTabs(), hasSize(1));
    assertThat(instance.tabPane.getTabs().get(0).getId(), is(TEST_CHANNEL_NAME));
  }

  @Test
  public void testSubscribeAnnotations() {
    assertThat(ReflectionUtils.findMethod(
        instance.getClass(), "onInitiatePrivateChatEvent", InitiatePrivateChatEvent.class),
        hasAnnotation(Subscribe.class));
  }
}
