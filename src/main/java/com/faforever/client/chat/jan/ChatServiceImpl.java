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

/**
 * Has currently two roles:
 * 1. Deal with all private message channel related stuff (could be moved to separate class).
 * 2. Provide a place to put general chat functionality that neither belongs to chat rooms nor private messages specifically.
 */
@Lazy
@Service
@Slf4j
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
public class ChatServiceImpl implements InitializingBean {

  private final EventBus eventBus;
  private final UserService userService;
  private final PlayerService playerService;
  private final PreferencesService preferencesService;

  private final PircBotXChatService irc;

  private ObservableMap<String, ChatChannelUser> pmChannelUsersByLowerName;

  public ChatServiceImpl(EventBus eventBus, UserService userService, PreferencesService preferencesService,
        PircBotXChatService irc, PlayerService playerService) {

    this.eventBus = eventBus;
    this.userService = userService;
    this.preferencesService = preferencesService;
    this.irc = irc;
    this.playerService = playerService;
    
    pmChannelUsersByLowerName = observableHashMap();
  }

  private ChatChannelUser getUser(String username) {
    return pmChannelUsersByLowerName.get(username.toLowerCase());
  }
  
  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);

    irc.registerPrivateMessageEventHandler(this::onPrivateChatMessageReceived);

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    JavaFxUtil.addListener(chatPrefs.userToColorProperty(),
        (MapChangeListener<? super String, ? super Color>) change -> preferencesService.storeInBackground()
    );
    // TODO make sure this color stuff is even used in private messages
    // iof not remove this listener.
    JavaFxUtil.addListener(chatPrefs.chatColorModeProperty(), (observable, oldValue, newValue) -> {
      synchronized (pmChannelUsersByLowerName) {
        switch (newValue) {
          case CUSTOM:
            chatPrefs.getUserToColor().entrySet().forEach(entry -> {
                getUser(entry.getKey()).setColor(entry.getValue());
            });
            break;

          case RANDOM:
            for (ChatChannelUser chatUser : pmChannelUsersByLowerName.values()) {
              chatUser.setColor(ColorGeneratorUtil.generateRandomColor(chatUser.getUsername().hashCode()));
            }
            break;

          case DEFAULT:
            for (ChatChannelUser chatUser : pmChannelUsersByLowerName.values()) {
              chatUser.setColor(null);
            }
            break;
        }
      }
    });
  }

  private void onPrivateChatMessageReceived(ChatMessage message) {
    log.info("Received pm message: {}", message.getMessage());
    JavaFxUtil.assertBackgroundThread();
    String username = message.getUsername();

    ChatChannelUser sender = getOrCreateChatUser(username);

    if (sender != null
        && sender.getPlayer().isPresent()
        && sender.getPlayer().get().getSocialStatus() == SocialStatus.FOE
        && preferencesService.getPreferences().getChat().getHideFoeMessages()) {
      log.debug("Suppressing chat message from foe '{}'", username);
      return;
    }
    eventBus.post(new ChatMessageEvent(message));
  }

  public ChatChannelUser getOrCreateChatUser(String username) {
    ChatChannelUser user = getUser(username);
    if (user == null) {
      user = createChatChannelUser(username, false);
      synchronized (pmChannelUsersByLowerName) {
        pmChannelUsersByLowerName.put(username.toLowerCase(), user);
      }
    }
    return user;
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

  /**
   * this can be done in AbstractChatTabController, no need for a chatService method
   */
  @Deprecated
  public void incrementUnreadMessagesCount(int delta) {
    eventBus.post(UpdateApplicationBadgeEvent.ofDelta(delta));
  }
  
  public void addPmChatUsersByNameListener(MapChangeListener<String, ChatChannelUser> listener) {
    synchronized (pmChannelUsersByLowerName) {
      JavaFxUtil.addListener(pmChannelUsersByLowerName, listener);
    }
  }
}
