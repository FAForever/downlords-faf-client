package com.faforever.client.chat.jan;

import static java.util.Locale.US;
import static javafx.collections.FXCollections.observableHashMap;
import static javafx.collections.FXCollections.observableMap;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.faforever.client.chat.Channel;
import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ChatMessage;
import com.faforever.client.chat.ChatUser;
import com.faforever.client.chat.ColorGeneratorUtil;
import com.faforever.client.chat.PircBotXChatService;
import com.faforever.client.chat.event.ChatMessageEvent;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerOnlineEvent;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.UserOfflineEvent;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.SocialMessage;
import com.faforever.client.user.UserService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;

@Lazy
@Service
@Slf4j
public class ChatRoomService implements InitializingBean {

  private final EventBus eventBus;
  private final UserService userService;
  private final FafService fafService;
  private final PreferencesService preferencesService;
  
  private final PircBotXChatService irc;
  private final ChatServiceImpl chatService;
  private final ChannelTargetStateService channelTargetState;
  
  private ObservableMap<String, ChatChannelUser> chatChannelUsersByChannelAndName;
  private ObservableMap<String, Channel> channels;
  
  public ChatRoomService(EventBus eventBus, UserService userService, FafService fafService, PreferencesService preferencesService,
      PircBotXChatService irc, ChannelTargetStateService channelTargetState, PlayerService playerService, ChatServiceImpl chatService) {

    this.eventBus = eventBus;
    this.userService = userService;
    this.fafService = fafService;
    this.preferencesService = preferencesService;
    this.irc = irc;
    this.channelTargetState = channelTargetState;
    this.chatService = chatService;
  
    channels = observableHashMap();
    chatChannelUsersByChannelAndName = observableMap(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
  }
  
  private String mapKey(String username, String channelName) {
    return username + channelName;
  }
  
  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
    irc.connectionStateProperty().addListener((observable, oldValue, newValue) -> {
      switch (newValue) {
        case DISCONNECTED:
          synchronized (channels) {
            // TODO why is this next line even needed? PM channels don't get cleared either.
            channels.clear();
          }
        case CONNECTING:
          synchronized (channels) {
            channels.values().forEach(Channel::clearUsers);
          }
          break;
      }
    });

    irc.registerEventHandlers(
        this::onChatLoginSuccess);
    irc.registerChatRoomEventHandlers(
        this::onChannelUsersReceived,
        this::onChannelTopicChanged,
        this::onUserJoinedChannel,
        this::onUserRoleChanged,
        this::onUserLeftChannel,
        this::onUserQuitChat,
        this::onChatMessageReceived);

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
          log.info("Social message received!");
          channelTargetState.addTemporaryChannel(channel);
        }
      }
    });
    ObservableList<String> savedAutoChannels = preferencesService.getPreferences().getChat().getAutoJoinChannels();
    if (savedAutoChannels != null) {
      for (String channel : savedAutoChannels) {
        log.info(channel);
        joinChannel(channel);
      }
    }
  }

  private void onChatLoginSuccess() {
    // TODO: join channels based on channelTargetState and add listener to listen for future changes
    
    channelTargetState.addPermanentChannel(irc.getDefaultChannelName());
    
    log.debug("Joining all channels: {}", channelTargetState);
    
    for (String channelName : channelTargetState.getAllChannels()) {
      irc.joinChannel(channelName);
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
    log.info("Received room message: {}", message.getMessage());
    eventBus.post(new ChatMessageEvent(message));
  }

  @NotNull
  private String userToColorKey(String username) {
    return username.toLowerCase(US);
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

  public Channel getOrCreateChannel(String channelName) {
    synchronized (channels) {
      if (!channels.containsKey(channelName)) {
        channels.put(channelName, new Channel(channelName));
      }
      return channels.get(channelName);
    }
  }

  public ChatChannelUser getOrCreateChatUser(String username, String channel, boolean isModerator) {
    synchronized (chatChannelUsersByChannelAndName) {
      String key = mapKey(username, channel);
      if (!chatChannelUsersByChannelAndName.containsKey(key)) {
        ChatChannelUser chatChannelUser = chatService.createChatChannelUser(username, isModerator);
        chatChannelUsersByChannelAndName.put(key, chatChannelUser);
      }
      return chatChannelUsersByChannelAndName.get(key);
    }
  }

  public void addUsersListener(String channelName, MapChangeListener<String, ChatChannelUser> listener) {
    getOrCreateChannel(channelName).addUsersListeners(listener);
  }

  public void addChatUsersByNameListener(MapChangeListener<String, ChatChannelUser> listener) {
    synchronized (chatChannelUsersByChannelAndName) {
      JavaFxUtil.addListener(chatChannelUsersByChannelAndName, listener);
    }
  }

  public void addChannelsListener(MapChangeListener<String, Channel> listener) {
    JavaFxUtil.addListener(channels, listener);
  }

  public void removeUsersListener(String channelName, MapChangeListener<String, ChatChannelUser> listener) {
    getOrCreateChannel(channelName).removeUserListener(listener);
  }

  public void joinChannel(String channelName) {
    channelTargetState.addTemporaryChannel(channelName);
    irc.joinChannel(channelName);
  }

  public void leaveChannel(String channelName) {
    irc.leaveChannel(channelName);
  }

  public ChatChannelUser getChatUser(String username, String channelName) {
    return Optional.ofNullable(chatChannelUsersByChannelAndName.get(mapKey(username, channelName)))
        .orElseThrow(() -> new IllegalArgumentException("Chat user '" + username + "' is unknown for channel '" + channelName + "'"));
  }
}
