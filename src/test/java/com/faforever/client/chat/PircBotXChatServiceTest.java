package com.faforever.client.chat;

import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.SocialMessage;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.user.UserService;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.hash.Hashing;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.concurrent.Task;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UserChannelDao;
import org.pircbotx.UserLevel;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.OpEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.events.QuitEvent;
import org.pircbotx.hooks.events.UserListEvent;
import org.pircbotx.output.OutputChannel;
import org.pircbotx.output.OutputIRC;
import org.pircbotx.snapshot.ChannelSnapshot;
import org.pircbotx.snapshot.UserChannelDaoSnapshot;
import org.pircbotx.snapshot.UserSnapshot;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.faforever.client.chat.ChatColorMode.CUSTOM;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PircBotXChatServiceTest extends AbstractPlainJavaFxTest {

  public static final String CHAT_USER_NAME = "junit";
  public static final String CHAT_PASSWORD = "123";
  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();
  private static final long TIMEOUT = 300000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
  private static final String DEFAULT_CHANNEL_NAME = "#defaultChannel";
  private static final String OTHER_CHANNEL_NAME = "#otherChannel";
  private static final int IRC_SERVER_PORT = 123;
  private PircBotXChatService instance;

  private ChatUser chatUser1;
  private ChatUser chatUser2;

  @Mock
  private User user1;
  @Mock
  private User user2;
  @Mock
  private org.pircbotx.Channel defaultChannel;
  @Mock
  private org.pircbotx.Channel otherChannel;
  @Mock
  private PircBotX pircBotX;
  @Mock
  private Configuration configuration;
  @Mock
  private UserChannelDaoSnapshot daoSnapshot;
  @Mock
  private OutputIRC outputIrc;
  @Mock
  private UserService userService;
  @Mock
  private TaskService taskService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private Preferences preferences;
  @Mock
  private ChatPrefs chatPrefs;
  @Mock
  private I18n i18n;
  @Mock
  private PircBotXFactory pircBotXFactory;
  @Mock
  private UserChannelDao<User, org.pircbotx.Channel> userChannelDao;
  @Mock
  private MapProperty<String, Color> userToColorProperty;
  @Mock
  private ObjectProperty<ChatColorMode> chatColorMode;
  @Mock
  private FafService fafService;
  @Mock
  private ThreadPoolExecutor threadPoolExecutor;
  @Mock
  private ChannelSnapshot defaultChannelSnapshot;
  @Mock
  private ChannelSnapshot otherChannelSnapshot;
  @Mock
  private PlayerService playerService;
  @Mock
  private PlayerInfoBean playerInfoBean;

  @Captor
  private ArgumentCaptor<Consumer<SocialMessage>> socialMessageListenerCaptor;
  @Captor
  private ArgumentCaptor<Configuration> configurationCaptor;

  private CountDownLatch botShutdownLatch;
  private CompletableFuture<Object> botStartedFuture;

  @Before
  public void setUp() throws Exception {
    instance = new PircBotXChatService();
    instance.fafService = fafService;
    instance.userService = userService;
    instance.taskService = taskService;
    instance.playerService = playerService;
    instance.notificationService = notificationService;
    instance.i18n = i18n;
    instance.pircBotXFactory = pircBotXFactory;
    instance.preferencesService = preferencesService;
    instance.threadPoolExecutor = threadPoolExecutor;
    instance.defaultChannelName = DEFAULT_CHANNEL_NAME;

    BooleanProperty loggedInProperty = new SimpleBooleanProperty();

    botShutdownLatch = new CountDownLatch(1);

    userToColorProperty = new SimpleMapProperty<>(FXCollections.observableHashMap());
    chatColorMode = new SimpleObjectProperty<>(CUSTOM);

    when(userService.getUsername()).thenReturn(CHAT_USER_NAME);
    when(userService.getPassword()).thenReturn(CHAT_PASSWORD);
    when(userService.loggedInProperty()).thenReturn(loggedInProperty);

    when(defaultChannel.getName()).thenReturn(DEFAULT_CHANNEL_NAME);
    when(defaultChannelSnapshot.getName()).thenReturn(DEFAULT_CHANNEL_NAME);
    when(otherChannel.getName()).thenReturn(OTHER_CHANNEL_NAME);
    when(otherChannelSnapshot.getName()).thenReturn(OTHER_CHANNEL_NAME);
    when(pircBotX.getConfiguration()).thenReturn(configuration);
    when(pircBotX.sendIRC()).thenReturn(outputIrc);
    when(pircBotX.getUserChannelDao()).thenReturn(userChannelDao);

    doAnswer(
        invocation -> {
          WaitForAsyncUtils.async(() -> invocation.getArgumentAt(0, Task.class).run());
          return null;
        }
    ).when(threadPoolExecutor).execute(any(Task.class));

    doAnswer((InvocationOnMock invocation) -> {
      @SuppressWarnings("unchecked")
      PrioritizedTask<Boolean> prioritizedTask = invocation.getArgumentAt(0, PrioritizedTask.class);
      prioritizedTask.run();

      Future<Boolean> result = WaitForAsyncUtils.asyncFx(prioritizedTask::getValue);
      return completedFuture(result.get(1, TimeUnit.SECONDS));
    }).when(instance.taskService).submitTask(any());

    botStartedFuture = new CompletableFuture<>();
    doAnswer(invocation -> {
      botStartedFuture.complete(true);
      botShutdownLatch.await();
      return null;
    }).when(pircBotX).startBot();

    when(pircBotXFactory.createPircBotX(any())).thenReturn(pircBotX);

    instance.ircHost = LOOPBACK_ADDRESS.getHostAddress();
    instance.ircPort = IRC_SERVER_PORT;
    instance.reconnectDelay = 100;

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getChat()).thenReturn(chatPrefs);
    when(chatPrefs.getChatColorMode()).thenReturn(chatColorMode.get());
    when(chatPrefs.getUserToColor()).thenReturn(userToColorProperty);

    when(chatPrefs.userToColorProperty()).thenReturn(userToColorProperty);
    when(chatPrefs.chatColorModeProperty()).thenReturn(chatColorMode);


    when(user1.getNick()).thenReturn("user1");
    when(user1.getChannels()).thenReturn(ImmutableSortedSet.of(defaultChannel));
    when(user1.getUserLevels(defaultChannel)).thenReturn(ImmutableSortedSet.of(UserLevel.VOICE));

    when(user2.getNick()).thenReturn("user2");
    when(user2.getChannels()).thenReturn(ImmutableSortedSet.of(defaultChannel));
    when(user2.getUserLevels(defaultChannel)).thenReturn(ImmutableSortedSet.of(UserLevel.VOICE));


    chatUser1 = instance.createOrGetChatUser(user1);
    chatUser2 = instance.createOrGetChatUser(user2);

    instance.postConstruct();

    verify(fafService).addOnMessageListener(eq(SocialMessage.class), socialMessageListenerCaptor.capture());
  }

  @After
  public void tearDown() {
    instance.close();
    botShutdownLatch.countDown();
  }

  @Test
  public void testOnChatUserList() throws Exception {
    Channel channel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME);
    assertThat(channel.getUsers(), empty());

    when(user1.compareTo(user2)).thenReturn(-1);
    when(user2.compareTo(user1)).thenReturn(1);

    connect();
    CountDownLatch usersJoinedLatch = new CountDownLatch(2);
    instance.addUsersListener(channel.getName(), change -> {
      if (change.wasAdded()) {
        usersJoinedLatch.countDown();
      }
    });

    firePircBotXEvent(new UserListEvent(pircBotX, defaultChannel, ImmutableSortedSet.of(user1, user2), true));

    assertTrue(usersJoinedLatch.await(TIMEOUT, TIMEOUT_UNIT));
    assertThat(channel.getUsers(), hasSize(2));
    assertThat(channel.getUser(chatUser1.getUsername()), sameInstance(chatUser1));
    assertThat(channel.getUser(chatUser2.getUsername()), sameInstance(chatUser2));
  }

  private void connect() throws Exception {
    CountDownLatch joinChannelLatch = new CountDownLatch(1);
    doAnswer(invocation -> {
      joinChannelLatch.countDown();
      return null;
    }).when(outputIrc).joinChannel(DEFAULT_CHANNEL_NAME);

    instance.connect();
    verify(pircBotXFactory).createPircBotX(configurationCaptor.capture());

    CountDownLatch latch = listenForConnected();
    firePircBotXEvent(new ConnectEvent(pircBotX));
    latch.await(TIMEOUT, TIMEOUT_UNIT);

    joinChannelLatch.countDown();

    SocialMessage socialMessage = new SocialMessage();
    socialMessage.setChannels(Collections.emptyList());

    socialMessageListenerCaptor.getValue().accept(socialMessage);
  }

  private void firePircBotXEvent(Event event) {
    configurationCaptor.getValue().getListenerManager().onEvent(event);
  }

  private CountDownLatch listenForConnected() {
    CountDownLatch latch = new CountDownLatch(1);
    instance.connectionStateProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == ConnectionState.CONNECTED) {
        latch.countDown();
      }
    });
    return latch;
  }

  @Test
  public void testOnUserJoinedChannel() throws Exception {
    Channel channel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME);
    assertThat(channel.getUsers(), empty());

    connect();

    joinChannel(defaultChannel, user1);
    joinChannel(defaultChannel, user2);

    assertThat(channel.getUsers(), hasSize(2));
    assertThat(channel.getUser(chatUser1.getUsername()), sameInstance(chatUser1));
    assertThat(channel.getUser(chatUser2.getUsername()), sameInstance(chatUser2));
  }

  private void joinChannel(org.pircbotx.Channel channel, User user) throws Exception {
    CompletableFuture<ChatUser> future = listenForUserJoined(channel);
    firePircBotXEvent(createJoinEvent(channel, user));
    future.get(TIMEOUT, TIMEOUT_UNIT);
  }

  private CompletableFuture<ChatUser> listenForUserJoined(org.pircbotx.Channel channel) {
    CompletableFuture<ChatUser> future = new CompletableFuture<>();
    instance.addUsersListener(channel.getName(), change -> {
      if (change.wasAdded()) {
        future.complete(change.getValueAdded());
      }
    });
    return future;
  }

  @NotNull
  private JoinEvent createJoinEvent(org.pircbotx.Channel channel, User user) {
    return new JoinEvent(pircBotX, channel, user, user);
  }

  @Test
  public void testOnChatUserLeftChannel() throws Exception {
    Channel channel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME);
    assertThat(channel.getUsers(), empty());

    connect();

    joinChannel(defaultChannel, user1);

    CompletableFuture<ChatUser> user2JoinedFuture = listenForUserJoined(defaultChannel);
    firePircBotXEvent(createJoinEvent(defaultChannel, user2));
    user2JoinedFuture.get(TIMEOUT, TIMEOUT_UNIT);

    CompletableFuture<ChatUser> user1PartFuture = listenForUserParted(defaultChannel);
    firePircBotXEvent(createPartEvent(defaultChannel, user1));
    user1PartFuture.get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(channel.getUsers(), hasSize(1));
    assertThat(channel.getUser(chatUser2.getUsername()), sameInstance(chatUser2));
  }

  private CompletableFuture<ChatUser> listenForUserParted(org.pircbotx.Channel channel) {
    CompletableFuture<ChatUser> future = new CompletableFuture<>();
    instance.addUsersListener(channel.getName(), change -> {
      if (change.wasRemoved()) {
        future.complete(change.getValueRemoved());
      }
    });
    return future;
  }

  @NotNull
  private PartEvent createPartEvent(org.pircbotx.Channel channel, User user) {
    return new PartEvent(pircBotX, daoSnapshot, defaultChannelSnapshot, user, new UserSnapshot(user), "");
  }

  @Test
  public void testOnChatUserQuit() throws Exception {
    Channel channel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME);
    assertThat(channel.getUsers(), empty());

    connect();

    joinChannel(defaultChannel, user1);
    joinChannel(defaultChannel, user2);
    quit(user1);

    assertThat(channel.getUsers(), hasSize(1));
    assertThat(channel.getUser(chatUser2.getUsername()), sameInstance(chatUser2));
  }

  private void quit(User user) throws Exception {
    CompletableFuture<ChatUser> future = listenForUserQuit();
    firePircBotXEvent(createQuitEvent(user));
    future.get(TIMEOUT, TIMEOUT_UNIT);
  }

  private CompletableFuture<ChatUser> listenForUserQuit() {
    CompletableFuture<ChatUser> future = new CompletableFuture<>();
    instance.addUsersListener(DEFAULT_CHANNEL_NAME, change -> {
      if (change.wasRemoved()) {
        future.complete(change.getValueRemoved());
      }
    });
    return future;
  }

  private QuitEvent createQuitEvent(User user) {
    return new QuitEvent(pircBotX, daoSnapshot, user, new UserSnapshot(user), "");
  }

  @Test
  public void testAddOnMessageListenerWithMessage() throws Exception {
    CompletableFuture<String> channelNameFuture = new CompletableFuture<>();
    CompletableFuture<ChatMessage> chatMessageFuture = new CompletableFuture<>();
    instance.addOnMessageListener(chatMessage -> {
      channelNameFuture.complete(chatMessage.getSource());
      chatMessageFuture.complete(chatMessage);
    });

    String message = "chat message";

    Channel channel = mock(Channel.class);
    when(channel.getName()).thenReturn(DEFAULT_CHANNEL_NAME);

    connect();

    CompletableFuture<ChatMessage> messageFuture = listenForMessage();
    firePircBotXEvent(createMessageEvent(defaultChannel, user1, message));
    messageFuture.get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(channelNameFuture.get(), is(DEFAULT_CHANNEL_NAME));
    assertThat(chatMessageFuture.get().getMessage(), is(message));
    assertThat(chatMessageFuture.get().getUsername(), is(chatUser1.getUsername()));
    assertThat(chatMessageFuture.get().getTime(), is(greaterThan(Instant.ofEpochMilli(System.currentTimeMillis() - 1000))));
    assertThat(chatMessageFuture.get().isAction(), is(false));
  }

  private CompletableFuture<ChatMessage> listenForMessage() {
    CompletableFuture<ChatMessage> future = new CompletableFuture<>();
    instance.addOnMessageListener(future::complete);
    return future;
  }

  private MessageEvent createMessageEvent(org.pircbotx.Channel channel, User user, String message) {
    return new MessageEvent(pircBotX, channel, channel.getName(), user, user, message, null);
  }

  @Test
  public void testAddOnMessageListenerWithAction() throws Exception {
    String action = "chat action";

    connect();
    CompletableFuture<ChatMessage> messageFuture = listenForMessage();
    firePircBotXEvent(createActionEvent(defaultChannel, user1, action));
    messageFuture.get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(messageFuture.get().getSource(), is(defaultChannel.getName()));
    assertThat(messageFuture.get().getMessage(), is(action));
    assertThat(messageFuture.get().getUsername(), is(chatUser1.getUsername()));
    assertThat(messageFuture.get().getTime(), is(greaterThan(Instant.ofEpochMilli(System.currentTimeMillis() - 10_000))));
    assertThat(messageFuture.get().isAction(), is(true));
  }

  private ActionEvent createActionEvent(org.pircbotx.Channel channel, User user, String action) {
    return new ActionEvent(pircBotX, user, user, channel, channel.getName(), action);
  }

  @Test
  public void testAddOnPrivateChatMessageListener() throws Exception {
    CompletableFuture<String> usernameFuture = new CompletableFuture<>();
    CompletableFuture<ChatMessage> chatMessageFuture = new CompletableFuture<>();
    instance.addOnPrivateChatMessageListener(chatMessage -> {
      usernameFuture.complete(chatMessage.getSource());
      chatMessageFuture.complete(chatMessage);
    });

    String message = "private message";

    User user = mock(User.class);
    when(user.getNick()).thenReturn(chatUser1.getUsername());

    Channel channel = mock(Channel.class);
    when(channel.getName()).thenReturn(DEFAULT_CHANNEL_NAME);

    connect();
    firePircBotXEvent(createPrivateMessageEvent(user, message));

    assertThat(chatMessageFuture.get().getMessage(), is(message));
    assertThat(chatMessageFuture.get().getUsername(), is(chatUser1.getUsername()));
    assertThat(chatMessageFuture.get().getTime(), is(greaterThan(Instant.ofEpochMilli(System.currentTimeMillis() - 1000))));
    assertThat(chatMessageFuture.get().isAction(), is(false));
  }

  private PrivateMessageEvent createPrivateMessageEvent(User sender, String message) {
    return new PrivateMessageEvent(pircBotX, sender, sender, message);
  }

  @Test
  public void testAddOnChatConnectedListener() throws Exception {
    CompletableFuture<Boolean> onChatConnectedFuture = new CompletableFuture<>();

    instance.connectionStateProperty().addListener((observable, oldValue, newValue) -> {
      switch (newValue) {
        case CONNECTED:
          onChatConnectedFuture.complete(null);
          break;
      }
    });

    String password = "123";
    when(userService.getPassword()).thenReturn(password);

    connect();

    assertThat(onChatConnectedFuture.get(TIMEOUT, TIMEOUT_UNIT), is(nullValue()));
  }

  @Test
  public void testAddOnChatDisconnectedListener() throws Exception {
    CompletableFuture<Void> onChatDisconnectedFuture = new CompletableFuture<>();
    instance.connectionStateProperty().addListener((observable, oldValue, newValue) -> {
      switch (newValue) {
        case DISCONNECTED:
          onChatDisconnectedFuture.complete(null);
          break;
      }
    });

    connect();
    CompletableFuture<Void> future = listenForDisconnected();
    firePircBotXEvent(new DisconnectEvent(pircBotX, daoSnapshot, null));
    future.get(TIMEOUT, TIMEOUT_UNIT);

    onChatDisconnectedFuture.get(TIMEOUT, TIMEOUT_UNIT);
  }

  private CompletableFuture<Void> listenForDisconnected() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    instance.connectionStateProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == ConnectionState.DISCONNECTED) {
        future.complete(null);
      }
    });
    return future;
  }

  @Test
  public void testAddOnModeratorSetListener() throws Exception {
    connect();

    joinChannel(defaultChannel, user1);

    assertThat(chatUser1.getModeratorInChannels(), empty());

    CompletableFuture<Set<String>> user1OpEvent = listenForUserOp(chatUser1);
    firePircBotXEvent(createOpEvent(defaultChannel, user1));
    user1OpEvent.get(TIMEOUT, TIMEOUT_UNIT);

    ObservableSet<String> moderatorInChannels = chatUser1.getModeratorInChannels();
    assertThat(moderatorInChannels, hasSize(1));
    assertThat(moderatorInChannels.iterator().next(), is(DEFAULT_CHANNEL_NAME));
  }

  @SuppressWarnings("unchecked")
  private CompletableFuture<Set<String>> listenForUserOp(ChatUser chatUser) {
    CompletableFuture<Set<String>> future = new CompletableFuture<>();
    chatUser.getModeratorInChannels().addListener((SetChangeListener<String>) change -> {
      if (change.wasAdded()) {
        future.complete((Set<String>) change.getSet());
      }
    });
    return future;
  }

  private OpEvent createOpEvent(org.pircbotx.Channel channel, User user) {
    return new OpEvent(pircBotX, channel, user, user, user, user, true);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testConnect() throws Exception {
    ArgumentCaptor<Configuration> captor = ArgumentCaptor.forClass(Configuration.class);
    when(userService.getUid()).thenReturn(681);

    connect();
    botStartedFuture.get(TIMEOUT, TIMEOUT_UNIT);

    verify(pircBotX).startBot();
    verify(pircBotXFactory).createPircBotX(captor.capture());
    Configuration configuration = captor.getValue();

    assertThat(configuration.getName(), is(CHAT_USER_NAME));
    assertThat(configuration.getLogin(), is("681"));
    assertThat(configuration.getRealName(), is(CHAT_USER_NAME));
    assertThat(configuration.getServers().get(0).getHostname(), is(LOOPBACK_ADDRESS.getHostAddress()));
    assertThat(configuration.getServers().get(0).getPort(), is(IRC_SERVER_PORT));
  }

  @Test
  public void testReconnect() throws Exception {
    CompletableFuture<Boolean> firstStartFuture = new CompletableFuture<>();
    CompletableFuture<Boolean> secondStartFuture = new CompletableFuture<>();
    doAnswer(invocation -> {
      if (!firstStartFuture.isDone()) {
        firstStartFuture.complete(true);
        throw new IOException("test exception");
      }

      secondStartFuture.complete(true);
      botShutdownLatch.await();
      return null;
    }).when(pircBotX).startBot();

    connect();
    firstStartFuture.get(TIMEOUT, TIMEOUT_UNIT);
    secondStartFuture.get(TIMEOUT, TIMEOUT_UNIT);

    verify(pircBotX, times(2)).startBot();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSendMessageInBackground() throws Exception {
    connect();

    String message = "test message";

    CompletableFuture<String> future = instance.sendMessageInBackground(DEFAULT_CHANNEL_NAME, message);

    assertThat(future.get(TIMEOUT, TIMEOUT_UNIT), is(message));
    verify(outputIrc).message(DEFAULT_CHANNEL_NAME, message);
  }

  @Test
  public void testGetChatUsersForChannelEmpty() throws Exception {
    Channel channel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME);
    assertThat(channel.getUsers(), empty());
  }

  @Test
  public void testGetChatUsersForChannelTwoUsersInDifferentChannel() throws Exception {
    connect();
    joinChannel(defaultChannel, user1);
    joinChannel(otherChannel, user2);

    List<ChatUser> usersInDefaultChannel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME).getUsers();
    assertThat(usersInDefaultChannel, hasSize(1));
    assertThat(usersInDefaultChannel.iterator().next(), sameInstance(chatUser1));

    List<ChatUser> usersInOtherChannel = instance.getOrCreateChannel(OTHER_CHANNEL_NAME).getUsers();
    assertThat(usersInOtherChannel, hasSize(1));
    assertThat(usersInOtherChannel.iterator().next(), sameInstance(chatUser2));
  }

  @Test
  public void testGetChatUsersForChannelTwoUsersInSameChannel() throws Exception {
    connect();
    joinChannel(defaultChannel, user1);
    joinChannel(defaultChannel, user2);

    Channel channel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME);

    List<ChatUser> users = channel.getUsers();
    assertThat(users, hasSize(2));
    assertThat(users, containsInAnyOrder(chatUser1, chatUser2));
  }

  @Test
  public void testAddChannelUserListListener() throws Exception {
    connect();
    @SuppressWarnings("unchecked")
    MapChangeListener<String, ChatUser> listener = mock(MapChangeListener.class);

    instance.addUsersListener(DEFAULT_CHANNEL_NAME, listener);

    joinChannel(defaultChannel, user1);
    joinChannel(defaultChannel, user2);

    verify(listener, times(2)).onChanged(any());
  }

  @Test
  public void testLeaveChannel() throws Exception {
    OutputChannel outputChannel = mock(OutputChannel.class);

    when(userChannelDao.getChannel(DEFAULT_CHANNEL_NAME)).thenReturn(defaultChannel);
    when(defaultChannel.send()).thenReturn(outputChannel);

    instance.connect();
    instance.leaveChannel(DEFAULT_CHANNEL_NAME);

    verify(outputChannel).part();
  }

  @Test
  public void testSendActionInBackground() throws Exception {
    connect();

    String action = "test action";

    CompletableFuture<String> future = instance.sendActionInBackground(DEFAULT_CHANNEL_NAME, action);

    assertThat(future.get(TIMEOUT, TIMEOUT_UNIT), is(action));
    verify(outputIrc).action(DEFAULT_CHANNEL_NAME, action);
  }

  @Test
  public void testUsersListenerJoinPart() throws Exception {
    connect();

    CompletableFuture<ChatUser> userJoinedFuture = listenForUserJoined(defaultChannel);
    CompletableFuture<ChatUser> userPartFuture = listenForUserParted(defaultChannel);

    firePircBotXEvent(createJoinEvent(defaultChannel, user1));
    assertThat(userJoinedFuture.get(TIMEOUT, TIMEOUT_UNIT), is(chatUser1));

    firePircBotXEvent(createPartEvent(defaultChannel, user1));
    assertThat(userPartFuture.get(TIMEOUT, TIMEOUT_UNIT), is(chatUser1));
  }

  @Test
  public void testUsersListenerJoinQuit() throws Exception {
    connect();

    CompletableFuture<ChatUser> userJoinedFuture = listenForUserJoined(defaultChannel);
    firePircBotXEvent(createJoinEvent(defaultChannel, user1));
    assertThat(userJoinedFuture.get(TIMEOUT, TIMEOUT_UNIT), is(chatUser1));

    CompletableFuture<ChatUser> userQuitFuture = listenForUserParted(defaultChannel);
    firePircBotXEvent(createQuitEvent(user1));
    assertThat(userQuitFuture.get(TIMEOUT, TIMEOUT_UNIT), is(chatUser1));
  }

  @Test
  public void testJoinChannel() throws Exception {
    reset(taskService);
    when(taskService.submitTask(any())).thenReturn(completedFuture(null));

    connect();
    botStartedFuture.get(TIMEOUT, TIMEOUT_UNIT);

    instance.connectionStateProperty().set(ConnectionState.CONNECTED);

    String channelToJoin = "#anotherChannel";
    instance.joinChannel(channelToJoin);

    verify(outputIrc).joinChannel(channelToJoin);
  }

  @Test
  public void testIsDefaultChannel() throws Exception {
    assertTrue(instance.isDefaultChannel(DEFAULT_CHANNEL_NAME));
  }

  @Test
  public void testOnConnected() throws Exception {
    String password = "123";

    when(userService.getPassword()).thenReturn(password);

    connect();
    botStartedFuture.get(TIMEOUT, TIMEOUT_UNIT);

    instance.connectionStateProperty().set(ConnectionState.CONNECTED);

    String md5Password = Hashing.md5().hashString(password, StandardCharsets.UTF_8).toString();
    verify(outputIrc).message("NICKSERV", String.format("IDENTIFY %s", md5Password));
  }

  @Test
  public void testOnDisconnected() throws Exception {
    connect();
    joinChannel(defaultChannel, user1);

    assertThat(instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME).getUsers(), hasSize(1));

    instance.connectionStateProperty().set(ConnectionState.DISCONNECTED);

    assertThat(instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME).getUsers(), empty());
  }

  @Test
  public void testClose() {
    instance.close();
  }

  @Test
  public void testCreateOrGetChatUserStringPopulatedMap() throws Exception {
    ChatUser addedUser = instance.getChatUser(chatUser1.getUsername());
    ChatUser returnedUser = instance.getChatUser(chatUser1.getUsername());

    assertThat(returnedUser, is(addedUser));
    assertEquals(returnedUser, addedUser);
  }

  @Test
  public void testCreateOrGetChatUserUserObjectPopulatedMap() throws Exception {
    ChatUser addedUser = instance.createOrGetChatUser(user1);
    ChatUser returnedUser = instance.createOrGetChatUser(user1);

    assertThat(returnedUser, is(addedUser));
    assertEquals(returnedUser, addedUser);
  }

  @Test
  public void getOrCreateChatUserFriendNotification() throws Exception {
    when(playerService.getPlayerForUsername(anyString())).thenReturn(playerInfoBean);
    when(playerInfoBean.getSocialStatus()).thenReturn(SocialStatus.FRIEND);
    when(playerInfoBean.getId()).thenReturn(1);

    instance.getOrCreateChatUser(CHAT_USER_NAME);

    verify(notificationService).addNotification(any(TransientNotification.class));
  }

  @Test
  public void getOrCreateChatUserFoeNoNotification() throws Exception {
    when(playerService.getPlayerForUsername(anyString())).thenReturn(playerInfoBean);
    when(playerInfoBean.getSocialStatus()).thenReturn(SocialStatus.FOE);
    when(playerInfoBean.getId()).thenReturn(1);

    instance.getOrCreateChatUser(CHAT_USER_NAME);

    verify(notificationService, never()).addNotification(any(TransientNotification.class));
  }

  @Test
  public void testRejoinChannel() throws Exception {
    OutputChannel outputChannel = mock(OutputChannel.class);

    reset(taskService);
    when(taskService.submitTask(any())).thenReturn(completedFuture(null));

    String channelToJoin = OTHER_CHANNEL_NAME;
    when(userService.getUsername()).thenReturn("user1");
    when(userChannelDao.getChannel(channelToJoin)).thenReturn(otherChannel);
    when(otherChannel.send()).thenReturn(outputChannel);
    doAnswer(invocation -> {
      firePircBotXEvent(createJoinEvent(otherChannel, user1));
      return null;
    }).when(outputIrc).joinChannel(channelToJoin);
    doAnswer(invocation -> {
      firePircBotXEvent(createPartEvent(otherChannel, user1));
      return null;
    }).when(outputChannel).part();

    connect();
    botStartedFuture.get(TIMEOUT, TIMEOUT_UNIT);

    instance.connectionStateProperty().set(ConnectionState.CONNECTED);

    CountDownLatch firstJoinLatch = new CountDownLatch(1);
    CountDownLatch secondJoinLatch = new CountDownLatch(1);
    CountDownLatch leaveLatch = new CountDownLatch(1);
    instance.addChannelsListener(change -> {
      if (change.wasAdded()) {
        if (firstJoinLatch.getCount() > 0) {
          firstJoinLatch.countDown();
        } else {
          secondJoinLatch.countDown();
        }
      } else if (change.wasRemoved()) {
        leaveLatch.countDown();
      }
    });

    instance.joinChannel(channelToJoin);
    assertTrue(firstJoinLatch.await(TIMEOUT, TIMEOUT_UNIT));

    instance.leaveChannel(channelToJoin);
    assertTrue(leaveLatch.await(TIMEOUT, TIMEOUT_UNIT));

    instance.joinChannel(channelToJoin);
    assertTrue(secondJoinLatch.await(TIMEOUT, TIMEOUT_UNIT));
  }

  @Test
  public void testOnModeratorJoined() throws Exception {
    connect();

    User moderator = mock(User.class);

    when(moderator.getNick()).thenReturn("moderator");
    when(moderator.getChannels()).thenReturn(ImmutableSortedSet.of(defaultChannel));
    when(moderator.getUserLevels(defaultChannel)).thenReturn(ImmutableSortedSet.of(UserLevel.OWNER));
    joinChannel(defaultChannel, moderator);

    firePircBotXEvent(createJoinEvent(defaultChannel, moderator));

    ChatUser chatUserModerator = instance.getChatUser(moderator.getNick());
    assertTrue(chatUserModerator.moderatorInChannelsProperty().getValue().contains(DEFAULT_CHANNEL_NAME));
  }
}
