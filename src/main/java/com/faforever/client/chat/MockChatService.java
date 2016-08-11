package com.faforever.client.chat;

import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.ConcurrentUtil;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.concurrent.Task;
import javafx.scene.paint.Color;
import org.pircbotx.User;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import static com.faforever.client.task.CompletableTask.Priority.HIGH;

// NOSONAR
public class MockChatService implements ChatService {

  private static final int CHAT_MESSAGE_INTERVAL = 5000;
  private static final long CONNECTION_DELAY = 1000;
  private final Timer timer;
  private final Collection<Consumer<ChatMessage>> onChatMessageListeners;
  private final Map<String, Channel> channelUserListListeners;
  private final ObjectProperty<ConnectionState> connectionState;
  private final IntegerProperty unreadMessagesCount;

  @Resource
  UserService userService;
  @Resource
  TaskService taskService;
  @Resource
  I18n i18n;

  public MockChatService() {
    connectionState = new SimpleObjectProperty<>();
    unreadMessagesCount = new SimpleIntegerProperty();

    onChatMessageListeners = new ArrayList<>();
    channelUserListListeners = new HashMap<>();

    timer = new Timer(true);
  }

  @PostConstruct
  void postConstruct() {
    userService.loggedInProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        connect();
      }
    });
  }

  private void simulateConnectionEstablished() {
    connectionState.set(ConnectionState.CONNECTED);
    joinChannel("#mockChannel");
  }

  @Override
  public void addOnMessageListener(Consumer<ChatMessage> listener) {
    onChatMessageListeners.add(listener);
  }

  @Override
  public void addOnPrivateChatMessageListener(Consumer<ChatMessage> listener) {

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
  public CompletionStage<String> sendMessageInBackground(String target, String message) {
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
  public CompletionStage<String> sendActionInBackground(String target, String action) {
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
  public ChatUser createOrGetChatUser(User user) {
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
