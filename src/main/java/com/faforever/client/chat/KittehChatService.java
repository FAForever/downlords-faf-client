package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.chat.ChatMessage.Type;
import com.faforever.client.chat.emoticons.Emoticon;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.chat.emoticons.Reaction;
import com.faforever.client.chat.kitteh.event.ChannelRedactMessageEvent;
import com.faforever.client.chat.kitteh.event.PrivateRedactMessageEvent;
import com.faforever.client.chat.kitteh.event.RedactMessageEvent;
import com.faforever.client.chat.kitteh.listener.RedactListener;
import com.faforever.client.chat.kitteh.listener.WhoAwayListener;
import com.faforever.client.chat.kitteh.listener.WhoAwayListener.WhoAwayMessageEvent;
import com.faforever.client.chat.kitteh.listener.WhoAwayListener.WhoComplete;
import com.faforever.client.chat.kitteh.network.WebsocketNetworkHandler;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Irc;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.ui.tray.TrayIconManager;
import com.faforever.client.ui.tray.event.UpdateApplicationBadgeEvent;
import com.faforever.client.user.LoginService;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.commons.lobby.Player.LeaderboardStats;
import com.faforever.commons.lobby.SocialInfo;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.paint.Color;
import javafx.util.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.Client.Builder.Server.SecurityType;
import org.kitteh.irc.client.library.Client.WithManagement;
import org.kitteh.irc.client.library.command.MessageCommand;
import org.kitteh.irc.client.library.command.TagMessageCommand;
import org.kitteh.irc.client.library.defaults.element.messagetag.DefaultMessageTagTyping;
import org.kitteh.irc.client.library.defaults.listener.DefaultListeners;
import org.kitteh.irc.client.library.defaults.listener.DefaultTagmsgListener;
import org.kitteh.irc.client.library.element.Actor;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.MessageTag;
import org.kitteh.irc.client.library.element.MessageTag.Label;
import org.kitteh.irc.client.library.element.MessageTag.MsgId;
import org.kitteh.irc.client.library.element.MessageTag.Time;
import org.kitteh.irc.client.library.element.MessageTag.Typing;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.element.mode.ChannelUserMode;
import org.kitteh.irc.client.library.element.mode.Mode;
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
import org.kitteh.irc.client.library.event.connection.ClientConnectionEndedEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionFailedEvent;
import org.kitteh.irc.client.library.event.helper.ActorEvent;
import org.kitteh.irc.client.library.event.helper.ActorMessageEvent;
import org.kitteh.irc.client.library.event.helper.MessageEvent;
import org.kitteh.irc.client.library.event.helper.PrivateEvent;
import org.kitteh.irc.client.library.event.helper.ServerMessageEvent;
import org.kitteh.irc.client.library.event.helper.TagMessageEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;
import org.kitteh.irc.client.library.event.user.PrivateTagMessageEvent;
import org.kitteh.irc.client.library.event.user.UserAwayMessageEvent;
import org.kitteh.irc.client.library.event.user.UserQuitEvent;
import org.kitteh.irc.client.library.feature.EventListenerSupplier;
import org.kitteh.irc.client.library.feature.auth.SaslPlain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import static com.faforever.client.chat.ChatColorMode.RANDOM;
import static java.util.Locale.US;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static javafx.collections.FXCollections.observableHashMap;
import static javafx.collections.FXCollections.synchronizedObservableMap;
import static org.kitteh.irc.client.library.feature.CapabilityManager.Defaults.ECHO_MESSAGE;
import static org.kitteh.irc.client.library.feature.CapabilityManager.Defaults.MESSAGE_TAGS;

@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class KittehChatService implements ChatService, InitializingBean, DisposableBean {

  private static final Logger ircLog = LoggerFactory.getLogger("faf-irc");

  public static final int MAX_GAMES_FOR_NEWBIE_CHANNEL = 50;
  private static final String NEWBIE_CHANNEL_NAME = "#newbie";
  private static final Set<Character> MODERATOR_PREFIXES = Set.of('~', '&', '@', '%');
  private final LoginService loginService;
  private final FafServerAccessor fafServerAccessor;
  private final ClientProperties clientProperties;
  private final PlayerService playerService;
  private final ChatPrefs chatPrefs;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final TrayIconManager trayIconManager;
  private final NotificationPrefs notificationPrefs;
  private final AudioService audioService;
  private final NotificationService notificationService;
  private final EmoticonService emoticonService;
  private final NavigationHandler navigationHandler;
  private final TaskScheduler taskScheduler;
  @Qualifier("userWebClient")
  private final ObjectFactory<WebClient> userWebClientFactory;

  /**
   * Maps channels by name.
   */
  private final ObservableMap<String, ChatChannel> channels = synchronizedObservableMap(observableHashMap());
  private final Map<ChatChannel, Set<Subscription>> channelSubscriptions = new ConcurrentHashMap<>();

  private final Map<ChatChannel, Instant> lastSentActiveMap = new ConcurrentHashMap<>();
  private final Map<ChatChannelUser, Future<?>> stopTypingFutureMap = new ConcurrentHashMap<>();

  /**
   * A list of channels the server wants us to join.
   */
  private final List<String> autoChannels = new ArrayList<>();
  private final Queue<String> bufferedChannels = new ArrayDeque<>();

  private boolean autoReconnect;

  @VisibleForTesting
  ObjectProperty<ConnectionState> connectionState = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);
  @VisibleForTesting
  String defaultChannelName;
  @VisibleForTesting
  Client.WithManagement client;

  @Override
  public void afterPropertiesSet() {
    loginService.loggedInProperty().subscribe(loggedIn -> {
      if (loggedIn) {
        connect();
      } else {
        disconnect();
      }
    });

    fafServerAccessor.getEvents(SocialInfo.class)
                     .doOnNext(this::onSocialMessage)
                     .doOnError(throwable -> log.warn("Unable to process social info", throwable))
                     .retry()
                     .subscribe();

    playerService.addPlayerOnlineListener(this::onPlayerOnline);
    playerService.addPlayerOfflineListener(this::onPlayerOffline);
    chatPrefs.groupToColorProperty().subscribe(this::updateUserColors);
    chatPrefs.chatColorModeProperty().subscribe(this::updateUserColors);
    chatPrefs.maxMessagesProperty()
             .subscribe(maxMessages -> channels.values()
                                               .forEach(channel -> channel.setMaxNumMessages(maxMessages.intValue())));
    connectionState.subscribe((oldValue, newValue) -> {
      if (autoReconnect && oldValue == ConnectionState.CONNECTED && newValue == ConnectionState.DISCONNECTED) {
        connect();
      }
    });
  }

  private void updateUserColors() {
    channels.values().stream().map(ChatChannel::getUsers).flatMap(Collection::stream).forEach(this::populateColor);
  }

  @Override
  public boolean userExistsInAnyChannel(String username) {
    return client.getChannels().stream().map(channel -> channel.getUser(username)).anyMatch(Optional::isPresent);
  }

  @Override
  public ChatChannelUser getOrCreateChatUser(String username, String channelName) {
    return getOrCreateChannel(channelName).createUserIfNecessary(username, this::initializeChatChannelUser);
  }

  private ChatChannelUser getOrCreateChatUser(User user, Channel channel) {
    return getOrCreateChannel(channel.getName()).createUserIfNecessary(user.getNick(), chatChannelUser -> {
      initializeChatChannelUser(chatChannelUser);
      boolean isModerator = channel.getUserModes(user)
                                   .stream()
                                   .flatMap(Collection::stream)
                                   .map(ChannelUserMode::getNickPrefix)
                                   .anyMatch(MODERATOR_PREFIXES::contains);
      chatChannelUser.setModerator(isModerator);
      chatChannelUser.setAway(user.isAway());
    });
  }

  private void updateChatUser(User user, Channel channel) {
    ChatChannelUser chatChannelUser = getOrCreateChatUser(user, channel);
    boolean isModerator = channel.getUserModes(user)
                                 .stream()
                                 .flatMap(Collection::stream)
                                 .map(ChannelUserMode::getNickPrefix)
                                 .anyMatch(MODERATOR_PREFIXES::contains);
    chatChannelUser.setModerator(isModerator);
    chatChannelUser.setAway(user.isAway());
  }

  private void initializeChatChannelUser(ChatChannelUser chatChannelUser) {
    playerService.getPlayerByNameIfOnline(chatChannelUser.getUsername()).ifPresent(chatChannelUser::setPlayer);
    chatChannelUser.categoryProperty().subscribe(() -> populateColor(chatChannelUser));
    populateColor(chatChannelUser);
  }

  @VisibleForTesting
  void onPlayerOnline(PlayerInfo player) {
    channels.values()
            .stream()
            .map(channel -> channel.getUser(player.getUsername()))
            .flatMap(Optional::stream)
            .forEach(chatChannelUser -> fxApplicationThreadExecutor.execute(() -> chatChannelUser.setPlayer(player)));
  }

  @VisibleForTesting
  void onPlayerOffline(PlayerInfo player) {
    channels.values()
            .stream()
            .map(channel -> channel.getUser(player.getUsername()))
            .flatMap(Optional::stream)
            .forEach(chatChannelUser -> fxApplicationThreadExecutor.execute(() -> chatChannelUser.setPlayer(null)));
  }

  @Handler
  public void onRedactMessage(RedactMessageEvent event) {
    if (!(event instanceof ActorEvent<?> actorEvent) || !(actorEvent.getActor() instanceof User user)) {
      return;
    }

    String senderNick = user.getNick();
    ChatChannel chatChannel = switch (event) {
      case PrivateRedactMessageEvent privateRedactMessageEvent ->
          channels.get(getPrivateMessageTarget(privateRedactMessageEvent, senderNick));
      case ChannelRedactMessageEvent channelRedactMessageEvent ->
          channels.get(channelRedactMessageEvent.getChannel().getName());
      default -> null;
    };

    if (chatChannel == null) {
      return;
    }

    chatChannel.removeMessage(event.getRedactedMessageId());
  }

  @Handler
  public void onTagMessage(TagMessageEvent event) {
    if (!(event instanceof ActorEvent<?> actorEvent) || !(actorEvent.getActor() instanceof User user)) {
      return;
    }

    String senderNick = user.getNick();
    switch (event) {
      case PrivateTagMessageEvent privateTagMessageEvent -> {
        String target = getPrivateMessageTarget(privateTagMessageEvent, senderNick);
        Optional.ofNullable(channels.get(target))
                .flatMap(channel -> channel.getUser(senderNick))
                .ifPresent(chatUser -> processTagMessage(privateTagMessageEvent, chatUser));
      }
      case ChannelTagMessageEvent channelTagMessageEvent -> {
        ChatChannelUser chatUser = getOrCreateChatUser(user, channelTagMessageEvent.getChannel());
        processTagMessage(channelTagMessageEvent, chatUser);
      }
      default -> {}
    }
  }

  private <T extends TagMessageEvent & ServerMessageEvent> void processTagMessage(T event, ChatChannelUser chatUser) {
    String messageId = event.getTag("msgid", MsgId.class)
                            .map(MsgId::getId)
                            .orElseThrow(() -> new IllegalArgumentException(
                                "Message does not have an id: %s".formatted(event.getSource())));

    event.getTag("+typing", Typing.class).ifPresent(typing -> updateUserTypingState(typing.getState(), chatUser));

    event.getTag("+draft/react")
         .flatMap(MessageTag::getValue)
         .map(emoticonService::getEmoticonByShortcode)
         .flatMap(emoticon -> event.getTag("+draft/reply")
                                   .flatMap(MessageTag::getValue)
                                   .map(targetMessageId -> new Reaction(messageId, targetMessageId, emoticon,
                                                                        chatUser.getUsername())))
         .ifPresent(reaction -> fxApplicationThreadExecutor.execute(() -> chatUser.getChannel().addReaction(reaction)));
  }

  @VisibleForTesting
  void updateUserTypingState(Typing.State state, ChatChannelUser chatChannelUser) {
    Future<?> stopTypingFuture = stopTypingFutureMap.remove(chatChannelUser);
    if (stopTypingFuture != null) {
      stopTypingFuture.cancel(true);
    }

    switch (state) {
      case ACTIVE -> {
        fxApplicationThreadExecutor.execute(() -> chatChannelUser.setTyping(true));
        Future<?> future = taskScheduler.schedule(() -> removeTyping(chatChannelUser), Instant.now().plusSeconds(6));
        stopTypingFutureMap.put(chatChannelUser, future);
      }
      case PAUSED -> {
        fxApplicationThreadExecutor.execute(() -> chatChannelUser.setTyping(true));
        Future<?> future = taskScheduler.schedule(() -> removeTyping(chatChannelUser), Instant.now().plusSeconds(30));
        stopTypingFutureMap.put(chatChannelUser, future);
      }
      case DONE -> fxApplicationThreadExecutor.execute(() -> chatChannelUser.setTyping(false));
    }
  }

  private void removeTyping(ChatChannelUser chatChannelUser) {
    stopTypingFutureMap.remove(chatChannelUser);
    fxApplicationThreadExecutor.execute(() -> chatChannelUser.setTyping(false));
  }

  @Override
  public void setActiveTypingState(ChatChannel channel) {
    Instant now = Instant.now();

    Instant lastActiveTypingSent = lastSentActiveMap.get(channel);
    if (lastActiveTypingSent == null || ChronoUnit.SECONDS.between(lastActiveTypingSent, now) > 3) {
      setTypingState(channel, TypingState.ACTIVE);
      lastSentActiveMap.put(channel, now);
    }
  }

  @Override
  public void setDoneTypingState(ChatChannel channel) {
    setTypingState(channel, TypingState.DONE);
  }

  @Handler
  public void onUserAway(UserAwayMessageEvent event) {
    User user = event.getActor();
    String username = user.getNick();
    channels.values()
            .forEach(chatChannel -> chatChannel.getUser(username)
                                               .ifPresent(chatUser -> fxApplicationThreadExecutor.execute(
                                                   () -> chatUser.setAway(event.isAway()))));
  }

  @Handler
  public void onConnect(ClientNegotiationCompleteEvent event) {
    connectionState.set(ConnectionState.CONNECTED);
    channels.keySet().forEach(this::joinChannel);
    joinSavedAutoChannels();
    joinBufferedChannels();

    if (loginService.getOwnPlayer()
                    .getRatings()
                    .values()
                    .stream()
                    .mapToInt(LeaderboardStats::getNumberOfGames)
                    .sum() < MAX_GAMES_FOR_NEWBIE_CHANNEL) {
      joinChannel(NEWBIE_CHANNEL_NAME);
    }

    client.commands()
          .capabilityRequest()
          .enable(ECHO_MESSAGE)
          .enable("draft/chathistory")
          .enable("draft/event-playback").enable("draft/message-redaction")
          .execute();
  }

  @Handler
  public void onJoinEvent(ChannelJoinEvent event) {
    if (isStale(event)) {
      return;
    }

    User user = event.getActor();
    updateChatUser(user, event.getChannel());
  }

  @Handler
  public void onChatUserList(ChannelNamesUpdatedEvent event) {
    Channel channel = event.getChannel();
    channel.getUsers().forEach(user -> updateChatUser(user, channel));
  }

  @Handler
  public void onWhoAway(WhoAwayMessageEvent event) {
    ChatChannel chatChannel = channels.get(event.channel());
    if (chatChannel != null) {
      chatChannel.getUser(event.userName()).ifPresent(chatUser -> chatUser.setAway(event.isAway()));
    }
  }

  @Handler
  public void onWhoComplete(WhoComplete event) {
    ChatChannel chatChannel = channels.get(event.getChannel().getName());
    if (chatChannel != null) {
      fxApplicationThreadExecutor.execute(() -> chatChannel.setLoaded(true));
    }
  }

  @Handler
  public void onPartEvent(ChannelPartEvent event) {
    if (isStale(event)) {
      return;
    }

    User user = event.getActor();
    onChatUserLeftChannel(event.getChannel().getName(), user.getNick());
  }

  @Handler
  public void onChatUserQuit(UserQuitEvent event) {
    if (isStale(event)) {
      return;
    }

    User user = event.getUser();
    String username = user.getNick();

    List.copyOf(channels.keySet()).forEach(channelName -> onChatUserLeftChannel(channelName, username));
  }

  @Handler
  public void onTopicChange(ChannelTopicEvent event) {
    String author = event.getNewTopic()
                         .getSetter()
                         .map(Actor::getName)
                         .map(name -> name.replaceFirst("!.*", ""))
                         .orElse("");
    String content = event.getNewTopic().getValue().orElse("");
    ChatChannelUser chatChannelUser = getOrCreateChatUser(author, event.getChannel().getName());
    chatChannelUser.getChannel().setTopic(new ChannelTopic(chatChannelUser, content));
  }

  @Handler
  public void onMessage(ActorMessageEvent<?> event) {
    if (!(event.getActor() instanceof User user) || user.getNick().equals("HistServ")) {
      return;
    }

    String senderNick = user.getNick();
    boolean hideFoeMessages = chatPrefs.isHideFoeMessages();
    ChatChannelUser sender = switch (event) {
      case ChannelMessageEvent channelMessageEvent -> getOrCreateChatUser(user, channelMessageEvent.getChannel());
      case PrivateMessageEvent privateMessageEvent when playerService.getPlayerByNameIfOnline(senderNick)
                                                                     .map(PlayerInfo::getSocialStatus)
                                                                     .map(SocialStatus.FOE::equals)
                                                                     .map(isFoe -> !(hideFoeMessages && isFoe))
                                                                     .orElse(true) -> {
        String target = getPrivateMessageTarget(privateMessageEvent, senderNick);
        yield getOrCreateChatUser(senderNick, target);
      }
      default -> null;
    };

    if (sender == null) {
      return;
    }

    processChatMessage(event, sender);
  }

  private String getPrivateMessageTarget(PrivateEvent privateEvent, String senderNick) {
    return senderNick.equals(getCurrentUsername()) ? privateEvent.getTarget() : senderNick;
  }

  private void processChatMessage(MessageEvent event, ChatChannelUser sender) {
    String text = event.getMessage();
    ChatChannel chatChannel = sender.getChannel();
    sender.setTyping(false);

    Instant messageTime = event.getTag("time", Time.class).map(Time::getTime).orElse(Instant.now());

    String messageId = event.getTag("msgid", MsgId.class)
                            .map(MsgId::getId)
                            .orElseThrow(
                                () -> new IllegalArgumentException("Message does not have an id: %s".formatted(event)));

    ChatMessage targetMessage = event.getTag("+draft/reply")
                                     .flatMap(MessageTag::getValue)
                                     .flatMap(chatChannel::getMessage)
                                     .orElse(null);

    event.getTag("label", Label.class).map(Label::getLabel).ifPresent(chatChannel::removePendingMessage);

    ChatMessage message = new ChatMessage(messageId, messageTime, sender, text, Type.MESSAGE, targetMessage);
    chatChannel.addMessage(message);

    switch (event) {
      case PrivateMessageEvent privateEvent when !isStale(privateEvent) -> notifyOnPrivateMessage(message);
      case ChannelMessageEvent channelEvent when !isStale(channelEvent) -> notifyIfMentioned(message);
      default -> {}
    }
  }

  private void notifyIfMentioned(ChatMessage chatMessage) {
    ChatChannelUser sender = chatMessage.getSender();
    if (sender.getCategory() == ChatUserCategory.FOE) {
      log.debug("Ignored mention from foe {}", sender);
      return;
    }

    String text = chatMessage.getContent();
    if (!(hasMention(text) || chatMessage.getTargetMessage()
                                         .map(ChatMessage::getSender)
                                         .map(ChatChannelUser::getUsername)
                                         .map(getCurrentUsername()::equals)
                                         .orElse(false))) {
      return;
    }

    ChatChannel channel = sender.getChannel();
    if (!channel.isOpen()) {
      audioService.playChatMentionSound();
      channel.setNumUnreadMessages(channel.getNumUnreadMessages() + 1);

      if (notificationPrefs.isPrivateMessageToastEnabled()) {
        String identIconSource = sender.getPlayer().map(PlayerInfo::getId)
                                       .map(String::valueOf)
                                       .orElse(sender.getUsername());
        notificationService.addNotification(
            new TransientNotification(sender.getUsername(), text, IdenticonUtil.createIdenticon(identIconSource),
                                      () -> navigationHandler.navigateTo(new NavigateEvent(NavigationItem.CHAT))));
      }
    }
  }

  private void notifyOnPrivateMessage(ChatMessage chatMessage) {
    ChatChannelUser sender = chatMessage.getSender();
    ChatChannel channel = sender.getChannel();
    if (channel.isPrivateChannel() && !channel.isOpen()) {
      audioService.playPrivateMessageSound();
      channel.setNumUnreadMessages(channel.getNumUnreadMessages() + 1);

      if (notificationPrefs.isPrivateMessageToastEnabled()) {
        String identIconSource = sender.getPlayer().map(PlayerInfo::getId)
                                       .map(String::valueOf)
                                       .orElse(sender.getUsername());
        notificationService.addNotification(new TransientNotification(sender.getUsername(), chatMessage.getContent(),
                                                                      IdenticonUtil.createIdenticon(identIconSource),
                                                                      () -> navigationHandler.navigateTo(
                                                                          new NavigateEvent(NavigationItem.CHAT))));
      }
    }
  }

  @Handler
  public void onChannelCTCP(ChannelCtcpEvent event) {
    User user = event.getActor();

    String channelName = event.getChannel().getName();

    String senderNick = user.getNick();
    String message = event.getMessage().replace("ACTION", senderNick);
    ChatChannelUser sender = getOrCreateChatUser(senderNick, channelName);
    ChatChannel chatChannel = sender.getChannel();
    sender.setTyping(false);

    Instant messageTime = event.getTag("time", Time.class)
                               .map(Time::getTime)
                               .orElse(Instant.now());

    String messageId = event.getTag("msgid", MsgId.class)
                            .map(MsgId::getId)
                            .orElseThrow(
                                () -> new IllegalArgumentException("Message does not have an id: %s".formatted(event)));
    ChatMessage targetMessage = event.getTag("+draft/reply")
                                     .flatMap(MessageTag::getValue)
                                     .flatMap(chatChannel::getMessage)
                                     .orElse(null);

    event.getTag("label", Label.class).map(Label::getLabel).ifPresent(chatChannel::removePendingMessage);

    chatChannel.addMessage(new ChatMessage(messageId, messageTime, sender, message, Type.ACTION, targetMessage));
  }

  @Handler
  public void onChannelModeChanged(ChannelModeEvent event) {
    event.getStatusList().getAll().forEach(channelModeStatus -> channelModeStatus.getParameter().ifPresent(username -> {
      Mode changedMode = channelModeStatus.getMode();
      Action modeAction = channelModeStatus.getAction();
      if (changedMode instanceof ChannelUserMode channelUserMode && MODERATOR_PREFIXES.contains(
          channelUserMode.getNickPrefix())) {
        ChatChannelUser chatChannelUser = getOrCreateChatUser(username, event.getChannel().getName());
        if (modeAction == Action.ADD) {
          chatChannelUser.setModerator(true);
        } else if (modeAction == Action.REMOVE) {
          chatChannelUser.setModerator(false);
        }
      }
    }));
  }

  private void joinAutoChannels() {
    log.trace("Joining auto channels: {}", autoChannels);
    autoChannels.forEach(this::joinChannel);
  }

  private void joinSavedAutoChannels() {
    ObservableList<String> savedAutoChannels = chatPrefs.getAutoJoinChannels();
    if (savedAutoChannels == null) {
      return;
    }
    log.trace("Joining user's saved auto channels: {}", savedAutoChannels);
    savedAutoChannels.forEach(this::joinChannel);
  }

  private void joinBufferedChannels() {
    String channel;
    while ((channel = bufferedChannels.poll()) != null) {
      joinChannel(channel);
    }
  }

  private void onChatUserLeftChannel(String channelName, String username) {
    ChatChannel chatChannel = channels.get(channelName);
    if (chatChannel == null) {
      return;
    }

    chatChannel.removeUser(username);
  }

  private void onMessage(String message) {
    ircLog.debug(message);
  }

  private void onException(Throwable throwable) {
    log.warn("Exception in message processing", throwable);
  }

  @Handler
  public void onDisconnect(ClientConnectionEndedEvent event) {
    client.getEventManager().unregisterEventListener(this);
    channels.values().forEach(ChatChannel::clearUsers);
    List.copyOf(channels.keySet()).forEach(this::removeChannel);
    connectionState.set(ConnectionState.DISCONNECTED);
    client.shutdown();
    event.getCause().ifPresent(throwable -> log.error("Chat disconnected with cause", throwable));
  }

  @Handler
  public void onFailedConnect(ClientConnectionFailedEvent event) {
    connectionState.set(ConnectionState.DISCONNECTED);
    client.shutdown();
    event.getCause().ifPresent(throwable -> log.error("Chat disconnected with cause", throwable));
  }

  private void onSocialMessage(SocialInfo socialMessage) {
    autoChannels.clear();
    autoChannels.addAll(socialMessage.getChannels());
    autoChannels.remove(defaultChannelName);
    autoChannels.addFirst(defaultChannelName);
    joinAutoChannels();
  }

  private void populateColor(ChatChannelUser chatChannelUser) {
    String lowercaseUsername = chatChannelUser.getUsername().toLowerCase(US);

    ObservableMap<ChatUserCategory, Color> groupToColor = chatPrefs.getGroupToColor();
    ObservableMap<String, Color> userToColor = chatPrefs.getUserToColor();

    Color color;
    if (chatPrefs.getChatColorMode() == RANDOM) {
      color = ColorGeneratorUtil.generateRandomColor(lowercaseUsername.hashCode());
    } else if (userToColor.containsKey(lowercaseUsername)) {
      color = userToColor.get(lowercaseUsername);
    } else {
      color = groupToColor.get(chatChannelUser.getCategory());
    }
    chatChannelUser.setColor(color);
  }

  @Override
  public void connect() {
    autoReconnect = true;
    if (connectionState.get() != ConnectionState.DISCONNECTED) {
      return;
    }

    connectionState.set(ConnectionState.CONNECTING);
    log.info("Connecting to IRC");
    Irc irc = clientProperties.getIrc();
    this.defaultChannelName = irc.getDefaultChannel();

    String username = loginService.getUsername();

    List<EventListenerSupplier> eventListenerSuppliers = new ArrayList<>();
    Arrays.stream(DefaultListeners.values())
          .filter(listener -> listener != DefaultListeners.WHO)
          .forEach(eventListenerSuppliers::add);
    eventListenerSuppliers.add(() -> WhoAwayListener::new);
    eventListenerSuppliers.add(() -> DefaultTagmsgListener::new);
    eventListenerSuppliers.add(() -> RedactListener::new);

    client = (WithManagement) Client.builder()
                                    .realName(username)
                                    .nick(username)
                                    .server()
                                    .host(irc.getHost())
                                    .port(irc.getPort(), SecurityType.SECURE)
                                    .then()
                                    .listeners()
                                    .input(this::onMessage)
                                    .output(this::onMessage)
                                    .exception(this::onException)
                                    .then()
                                    .management()
                                    .eventListeners(eventListenerSuppliers)
                                    .networkHandler(WebsocketNetworkHandler.getInstance())
                                    .then()
                                    .build();

    client.getMessageTagManager().registerTagCreator(MESSAGE_TAGS, "+typing", DefaultMessageTagTyping.FUNCTION);
    client.getActorTracker().setQueryChannelInformation(false);
    client.getEventManager().registerEventListener(this);

    userWebClientFactory.getObject()
                        .get()
                        .uri("irc/ergochat/token")
                        .retrieve()
                        .bodyToMono(IrcChatToken.class)
                        .map(IrcChatToken::value)
                        .subscribe(token -> {
                          client.getAuthManager()
                                .addProtocol(
                                    new SaslPlain(client, "%s@FAF".formatted(username), "token:%s".formatted(token)));
                          client.connect();
                        });
  }

  private boolean isStale(ServerMessageEvent event) {
    return event.getTag("time", Time.class)
                .map(Time::getTime)
                .map(time -> time.isBefore(Instant.now().minusSeconds(60)))
                .orElse(true);
  }

  @Override
  public void disconnect() {
    autoReconnect = false;
    if (client != null) {
      log.info("Disconnecting from IRC");
      client.shutdown("Goodbye");
    }
  }

  @Override
  public CompletableFuture<Void> redactMessageInBackground(ChatChannel channel, String messageId) {
    return CompletableFuture.runAsync(() -> client.sendRawLine("REDACT " + channel.getName() + " " + messageId));
  }

  @Override
  public CompletableFuture<Void> reactToMessageInBackground(ChatMessage targetMessage, Emoticon reaction) {
    return CompletableFuture.runAsync(
        () -> new TagMessageCommand(client).target(targetMessage.getSender().getChannel().getName())
                                           .tags()
                                           .add("+draft/reply", targetMessage.getId())
                                           .add("+draft/react", reaction.shortcodes().getFirst())
                                           .then()
                                           .execute());
  }

  @Override
  public CompletableFuture<Void> sendReplyInBackground(ChatMessage targetMessage, String message) {
    ChatChannel chatChannel = targetMessage.getSender().getChannel();
    String channelName = chatChannel.getName();
    ChatChannelUser sender = getOrCreateChatUser(getCurrentUsername(), channelName);
    String id = String.valueOf(new Random().nextInt());
    return CompletableFuture.runAsync(() -> {
      new MessageCommand(client).target(channelName)
                                .message(message)
                                .tags()
                                .add("label", id)
                                .add("+draft/reply", targetMessage.getId())
                                .then()
                                .execute();
      chatChannel.addMessage(new ChatMessage(id, Instant.now(), sender, message, Type.PENDING, targetMessage));
    });
  }

  @Override
  public CompletableFuture<Void> sendMessageInBackground(ChatChannel chatChannel, String message) {
    ChatChannelUser sender = getOrCreateChatUser(getCurrentUsername(), chatChannel.getName());
    String id = String.valueOf(new Random().nextInt());
    return CompletableFuture.runAsync(() -> {
      new MessageCommand(client).target(chatChannel.getName())
                                .message(message)
                                .tags()
                                .add("label", id)
                                .then()
                                .execute();
      chatChannel.addMessage(new ChatMessage(id, Instant.now(), sender, message, Type.PENDING, null));
    });
  }

  @Override
  public ChatChannel getOrCreateChannel(String channelName) {
    return channels.computeIfAbsent(channelName, name -> {
      ChatChannel chatChannel = new ChatChannel(name);
      chatChannel.maxNumMessagesProperty().bind(chatPrefs.maxMessagesProperty());
      Subscription unreadMessagesSubscription = chatChannel.numUnreadMessagesProperty()
                                                           .subscribe(this::incrementUnreadMessagesCount);
      channelSubscriptions.computeIfAbsent(chatChannel, ignored -> ConcurrentHashMap.newKeySet())
                          .add(unreadMessagesSubscription);
      return chatChannel;
    });
  }

  @Override
  public void addChannelsListener(MapChangeListener<String, ChatChannel> listener) {
    channels.addListener(listener);
  }

  @Override
  public void removeChannelsListener(MapChangeListener<String, ChatChannel> listener) {
    channels.removeListener(listener);
  }

  @Override
  public void leaveChannel(ChatChannel channel) {
    if (!channel.isPrivateChannel()) {
      client.removeChannel(channel.getName());
    }

    removeChannel(channel.getName());
  }

  private void removeChannel(String channelName) {
    ChatChannel removedChannel = channels.remove(channelName);
    lastSentActiveMap.remove(removedChannel);
    Set<Subscription> subscriptions = channelSubscriptions.remove(removedChannel);
    if (subscriptions != null) {
      subscriptions.forEach(Subscription::unsubscribe);
    }
  }

  @Override
  public void joinChannel(String channelName) {
    log.debug("Joining channel: {}", channelName);
    if (client == null) {
      bufferedChannels.add(channelName);
    } else {
      client.addChannel(channelName);
      client.sendRawLine("CHATHISTORY LATEST " + channelName + " * " + (chatPrefs.getMaxMessages() * 2));
      client.sendRawLine("WHO " + channelName);
    }
  }

  @Override
  public void setChannelTopic(ChatChannel chatChannel, String text) {
    client.getChannel(chatChannel.getName())
          .orElseThrow(
              () -> new IllegalArgumentException(String.format("No channel with `%s` name", chatChannel.getName())))
          .setTopic(text);
  }

  private void setTypingState(ChatChannel chatChannel, TypingState state) {
    new TagMessageCommand(client).target(chatChannel.getName())
                                 .tags()
                                 .add("+typing", state.getValue())
                                 .then()
                                 .execute();
  }

  @Override
  public boolean isDefaultChannel(ChatChannel chatChannel) {
    return defaultChannelName.equals(chatChannel.getName());
  }

  @Override
  public void destroy() {
    close();
  }

  @Override
  public void close() {
    if (client != null) {
      client.shutdown();
    }
  }

  @Override
  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return connectionState;
  }

  @Override
  public ConnectionState getConnectionState() {
    return connectionState.get();
  }

  @Override
  public void reconnect() {
    disconnect();
    connect();
  }

  private void incrementUnreadMessagesCount(Number oldValue, Number newValue) {
    trayIconManager.onSetApplicationBadgeEvent(
        UpdateApplicationBadgeEvent.ofDelta(newValue.intValue() - oldValue.intValue()));
  }

  @Override
  public void joinPrivateChat(String username) {
    getOrCreateChannel(username);
    client.sendRawLine("CHATHISTORY LATEST " + username + " * " + (chatPrefs.getMaxMessages() * 2));
  }

  @Override
  public Set<ChatChannel> getChannels() {
    return Set.copyOf(channels.values());
  }

  @Override
  public String getCurrentUsername() {
    return loginService.getUsername();
  }

  @Override
  public Pattern getMentionPattern() {
    return Pattern.compile("(^|[^A-Za-z0-9-])" + Pattern.quote(loginService.getUsername()) + "([^A-Za-z0-9-]|$)",
                           CASE_INSENSITIVE);
  }

  @VisibleForTesting
  boolean hasMention(String text) {
    if (!getMentionPattern().matcher(text).find()) {
      return false;
    }

    return !notificationPrefs.isNotifyOnAtMentionOnlyEnabled() || text.contains("@" + loginService.getUsername());
  }
}
