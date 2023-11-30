package com.faforever.client.chat;

import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.LoginService;
import com.faforever.commons.api.dto.MeResult;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.scene.control.Tab;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TODO those unit tests need to be improved (missing verifications)
public class ChatControllerTest extends PlatformTest {

  public static final String TEST_USER_NAME = "junit";
  private static final String TEST_CHANNEL_NAME = "#testChannel";

  @Mock
  private ChannelTabController channelTabController;
  @Mock
  private PrivateChatTabController privateChatTabController;
  @Mock
  private LoginService loginService;
  @Mock
  private UiService uiService;
  @Mock
  private ChatService chatService;
  @Mock
  private NotificationService notificationService;
  @Captor
  private ArgumentCaptor<MapChangeListener<String, ChatChannel>> channelsListener;

  @InjectMocks
  private ChatController instance;
  private SimpleObjectProperty<ConnectionState> connectionState;

  @BeforeEach
  public void setUp() throws Exception {
    connectionState = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);

    when(uiService.loadFxml("theme/chat/private_chat_tab.fxml")).thenReturn(privateChatTabController);
    when(uiService.loadFxml("theme/chat/channel_tab.fxml")).thenReturn(channelTabController);
    when(uiService.createShowingProperty(any())).thenReturn(new SimpleBooleanProperty(true));
    when(loginService.getUsername()).thenReturn(TEST_USER_NAME);
    when(loginService.getOwnUser()).thenReturn(new MeResult());
    when(chatService.connectionStateProperty()).thenReturn(connectionState);

    loadFxml("theme/chat/chat.fxml", clazz -> instance);

    verify(chatService).addChannelsListener(channelsListener.capture());
  }

  @Test
  public void testOnDisconnected() throws Exception {
    connectionState.set(ConnectionState.DISCONNECTED);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.chatRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testOnChannelsJoinedRequest() throws Exception {
    channelJoined(TEST_CHANNEL_NAME);

    connectionState.set(ConnectionState.DISCONNECTED);
  }

  private void channelJoined(String channel) {
    MapChangeListener.Change<? extends String, ? extends ChatChannel> testChannelChange = mock(MapChangeListener.Change.class);
    channelsListener.getValue().onChanged(testChannelChange);
  }

  @Test
  public void testOnJoinChannelButtonClicked() throws Exception {
    assertEquals(instance.tabPane.getTabs().size(), 1);

    Tab tab = new Tab();
    tab.setId(TEST_CHANNEL_NAME);

    when(channelTabController.getRoot()).thenReturn(tab);
    when(loginService.getUsername()).thenReturn(TEST_USER_NAME);
    doAnswer(invocation -> {
      MapChangeListener.Change<? extends String, ? extends ChatChannel> change = mock(MapChangeListener.Change.class);
      when(change.wasAdded()).thenReturn(true);
      doReturn(new ChatChannel(invocation.getArgument(0))).when(change).getValueAdded();
      channelsListener.getValue().onChanged(change);
      return null;
    }).when(chatService).joinChannel(anyString());

    instance.channelNameTextField.setText(TEST_CHANNEL_NAME);
    instance.onJoinChannelButtonClicked();

    verify(chatService).joinChannel(TEST_CHANNEL_NAME);

    CountDownLatch tabAddedLatch = new CountDownLatch(1);
    instance.tabPane.getTabs().addListener((InvalidationListener) observable -> tabAddedLatch.countDown());
    tabAddedLatch.await(2, TimeUnit.SECONDS);

    assertThat(instance.tabPane.getTabs(), hasSize(2));
    assertThat(instance.tabPane.getTabs().get(0).getId(), is(TEST_CHANNEL_NAME));
  }

  @Test
  public void testOnJoinChannelButtonClickedInvalidChannel() throws Exception {
    assertEquals(instance.tabPane.getTabs().size(), 1);

    Tab tab = new Tab();
    tab.setId(TEST_CHANNEL_NAME);

    instance.channelNameTextField.setText(TEST_CHANNEL_NAME.replace("#", ""));
    instance.onJoinChannelButtonClicked();

    verify(chatService).joinChannel(TEST_CHANNEL_NAME);
  }
}
