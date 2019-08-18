package com.faforever.client.chat.jan;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.faforever.client.FafClientApplication;
import com.faforever.client.chat.Channel;
import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ChatMessage;
import com.faforever.client.chat.ChatUserCreatedEvent;
import com.faforever.client.chat.ColorGeneratorUtil;
import com.faforever.client.chat.PircBotXChatService;
import com.faforever.client.chat.event.ChatMessageEvent;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerOnlineEvent;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.ui.tray.event.UpdateApplicationBadgeEvent;
import com.faforever.client.user.UserService;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import javafx.collections.MapChangeListener;
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
public class ChatServiceImpl implements InitializingBean {

  private final EventBus eventBus;
  private final UserService userService;
  private final FafService fafService;
  private final PlayerService playerService;
  private final PreferencesService preferencesService;

  private final PircBotXChatService irc;
  // TODO remove
  private final ChannelTargetStateService channelTargetState;

  // TODO remove channels, directly map usernames to ChatChannelUsers for pms
  private ObservableMap<String, ChatChannelUser> chatChannelUsersByChannelAndName;
  // TODO remove
  private ObservableMap<String, Channel> channels;

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
  }

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);

    irc.registerPrivateMessageEventHandler(this::onPrivateChatMessageReceived);

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
  }

  private void onPrivateChatMessageReceived(ChatMessage message) {
    log.info("Received pm message: {}", message.getMessage());
    JavaFxUtil.assertBackgroundThread();
    String username = message.getUsername();
    // TODO This method just wants to get its grabby hands on the player object.
    // In the previous design, the chatChannelUsersByChannelAndName variable contained
    // both chatroom users as well as private channel users. This needs to be split
    // so we can still access ChatChannelUser for private chats without having to
    // deal with ChatRoomService.
    
    //ChatChannelUser sender = getOrCreateChatUser(username,username, false);
    ChatChannelUser sender = createChatChannelUser(username, false);// TODO
    
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
    irc.connect();
  }

  @Subscribe
  public void onLoggedOutEvent(LoggedOutEvent event) {
    irc.disconnect();
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
  
  public ChatChannelUser createChatChannelUser(String username, boolean isModerator) {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    Color color = null;
    if (chatPrefs.getChatColorMode() == CUSTOM && chatPrefs.getUserToColor().containsKey(userToColorKey(username))) {
      color = chatPrefs.getUserToColor().get(userToColorKey(username));
    } else if (chatPrefs.getChatColorMode() == RANDOM) {
      color = ColorGeneratorUtil.generateRandomColor(userToColorKey(username).hashCode());
    }

    ChatChannelUser chatChannelUser = new ChatChannelUser(username, color, isModerator);
    // TODO the event will cause the ChatChannelUser object to be injected with the actual Player object
    // at SOME time and that will hapen in the application thread, so how can it happen in time for calles to use it?
    eventBus.post(new ChatUserCreatedEvent(chatChannelUser));
    return chatChannelUser;
  }

  public CompletableFuture<String> sendMessageInBackground(String target, String message) {
    eventBus.post(new ChatMessageEvent(new ChatMessage(target, Instant.now(), userService.getUsername(), message)));
    return irc.sendMessageInBackground(target, message);
  }

  public CompletableFuture<String> sendActionInBackground(String target, String action) {
    return irc.sendActionInBackground(target, action);
  }

  public void incrementUnreadMessagesCount(int delta) {
    eventBus.post(UpdateApplicationBadgeEvent.ofDelta(delta));
  }
}
