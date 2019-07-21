package com.faforever.client.chat.jan;

import static java.util.Locale.US;
import static javafx.collections.FXCollections.observableHashMap;
import static javafx.collections.FXCollections.observableMap;

import java.util.List;
import java.util.Map.Entry;
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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import javafx.beans.property.MapProperty;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;

/**
 * Deals with all the chat room state. Provides methods to change chat room state and listen for changes.
 */
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
  }
  
  private List<ChatChannelUser> getAllUserInstances(String username) {
    return channels.values().stream()
        .map(channel -> channel.getUser(username))
        .filter(user -> user != null)
        .collect(Collectors.toList());
  }
  
  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
    irc.connectionStateProperty().addListener((observable, oldValue, newValue) -> {
      switch (newValue) {
        case DISCONNECTED:
          synchronized (channels) {
            channels.clear();
          }
        case CONNECTING:
          synchronized (channels) {
            channels.values().forEach(Channel::clearUsers);
          }
          break;
        case CONNECTED:
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
    JavaFxUtil.addListener(chatPrefs.chatColorModeProperty(), (observable, oldValue, newValue) -> {
      switch (newValue) {
        case CUSTOM:
          chatPrefs.getUserToColor().entrySet().forEach(entry -> {
            for (ChatChannelUser user : getAllUserInstances(entry.getKey())) {
              user.setColor(entry.getValue());
            }
          });
  
        case RANDOM:
          channels.values().forEach(channel -> channel.forAllUsers(user -> {
            user.setColor(ColorGeneratorUtil.generateRandomColor(user.getUsername().hashCode()));
          }));
          break;
  
        case DEFAULT:
          channels.values().forEach(channel -> channel.forAllUsers(user -> {
            user.setColor(null);
          }));
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
        channelTargetState.addTemporaryChannel(channel);
      }
    }
  }

  private void onChatLoginSuccess() {
    // TODO: join channels based on channelTargetState and add listener to listen for future changes
    
    channelTargetState.addPermanentChannel(irc.getDefaultChannelName());
    
    log.debug("Joining all room channels: {}", channelTargetState);
    
    for (String channelName : channelTargetState.getAllChannels()) {
      irc.joinChannel(channelName);
    }
  }
  
  public void joinChannel(String channelName) {
    channelTargetState.addTemporaryChannel(channelName);
    irc.joinChannel(channelName);
  }

  public void leaveChannel(String channelName) {
    channelTargetState.removeTemporaryChannel(channelName);
    irc.leaveChannel(channelName);
  }

  private void onChannelTopicChanged(String channelName, String topic) {
    getOrCreateChannel(channelName).setTopic(topic);
  }

  private void onChannelUsersReceived(String channelName, Set<ChatUser> users) {
    Channel channel = getOrCreateChannel(channelName);
    users.forEach(user -> channel.addUser(getOrCreateChatRoomUser(user.getUsername(), channelName, user.isModerator())));
  }

  private void onUserJoinedChannel(String channelName, ChatUser user) {
    getOrCreateChannel(channelName).addUser(getOrCreateChatRoomUser(user.getUsername(), channelName, user.isModerator()));
  }

  private void onUserLeftChannel(String channelName, String username) {
    if (getOrCreateChannel(channelName).removeUser(username) == null) {
      return;
    }
    log.debug("User '{}' left channel: {}", username, channelName);
    if (userService.getUsername().equalsIgnoreCase(username)) {
      channels.remove(channelName);
    }
    // The server doesn't yet tell us when a user goes offline, so we have to rely on the user leaving IRC.
    if (irc.getDefaultChannelName().equals(channelName)) {
      eventBus.post(new UserOfflineEvent(username));
    }
  }

  private void onUserQuitChat(String username) {
    channels.values().forEach(channel -> onUserLeftChannel(channel.getName(), username));
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
    player.getChatChannelUsers().addAll(getAllUserInstances(player.getUsername()));
  }

  public Channel getOrCreateChannel(String channelName) {
    if (!channels.containsKey(channelName)) {
      synchronized (channels) {
        channels.put(channelName, new Channel(channelName));
      }
    }
    return channels.get(channelName);
  }

  @VisibleForTesting
  public ChatChannelUser getOrCreateChatRoomUser(String username, String channel, boolean isModerator) {
    Channel chatChannel = channels.get(channel);
    if (chatChannel != null) {
      ChatChannelUser chatChannelUser = channels.get(channel).getUser(username);
      if (chatChannelUser != null) {
        return chatChannelUser;
      }
    }
    return chatService.createChatChannelUser(username, isModerator);
  }

  public void addUsersListener(String channelName, MapChangeListener<String, ChatChannelUser> listener) {
    getOrCreateChannel(channelName).addUsersListeners(listener);
  }

  public void addChannelsListener(MapChangeListener<String, Channel> listener) {
    JavaFxUtil.addListener(channels, listener);
  }

  public void removeUsersListener(String channelName, MapChangeListener<String, ChatChannelUser> listener) {
    getOrCreateChannel(channelName).removeUserListener(listener);
  }

  public ChatChannelUser getChatUser(String username, String channelName) {
    return Optional.ofNullable(channels.get(channelName).getUser(username))
        .orElseThrow(() -> new IllegalArgumentException("Chat user '" + username + "' is unknown for channel '" + channelName + "'"));
  }
}
