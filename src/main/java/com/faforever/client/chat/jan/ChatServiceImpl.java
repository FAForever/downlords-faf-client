package com.faforever.client.chat.jan;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.faforever.client.FafClientApplication;
import com.faforever.client.chat.Channel;
import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ChatMessage;
import com.faforever.client.chat.ChatService;
import com.faforever.client.chat.ChatUser;
import com.faforever.client.chat.ChatUserCreatedEvent;
import com.faforever.client.chat.ColorGeneratorUtil;
import com.faforever.client.chat.PircBotXChatService;
import com.faforever.client.chat.event.ChatMessageEvent;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerOnlineEvent;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.player.UserOfflineEvent;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.SocialMessage;
import com.faforever.client.ui.tray.event.UpdateApplicationBadgeEvent;
import com.faforever.client.user.UserService;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;

import static com.faforever.client.chat.ChatColorMode.CUSTOM;
import static com.faforever.client.chat.ChatColorMode.RANDOM;
import static java.util.Locale.US;
import static javafx.collections.FXCollections.observableHashMap;
import static javafx.collections.FXCollections.observableMap;

@Lazy
@Service
@Slf4j
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
public class ChatServiceImpl implements ChatService, InitializingBean, DisposableBean {

  private final EventBus eventBus;
  private final UserService userService;
  private final FafService fafService;
  private final PreferencesService preferencesService;
  private final PlayerService playerService;

  private final PircBotXChatService irc;
  private final ChannelTargetStateService channelTargetState;

  private ObservableMap<String, ChatChannelUser> chatChannelUsersByChannelAndName;
  private ObservableMap<String, Channel> channels;
  private SimpleIntegerProperty unreadMessagesCount;

  private String mapKey(String username, String channelName) {
    return username + channelName;
  }

  public ChatServiceImpl(EventBus eventBus, UserService userService, FafService fafService, PreferencesService preferencesService,
        PircBotXChatService irc, ChannelTargetStateService channelTargetState, PlayerService playerService) {

    this.eventBus = eventBus;
    this.userService = userService;
    this.fafService = fafService;
    this.preferencesService = preferencesService;
    this.irc = irc;
    this.channelTargetState = channelTargetState;
    this.playerService = playerService;

    channels = observableHashMap();
    chatChannelUsersByChannelAndName = observableMap(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
    unreadMessagesCount = new SimpleIntegerProperty();
  }

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
    irc.connectionStateProperty().addListener((observable, oldValue, newValue) -> {
      switch (newValue) {
        case DISCONNECTED:
        case CONNECTING:
          synchronized (channels) {
            channels.values().forEach(Channel::clearUsers);
          }
          break;
      }
    });

    irc.registerEventHandlers(
        this::onChatLoginSuccess,
        this::onChannelUsersReceived,
        this::onChannelTopicChanged,
        this::onUserJoinedChannel,
        this::onUserRoleChanged,
        this::onUserLeftChannel,
        this::onUserQuitChat,
        this::onChatMessageReceived,
        this::onPrivateChatMessageReceived);

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    JavaFxUtil.addListener(chatPrefs.userToColorProperty(),
        (MapChangeListener<? super String, ? super Color>) change -> preferencesService.store()
    );
    JavaFxUtil.addListener(chatPrefs.chatColorModeProperty(), (observable, oldValue, newValue) -> {
      synchronized (chatChannelUsersByChannelAndName) {
        switch (newValue) {
          case CUSTOM:
            chatChannelUsersByChannelAndName.values().stream()
                .filter(chatUser -> chatPrefs.getUserToColor().containsKey(userToColorKey(chatUser.getUsername())))
                .forEach(chatUser -> chatUser.setColor(chatPrefs.getUserToColor().get(userToColorKey(chatUser.getUsername()))));
            break;

          case RANDOM:
            for (ChatChannelUser chatUser : chatChannelUsersByChannelAndName.values()) {
              chatUser.setColor(ColorGeneratorUtil.generateRandomColor(chatUser.getUsername().hashCode()));
            }
            break;

          default:
            for (ChatChannelUser chatUser : chatChannelUsersByChannelAndName.values()) {
              chatUser.setColor(null);
            }
        }
      }
    });

    fafService.addOnMessageListener(SocialMessage.class, socialMessage -> {
      if (socialMessage.getChannels() != null) {
        for (String channel : socialMessage.getChannels()) {
          channelTargetState.addTemporaryChannel(channel);
        }
      }
    });
    ObservableList<String> savedAutoChannels = preferencesService.getPreferences().getChat().getAutoJoinChannels();
    if (savedAutoChannels == null) {
      return;
    }
    savedAutoChannels.forEach(this::joinChannel);
    for (String channel : savedAutoChannels) {
      channelTargetState.addTemporaryChannel(channel);
    }
//    if (playerService.getCurrentPlayer().get().getNumberOfGames() < 50) {
//      channelTargetState.addPermanentChannel("#newbie");
//    }
  }

  @Override
  public void destroy() {
    this.close();
  }

  private void onChatLoginSuccess() {
    // TODO: join channels based on channelTargetState and add listener to listen for future changes
    
    log.debug("Joining all channels: {}", channelTargetState);
    
    for (String channelName : channelTargetState.getAllChannels()) {
      joinChannel(channelName);
    }
  }

  private void onChannelTopicChanged(String channelName, String topic) {
    getOrCreateChannel(channelName).setTopic(topic);
  }

  private void onChannelUsersReceived(String channelName, Set<ChatUser> users) {
    Channel channel = getOrCreateChannel(channelName);
    users.forEach(user -> channel.addUser(getOrCreateChatUser(user.getUsername(), channelName, user.isModerator())));
  }

  private void onUserJoinedChannel(String channelName, ChatUser user) {
    getOrCreateChannel(channelName).addUser(getOrCreateChatUser(user.getUsername(), channelName, user.isModerator()));
  }

  private void onUserLeftChannel(String channelName, String username) {
    if (getOrCreateChannel(channelName).removeUser(username) == null) {
      return;
    }
    log.debug("User '{}' left channel: {}", username, channelName);
    if (userService.getUsername().equalsIgnoreCase(username)) {
      synchronized (channels) {
        channels.remove(channelName);
      }
    }
    synchronized (chatChannelUsersByChannelAndName) {
      chatChannelUsersByChannelAndName.remove(mapKey(username, channelName));
    }
    // The server doesn't yet tell us when a user goes offline, so we have to rely on the user leaving IRC.
    if (irc.getDefaultChannelName().equals(channelName)) {
      eventBus.post(new UserOfflineEvent(username));
    }
  }

  private void onUserQuitChat(String username) {
    synchronized (channels) {
      channels.values().forEach(channel -> onUserLeftChannel(channel.getName(), username));
    }
  }

  private void onUserRoleChanged(String channelName, ChatUser user) {
    getOrCreateChannel(channelName).setModerator(user.getUsername(), user.isModerator());
  }

  private void onChatMessageReceived(ChatMessage message) {
    eventBus.post(new ChatMessageEvent(message));
  }

  private void onPrivateChatMessageReceived(ChatMessage message) {
    String username = message.getUsername();
    ChatChannelUser sender = getOrCreateChatUser(username,username, false);
    if (sender != null
        && sender.getPlayer().isPresent()
        && sender.getPlayer().get().getSocialStatus() == SocialStatus.FOE
        && preferencesService.getPreferences().getChat().getHideFoeMessages()) {
      log.debug("Suppressing chat message from foe '{}'", username);
      return;
    }
    eventBus.post(new ChatMessageEvent(message));
  }

  @NotNull
  private String userToColorKey(String username) {
    return username.toLowerCase(US);
  }

  @Subscribe
  public void onLoginSuccessEvent(LoginSuccessEvent event) {
    connect();
  }

  @Subscribe
  public void onLoggedOutEvent(LoggedOutEvent event) {
    disconnect();
    eventBus.post(UpdateApplicationBadgeEvent.ofNewValue(0));
  }

  @Subscribe
  public void onPlayerOnline(PlayerOnlineEvent event) {
    Player player = event.getPlayer();

    synchronized (channels) {
      List<ChatChannelUser> channelUsers = channels.values().stream()
          .map(channel -> chatChannelUsersByChannelAndName.get(mapKey(player.getUsername(), channel.getName())))
          .filter(Objects::nonNull)
          .peek(chatChannelUser -> chatChannelUser.setPlayer(player))
          .collect(Collectors.toList());

      player.getChatChannelUsers().addAll(channelUsers);
    }
  }

  @Override
  public void connect() {
    irc.connect();
    
    // TODO if the IRC service has channelTargetState injected, it can do this itself.
    // Its weird though that the default channel comes from IRC but the saved channels come from
    // python server. Maybe  both should come from the same server, probably python server.
    channelTargetState.addPermanentChannel(irc.getDefaultChannelName());
  }

  @Override
  public void disconnect() {
    irc.disconnect();
    synchronized (channels) {
      channels.clear();
    }
  }

  @Override
  public void reconnect() {
    disconnect();
    connect();
  }

  @Override
  public Channel getOrCreateChannel(String channelName) {
    synchronized (channels) {
      if (!channels.containsKey(channelName)) {
        channels.put(channelName, new Channel(channelName));
      }
      return channels.get(channelName);
    }
  }

  @Override
  public ChatChannelUser getOrCreateChatUser(String username, String channel, boolean isModerator) {
    synchronized (chatChannelUsersByChannelAndName) {
      String key = mapKey(username, channel);
      if (!chatChannelUsersByChannelAndName.containsKey(key)) {
        ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
        Color color = null;

        if (chatPrefs.getChatColorMode() == CUSTOM && chatPrefs.getUserToColor().containsKey(userToColorKey(username))) {
          color = chatPrefs.getUserToColor().get(userToColorKey(username));
        } else if (chatPrefs.getChatColorMode() == RANDOM) {
          color = ColorGeneratorUtil.generateRandomColor(userToColorKey(username).hashCode());
        }

        ChatChannelUser chatChannelUser = new ChatChannelUser(username, color, isModerator);
        eventBus.post(new ChatUserCreatedEvent(chatChannelUser));
        chatChannelUsersByChannelAndName.put(key, chatChannelUser);
      }
      return chatChannelUsersByChannelAndName.get(key);
    }
  }

  @Override
  public void addUsersListener(String channelName, MapChangeListener<String, ChatChannelUser> listener) {
    getOrCreateChannel(channelName).addUsersListeners(listener);
  }

  @Override
  public void addChatUsersByNameListener(MapChangeListener<String, ChatChannelUser> listener) {
    synchronized (chatChannelUsersByChannelAndName) {
      JavaFxUtil.addListener(chatChannelUsersByChannelAndName, listener);
    }
  }

  @Override
  public void addChannelsListener(MapChangeListener<String, Channel> listener) {
    JavaFxUtil.addListener(channels, listener);
  }

  @Override
  public void removeUsersListener(String channelName, MapChangeListener<String, ChatChannelUser> listener) {
    getOrCreateChannel(channelName).removeUserListener(listener);
  }

  @Override
  public void joinChannel(String channelName) {
    channelTargetState.addTemporaryChannel(channelName);
    irc.joinChannel(channelName);
  }

  @Override
  public void leaveChannel(String channelName) {
    irc.leaveChannel(channelName);
  }

  @Override
  public CompletableFuture<String> sendMessageInBackground(String target, String message) {
    eventBus.post(new ChatMessageEvent(new ChatMessage(target, Instant.now(), userService.getUsername(), message)));
    return irc.sendMessageInBackground(target, message);
  }

  @Override
  public CompletableFuture<String> sendActionInBackground(String target, String action) {
    return irc.sendActionInBackground(target, action);
  }

  @Override
  public boolean isDefaultChannel(String channelName) {
    return irc.isDefaultChannel(channelName);
  }

  @Override
  public void close() {
    irc.close();
  }

  @Override
  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return irc.connectionStateProperty();
  }

  @Override
  public void whois(String username) {
    irc.whois(username);
  }

  @Override
  public void incrementUnreadMessagesCount(int delta) {
    eventBus.post(UpdateApplicationBadgeEvent.ofDelta(delta));
  }

  @Override
  public ReadOnlyIntegerProperty unreadMessagesCount() {
    return unreadMessagesCount;
  }

  @Override
  public ChatChannelUser getChatUser(String username, String channelName) {
    return Optional.ofNullable(chatChannelUsersByChannelAndName.get(mapKey(username, channelName)))
        .orElseThrow(() -> new IllegalArgumentException("Chat user '" + username + "' is unknown for channel '" + channelName + "'"));
  }

  @Override
  public String getDefaultChannelName() {
    return irc.getDefaultChannelName();
  }
}
