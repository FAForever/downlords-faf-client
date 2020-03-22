package com.faforever.client.chat;

import com.faforever.client.FafClientApplication;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.remote.domain.IrcPasswordServerMessage;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.ConcurrentUtil;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.concurrent.Task;
import javafx.scene.paint.Color;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.faforever.client.task.CompletableTask.Priority.HIGH;

@Lazy
@Service
@Profile(FafClientApplication.PROFILE_OFFLINE)
@RequiredArgsConstructor
// NOSONAR
public class MockChatService implements ChatService, InitializingBean {

  private static final int CHAT_MESSAGE_INTERVAL = 5000;
  private static final long CONNECTION_DELAY = 1000;

  private final UserService userService;
  private final TaskService taskService;
  private final I18n i18n;
  private final EventBus eventBus;

  private Timer timer = new Timer(true);
  private Collection<Consumer<ChatMessage>> onChatMessageListeners = new ArrayList<>();
  private Map<String, Channel> channelUserListListeners = new HashMap<>();
  private ObjectProperty<ConnectionState> connectionState = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);
  private IntegerProperty unreadMessagesCount = new SimpleIntegerProperty();
  private String password;

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  @EventListener
  public void onIrcPassword(IrcPasswordServerMessage event) {
    password = event.getPassword();
    connect();
  }

  private void simulateConnectionEstablished() {
    connectionState.set(ConnectionState.CONNECTED);
    joinChannel("#mockChannel");
  }

  @Override
  public void connect() {
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        simulateConnectionEstablished();
      }
    }, CONNECTION_DELAY);
  }

  @Override
  public void disconnect() {
    timer.cancel();
  }

  @Override
  public CompletableFuture<String> sendMessageInBackground(String target, String message) {
    return taskService.submitTask(new CompletableTask<String>(HIGH) {
      @Override
      protected String call() throws Exception {
        updateTitle(i18n.get("chat.sendMessageTask.title"));

        Thread.sleep(200);
        return message;
      }
    }).getFuture();
  }

  @Override
  public Channel getOrCreateChannel(String channelName) {
    channelUserListListeners.putIfAbsent(channelName, new Channel(channelName));
    return channelUserListListeners.get(channelName);
  }

  @Override
  public ChatChannelUser getOrCreateChatUser(String username, String channel, boolean isModerator) {
    return null;
  }

  @Override
  public void addUsersListener(String channelName, MapChangeListener<String, ChatChannelUser> listener) {
    getOrCreateChannel(channelName).addUsersListeners(listener);
  }

  @Override
  public void addChatUsersByNameListener(MapChangeListener<String, ChatChannelUser> listener) {

  }

  @Override
  public void addChannelsListener(MapChangeListener<String, Channel> listener) {

  }

  @Override
  public void removeUsersListener(String channelName, MapChangeListener<String, ChatChannelUser> listener) {

  }

  @Override
  public void leaveChannel(String channelName) {

  }

  @Override
  public CompletableFuture<String> sendActionInBackground(String target, String action) {
    return sendMessageInBackground(target, action);
  }

  @Override
  public void joinChannel(String channelName) {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        ChatChannelUser chatUser = new ChatChannelUser(userService.getUsername(), null, false);
        ChatChannelUser mockUser = new ChatChannelUser("MockUser", null, false);
        ChatChannelUser moderatorUser = new ChatChannelUser("MockModerator", null, true);

        Channel channel = getOrCreateChannel(channelName);
        channel.addUser(chatUser);
        channel.addUser(mockUser);
        channel.addUser(moderatorUser);
        channel.setTopic("le wild channel topic appears");

        return null;
      }
    });

    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        for (Consumer<ChatMessage> onChatMessageListener : onChatMessageListeners) {
          ChatMessage chatMessage = new ChatMessage(channelName, Instant.now(), "Mock User",
              String.format(
                  "%1$s Lorem ipsum dolor sit amet, consetetur %1$s sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam %1$s " +
                      "http://www.faforever.com/wp-content/uploads/2013/07/cropped-backForum41.jpg",
                  userService.getUsername()
              )
          );

          onChatMessageListener.accept(chatMessage);
        }
      }
    }, 0, CHAT_MESSAGE_INTERVAL);
  }

  @Override
  public boolean isDefaultChannel(String channelName) {
    return true;
  }

  @Override
  public void close() {

  }

  @Override
  public ObjectProperty<ConnectionState> connectionStateProperty() {
    return connectionState;
  }

  @Override
  public void reconnect() {

  }

  @Override
  public void whois(String username) {

  }

  @Override
  public void incrementUnreadMessagesCount(int delta) {
    synchronized (unreadMessagesCount) {
      unreadMessagesCount.set(unreadMessagesCount.get() + delta);
    }
  }

  @Override
  public ReadOnlyIntegerProperty unreadMessagesCount() {
    return unreadMessagesCount;
  }

  @Override
  public ChatChannelUser getChatUser(String username, String channelName) {
    return new ChatChannelUser(username, Color.ALICEBLUE, false);
  }

  @Override
  public String getDefaultChannelName() {
    return channelUserListListeners.keySet().iterator().next();
  }
}
