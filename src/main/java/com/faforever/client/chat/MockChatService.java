package com.faforever.client.chat;

import com.faforever.client.FafClientApplication;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.UserService;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.faforever.client.util.ConcurrentUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.concurrent.Task;
import javafx.scene.paint.Color;
import org.pircbotx.User;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.faforever.client.task.CompletableTask.Priority.HIGH;

@Lazy
@Service
@Profile(FafClientApplication.POFILE_OFFLINE)
// NOSONAR
public class MockChatService implements ChatService {

  private static final int CHAT_MESSAGE_INTERVAL = 5000;
  private static final long CONNECTION_DELAY = 1000;
  private final Timer timer;
  private final Collection<Consumer<ChatMessage>> onChatMessageListeners;
  private final Map<String, Channel> channelUserListListeners;
  private final ObjectProperty<ConnectionState> connectionState;
  private final IntegerProperty unreadMessagesCount;

  private final UserService userService;
  private final TaskService taskService;
  private final I18n i18n;
  private final EventBus eventBus;

  @Inject
  public MockChatService(UserService userService, TaskService taskService, I18n i18n, EventBus eventBus) {
    connectionState = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);
    unreadMessagesCount = new SimpleIntegerProperty();

    onChatMessageListeners = new ArrayList<>();
    channelUserListListeners = new HashMap<>();

    timer = new Timer(true);
    this.userService = userService;
    this.taskService = taskService;
    this.i18n = i18n;
    this.eventBus = eventBus;
  }

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
  }

  @Subscribe
  public void onLoginSuccessEvent(LoginSuccessEvent event) {
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
  public ChatUser getOrCreateChatUser(String username) {
    return new ChatUser(username, Color.ALICEBLUE);
  }

  @Override
  public void addUsersListener(String channelName, MapChangeListener<String, ChatUser> listener) {
    getOrCreateChannel(channelName).addUsersListeners(listener);
  }

  @Override
  public void addChatUsersByNameListener(MapChangeListener<String, ChatUser> listener) {

  }

  @Override
  public void addChannelsListener(MapChangeListener<String, Channel> listener) {

  }

  @Override
  public void removeUsersListener(String channelName, MapChangeListener<String, ChatUser> listener) {

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
        ChatUser chatUser = new ChatUser(userService.getUsername(), null);
        ChatUser mockUser = new ChatUser("MockUser", null);
        ChatUser moderatorUser = new ChatUser("MockModerator", Collections.singleton(channelName), null);

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
  public ChatUser getOrCreateChatUser(User user) {
    return null;
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
}
