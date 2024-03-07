package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.chat.ChatMessage.Type;
import com.faforever.client.chat.emoticons.Emoticon;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.chat.kitteh.event.ChannelRedactMessageEvent;
import com.faforever.client.chat.kitteh.event.PrivateRedactMessageEvent;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Irc;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
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
import org.kitteh.irc.client.library.defaults.element.messagetag.DefaultMessageTagMsgId;
import org.kitteh.irc.client.library.defaults.element.messagetag.DefaultMessageTagTime;
import org.kitteh.irc.client.library.defaults.element.messagetag.DefaultMessageTagTyping;
import org.kitteh.irc.client.library.defaults.element.mode.DefaultChannelUserMode;
import org.kitteh.irc.client.library.defaults.element.mode.DefaultModeStatus;
import org.kitteh.irc.client.library.defaults.element.mode.DefaultModeStatusList;
import org.kitteh.irc.client.library.defaults.feature.DefaultEventManager;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.MessageTag;
import org.kitteh.irc.client.library.element.MessageTag.MsgId;
import org.kitteh.irc.client.library.element.MessageTag.Typing;
import org.kitteh.irc.client.library.element.MessageTag.Typing.State;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.element.mode.ChannelUserMode;
import org.kitteh.irc.client.library.element.mode.ModeStatus.Action;
import org.kitteh.irc.client.library.event.channel.ChannelCtcpEvent;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.channel.ChannelModeEvent;
import org.kitteh.irc.client.library.event.channel.ChannelNamesUpdatedEvent;
import org.kitteh.irc.client.library.event.channel.ChannelPartEvent;
import org.kitteh.irc.client.library.event.channel.ChannelTagMessageEvent;
import org.kitteh.irc.client.library.event.channel.ChannelTopicEvent;
import org.kitteh.irc.client.library.event.client.ClientNegotiationCompleteEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionClosedEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;
import org.kitteh.irc.client.library.event.user.PrivateTagMessageEvent;
import org.kitteh.irc.client.library.event.user.UserAwayMessageEvent;
import org.kitteh.irc.client.library.event.user.UserQuitEvent;
import org.kitteh.irc.client.library.feature.MessageTagManager.DefaultMessageTag;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.scheduling.TaskScheduler;
import reactor.test.publisher.TestPublisher;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.faforever.client.chat.ChatColorMode.DEFAULT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KittehChatServiceTest extends ServiceTest {

  private static final String CHAT_USER_NAME = "junit";
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
  private EmoticonService emoticonService;

  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private PlayerService playerService;
  @Mock
  private FxApplicationThreadExecutor fxApplicationThreadExecutor;
  @Mock
  private TaskScheduler taskScheduler;
  @Spy
  private ClientProperties clientProperties;
  @Spy
  private ChatPrefs chatPrefs;
  @Spy
  private NotificationPrefs notificationPrefs;

  @Mock
  private ScheduledFuture<?> future;

  private DefaultEventManager eventManager;
  private DefaultClient spyClient;
  private PlayerInfo player1;
  private DefaultClient realClient;

  private final BooleanProperty loggedIn = new SimpleBooleanProperty();
  private final TestPublisher<SocialInfo> socialInfoTestPublisher = TestPublisher.create();

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
                                       .then()
                                       .build();

    spyClient = spy(realClient);

    instance.client = spyClient;

    eventManager = (DefaultEventManager) realClient.getEventManager();

    chatPrefs.setChatColorMode(DEFAULT);

    lenient().when(taskScheduler.schedule(any(), any(Instant.class))).thenAnswer(invocation -> future);
    lenient().when(loginService.getUsername()).thenReturn(CHAT_USER_NAME);
    lenient().when(loginService.getOwnPlayer())
             .thenReturn(new Player(0, CHAT_USER_NAME, null, null, "", Map.of(), Map.of(), null));
    lenient().when(loginService.loggedInProperty()).thenReturn(loggedIn);
    lenient().when(defaultChannel.getClient()).thenReturn(realClient);
    lenient().when(defaultChannel.getName()).thenReturn(DEFAULT_CHANNEL_NAME);
    lenient().when(defaultChannel.getMessagingName()).thenReturn(DEFAULT_CHANNEL_NAME);

    Character userPrefix = '+';

    lenient().when(user1.getClient()).thenReturn(realClient);
    lenient().when(user1.getNick()).thenReturn("user1");
    lenient().when(defaultChannel.getUserModes(user1))
             .thenReturn(Optional.of(ImmutableSortedSet.orderedBy(Comparator.comparing(ChannelUserMode::getNickPrefix))
                                                       .add(user1Mode)
                                                       .build()));

    lenient().when(user2.getClient()).thenReturn(realClient);
    lenient().when(user2.getNick()).thenReturn("user2");
    lenient().when(user2Mode.getNickPrefix()).thenReturn(userPrefix);
    lenient().when(defaultChannel.getUserModes(user1))
             .thenReturn(Optional.of(ImmutableSortedSet.orderedBy(Comparator.comparing(ChannelUserMode::getNickPrefix))
                                                       .add(user2Mode)
                                                       .build()));

    player1 = PlayerInfoBuilder.create().defaultValues().get();
    lenient().when(playerService.getPlayerByNameIfOnline(user1.getNick())).thenReturn(Optional.of(player1));
    lenient().when(playerService.getPlayerByNameIfOnline(user2.getNick())).thenReturn(Optional.empty());

    lenient().when(spyClient.getChannel(DEFAULT_CHANNEL_NAME)).thenReturn(Optional.of(defaultChannel));
    lenient().when(defaultChannel.getUser(user1.getNick())).thenReturn(Optional.of(user1));

    lenient().when(fafServerAccessor.getEvents(SocialInfo.class)).thenReturn(socialInfoTestPublisher.flux());

    lenient().doAnswer(invocation -> {
      Runnable runnable = invocation.getArgument(0);
      runnable.run();
      return null;
    }).when(fxApplicationThreadExecutor).execute(any());

    instance.afterPropertiesSet();
  }

  @AfterEach
  public void tearDown() {
    instance.destroy();
  }

  private void join(Channel channel, User user) {
    DefaultMessageTagTime time = DefaultMessageTagTime.FUNCTION.apply(realClient, "time", Instant.now().toString());
    eventManager.callEvent(new ChannelJoinEvent(realClient, new StringCommand("", "", List.of(time)), channel, user));
  }

  private void quit(User user) {
    DefaultMessageTagTime time = DefaultMessageTagTime.FUNCTION.apply(realClient, "time", Instant.now().toString());
    eventManager.callEvent(new UserQuitEvent(realClient, new StringCommand("", "", List.of(time)), user,
                                             String.format("%s quit", user.getNick())));
  }

  private void part(Channel channel, User user) {
    DefaultMessageTagTime time = DefaultMessageTagTime.FUNCTION.apply(realClient, "time", Instant.now().toString());
    eventManager.callEvent(new ChannelPartEvent(realClient, new StringCommand("", "", List.of(time)), channel, user,
                                                String.format("%s left %s", user.getNick(), channel.getName())));
  }

  private void messageChannel(Channel channel, User user, String message) {
    DefaultMessageTagMsgId msgid = DefaultMessageTagMsgId.FUNCTION.apply(realClient, "msgid",
                                                                         String.valueOf(new Random().nextInt()));
    DefaultMessageTagTime time = DefaultMessageTagTime.FUNCTION.apply(realClient, "time", Instant.now().toString());
    eventManager.callEvent(new ChannelMessageEvent(realClient, new StringCommand("", "", List.of(msgid, time)), user,
                                                   channel, message));
  }

  private void actionChannel(Channel channel, User user, String message) {
    DefaultMessageTagMsgId msgid = DefaultMessageTagMsgId.FUNCTION.apply(realClient, "msgid",
                                                                         String.valueOf(new Random().nextInt()));
    DefaultMessageTagTime time = DefaultMessageTagTime.FUNCTION.apply(realClient, "time", Instant.now().toString());
    eventManager.callEvent(new ChannelCtcpEvent(realClient, new StringCommand("", "", List.of(msgid, time)), user,
                                                channel, message));
  }

  private void sendPrivateMessage(User user, String message) {
    DefaultMessageTagMsgId msgid = DefaultMessageTagMsgId.FUNCTION.apply(realClient, "msgid",
                                                                         String.valueOf(new Random().nextInt()));
    DefaultMessageTagTime time = DefaultMessageTagTime.FUNCTION.apply(realClient, "time", Instant.now().toString());
    eventManager.callEvent(new PrivateMessageEvent(realClient, new StringCommand("", "", List.of(msgid, time)), user,
                                                   "me", message));
  }

  private void connect() {
    eventManager.registerEventListener(instance);
    realClient.getActorTracker().setQueryChannelInformation(false);

    eventManager.callEvent(new ClientNegotiationCompleteEvent(realClient, new DefaultActor(realClient, "server"),
                                                              realClient.getServerInfo()));

    SocialInfo socialMessage = new SocialInfo(List.of(), List.of(), List.of(), List.of(), 0);

    socialInfoTestPublisher.next(socialMessage);
  }

  @Test
  public void testGroupToColorChangeFriend() {
    PlayerInfo player = PlayerInfoBuilder.create()
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
                                          .flatMap(ChatChannelUser::getColor)
                                          .orElse(null));
    assertTrue(instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME)
                       .getUser(user2.getNick())
                       .flatMap(ChatChannelUser::getColor)
                       .isEmpty());
  }

  @Test
  public void testGroupToColorChangeFoe() {
    PlayerInfo player = PlayerInfoBuilder.create()
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
                                          .flatMap(ChatChannelUser::getColor)
                                          .orElse(null));
    assertTrue(instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME)
                       .getUser(user2.getNick())
                       .flatMap(ChatChannelUser::getColor)
                       .isEmpty());
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
  public void testOnUserJoinedChannelStale() {
    ChatChannel chatChannel = instance.getOrCreateChannel(defaultChannel.getName());
    assertThat(chatChannel.getUsers(), empty());

    connect();

    DefaultMessageTagTime time = DefaultMessageTagTime.FUNCTION.apply(realClient, "time",
                                                                      Instant.now().minusSeconds(61).toString());
    eventManager.callEvent(
        new ChannelJoinEvent(realClient, new StringCommand("", "", List.of(time)), defaultChannel, user1));

    assertThat(chatChannel.getUsers(), empty());
  }

  @Test
  public void testOnUsersLeaveChannelStale() {
    ChatChannel chatChannel = instance.getOrCreateChannel(defaultChannel.getName());
    assertThat(chatChannel.getUsers(), empty());

    connect();

    join(defaultChannel, user1);

    assertThat(chatChannel.getUsers(), hasSize(1));
    assertNotNull(chatChannel.getUser(user1.getNick()).orElse(null));

    DefaultMessageTagTime time = DefaultMessageTagTime.FUNCTION.apply(realClient, "time",
                                                                      Instant.now().minusSeconds(61).toString());
    eventManager.callEvent(
        new ChannelPartEvent(realClient, new StringCommand("", "", List.of(time)), defaultChannel, user1, ""));

    assertThat(chatChannel.getUsers(), hasSize(1));
    assertNotNull(chatChannel.getUser(user1.getNick()).orElse(null));
  }

  @Test
  public void testOnPlayerOnline() {
    connect();

    join(defaultChannel, user2);

    PlayerInfo player = PlayerInfoBuilder.create().defaultValues().username(user2.getNick()).get();

    instance.onPlayerOnline(player);

    assertEquals(player, instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME).getUser(user2.getNick())
                                 .flatMap(ChatChannelUser::getPlayer).orElse(null));
  }

  @Test
  public void testOnChatUserList() {
    ChatChannel chatChannel = instance.getOrCreateChannel(defaultChannel.getName());
    assertThat(chatChannel.getUsers(), empty());

    when(defaultChannel.getUsers()).thenReturn(List.of(user1, user2));

    connect();

    eventManager.callEvent(new ChannelNamesUpdatedEvent(realClient, List.of(), defaultChannel));

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
    when(otherChannel.getClient()).thenReturn(realClient);
    when(otherChannel.getName()).thenReturn(OTHER_CHANNEL_NAME);
    when(user1Mode.getNickPrefix()).thenReturn('+');
    when(otherChannel.getUserModes(user1)).thenReturn(Optional.of(
        ImmutableSortedSet.orderedBy(Comparator.comparing(ChannelUserMode::getNickPrefix)).add(user1Mode).build()));

    ChatChannel chatChannel1 = instance.getOrCreateChannel(defaultChannel.getName());
    ChatChannel chatChannel2 = instance.getOrCreateChannel(otherChannel.getName());
    assertThat(chatChannel1.getUsers(), empty());
    assertThat(chatChannel2.getUsers(), empty());

    connect();

    join(defaultChannel, user1);
    join(otherChannel, user1);

    instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME)
            .getUser(user1.getNick())
            .ifPresent(chatUser -> chatUser.setPlayer(PlayerInfoBuilder.create().defaultValues().get()));

    assertThat(chatChannel1.getUsers(), hasSize(1));
    assertThat(chatChannel2.getUsers(), hasSize(1));

    quit(user1);

    assertThat(chatChannel1.getUsers(), empty());
    assertThat(chatChannel2.getUsers(), empty());
  }

  @Test
  public void testJoinPrivateChat() {
    instance.joinPrivateChat("junit");

    verify(spyClient).sendRawLine("CHATHISTORY LATEST junit" + " * " + (chatPrefs.getMaxMessages() * 2));
  }

  @Test
  public void testTopicChange() {
    ChatChannel chatChannel = instance.getOrCreateChannel(defaultChannel.getName());
    assertThat(chatChannel.getUsers(), empty());

    connect();

    eventManager.callEvent(new ChannelTopicEvent(realClient, new StringCommand("", "", List.of()), defaultChannel,
                                                 new DefaultChannelTopic(null, "old topic",
                                                                         new DefaultActor(mock(WithManagement.class),
                                                                                          "junit1!IP")),
                                                 new DefaultChannelTopic(null, "new topic",
                                                                         new DefaultActor(mock(WithManagement.class),
                                                                                          "junit2!IP")), false));

    assertEquals("new topic", chatChannel.getTopic().content());
  }

  @Test
  public void testChatMessageEventTriggeredByChannelMessage() throws Exception {
    String message = "chat message";

    connect();

    ChatChannel channel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME);

    messageChannel(defaultChannel, user1, message);

    ChatMessage chatMessage = channel.getMessages()
                                     .stream()
                                     .max(Comparator.comparing(ChatMessage::getTime))
                                     .orElseThrow();

    assertThat(chatMessage.getContent(), is(message));
    assertThat(chatMessage.getSender().getUsername(), is(user1.getNick()));
  }

  @Test
  public void testChatMessageEventTriggeredByChannelAction() throws Exception {
    String message = "chat onAction";

    connect();

    ChatChannel channel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME);

    actionChannel(defaultChannel, user1, message);

    ChatMessage chatMessage = channel.getMessages()
                                     .stream()
                                     .max(Comparator.comparing(ChatMessage::getTime))
                                     .orElseThrow();

    assertThat(chatMessage.getContent(), is(message));
    assertThat(chatMessage.getSender().getUsername(), is(user1.getNick()));
  }

  @Test
  public void testChatMessageEventTriggeredByPrivateMessage() throws Exception {
    String message = "private message";

    connect();

    ChatChannel channel = instance.getOrCreateChannel(user1.getNick());

    sendPrivateMessage(user1, message);

    ChatMessage chatMessage = channel.getMessages()
                                     .stream()
                                     .max(Comparator.comparing(ChatMessage::getTime))
                                     .orElseThrow();

    assertThat(chatMessage.getContent(), is(message));
    assertThat(chatMessage.getSender().getUsername(), is(user1.getNick()));
  }

  @Test
  public void testChatMessageEventNotTriggeredByPrivateMessageFromFoe() {
    PlayerInfo playerInfo = PlayerInfoBuilder.create()
                                             .defaultValues()
                                             .username(user1.getNick())
                                             .socialStatus(SocialStatus.FOE)
                                             .get();
    when(playerService.getPlayerByNameIfOnline(user1.getNick())).thenReturn(Optional.of(playerInfo));

    String message = "private message";

    connect();

    ChatChannel channel = instance.getOrCreateChannel(user1.getNick());

    sendPrivateMessage(user1, message);

    assertThat(channel.getMessages(), empty());
  }

  @Test
  public void testAddModerator() {
    connect();

    join(defaultChannel, user1);

    eventManager.callEvent(new ChannelModeEvent(realClient, new StringCommand("", "", List.of()), user1, defaultChannel,
                                                DefaultModeStatusList.of(new DefaultModeStatus<>(Action.ADD,
                                                                                                 new DefaultChannelUserMode(
                                                                                                     realClient, 'o',
                                                                                                     '%'),
                                                                                                 user1.getNick()))));

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

    eventManager.callEvent(new ChannelModeEvent(realClient, new StringCommand("", "", List.of()), user1, defaultChannel,
                                                DefaultModeStatusList.of(new DefaultModeStatus<>(Action.REMOVE,
                                                                                                 new DefaultChannelUserMode(
                                                                                                     realClient, 'o',
                                                                                                     '%'),
                                                                                                 user1.getNick()))));

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

    ChatChannel chatChannel = new ChatChannel(DEFAULT_CHANNEL_NAME);

    instance.sendMessageInBackground(chatChannel, message).get(TIMEOUT, TIMEOUT_UNIT);

    ChatMessage chatMessage = chatChannel.getMessages()
                                         .stream()
                                         .max(Comparator.comparing(ChatMessage::getTime))
                                         .orElseThrow();
    assertThat(chatMessage.getContent(), is(message));
  }

  @Test
  public void testSendReplyInBackground() throws Exception {
    connect();

    String message = "test message";

    ChatChannel chatChannel = new ChatChannel(DEFAULT_CHANNEL_NAME);

    instance.sendReplyInBackground(
        new ChatMessage("1", Instant.now(), new ChatChannelUser(CHAT_USER_NAME, chatChannel), "", Type.MESSAGE, null),
        message).get(TIMEOUT, TIMEOUT_UNIT);

    ChatMessage chatMessage = chatChannel.getMessages()
                                         .stream()
                                         .max(Comparator.comparing(ChatMessage::getTime))
                                         .orElseThrow();
    assertThat(chatMessage.getContent(), is(message));

    ArgumentCaptor<String> captor = ArgumentCaptor.captor();
    verify(spyClient, atLeastOnce()).sendRawLine(captor.capture());

    String line = captor.getValue();
    assertThat(line, containsString("+draft/reply=1"));
    assertThat(line, containsString("label="));
  }

  @Test
  public void testReactToMessageInBackground() throws Exception {
    connect();

    instance.reactToMessageInBackground(
        new ChatMessage("1", Instant.now(), new ChatChannelUser(CHAT_USER_NAME, new ChatChannel(DEFAULT_CHANNEL_NAME)),
                        "", Type.MESSAGE, null), new Emoticon(List.of(":)"), "")).get(TIMEOUT, TIMEOUT_UNIT);
    ArgumentCaptor<String> captor = ArgumentCaptor.captor();
    verify(spyClient, atLeastOnce()).sendRawLine(captor.capture());

    String line = captor.getValue();
    assertThat(line, containsString("+draft/reply=1"));
    assertThat(line, containsString("+draft/react=:)"));
  }

  @Test
  public void testGetChatUsersForChannelEmpty() {
    ChatChannel chatChannel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME);
    assertThat(chatChannel.getUsers(), empty());
  }

  @Test
  public void testGetChatUsersForChannelTwoUsersInDifferentChannels() {
    when(otherChannel.getClient()).thenReturn(realClient);
    when(otherChannel.getName()).thenReturn(OTHER_CHANNEL_NAME);
    when(otherChannel.getUserModes(user2)).thenReturn(Optional.of(
        ImmutableSortedSet.orderedBy(Comparator.comparing(ChannelUserMode::getNickPrefix)).add(user2Mode).build()));

    connect();
    join(defaultChannel, user1);
    join(otherChannel, user2);

    ObservableList<ChatChannelUser> usersInDefaultChannel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME)
                                                                    .getUsers();
    assertThat(usersInDefaultChannel, hasSize(1));
    assertEquals(user1.getNick(), usersInDefaultChannel.getFirst().getUsername());

    ObservableList<ChatChannelUser> usersInOtherChannel = instance.getOrCreateChannel(OTHER_CHANNEL_NAME).getUsers();
    assertThat(usersInOtherChannel, hasSize(1));
    assertEquals(user2.getNick(), usersInOtherChannel.getFirst().getUsername());
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
  public void testJoinChannel() {
    connect();
    instance.connectionState.set(ConnectionState.CONNECTED);
    String channelToJoin = "#anotherChannel";
    instance.joinChannel(channelToJoin);

    verify(spyClient).addChannel(channelToJoin);
    verify(spyClient).sendRawLine("CHATHISTORY LATEST " + channelToJoin + " * " + (chatPrefs.getMaxMessages() * 2));
    verify(spyClient).sendRawLine("WHO " + channelToJoin);
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
  }

  @Test
  public void testActiveTypingNotification() {
    ChatChannel chatChannel = new ChatChannel(DEFAULT_CHANNEL_NAME);
    instance.setActiveTypingState(chatChannel);

    ArgumentCaptor<String> lineCaptor = ArgumentCaptor.forClass(String.class);

    verify(spyClient).sendRawLine(lineCaptor.capture());
    String sentLine = lineCaptor.getValue();
    assertThat(sentLine, containsString(DEFAULT_CHANNEL_NAME));
    assertThat(sentLine, containsString("+typing=active"));
  }

  @Test
  public void testActiveTypingNotificationThrottled() {
    ChatChannel chatChannel = new ChatChannel(DEFAULT_CHANNEL_NAME);
    instance.setActiveTypingState(chatChannel);
    instance.setActiveTypingState(chatChannel);

    verify(spyClient, times(1)).sendRawLine(any());
  }

  @Test
  public void testActiveTypingNotificationPastThrottleThreshold() throws Exception {
    ChatChannel chatChannel = new ChatChannel(DEFAULT_CHANNEL_NAME);
    instance.setActiveTypingState(chatChannel);
    Thread.sleep(Duration.ofSeconds(4));
    instance.setActiveTypingState(chatChannel);

    verify(spyClient, times(2)).sendRawLine(any());
  }

  @Test
  public void testDoneTypingNotification() {
    ChatChannel chatChannel = new ChatChannel(DEFAULT_CHANNEL_NAME);
    instance.setActiveTypingState(chatChannel);
    instance.setDoneTypingState(chatChannel);

    ArgumentCaptor<String> lineCaptor = ArgumentCaptor.forClass(String.class);

    verify(spyClient, times(2)).sendRawLine(lineCaptor.capture());
    List<String> sentLines = lineCaptor.getAllValues();
    assertThat(sentLines.getFirst(), containsString(DEFAULT_CHANNEL_NAME));
    assertThat(sentLines.getFirst(), containsString("+typing=active"));

    assertThat(sentLines.get(1), containsString(DEFAULT_CHANNEL_NAME));
    assertThat(sentLines.get(1), containsString("+typing=done"));
  }

  @Test
  public void testUserAway() {
    connect();

    join(defaultChannel, user1);
    ChatChannelUser chatUser = instance.getOrCreateChatUser(user1.getNick(), DEFAULT_CHANNEL_NAME);

    eventManager.callEvent(new UserAwayMessageEvent(realClient, new StringCommand("", "", List.of()), user1, "away"));

    assertTrue(chatUser.isAway());

    eventManager.callEvent(new UserAwayMessageEvent(realClient, new StringCommand("", "", List.of()), user1, null));

    assertFalse(chatUser.isAway());
  }

  @Test
  public void testUpdateUserTypingState() {
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
    Instant now = Instant.now();

    ChatChannelUser chatUser = instance.getOrCreateChatUser(user1.getNick(), user1.getNick());

    instance.updateUserTypingState(State.ACTIVE, chatUser);

    assertTrue(chatUser.isTyping());
    assertThat(chatUser.getChannel().getTypingUsers(), hasSize(1));
    verify(taskScheduler).schedule(runnableCaptor.capture(), instantCaptor.capture());

    assertTrue(ChronoUnit.SECONDS.between(now, instantCaptor.getValue()) >= 6);

    runnableCaptor.getValue().run();
    assertFalse(chatUser.isTyping());
    assertThat(chatUser.getChannel().getTypingUsers(), empty());

    instance.updateUserTypingState(State.PAUSED, chatUser);

    assertTrue(chatUser.isTyping());
    assertThat(chatUser.getChannel().getTypingUsers(), hasSize(1));
    verify(taskScheduler, times(2)).schedule(runnableCaptor.capture(), instantCaptor.capture());

    assertTrue(ChronoUnit.SECONDS.between(now, instantCaptor.getValue()) >= 30);

    instance.updateUserTypingState(State.DONE, chatUser);

    assertFalse(chatUser.isTyping());
    assertThat(chatUser.getChannel().getTypingUsers(), empty());
    verify(future).cancel(anyBoolean());
  }

  @Test
  public void testChannelReactTagMessage() {
    Emoticon emoticon = new Emoticon(List.of(), "");
    when(emoticonService.getEmoticonByShortcode(any())).thenReturn(emoticon);
    connect();

    ChatChannelUser chatUser = instance.getOrCreateChatUser(user1.getNick(), defaultChannel.getName());

    ChatMessage message = new ChatMessage("1", Instant.now(), chatUser, "", Type.MESSAGE, null);
    chatUser.getChannel().addMessage(message);

    MsgId id = DefaultMessageTagMsgId.FUNCTION.apply(realClient, "msgid", "1");
    MessageTag react = new DefaultMessageTag("+draft/react", ":)");
    MessageTag reply = new DefaultMessageTag("+draft/reply", "1");
    eventManager.callEvent(
        new ChannelTagMessageEvent(realClient, new StringCommand("TAGMSG", "", List.of(react, reply, id)), user1,
                                   defaultChannel));

    assertThat(message.getReactions(), hasKey(emoticon));
    assertThat(message.getReactions().get(emoticon), hasKey(chatUser.getUsername()));
    assertThat(message.getReactions().get(emoticon).get(chatUser.getUsername()), equalTo("1"));
  }

  @Test
  public void testPrivateTagMessageDoesNotCreateChannel() {
    connect();

    Typing tag = DefaultMessageTagTyping.FUNCTION.apply(realClient, "+typing", "active");
    eventManager.callEvent(
        new PrivateTagMessageEvent(realClient, new StringCommand("TAGMSG", "", List.of(tag)), user1, "me"));

    assertThat(instance.getChannels(), empty());
  }

  @Test
  public void testPrivateTypingTagMessage() {
    connect();

    ChatChannelUser chatUser = instance.getOrCreateChatUser(user1.getNick(), user1.getNick());

    MsgId id = DefaultMessageTagMsgId.FUNCTION.apply(realClient, "msgid", "1");
    Typing tag = DefaultMessageTagTyping.FUNCTION.apply(realClient, "+typing", "active");
    eventManager.callEvent(
        new PrivateTagMessageEvent(realClient, new StringCommand("TAGMSG", "", List.of(tag, id)), user1, "me"));

    assertTrue(chatUser.isTyping());
  }

  @Test
  public void testPrivateReactTagMessage() {
    Emoticon emoticon = new Emoticon(List.of(), "");
    when(emoticonService.getEmoticonByShortcode(any())).thenReturn(emoticon);
    connect();

    ChatChannelUser chatUser = instance.getOrCreateChatUser(user1.getNick(), user1.getNick());

    ChatMessage message = new ChatMessage("1", Instant.now(), chatUser, "", Type.MESSAGE, null);
    chatUser.getChannel().addMessage(message);

    MsgId id = DefaultMessageTagMsgId.FUNCTION.apply(realClient, "msgid", "1");
    MessageTag react = new DefaultMessageTag("+draft/react", ":)");
    MessageTag reply = new DefaultMessageTag("+draft/reply", "1");
    eventManager.callEvent(
        new PrivateTagMessageEvent(realClient, new StringCommand("TAGMSG", "", List.of(react, reply, id)), user1,
                                   "me"));

    assertThat(message.getReactions(), hasKey(emoticon));
    assertThat(message.getReactions().get(emoticon), hasKey(chatUser.getUsername()));
    assertThat(message.getReactions().get(emoticon).get(chatUser.getUsername()), equalTo("1"));
  }

  @Test
  public void testChannelTypingTagMessage() {
    connect();

    ChatChannelUser chatUser = instance.getOrCreateChatUser(user1.getNick(), defaultChannel.getName());

    MsgId id = DefaultMessageTagMsgId.FUNCTION.apply(realClient, "msgid", "1");
    Typing typing = DefaultMessageTagTyping.FUNCTION.apply(realClient, "+typing", "active");
    eventManager.callEvent(
        new ChannelTagMessageEvent(realClient, new StringCommand("TAGMSG", "", List.of(typing, id)), user1,
                                   defaultChannel));

    assertTrue(chatUser.isTyping());
  }

  @Test
  public void testChannelMessageRemovesTyping() {
    connect();

    join(defaultChannel, user1);

    ChatChannelUser chatUser = instance.getOrCreateChatUser(user1.getNick(), defaultChannel.getName());
    chatUser.setTyping(true);

    messageChannel(defaultChannel, user1, "");

    assertFalse(chatUser.isTyping());
  }

  @Test
  public void testChannelActionRemovesTyping() {
    connect();

    join(defaultChannel, user1);

    ChatChannelUser chatUser = instance.getOrCreateChatUser(user1.getNick(), defaultChannel.getName());
    chatUser.setTyping(true);

    actionChannel(defaultChannel, user1, "");

    assertFalse(chatUser.isTyping());
  }

  @Test
  public void testPrivateMessageRemovesTyping() {
    connect();

    ChatChannelUser chatUser = instance.getOrCreateChatUser(user1.getNick(), user1.getNick());
    chatUser.setTyping(true);

    sendPrivateMessage(user1, "");

    assertFalse(chatUser.isTyping());
  }

  @Test
  public void testChannelRedactMessage() {
    connect();

    ChatChannelUser chatUser = instance.getOrCreateChatUser(user1.getNick(), defaultChannel.getName());

    ChatMessage message = new ChatMessage("1", Instant.now(), chatUser, "", Type.MESSAGE, null);
    chatUser.getChannel().addMessage(message);

    MsgId id = DefaultMessageTagMsgId.FUNCTION.apply(realClient, "msgid", "2");
    eventManager.callEvent(new ChannelRedactMessageEvent(realClient,
                                                         new StringCommand("REDACT", defaultChannel.getName() + " 1",
                                                                           List.of(id)), user1, defaultChannel, null,
                                                         "1"));

    assertThat(chatUser.getChannel().getMessages(), empty());
  }

  @Test
  public void testPrivateRedactMessage() {
    connect();

    ChatChannelUser chatUser = instance.getOrCreateChatUser(user1.getNick(), user1.getNick());

    ChatMessage message = new ChatMessage("1", Instant.now(), chatUser, "", Type.MESSAGE, null);
    chatUser.getChannel().addMessage(message);

    MsgId id = DefaultMessageTagMsgId.FUNCTION.apply(realClient, "msgid", "2");
    eventManager.callEvent(new PrivateRedactMessageEvent(realClient,
                                                         new StringCommand("REDACT", defaultChannel.getName() + " 1",
                                                                           List.of(id)), user1, user1.getNick(), null,
                                                         "1"));

    assertThat(chatUser.getChannel().getMessages(), empty());
  }

  @Test
  public void testPrivateMessageUnread() {
    connect();

    join(defaultChannel, user1);

    sendPrivateMessage(user1, "CHAT_USER_NAME");

    ChatChannel channel = instance.getOrCreateChannel(user1.getNick());
    verify(audioService).playPrivateMessageSound();
    verify(notificationService).addNotification(any(TransientNotification.class));
    assertEquals(1, channel.getNumUnreadMessages());
  }

  @Test
  public void testPrivateMessageChannelOpen() {
    connect();

    join(defaultChannel, user1);
    ChatChannel channel = instance.getOrCreateChannel(user1.getNick());
    channel.setOpen(true);
    sendPrivateMessage(user1, "CHAT_USER_NAME");

    verify(audioService, never()).playPrivateMessageSound();
    verify(notificationService, never()).addNotification(any(TransientNotification.class));
    assertEquals(0, channel.getNumUnreadMessages());
  }

  @Test
  public void testMention() {
    connect();

    join(defaultChannel, user1);

    messageChannel(defaultChannel, user1, CHAT_USER_NAME);

    ChatChannel channel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME);
    verify(audioService).playChatMentionSound();
    verify(notificationService).addNotification(any(TransientNotification.class));
    assertEquals(1, channel.getNumUnreadMessages());
  }

  @Test
  public void testMentionStale() {
    connect();

    join(defaultChannel, user1);

    DefaultMessageTagMsgId msgid = DefaultMessageTagMsgId.FUNCTION.apply(realClient, "msgid",
                                                                         String.valueOf(new Random().nextInt()));
    DefaultMessageTagTime time = DefaultMessageTagTime.FUNCTION.apply(realClient, "time",
                                                                      Instant.now().minusSeconds(61).toString());
    eventManager.callEvent(
        new ChannelMessageEvent(realClient, new StringCommand("", "", List.of(msgid, time)), user1, defaultChannel,
                                CHAT_USER_NAME));

    ChatChannel channel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME);
    verify(audioService, never()).playChatMentionSound();
    verify(notificationService, never()).addNotification(any(TransientNotification.class));
    assertEquals(0, channel.getNumUnreadMessages());
  }

  @Test
  public void testReplyMention() {
    when(user1.getNick()).thenReturn(CHAT_USER_NAME);

    connect();

    join(defaultChannel, user1);

    MsgId id1 = DefaultMessageTagMsgId.FUNCTION.apply(realClient, "msgid", "1");
    eventManager.callEvent(
        new ChannelMessageEvent(realClient, new StringCommand("", "", List.of(id1)), user1, defaultChannel, ""));

    MsgId id2 = DefaultMessageTagMsgId.FUNCTION.apply(realClient, "msgid", "2");
    DefaultMessageTagTime time = DefaultMessageTagTime.FUNCTION.apply(realClient, "time", Instant.now().toString());
    MessageTag reply = new DefaultMessageTag("+draft/reply", "1");
    eventManager.callEvent(
        new ChannelMessageEvent(realClient, new StringCommand("", "", List.of(id2, reply, time)), user2, defaultChannel,
                                ""));

    ChatChannel channel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME);
    verify(audioService).playChatMentionSound();
    verify(notificationService).addNotification(any(TransientNotification.class));
    assertEquals(1, channel.getNumUnreadMessages());
  }

  @Test
  public void testMentionChannelOpen() {
    connect();

    join(defaultChannel, user1);
    ChatChannel channel = instance.getOrCreateChannel(DEFAULT_CHANNEL_NAME);
    channel.setOpen(true);
    messageChannel(defaultChannel, user1, CHAT_USER_NAME);

    verify(audioService, never()).playChatMentionSound();
    verify(notificationService, never()).addNotification(any(TransientNotification.class));
    assertEquals(0, channel.getNumUnreadMessages());
  }

  @Test
  public void testAtMention() {
    notificationPrefs.notifyOnAtMentionOnlyEnabledProperty().setValue(false);
    assertTrue(instance.hasMention("hello @" + CHAT_USER_NAME + "!!"));
  }

  @Test
  public void testAtMentionWhenFlagIsEnabled() {
    notificationPrefs.notifyOnAtMentionOnlyEnabledProperty().setValue(true);
    assertTrue(instance.hasMention("hello @" + CHAT_USER_NAME + "!!"));
  }

  @Test
  public void testNormalMention() {
    notificationPrefs.notifyOnAtMentionOnlyEnabledProperty().setValue(false);
    assertTrue(instance.hasMention("hello " + CHAT_USER_NAME + "!!"));
  }

  @Test
  public void testNormalMentionDoesNotTriggerNotificationWhenFlagIsEnabled() {
    notificationPrefs.notifyOnAtMentionOnlyEnabledProperty().setValue(true);
    assertFalse(instance.hasMention("hello " + CHAT_USER_NAME + "!!"));
  }

  @Test
  public void testMentionDetection() {
    when(loginService.getUsername()).thenReturn("-Box-");

    assertTrue(instance.hasMention("-Box-"));
    assertTrue(instance.hasMention("-Box-!"));
    assertTrue(instance.hasMention("!-Box-"));
    assertTrue(instance.hasMention("Goodbye -Box-"));
    assertFalse(instance.hasMention(" "));
    assertFalse(instance.hasMention(""));
    assertFalse(instance.hasMention("-Box-h"));
    assertFalse(instance.hasMention("h-Box-"));
  }
}
