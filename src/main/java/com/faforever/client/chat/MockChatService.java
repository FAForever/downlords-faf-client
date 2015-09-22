package com.faforever.client.chat;

import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.OnJoinChannelsRequestListener;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.ConcurrentUtil;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.task.AbstractPrioritizedTask.Priority.HIGH;

// NOSONAR
public class MockChatService implements ChatService {

  private static final int CHAT_MESSAGE_INTERVAL = 5000;
  private static final long CONNECTION_DELAY = 1000;
  private final Timer timer;
  private final Collection<OnChatMessageListener> onChatMessageListeners;
  private final Collection<OnChatConnectedListener> onChatConnectedListeners;
  private final Collection<OnChatUserListListener> onUserListListeners;
  private final Collection<OnChatDisconnectedListener> onDisconnectedListeners;
  private final Collection<OnPrivateChatMessageListener> onPrivateChatMessageListeners;
  private final Collection<OnChatUserJoinedChannelListener> onChannelJoinedListeners;
  private final Collection<OnChatUserQuitListener> onChatUserQuitListeners;
  private final Map<String, ObservableMap<String, ChatUser>> channelUserListListeners;
  @Autowired
  UserService userService;
  @Autowired
  TaskService taskService;
  @Autowired
  I18n i18n;

  public MockChatService() {
    onChatMessageListeners = new ArrayList<>();
    onChatConnectedListeners = new ArrayList<>();
    onUserListListeners = new ArrayList<>();
    onDisconnectedListeners = new ArrayList<>();
    onPrivateChatMessageListeners = new ArrayList<>();
    onChannelJoinedListeners = new ArrayList<>();
    onChatUserQuitListeners = new ArrayList<>();
    channelUserListListeners = new HashMap<>();

    timer = new Timer(true);
  }

  @Override
  public void addOnMessageListener(OnChatMessageListener listener) {
    onChatMessageListeners.add(listener);
  }

  @Override
  public void addOnChatConnectedListener(OnChatConnectedListener listener) {
    onChatConnectedListeners.add(listener);
  }

  @Override
  public void addOnUserListListener(OnChatUserListListener listener) {
    onUserListListeners.add(listener);
  }

  @Override
  public void addOnChatDisconnectedListener(OnChatDisconnectedListener listener) {
    onDisconnectedListeners.add(listener);
  }

  @Override
  public void addOnPrivateChatMessageListener(OnPrivateChatMessageListener listener) {
    onPrivateChatMessageListeners.add(listener);
  }

  @Override
  public void addOnChatUserJoinedChannelListener(OnChatUserJoinedChannelListener listener) {
    onChannelJoinedListeners.add(listener);
  }

  @Override
  public void addOnChatUserLeftChannelListener(OnChatUserLeftChannelListener listener) {

  }

  @Override
  public void addOnModeratorSetListener(OnModeratorSetListener listener) {

  }

  @Override
  public void addOnChatUserQuitListener(OnChatUserQuitListener listener) {
    onChatUserQuitListeners.add(listener);
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

  private void simulateConnectionEstablished() {
    onChatConnectedListeners.forEach(OnChatConnectedListener::onConnected);
    joinChannel("#mockChannel");
  }

  @Override
  public CompletableFuture<String> sendMessageInBackground(String target, String message) {
    return taskService.submitTask(new AbstractPrioritizedTask<String>(HIGH) {
      @Override
      protected String call() throws Exception {
        updateTitle(i18n.get("chat.sendMessageTask.title"));

        Thread.sleep(200);
        return message;
      }
    });
  }

  @Override
  public ObservableMap<String, ChatUser> getChatUsersForChannel(String channelName) {
    channelUserListListeners.putIfAbsent(channelName, FXCollections.observableHashMap());
    return channelUserListListeners.get(channelName);
  }

  @Override
  public void addChannelUserListListener(String channelName, MapChangeListener<String, ChatUser> listener) {
    ObservableMap<String, ChatUser> chatUsersForChannel = getChatUsersForChannel(channelName);
    synchronized (chatUsersForChannel) {
      chatUsersForChannel.addListener(listener);
    }
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
        ChatUser chatUser = new ChatUser(userService.getUsername());
        ChatUser mockUser = new ChatUser("MockUser");
        ChatUser moderatorUser = new ChatUser("MockModerator", Collections.singleton(channelName));

        for (OnChatUserJoinedChannelListener onChannelJoinedListener : onChannelJoinedListeners) {
          onChannelJoinedListener.onUserJoinedChannel(channelName, chatUser);
          onChannelJoinedListener.onUserJoinedChannel(channelName, mockUser);
          onChannelJoinedListener.onUserJoinedChannel(channelName, moderatorUser);
        }

        ObservableMap<String, ChatUser> chatUsersForChannel = getChatUsersForChannel(channelName);

        synchronized (chatUsersForChannel) {
          chatUsersForChannel.put(chatUser.getUsername(), chatUser);
          chatUsersForChannel.put(mockUser.getUsername(), mockUser);
          chatUsersForChannel.put(moderatorUser.getUsername(), moderatorUser);
        }

        return null;
      }
    });

    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        for (OnChatMessageListener onChatMessageListener : onChatMessageListeners) {
          ChatMessage chatMessage = new ChatMessage(Instant.now(), "Mock User",
              String.format(
                  "%1$s Lorem ipsum dolor sit amet, consetetur %1$s sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam %1$s " +
                      "http://www.faforever.com/wp-content/uploads/2013/07/cropped-backForum41.jpg",
                  userService.getUsername()
              )
          );

          onChatMessageListener.onMessage(channelName, chatMessage);
        }
      }
    }, 0, CHAT_MESSAGE_INTERVAL);
  }

  @Override
  public void addOnJoinChannelsRequestListener(OnJoinChannelsRequestListener listener) {

  }

  @Override
  public boolean isDefaultChannel(String channelName) {
    return true;
  }

  @Override
  public void close() {

  }
}
