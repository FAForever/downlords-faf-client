package com.faforever.client.chat;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.chat.event.ChatMessageEvent;
import com.faforever.client.chat.event.ChatUserColorChangeEvent;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Irc;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.PlayerOfflineEvent;
import com.faforever.client.player.PlayerOnlineEvent;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.user.UserService;
import com.faforever.commons.lobby.IrcPasswordInfo;
import com.faforever.commons.lobby.SocialInfo;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.eventbus.EventBus;
import javafx.collections.MapChangeListener;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.Client.Builder.Server.SecurityType;
import org.kitteh.irc.client.library.defaults.DefaultClient;
import org.kitteh.irc.client.library.defaults.element.DefaultActor;
import org.kitteh.irc.client.library.defaults.element.DefaultChannelTopic;
import org.kitteh.irc.client.library.defaults.element.DefaultServerMessage.StringCommand;
import org.kitteh.irc.client.library.defaults.element.mode.DefaultChannelUserMode;
import org.kitteh.irc.client.library.defaults.element.mode.DefaultModeStatus;
import org.kitteh.irc.client.library.defaults.element.mode.DefaultModeStatusList;
import org.kitteh.irc.client.library.defaults.feature.DefaultEventManager;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.element.mode.ChannelUserMode;
import org.kitteh.irc.client.library.element.mode.ModeStatus.Action;
import org.kitteh.irc.client.library.event.channel.ChannelCtcpEvent;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.channel.ChannelModeEvent;
import org.kitteh.irc.client.library.event.channel.ChannelNamesUpdatedEvent;
import org.kitteh.irc.client.library.event.channel.ChannelPartEvent;
import org.kitteh.irc.client.library.event.channel.ChannelTopicEvent;
import org.kitteh.irc.client.library.event.client.ClientNegotiationCompleteEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionClosedEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;
import org.kitteh.irc.client.library.event.user.UserQuitEvent;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.net.InetAddress;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.faforever.client.chat.ChatColorMode.DEFAULT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KittehChatServiceTest extends ServiceTest {

  private static final String CHAT_USER_NAME = "junit";
  private static final String CHAT_PASSWORD = "123";
  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();
  private static final long TIMEOUT = 30000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
  private static final String DEFAULT_CHANNEL_NAME = "#defaultChannel";
  private static final String OTHER_CHANNEL_NAME = "#otherChannel";
  private static final int IRC_SERVER_PORT = 123;

  @InjectMocks
  private KittehChatService instance;

  private ChatChannelUser defaultChatUser1;
  private ChatChannelUser otherChatUser1;
  private ChatChannelUser defaultChatUser2;

  @Mock
  private User user1;
  @Mock
  private ChannelUserMode user1Mode;
  @Mock
  private User user2;
  @Mock
  private ChannelUserMode user2Mode;
  @Mock
  private Channel defaultChannel;
  @Mock
  private Channel otherChannel;
  @Mock
  private UserService userService;
  @Mock
  private ChatUserService chatUserService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private PlayerService playerService;
  @Mock
  private EventBus eventBus;
  @Spy
  private ClientProperties clientProperties = new ClientProperties();

  @Captor
  private ArgumentCaptor<Consumer<SocialInfo>> socialMessageListenerCaptor;

  private DefaultEventManager eventManager;
  private DefaultClient client;
  private Preferences preferences;
  private PlayerBean player1;

  @BeforeEach
  public void setUp() throws Exception {

    clientProperties.getIrc()
        .setHost(LOOPBACK_ADDRESS.getHostAddress())
        .setPort(IRC_SERVER_PORT)
        .setDefaultChannel(DEFAULT_CHANNEL_NAME)
        .setReconnectDelay(100);

    Irc irc = clientProperties.getIrc();
    instance.defaultChannelName = irc.getDefaultChannel();

    instance.client = (DefaultClient) Client.builder()
        .user(CHAT_USER_NAME)
        .realName(CHAT_USER_NAME)
        .nick(CHAT_USER_NAME)
        .server()
        .host(irc.getHost())
        .port(irc.getPort(), SecurityType.SECURE)
        .secureTrustManagerFactory(new TrustEveryoneFactory())
        .password(CHAT_PASSWORD)
        .then()
        .build();

    client = instance.client;

    eventManager = (DefaultEventManager) client.getEventManager();

    preferences = PreferencesBuilder.create().defaultValues()
        .chatPrefs()
        .chatColorMode(DEFAULT)
        .then()
        .get();

    when(userService.getUsername()).thenReturn(CHAT_USER_NAME);

    when(defaultChannel.getClient()).thenReturn(instance.client);
    when(defaultChannel.getName()).thenReturn(DEFAULT_CHANNEL_NAME);
    when(otherChannel.getClient()).thenReturn(instance.client);
    when(otherChannel.getName()).thenReturn(OTHER_CHANNEL_NAME);

    when(preferencesService.getPreferences()).thenReturn(preferences);

    Character userPrefix = '+';

    when(user1.getClient()).thenReturn(instance.client);
    when(user1.getNick()).thenReturn("user1");
    when(user1Mode.getNickPrefix()).thenReturn(userPrefix);
    when(defaultChannel.getUserModes(user1)).thenReturn(Optional.of(ImmutableSortedSet.orderedBy(Comparator.comparing(ChannelUserMode::getNickPrefix)).add(user1Mode).build()));
    when(otherChannel.getUserModes(user1)).thenReturn(Optional.of(ImmutableSortedSet.orderedBy(Comparator.comparing(ChannelUserMode::getNickPrefix)).add(user1Mode).build()));

    when(user2.getClient()).thenReturn(instance.client);
    when(user2.getNick()).thenReturn("user2");
    when(user2Mode.getNickPrefix()).thenReturn(userPrefix);
    when(defaultChannel.getUserModes(user1)).thenReturn(Optional.of(ImmutableSortedSet.orderedBy(Comparator.comparing(ChannelUserMode::getNickPrefix)).add(user2Mode).build()));
    when(otherChannel.getUserModes(user1)).thenReturn(Optional.of(ImmutableSortedSet.orderedBy(Comparator.comparing(ChannelUserMode::getNickPrefix)).add(user1Mode).build()));

    player1 = PlayerBeanBuilder.create().defaultValues().get();
    when(playerService.getPlayerByNameIfOnline(user1.getNick())).thenReturn(Optional.of(player1));
    when(playerService.getPlayerByNameIfOnline(user2.getNick())).thenReturn(Optional.empty());

    defaultChatUser1 = instance.getOrCreateChatUser(user1.getNick(), DEFAULT_CHANNEL_NAME, false);
    otherChatUser1 = instance.getOrCreateChatUser(user1.getNick(), OTHER_CHANNEL_NAME, false);
    defaultChatUser2 = instance.getOrCreateChatUser(user2.getNick(), DEFAULT_CHANNEL_NAME, false);

    instance.afterPropertiesSet();

    verify(fafServerAccessor).addEventListener(eq(SocialInfo.class), socialMessageListenerCaptor.capture());
  }

  @AfterEach
  public void tearDown() {
    instance.destroy();
  }

  private void join(Channel channel, User user) {
    eventManager.callEvent(new ChannelJoinEvent(client,
        new StringCommand("", "", List.of()),
        channel,
        user));
  }

  private void quit(User user) {
    eventManager.callEvent(new UserQuitEvent(client,
        new StringCommand("", "", List.of()),
        user,
        String.format("%s quit", user.getNick())));
  }

  private void part(Channel channel, User user) {
    eventManager.callEvent(new ChannelPartEvent(client,
        new StringCommand("", "", List.of()),
        channel,
        user,
        String.format("%s left %s", user.getNick(), channel.getName())));
  }

  private void messageChannel(Channel channel, User user, String message) {
    eventManager.callEvent(new ChannelMessageEvent(client,
        new StringCommand("", "", List.of()),
        user,
        channel,
        message));
  }

  private void actionChannel(Channel channel, User user, String message) {
    eventManager.callEvent(new ChannelCtcpEvent(client,
        new StringCommand("", "", List.of()),
        user,
        channel,
        message));
  }

  private void sendPrivateMessage(User user, String message) {
    eventManager.callEvent(new PrivateMessageEvent(client,
        new StringCommand("", "", List.of()),
        user,
        "me",
        message));
  }

  private void connect() {
    eventManager.registerEventListener(instance);
    client.getActorTracker().setQueryChannelInformation(false);

    eventManager.callEvent(new ClientNegotiationCompleteEvent(client,
        new DefaultActor(client, "server"),
        client.getServerInfo()));

    SocialInfo socialMessage = new SocialInfo(List.of(), List.of(), List.of(), List.of(), 0);

    socialMessageListenerCaptor.getValue().accept(socialMessage);
  }

  @Test
  public void testUserToColorChangeSaved() {
    preferences.getChat().getUserToColor().put(user1.getNick(), Color.ALICEBLUE);

    verify(preferencesService).storeInBackground();
  }

  @Test
  public void testGroupToColorChangeFriend() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().username(user1.getNick()).socialStatus(SocialStatus.FRIEND).get();
    defaultChatUser1.setPlayer(player);

    connect();

    join(defaultChannel, user1);
    join(defaultChannel, user2);

    preferences.getChat().getGroupToColor().put(ChatUserCategory.FRIEND, Color.ALICEBLUE);

    verify(preferencesService).storeInBackground();
    assertEquals(Color.ALICEBLUE, defaultChatUser1.getColor().get());
    assertTrue(defaultChatUser2.getColor().isEmpty());
  }

  @Test
  public void testGroupToColorChangeFoe() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().username(user1.getNick()).socialStatus(SocialStatus.FOE).get();
    defaultChatUser1.setPlayer(player);

    connect();

    join(defaultChannel, user1);
    join(defaultChannel, user2);

    preferences.getChat().getGroupToColor().put(ChatUserCategory.FOE, Color.ALICEBLUE);

    verify(preferencesService).storeInBackground();
    assertEquals(Color.ALICEBLUE, defaultChatUser1.getColor().get());
    assertTrue(defaultChatUser2.getColor().isEmpty());
  }

  @Test
  public void testGroupToColorChangeOther() {
    connect();

    join(defaultChannel, user1);
    join(defaultChannel, user2);

    preferences.getChat().getGroupToColor().put(ChatUserCategory.OTHER, Color.ALICEBLUE);

    verify(preferencesService).storeInBackground();
  }

  @Test
  public void testOnConnect() {
    connect();

    assertEquals(ConnectionState.CONNECTED, instance.connectionState.get());
  }

  @Test
  public void testOnUsersJoinedChannel() {
    ChatChannel chatChannel = instance.getOrCreateChannel(defaultChannel.getName());
    assertThat(chatChannel.getUsers(), empty());

    connect();

    join(defaultChannel, user1);
    join(defaultChannel, user2);

    assertThat(chatChannel.getUsers(), hasSize(2));
    assertThat(chatChannel.getUser(user1.getNick()), sameInstance(defaultChatUser1));
    assertThat(chatChannel.getUser(user2.getNick()), sameInstance(defaultChatUser2));

    verify(chatUserService).associatePlayerToChatUser(defaultChatUser1, player1);
    verify(chatUserService).populateColor(defaultChatUser2);
  }

  @Test
  public void testOnPlayerOnline() {
    connect();

    join(defaultChannel, user2);

    PlayerBean player = PlayerBeanBuilder.create().defaultValues().username(user2.getNick()).get();

    instance.onPlayerOnline(new PlayerOnlineEvent(player));

    verify(chatUserService).associatePlayerToChatUser(defaultChatUser2, player);
  }

  @Test
  public void testOnChatUserList() {
    ChatChannel chatChannel = instance.getOrCreateChannel(defaultChannel.getName());
    assertThat(chatChannel.getUsers(), empty());

    when(defaultChannel.getUsers()).thenReturn(List.of(user1, user2));

    connect();

    eventManager.callEvent(new ChannelNamesUpdatedEvent(client,
        List.of(),
        defaultChannel));

    assertThat(chatChannel.getUsers(), hasSize(2));
    assertThat(chatChannel.getUser(user1.getNick()), sameInstance(defaultChatUser1));
    assertThat(chatChannel.getUser(user2.getNick()), sameInstance(defaultChatUser2));
  }

  @Test
  public void testOnChatUserLeftChannel() {
    ChatChannel chatChannel = instance.getOrCreateChannel(defaultChannel.getName());
    assertThat(chatChannel.getUsers(), empty());

    connect();

    join(defaultChannel, user1);
    join(defaultChannel, user2);

    assertThat(chatChannel.getUsers(), hasSize(2));
    assertThat(chatChannel.getUser(user1.getNick()), sameInstance(defaultChatUser1));
    assertThat(chatChannel.getUser(user2.getNick()), sameInstance(defaultChatUser2));

    part(defaultChannel, user1);

    assertThat(chatChannel.getUsers(), contains(defaultChatUser2));
  }

  @Test
  public void testOnChatUserQuit() {
    defaultChatUser1.setPlayer(PlayerBeanBuilder.create().defaultValues().get());

    ChatChannel chatChannel1 = instance.getOrCreateChannel(defaultChannel.getName());
    ChatChannel chatChannel2 = instance.getOrCreateChannel(otherChannel.getName());
    assertThat(chatChannel1.getUsers(), empty());
    assertThat(chatChannel2.getUsers(), empty());

    connect();

    join(defaultChannel, user1);
    join(otherChannel, user1);

    assertThat(chatChannel1.getUsers(), contains(defaultChatUser1));
    assertThat(chatChannel1.getUser(user1.getNick()), sameInstance(defaultChatUser1));
    assertThat(chatChannel2.getUsers(), contains(otherChatUser1));
    assertThat(chatChannel2.getUser(user1.getNick()), sameInstance(otherChatUser1));

    quit(user1);

    verify(eventBus).post(any(PlayerOfflineEvent.class));
    assertThat(chatChannel1.getUsers(), empty());
    assertThat(chatChannel2.getUsers(), empty());
  }

  @Test
  public void testTopicChange() {
    ChatChannel chatChannel = instance.getOrCreateChannel(defaultChannel.getName());
    assertThat(chatChannel.getUsers(), empty());

    connect();

    eventManager.callEvent(new ChannelTopicEvent(client,
        new StringCommand("", "", List.of()),
        defaultChannel,
        new DefaultChannelTopic(null, "old topic", null),
        new DefaultChannelTopic(null, "new topic", null),
        false));

    assertThat(chatChannel.getTopic(), is("new topic"));
  }

  @Test
  public void testChatMessageEventTriggeredByChannelMessage() throws Exception {
    CompletableFuture<ChatMessage> chatMessageFuture = new CompletableFuture<>();
    doAnswer(invocation -> chatMessageFuture.complete(((ChatMessageEvent) invocation.getArgument(0)).getMessage()))
        .when(eventBus).post(any());

    String message = "chat message";

    connect();

    messageChannel(defaultChannel, user1, message);

    ChatMessage chatMessage = chatMessageFuture.get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(chatMessage.getSource(), is(defaultChannel.getName()));
    assertThat(chatMessage.getMessage(), is(message));
    assertThat(chatMessage.getUsername(), is(defaultChatUser1.getUsername()));
    assertThat(chatMessage.isAction(), is(false));
  }

  @Test
  public void testChatMessageEventTriggeredByChannelAction() throws Exception {
    CompletableFuture<ChatMessage> chatMessageFuture = new CompletableFuture<>();
    doAnswer(invocation -> chatMessageFuture.complete(((ChatMessageEvent) invocation.getArgument(0)).getMessage()))
        .when(eventBus).post(any());

    String message = "chat action";

    connect();

    actionChannel(defaultChannel, user1, message);

    ChatMessage chatMessage = chatMessageFuture.get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(chatMessage.getSource(), is(defaultChannel.getName()));
    assertThat(chatMessage.getMessage(), is(message));
    assertThat(chatMessage.getUsername(), is(defaultChatUser1.getUsername()));
    assertThat(chatMessage.isAction(), is(true));
  }

  @Test
  public void testChatMessageEventTriggeredByPrivateMessage() throws Exception {
    CompletableFuture<ChatMessage> chatMessageFuture = new CompletableFuture<>();
    doAnswer(invocation -> chatMessageFuture.complete(((ChatMessageEvent) invocation.getArgument(0)).getMessage()))
        .when(eventBus).post(any(ChatMessageEvent.class));

    String message = "private message";

    connect();
    sendPrivateMessage(user1, message);

    ChatMessage chatMessage = chatMessageFuture.get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(chatMessage.getMessage(), is(message));
    assertThat(chatMessage.getSource(), is(user1.getNick()));
    assertThat(chatMessage.getUsername(), is(user1.getNick()));
    assertThat(chatMessage.isAction(), is(false));
  }

  @Test
  public void testChatMessageEventNotTriggeredByPrivateMessageFromFoe() {
    ChatChannelUser foeUser = instance.getOrCreateChatUser(user1.getNick(), user1.getNick(), false);
    foeUser.setPlayer(PlayerBeanBuilder.create().defaultValues().username(defaultChatUser1.getUsername()).socialStatus(SocialStatus.FOE).get());

    String message = "private message";

    connect();
    sendPrivateMessage(user1, message);

    verify(eventBus, never()).post(any(ChatMessageEvent.class));
  }

  @Test
  public void testAddModerator() {
    connect();

    eventManager.callEvent(new ChannelModeEvent(client,
        new StringCommand("", "", List.of()),
        user1,
        defaultChannel,
        DefaultModeStatusList.of(new DefaultModeStatus<>(Action.ADD, new DefaultChannelUserMode(client, 'o', '%'), user1.getNick()))));

    assertTrue(defaultChatUser1.isModerator());
  }

  @Test
  public void testRemoveModerator() {
    defaultChatUser1.setModerator(true);

    connect();

    eventManager.callEvent(new ChannelModeEvent(client,
        new StringCommand("", "", List.of()),
        user1,
        defaultChannel,
        DefaultModeStatusList.of(new DefaultModeStatus<>(Action.REMOVE, new DefaultChannelUserMode(client, 'o', '%'), user1.getNick()))));

    assertFalse(defaultChatUser1.isModerator());
  }

  @Test
  public void testAddOnChatConnectedListener() throws Exception {
    CompletableFuture<Boolean> onChatConnectedFuture = new CompletableFuture<>();

    instance.connectionState.addListener((observable, oldValue, newValue) -> {
      if (newValue == ConnectionState.CONNECTED) {
        onChatConnectedFuture.complete(null);
      }
    });

    connect();

    assertThat(onChatConnectedFuture.get(TIMEOUT, TIMEOUT_UNIT), is(nullValue()));
  }

  @Test
  public void testSendMessageInBackground() throws Exception {
    connect();

    String message = "test message";

    CompletableFuture<String> future = instance.sendMessageInBackground(DEFAULT_CHANNEL_NAME, message).toCompletableFuture();

    assertThat(future.get(TIMEOUT, TIMEOUT_UNIT), is(message));
  }

  @Test
  public void testGetChatUsersForChannelEmpty() {
    ChatChannel chatChannel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME);
    assertThat(chatChannel.getUsers(), empty());
  }

  @Test
  public void testGetChatUsersForChannelTwoUsersInDifferentChannels() {
    connect();
    join(defaultChannel, user1);
    join(otherChannel, user2);

    List<ChatChannelUser> usersInDefaultChannel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME).getUsers();
    assertThat(usersInDefaultChannel, contains(defaultChatUser1));
    assertThat(usersInDefaultChannel.get(0), sameInstance(defaultChatUser1));

    List<ChatChannelUser> usersInOtherChannel = instance.getOrCreateChannel(OTHER_CHANNEL_NAME).getUsers();

    assertThat(usersInOtherChannel.get(0).getChannel(), is(otherChannel.getName()));
    assertThat(usersInOtherChannel.get(0).getUsername(), is(defaultChatUser2.getUsername()));
  }

  @Test
  public void testGetChatUsersForChannelTwoUsersInSameChannel() {
    connect();
    join(defaultChannel, user1);
    join(defaultChannel, user2);

    ChatChannel chatChannel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME);

    List<ChatChannelUser> users = chatChannel.getUsers();
    assertThat(users, hasSize(2));
    assertThat(users, containsInAnyOrder(defaultChatUser1, defaultChatUser2));
  }

  @Test
  public void testAddChannelUserListListener() {
    connect();
    @SuppressWarnings("unchecked")
    MapChangeListener<String, ChatChannelUser> listener = mock(MapChangeListener.class);

    instance.addUsersListener(DEFAULT_CHANNEL_NAME, listener);

    join(defaultChannel, user1);
    join(defaultChannel, user2);

    verify(listener, times(2)).onChanged(any());
  }

  @Test
  public void testLeaveChannel() {
    IrcPasswordInfo event = new IrcPasswordInfo("abc");
    instance.onIrcPassword(event);
    instance.leaveChannel(DEFAULT_CHANNEL_NAME);
  }

  @Test
  public void testGroupColorChange() {
    preferences.getChat().getGroupToColor().put(ChatUserCategory.FOE, Color.ALICEBLUE);



    verify(eventBus, times(3)).post(any(ChatUserColorChangeEvent.class));
  }

  @Test
  public void testSendActionInBackground() throws Exception {
    connect();

    String action = "test action";

    CompletableFuture<String> future = instance.sendActionInBackground(DEFAULT_CHANNEL_NAME, action).toCompletableFuture();

    assertThat(future.get(TIMEOUT, TIMEOUT_UNIT), is(action));
  }

  @Test
  public void testJoinChannel() {
    connect();
    instance.connectionState.set(ConnectionState.CONNECTED);
    String channelToJoin = "#anotherChannel";
    instance.joinChannel(channelToJoin);
  }

  @Test
  public void testIsDefaultChannel() {
    connect();
    assertTrue(instance.isDefaultChannel(DEFAULT_CHANNEL_NAME));
  }

  @Test
  public void testOnDisconnected() {
    connect();
    join(defaultChannel, user1);

    assertThat(instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME).getUsers(), hasSize(1));

    eventManager.callEvent(new ClientConnectionClosedEvent(client, false, null, null));

    assertThat(instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME).getUsers(), empty());
  }

  @Test
  public void testClose() {
    instance.destroy();
  }

  @Test
  public void testCreateOrGetChatUserStringPopulatedMap() {
    ChatChannelUser addedUser = instance.getOrCreateChatUser(defaultChatUser1.getUsername(), DEFAULT_CHANNEL_NAME, false);
    ChatChannelUser returnedUser = instance.getOrCreateChatUser(defaultChatUser1.getUsername(), DEFAULT_CHANNEL_NAME, false);

    assertThat(returnedUser, is(addedUser));
    assertEquals(returnedUser, addedUser);
  }

  @Test
  public void testCreateOrGetChatUserUserObjectPopulatedMap() {
    ChatChannelUser addedUser = instance.getOrCreateChatUser(user1.getNick(), DEFAULT_CHANNEL_NAME, false);
    ChatChannelUser returnedUser = instance.getOrCreateChatUser(user1.getNick(), DEFAULT_CHANNEL_NAME, false);

    assertThat(returnedUser, is(addedUser));
    assertEquals(returnedUser, addedUser);
  }
}
