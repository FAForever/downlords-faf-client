package com.faforever.client.chat;

import com.faforever.client.chat.event.ChatMessageEvent;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.SocialMessage;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.user.UserService;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.eventbus.EventBus;
import com.google.common.hash.Hashing;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UserChannelDao;
import org.pircbotx.UserHostmask;
import org.pircbotx.UserLevel;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.NoticeEvent;
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
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.faforever.client.chat.ChatColorMode.CUSTOM;
import static java.nio.charset.StandardCharsets.UTF_8;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PircBotXChatServiceTest extends AbstractPlainJavaFxTest {

  private static final String CHAT_USER_NAME = "junit";
  private static final String CHAT_PASSWORD = "123";
  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();
  private static final long TIMEOUT = 30000;
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
  private I18n i18n;
  @Mock
  private PircBotXFactory pircBotXFactory;
  @Mock
  private UserChannelDao<User, org.pircbotx.Channel> userChannelDao;
  @Mock
  private FafService fafService;
  @Mock
  private ThreadPoolExecutor threadPoolExecutor;
  @Mock
  private PlayerService playerService;
  @Mock
  private EventBus eventBus;

  @Captor
  private ArgumentCaptor<Consumer<SocialMessage>> socialMessageListenerCaptor;
  @Captor
  private ArgumentCaptor<Configuration> configurationCaptor;

  private CountDownLatch botShutdownLatch;
  private CompletableFuture<Object> botStartedFuture;
  private Preferences preferences = new Preferences();

  @Before
  public void setUp() throws Exception {
    ClientProperties clientProperties = new ClientProperties();
    clientProperties.getIrc()
        .setHost(LOOPBACK_ADDRESS.getHostAddress())
        .setPort(IRC_SERVER_PORT)
        .setDefaultChannel(DEFAULT_CHANNEL_NAME)
        .setReconnectDelay(100);

    instance = new PircBotXChatService(preferencesService, userService, taskService, fafService, i18n, pircBotXFactory,
        threadPoolExecutor, eventBus, clientProperties);

    botShutdownLatch = new CountDownLatch(1);

    preferences.getChat().setChatColorMode(CUSTOM);

    when(userService.getUsername()).thenReturn(CHAT_USER_NAME);
    when(userService.getPassword()).thenReturn(CHAT_PASSWORD);

    when(defaultChannel.getName()).thenReturn(DEFAULT_CHANNEL_NAME);
    when(otherChannel.getName()).thenReturn(OTHER_CHANNEL_NAME);
    when(pircBotX.getConfiguration()).thenReturn(configuration);
    when(pircBotX.sendIRC()).thenReturn(outputIrc);
    when(pircBotX.getUserChannelDao()).thenReturn(userChannelDao);

    doAnswer(invocation -> {
      WaitForAsyncUtils.async(() -> ((Runnable) invocation.getArgument(0)).run());
      return null;
    }).when(threadPoolExecutor).execute(any(Runnable.class));

    doAnswer((InvocationOnMock invocation) -> {
      @SuppressWarnings("unchecked")
      CompletableTask<Void> task = invocation.getArgument(0);
      task.run();
      return task;
    }).when(taskService).submitTask(any());

    botStartedFuture = new CompletableFuture<>();
    doAnswer(invocation -> {
      botStartedFuture.complete(true);
      botShutdownLatch.await();
      return null;
    }).when(pircBotX).startBot();

    when(pircBotXFactory.createPircBotX(any())).thenReturn(pircBotX);

    when(preferencesService.getPreferences()).thenReturn(preferences);

    when(user1.getNick()).thenReturn("user1");
    when(user1.getChannels()).thenReturn(ImmutableSortedSet.of(defaultChannel));
    when(user1.getUserLevels(defaultChannel)).thenReturn(ImmutableSortedSet.of(UserLevel.VOICE));

    when(user2.getNick()).thenReturn("user2");
    when(user2.getChannels()).thenReturn(ImmutableSortedSet.of(defaultChannel));
    when(user2.getUserLevels(defaultChannel)).thenReturn(ImmutableSortedSet.of(UserLevel.VOICE));


    chatUser1 = instance.getOrCreateChatUser(user1);
    chatUser2 = instance.getOrCreateChatUser(user2);

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
    instance.connect();
    verify(pircBotXFactory).createPircBotX(configurationCaptor.capture());

    CountDownLatch latch = listenForConnected();
    firePircBotXEvent(new ConnectEvent(pircBotX));
    assertTrue(latch.await(TIMEOUT, TIMEOUT_UNIT));

    UserHostmask nickServHostMask = mock(UserHostmask.class);
    when(nickServHostMask.getHostmask()).thenReturn("nickserv");
    when(configuration.getNickservNick()).thenReturn("nickserv");
    when(configuration.getNickservOnSuccess()).thenReturn("you are now");

    firePircBotXEvent(new NoticeEvent(pircBotX, nickServHostMask, null, null, "", "you are now identified"));

    SocialMessage socialMessage = new SocialMessage();
    socialMessage.setChannels(Collections.emptyList());

    socialMessageListenerCaptor.getValue().accept(socialMessage);
    verify(outputIrc, timeout(TIMEOUT).atLeastOnce()).joinChannel(DEFAULT_CHANNEL_NAME);
  }

  private void firePircBotXEvent(Event event) {
    configurationCaptor.getValue().getListenerManager().onEvent(event);
  }

  private CountDownLatch listenForConnected() {
    CountDownLatch latch = new CountDownLatch(1);
    instance.connectionState.addListener((observable, oldValue, newValue) -> {
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
    return new PartEvent(pircBotX, daoSnapshot, channelSnapshot(channel), user, new UserSnapshot(user), "");
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
  public void testChatMessageEventTriggeredByChannelMessage() throws Exception {
    CompletableFuture<ChatMessage> chatMessageFuture = new CompletableFuture<>();
    doAnswer(invocation -> chatMessageFuture.complete(((ChatMessageEvent) invocation.getArgument(0)).getMessage()))
        .when(eventBus).post(any());

    String message = "chat message";

    connect();

    firePircBotXEvent(createMessageEvent(defaultChannel, user1, message));

    ChatMessage chatMessage = chatMessageFuture.get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(chatMessage.getSource(), is(defaultChannel.getName()));
    assertThat(chatMessage.getMessage(), is(message));
    assertThat(chatMessage.getUsername(), is(chatUser1.getUsername()));
    assertThat(chatMessage.getTime(), is(greaterThan(Instant.ofEpochMilli(System.currentTimeMillis() - 1000))));
    assertThat(chatMessage.isAction(), is(false));
  }

  private MessageEvent createMessageEvent(org.pircbotx.Channel channel, User user, String message) {
    return new MessageEvent(pircBotX, channel, channel.getName(), user, user, message, null);
  }

  @Test
  public void testChatMessageEventTriggeredByAction() throws Exception {
    CompletableFuture<ChatMessage> chatMessageFuture = new CompletableFuture<>();
    doAnswer(invocation -> chatMessageFuture.complete(((ChatMessageEvent) invocation.getArgument(0)).getMessage()))
        .when(eventBus).post(any());

    String action = "chat action";

    connect();
    firePircBotXEvent(createActionEvent(defaultChannel, user1, action));

    ChatMessage chatMessage = chatMessageFuture.get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(chatMessage.getSource(), is(defaultChannel.getName()));
    assertThat(chatMessage.getMessage(), is(action));
    assertThat(chatMessage.getUsername(), is(chatUser1.getUsername()));
    assertThat(chatMessage.getTime(), is(greaterThan(Instant.ofEpochMilli(System.currentTimeMillis() - 10_000))));
    assertThat(chatMessage.isAction(), is(true));
  }

  private ActionEvent createActionEvent(org.pircbotx.Channel channel, User user, String action) {
    return new ActionEvent(pircBotX, user, user, channel, channel.getName(), action);
  }

  @Test
  public void testChatMessageEventTriggeredByPrivateMessage() throws Exception {
    CompletableFuture<ChatMessage> chatMessageFuture = new CompletableFuture<>();
    doAnswer(invocation -> chatMessageFuture.complete(((ChatMessageEvent) invocation.getArgument(0)).getMessage()))
        .when(eventBus).post(any());

    String message = "private message";

    User user = mock(User.class);
    when(user.getNick()).thenReturn(chatUser1.getUsername());

    connect();
    firePircBotXEvent(createPrivateMessageEvent(user, message));

    ChatMessage chatMessage = chatMessageFuture.get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(chatMessage.getMessage(), is(message));
    assertThat(chatMessage.getSource(), is(chatUser1.getUsername()));
    assertThat(chatMessage.getUsername(), is(chatUser1.getUsername()));
    assertThat(chatMessage.getTime(), is(greaterThan(Instant.ofEpochMilli(System.currentTimeMillis() - 1000))));
    assertThat(chatMessage.isAction(), is(false));
  }

  private PrivateMessageEvent createPrivateMessageEvent(User sender, String message) {
    return new PrivateMessageEvent(pircBotX, sender, sender, message);
  }

  @Test
  public void testAddOnChatConnectedListener() throws Exception {
    CompletableFuture<Boolean> onChatConnectedFuture = new CompletableFuture<>();

    instance.connectionState.addListener((observable, oldValue, newValue) -> {
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
    instance.connectionState.addListener((observable, oldValue, newValue) -> {
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
    instance.connectionState.addListener((observable, oldValue, newValue) -> {
      if (newValue == ConnectionState.DISCONNECTED) {
        future.complete(null);
      }
    });
    return future;
  }

  @Test
  public void testAddOnModeratorSetListener() throws Exception {
    Channel channel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME);
    assertThat(channel.getUsers(), empty());

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
    JavaFxUtil.addListener(chatUser.getModeratorInChannels(), (SetChangeListener<String>) change -> {
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
    when(userService.getUserId()).thenReturn(681);

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

    CompletableFuture<String> future = instance.sendMessageInBackground(DEFAULT_CHANNEL_NAME, message).toCompletableFuture();

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

    CompletableFuture<String> future = instance.sendActionInBackground(DEFAULT_CHANNEL_NAME, action).toCompletableFuture();

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

    connect();
    botStartedFuture.get(TIMEOUT, TIMEOUT_UNIT);

    instance.connectionState.set(ConnectionState.CONNECTED);

    String channelToJoin = "#anotherChannel";
    instance.joinChannel(channelToJoin);

    verify(outputIrc).joinChannel(channelToJoin);
  }

  @Test
  public void testIsDefaultChannel() throws Exception {
    assertTrue(instance.isDefaultChannel(DEFAULT_CHANNEL_NAME));
  }

  @Test
  public void testRegisterOnNotRegisteredNotice() throws Exception {
    String password = "123";

    when(userService.getPassword()).thenReturn(password);

    connect();
    botStartedFuture.get(TIMEOUT, TIMEOUT_UNIT);

    UserHostmask nickServHostMask = mock(UserHostmask.class);
    when(nickServHostMask.getHostmask()).thenReturn("nickserv");
    firePircBotXEvent(new NoticeEvent(pircBotX, nickServHostMask, null, null, "", "User foo isn't registered"));

    instance.connectionState.set(ConnectionState.CONNECTED);

    String md5sha256Password = Hashing.md5().hashString(Hashing.sha256().hashString(password, UTF_8).toString(), UTF_8).toString();
    verify(outputIrc, timeout(100)).message("nickserv", String.format("register %s junit@users.faforever.com", md5sha256Password));
  }

  @Test
  public void testOnDisconnected() throws Exception {
    connect();
    joinChannel(defaultChannel, user1);

    assertThat(instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME).getUsers(), hasSize(1));

    instance.connectionState.set(ConnectionState.DISCONNECTED);

    assertThat(instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME).getUsers(), empty());
  }

  @Test
  public void testClose() {
    instance.close();
  }

  @Test
  public void testCreateOrGetChatUserStringPopulatedMap() throws Exception {
    ChatUser addedUser = instance.getOrCreateChatUser(chatUser1.getUsername());
    ChatUser returnedUser = instance.getOrCreateChatUser(chatUser1.getUsername());

    assertThat(returnedUser, is(addedUser));
    assertEquals(returnedUser, addedUser);
  }

  @Test
  public void testCreateOrGetChatUserUserObjectPopulatedMap() throws Exception {
    ChatUser addedUser = instance.getOrCreateChatUser(user1);
    ChatUser returnedUser = instance.getOrCreateChatUser(user1);

    assertThat(returnedUser, is(addedUser));
    assertEquals(returnedUser, addedUser);
  }

  @Test
  public void getOrCreateChatUserFoeNoNotification() throws Exception {
    instance.getOrCreateChatUser(CHAT_USER_NAME);

    verify(notificationService, never()).addNotification(any(TransientNotification.class));
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

    ChatUser chatUserModerator = instance.getOrCreateChatUser(moderator.getNick());
    assertTrue(chatUserModerator.moderatorInChannelsProperty().getValue().contains(DEFAULT_CHANNEL_NAME));
  }

  private ChannelSnapshot channelSnapshot(org.pircbotx.Channel channel) {
    String name = channel.getName();
    ChannelSnapshot channelSnapshot = mock(ChannelSnapshot.class);
    when(channelSnapshot.getName()).thenReturn(name);
    return channelSnapshot;
  }

  @Test
  public void testSuppressFoeMessage() throws Exception {
    String message = "private message";

    preferences.getChat().setHideFoeMessages(true);

    User user = mock(User.class);
    when(user.getNick()).thenReturn(chatUser1.getUsername());
    Player playerMock = mock(Player.class);
    when(playerService.getPlayerForUsername(chatUser1.getUsername())).thenReturn(playerMock);
    when(playerMock.getSocialStatus()).thenReturn(SocialStatus.FOE);

    connect();
    firePircBotXEvent(createPrivateMessageEvent(user, message));

    verify(eventBus, never()).post(any(Object.class));
  }
}
