package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Irc;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.user.LoginService;
import com.faforever.commons.lobby.Player;
import com.faforever.commons.lobby.SocialInfo;
import com.google.common.collect.ImmutableSortedSet;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.Client.Builder.Server.SecurityType;
import org.kitteh.irc.client.library.Client.WithManagement;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.faforever.client.chat.ChatColorMode.DEFAULT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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
  private LoginService loginService;
  @Mock
  private AudioService audioService;
  @Mock
  private NotificationService notificationService;

  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private PlayerService playerService;
  @Mock
  private FxApplicationThreadExecutor fxApplicationThreadExecutor;
  @Spy
  private ClientProperties clientProperties;
  @Spy
  private ChatPrefs chatPrefs;
  @Spy
  private NotificationPrefs notificationPrefs;

  @Captor
  private ArgumentCaptor<Consumer<SocialInfo>> socialMessageListenerCaptor;

  private DefaultEventManager eventManager;
  private DefaultClient spyClient;
  private PlayerBean player1;
  private DefaultClient realClient;

  private final BooleanProperty loggedIn = new SimpleBooleanProperty();

  @BeforeEach
  public void setUp() throws Exception {

    clientProperties.getIrc()
        .setHost(LOOPBACK_ADDRESS.getHostAddress())
        .setPort(IRC_SERVER_PORT)
        .setDefaultChannel(DEFAULT_CHANNEL_NAME)
        .setReconnectDelay(100);

    Irc irc = clientProperties.getIrc();
    instance.defaultChannelName = irc.getDefaultChannel();

    realClient = (DefaultClient) Client.builder()
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

    spyClient = spy(realClient);

    instance.client = spyClient;

    eventManager = (DefaultEventManager) realClient.getEventManager();

    chatPrefs.setChatColorMode(DEFAULT);

    when(loginService.getUsername()).thenReturn(CHAT_USER_NAME);
    when(loginService.getOwnPlayer()).thenReturn(new Player(0, CHAT_USER_NAME, null, null, "", Map.of(), Map.of()));
    when(loginService.loggedInProperty()).thenReturn(loggedIn);
    when(defaultChannel.getClient()).thenReturn(realClient);
    when(defaultChannel.getName()).thenReturn(DEFAULT_CHANNEL_NAME);
    when(otherChannel.getClient()).thenReturn(realClient);
    when(otherChannel.getName()).thenReturn(OTHER_CHANNEL_NAME);

    Character userPrefix = '+';

    when(user1.getClient()).thenReturn(realClient);
    when(user1.getNick()).thenReturn("user1");
    when(user1Mode.getNickPrefix()).thenReturn(userPrefix);
    when(defaultChannel.getUserModes(user1)).thenReturn(Optional.of(ImmutableSortedSet.orderedBy(Comparator.comparing(ChannelUserMode::getNickPrefix))
        .add(user1Mode)
        .build()));
    when(otherChannel.getUserModes(user1)).thenReturn(Optional.of(ImmutableSortedSet.orderedBy(Comparator.comparing(ChannelUserMode::getNickPrefix))
        .add(user1Mode)
        .build()));

    when(user2.getClient()).thenReturn(realClient);
    when(user2.getNick()).thenReturn("user2");
    when(user2Mode.getNickPrefix()).thenReturn(userPrefix);
    when(defaultChannel.getUserModes(user1)).thenReturn(Optional.of(ImmutableSortedSet.orderedBy(Comparator.comparing(ChannelUserMode::getNickPrefix))
        .add(user2Mode)
        .build()));
    when(otherChannel.getUserModes(user1)).thenReturn(Optional.of(ImmutableSortedSet.orderedBy(Comparator.comparing(ChannelUserMode::getNickPrefix))
        .add(user1Mode)
        .build()));

    player1 = PlayerBeanBuilder.create().defaultValues().get();
    when(playerService.getPlayerByNameIfOnline(user1.getNick())).thenReturn(Optional.of(player1));
    when(playerService.getPlayerByNameIfOnline(user2.getNick())).thenReturn(Optional.empty());

    when(spyClient.getChannel(DEFAULT_CHANNEL_NAME)).thenReturn(Optional.of(defaultChannel));
    when(spyClient.getChannel(OTHER_CHANNEL_NAME)).thenReturn(Optional.of(otherChannel));
    when(defaultChannel.getUser(user1.getNick())).thenReturn(Optional.of(user1));
    when(otherChannel.getUser(user1.getNick())).thenReturn(Optional.of(user1));
    when(defaultChannel.getUser(user2.getNick())).thenReturn(Optional.of(user2));

    doAnswer(invocation -> {
      Runnable runnable = invocation.getArgument(0);
      runnable.run();
      return null;
    }).when(fxApplicationThreadExecutor).execute(any());

    instance.afterPropertiesSet();

    verify(fafServerAccessor).addEventListener(eq(SocialInfo.class), socialMessageListenerCaptor.capture());
  }

  @AfterEach
  public void tearDown() {
    instance.destroy();
  }

  private void join(Channel channel, User user) {
    eventManager.callEvent(new ChannelJoinEvent(realClient,
        new StringCommand("", "", List.of()),
        channel,
        user));
  }

  private void quit(User user) {
    eventManager.callEvent(new UserQuitEvent(realClient,
        new StringCommand("", "", List.of()),
        user,
        String.format("%s quit", user.getNick())));
  }

  private void part(Channel channel, User user) {
    eventManager.callEvent(new ChannelPartEvent(realClient,
        new StringCommand("", "", List.of()),
        channel,
        user,
        String.format("%s left %s", user.getNick(), channel.getName())));
  }

  private void messageChannel(Channel channel, User user, String message) {
    eventManager.callEvent(new ChannelMessageEvent(realClient,
        new StringCommand("", "", List.of()),
        user,
        channel,
        message));
  }

  private void actionChannel(Channel channel, User user, String message) {
    eventManager.callEvent(new ChannelCtcpEvent(realClient,
        new StringCommand("", "", List.of()),
        user,
        channel,
        message));
  }

  private void sendPrivateMessage(User user, String message) {
    eventManager.callEvent(new PrivateMessageEvent(realClient,
        new StringCommand("", "", List.of()),
        user,
        "me",
        message));
  }

  private void connect() {
    eventManager.registerEventListener(instance);
    realClient.getActorTracker().setQueryChannelInformation(false);

    eventManager.callEvent(new ClientNegotiationCompleteEvent(realClient,
        new DefaultActor(realClient, "server"),
        realClient.getServerInfo()));

    SocialInfo socialMessage = new SocialInfo(List.of(), List.of(), List.of(), List.of(), 0);

    socialMessageListenerCaptor.getValue().accept(socialMessage);
  }

  @Test
  public void testGroupToColorChangeFriend() {
    PlayerBean player = PlayerBeanBuilder.create()
        .defaultValues()
        .username(user1.getNick())
        .socialStatus(SocialStatus.FRIEND)
        .get();

    connect();

    join(defaultChannel, user1);
    join(defaultChannel, user2);

    instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME)
        .getUser(user1.getNick())
        .ifPresent(chatUser -> chatUser.setPlayer(player));

    chatPrefs.getGroupToColor().put(ChatUserCategory.FRIEND, Color.ALICEBLUE);

    assertEquals(Color.ALICEBLUE, instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME)
        .getUser(user1.getNick())
        .get()
        .getColor()
        .get());
    assertTrue(instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME).getUser(user2.getNick()).get().getColor().isEmpty());
  }

  @Test
  public void testGroupToColorChangeFoe() {
    PlayerBean player = PlayerBeanBuilder.create()
        .defaultValues()
        .username(user1.getNick())
        .socialStatus(SocialStatus.FOE)
        .get();

    connect();

    join(defaultChannel, user1);
    join(defaultChannel, user2);

    instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME)
        .getUser(user1.getNick())
        .ifPresent(chatUser -> chatUser.setPlayer(player));

    chatPrefs.getGroupToColor().put(ChatUserCategory.FOE, Color.ALICEBLUE);

    assertEquals(Color.ALICEBLUE, instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME)
        .getUser(user1.getNick())
        .get()
        .getColor()
        .get());
    assertTrue(instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME).getUser(user2.getNick()).get().getColor().isEmpty());
  }

  @Test
  public void testGroupToColorChangeOther() {
    connect();

    ChatChannel chatChannel = instance.getOrCreateChannel(defaultChannel.getName());

    join(defaultChannel, user1);

    chatPrefs.getGroupToColor().put(ChatUserCategory.OTHER, Color.ALICEBLUE);

    assertEquals(Color.ALICEBLUE, chatChannel.getUser(user1.getNick()).flatMap(ChatChannelUser::getColor).orElse(null));
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
    assertNotNull(chatChannel.getUser(user1.getNick()).orElse(null));
    assertNotNull(chatChannel.getUser(user2.getNick()).orElse(null));

    assertEquals(player1, chatChannel.getUser(user1.getNick()).flatMap(ChatChannelUser::getPlayer).orElse(null));
  }

  @Test
  public void testOnPlayerOnline() {
    connect();

    join(defaultChannel, user2);

    PlayerBean player = PlayerBeanBuilder.create().defaultValues().username(user2.getNick()).get();

    instance.onPlayerOnline(player);

    assertEquals(player, instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME)
        .getUser(user2.getNick())
                                 .flatMap(ChatChannelUser::getPlayer)
        .orElse(null));
  }

  @Test
  public void testOnChatUserList() {
    ChatChannel chatChannel = instance.getOrCreateChannel(defaultChannel.getName());
    assertThat(chatChannel.getUsers(), empty());

    when(defaultChannel.getUsers()).thenReturn(List.of(user1, user2));

    connect();

    eventManager.callEvent(new ChannelNamesUpdatedEvent(realClient,
        List.of(),
        defaultChannel));

    assertThat(chatChannel.getUsers(), hasSize(2));
  }

  @Test
  public void testOnChatUserLeftChannel() {
    ChatChannel chatChannel = instance.getOrCreateChannel(defaultChannel.getName());
    assertThat(chatChannel.getUsers(), empty());

    connect();

    join(defaultChannel, user1);
    join(defaultChannel, user2);

    assertThat(chatChannel.getUsers(), hasSize(2));
    assertNotNull(chatChannel.getUser(user1.getNick()).orElse(null));
    assertNotNull(chatChannel.getUser(user2.getNick()).orElse(null));

    part(defaultChannel, user1);

    assertThat(chatChannel.getUsers(), hasSize(1));
    assertNull(chatChannel.getUser(user1.getNick()).orElse(null));
    assertNotNull(chatChannel.getUser(user2.getNick()).orElse(null));
  }

  @Test
  public void testOnChatUserQuit() {
    ChatChannel chatChannel1 = instance.getOrCreateChannel(defaultChannel.getName());
    ChatChannel chatChannel2 = instance.getOrCreateChannel(otherChannel.getName());
    assertThat(chatChannel1.getUsers(), empty());
    assertThat(chatChannel2.getUsers(), empty());

    connect();

    join(defaultChannel, user1);
    join(otherChannel, user1);

    instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME)
        .getUser(user1.getNick())
        .ifPresent(chatUser -> chatUser.setPlayer(PlayerBeanBuilder.create().defaultValues().get()));

    assertThat(chatChannel1.getUsers(), hasSize(1));
    assertThat(chatChannel2.getUsers(), hasSize(1));

    quit(user1);

    verify(playerService).removePlayerIfOnline(user1.getNick());
    assertThat(chatChannel1.getUsers(), empty());
    assertThat(chatChannel2.getUsers(), empty());
  }

  @Test
  public void testTopicChange() {
    ChatChannel chatChannel = instance.getOrCreateChannel(defaultChannel.getName());
    assertThat(chatChannel.getUsers(), empty());

    connect();

    eventManager.callEvent(new ChannelTopicEvent(realClient,
        new StringCommand("", "", List.of()),
        defaultChannel,
        new DefaultChannelTopic(null, "old topic", new DefaultActor(mock(WithManagement.class), "junit1!IP")),
        new DefaultChannelTopic(null, "new topic", new DefaultActor(mock(WithManagement.class), "junit2!IP")),
        false));

    assertEquals("junit2", chatChannel.getTopic().author());
    assertEquals("new topic", chatChannel.getTopic().content());
  }

  @Test
  public void testChatMessageEventTriggeredByChannelMessage() throws Exception {
    CompletableFuture<ChatMessage> chatMessageFuture = new CompletableFuture<>();

    String message = "chat message";

    connect();

    instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME).addMessageListener(chatMessageFuture::complete);

    messageChannel(defaultChannel, user1, message);

    ChatMessage chatMessage = chatMessageFuture.get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(chatMessage.message(), is(message));
    assertThat(chatMessage.username(), is(user1.getNick()));
    assertThat(chatMessage.action(), is(false));
  }

  @Test
  public void testChatMessageEventTriggeredByChannelAction() throws Exception {
    CompletableFuture<ChatMessage> chatMessageFuture = new CompletableFuture<>();

    String message = "chat action";

    connect();

    instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME).addMessageListener(chatMessageFuture::complete);

    actionChannel(defaultChannel, user1, message);

    ChatMessage chatMessage = chatMessageFuture.get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(chatMessage.message(), is(message));
    assertThat(chatMessage.username(), is(user1.getNick()));
    assertThat(chatMessage.action(), is(true));
  }

  @Test
  public void testChatMessageEventTriggeredByPrivateMessage() throws Exception {
    CompletableFuture<ChatMessage> chatMessageFuture = new CompletableFuture<>();

    Channel privateChannel = mock(Channel.class);
    when(spyClient.getChannel(user1.getNick())).thenReturn(Optional.of(privateChannel));
    when(privateChannel.getUser(user1.getNick())).thenReturn(Optional.of(user1));

    String message = "private message";

    connect();

    instance.getOrCreateChannel(user1.getNick()).addMessageListener(chatMessageFuture::complete);

    sendPrivateMessage(user1, message);

    ChatMessage chatMessage = chatMessageFuture.get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(chatMessage.message(), is(message));
    assertThat(chatMessage.username(), is(user1.getNick()));
    assertThat(chatMessage.action(), is(false));
  }

  @Test
  public void testChatMessageEventNotTriggeredByPrivateMessageFromFoe() {
    Channel privateChannel = mock(Channel.class);
    when(spyClient.getChannel(user1.getNick())).thenReturn(Optional.of(privateChannel));
    when(privateChannel.getUser(user1.getNick())).thenReturn(Optional.of(user1));

    ChatChannelUser foeUser = instance.getOrCreateChatUser(user1.getNick(), user1.getNick());
    foeUser.setPlayer(PlayerBeanBuilder.create()
        .defaultValues()
        .username(user1.getNick())
        .socialStatus(SocialStatus.FOE)
        .get());

    String message = "private message";

    connect();

    instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME).addMessageListener(ignored -> fail());

    sendPrivateMessage(user1, message);
  }

  @Test
  public void testAddModerator() {
    connect();

    join(defaultChannel, user1);

    eventManager.callEvent(new ChannelModeEvent(realClient,
        new StringCommand("", "", List.of()),
        user1,
        defaultChannel,
        DefaultModeStatusList.of(new DefaultModeStatus<>(Action.ADD, new DefaultChannelUserMode(realClient, 'o', '%'), user1.getNick()))));

    assertTrue(instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME)
        .getUser(user1.getNick())
        .map(ChatChannelUser::isModerator)
        .orElseThrow());
  }

  @Test
  public void testRemoveModerator() {
    connect();

    join(defaultChannel, user1);

    instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME)
        .getUser(user1.getNick())
        .ifPresent(chatUser -> chatUser.setModerator(true));

    eventManager.callEvent(new ChannelModeEvent(realClient,
        new StringCommand("", "", List.of()),
        user1,
        defaultChannel,
        DefaultModeStatusList.of(new DefaultModeStatus<>(Action.REMOVE, new DefaultChannelUserMode(realClient, 'o', '%'), user1.getNick()))));

    assertFalse(instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME)
        .getUser(user1.getNick())
        .map(ChatChannelUser::isModerator)
        .orElseThrow());
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

    CompletableFuture<String> future = new CompletableFuture<>();
    ChatChannel chatChannel = new ChatChannel(DEFAULT_CHANNEL_NAME);
    chatChannel.addMessageListener(msg -> future.complete(msg.message()));

    instance.sendMessageInBackground(chatChannel, message);

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

    ObservableList<ChatChannelUser> usersInDefaultChannel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME)
        .getUsers();
    assertThat(usersInDefaultChannel, hasSize(1));
    assertEquals(user1.getNick(), usersInDefaultChannel.get(0).getUsername());

    ObservableList<ChatChannelUser> usersInOtherChannel = instance.getOrCreateChannel(OTHER_CHANNEL_NAME).getUsers();
    assertThat(usersInOtherChannel, hasSize(1));
    assertEquals(user2.getNick(), usersInOtherChannel.get(0).getUsername());
  }

  @Test
  public void testGetChatUsersForChannelTwoUsersInSameChannel() {
    connect();
    join(defaultChannel, user1);
    join(defaultChannel, user2);

    ChatChannel chatChannel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME);

    ObservableList<ChatChannelUser> users = chatChannel.getUsers();
    assertThat(users, hasSize(2));
  }

  @Test
  public void testAddChannelUserListListener() {
    connect();
    @SuppressWarnings("unchecked")
    ListChangeListener<ChatChannelUser> listener = mock(ListChangeListener.class);

    instance.addUsersListener(DEFAULT_CHANNEL_NAME, listener);

    join(defaultChannel, user1);
    join(defaultChannel, user2);

    verify(listener, times(2)).onChanged(any());
  }

  @Test
  public void testSendActionInBackground() throws Exception {
    connect();

    String action = "test action";

    CompletableFuture<String> future = new CompletableFuture<>();
    ChatChannel chatChannel = new ChatChannel(DEFAULT_CHANNEL_NAME);
    chatChannel.addMessageListener(msg -> future.complete(msg.message()));

    instance.sendActionInBackground(chatChannel, action);

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
    assertTrue(instance.isDefaultChannel(new ChatChannel(DEFAULT_CHANNEL_NAME)));
  }

  @Test
  public void testOnDisconnected() {
    connect();
    join(defaultChannel, user1);

    assertThat(instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME).getUsers(), hasSize(1));

    eventManager.callEvent(new ClientConnectionClosedEvent(realClient, false, null, null));

    assertThat(instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME).getUsers(), empty());
  }

  @Test
  public void testClose() {
    instance.destroy();
  }

  @Test
  public void testCreateOrGetChatUserStringPopulatedMap() {
    ChatChannelUser addedUser = instance.getOrCreateChatUser(user1.getNick(), DEFAULT_CHANNEL_NAME);
    ChatChannelUser returnedUser = instance.getOrCreateChatUser(user1.getNick(), DEFAULT_CHANNEL_NAME);

    assertThat(returnedUser, is(addedUser));
    assertEquals(returnedUser, addedUser);
  }

  @Test
  public void testCreateOrGetChatUserUserObjectPopulatedMap() {
    ChatChannelUser addedUser = instance.getOrCreateChatUser(user1.getNick(), DEFAULT_CHANNEL_NAME);
    ChatChannelUser returnedUser = instance.getOrCreateChatUser(user1.getNick(), DEFAULT_CHANNEL_NAME);

    assertThat(returnedUser, is(addedUser));
    assertEquals(returnedUser, addedUser);
  }
}
