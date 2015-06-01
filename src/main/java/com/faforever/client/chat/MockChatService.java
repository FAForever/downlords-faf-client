package com.faforever.client.chat;

import com.faforever.client.user.UserService;
import com.faforever.client.util.Callback;
import com.faforever.client.util.ConcurrentUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.concurrent.Task;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MockChatService implements ChatService {

  private static final long CONNECTION_DELAY = 5000;
  public static final int CHAT_MESSAGE_INTERVAL = 5000;
  private final Timer timer;

  @Autowired
  UserService userService;

  private Collection<OnMessageListener> onMessageListeners;
  private Collection<OnConnectedListener> onConnectedListeners;
  private Collection<OnChatUserListListener> onUserListListeners;
  private Collection<OnChatDisconnectedListener> onDisconnectedListeners;
  private Collection<OnPrivateMessageListener> onPrivateMessageListeners;
  private Collection<OnUserJoinedChannelListener> onChannelJoinedListeners;
  private Collection<OnChatUserLeftListener> onChatUserLeftListeners;
  private Map<String, ObservableSet<ChatUser>> channelUserListListeners;

  public MockChatService() {
    onMessageListeners = new ArrayList<>();
    onConnectedListeners = new ArrayList<>();
    onUserListListeners = new ArrayList<>();
    onDisconnectedListeners = new ArrayList<>();
    onPrivateMessageListeners = new ArrayList<>();
    onChannelJoinedListeners = new ArrayList<>();
    onChatUserLeftListeners = new ArrayList<>();
    channelUserListListeners = new HashMap<>();

    timer = new Timer(true);
  }

  @Override
  public void addOnMessageListener(OnMessageListener listener) {
    onMessageListeners.add(listener);
  }

  @Override
  public void addOnConnectedListener(OnConnectedListener listener) {
    onConnectedListeners.add(listener);
  }

  @Override
  public void addOnUserListListener(OnChatUserListListener listener) {
    onUserListListeners.add(listener);
  }

  @Override
  public void addOnDisconnectedListener(OnChatDisconnectedListener listener) {
    onDisconnectedListeners.add(listener);
  }

  @Override
  public void addOnPrivateMessageListener(OnPrivateMessageListener listener) {
    onPrivateMessageListeners.add(listener);
  }

  @Override
  public void addOnUserJoinedChannelListener(OnUserJoinedChannelListener listener) {
    onChannelJoinedListeners.add(listener);
  }

  @Override
  public void addOnChatUserLeftListener(OnChatUserLeftListener listener) {
    onChatUserLeftListeners.add(listener);
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
    onConnectedListeners.forEach(OnConnectedListener::onConnected);
    joinChannel("#aeolus");
  }

  @Override
  public void sendMessageInBackground(String target, String message, Callback<String> callback) {
    ConcurrentUtil.executeInBackground(new Task<String>() {
      @Override
      protected String call() throws Exception {
        Thread.sleep(200);
        return message;
      }
    }, callback);
  }

  @Override
  public ObservableSet<ChatUser> getChatUsersForChannel(String channelName) {
    return FXCollections.emptyObservableSet();
  }

  @Override
  public void addChannelUserListListener(String channelName, SetChangeListener<ChatUser> listener) {
    channelUserListListeners.putIfAbsent(channelName, FXCollections.observableSet());
    channelUserListListeners.get(channelName).addListener(listener);
  }

  @Override
  public void leaveChannel(String channelName) {

  }

  @Override
  public void sendAction(String target, String action, Callback<String> callback) {

  }

  @Override
  public void joinChannel(String channelName) {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        ChatUser chatUser = new ChatUser(userService.getUsername());
        ChatUser mockUser = new ChatUser("MockUser");

        for (OnUserJoinedChannelListener onChannelJoinedListener : onChannelJoinedListeners) {
          onChannelJoinedListener.onUserJoinedChannel(channelName, chatUser);
          onChannelJoinedListener.onUserJoinedChannel(channelName, mockUser);
        }

        channelUserListListeners.get(channelName).add(chatUser);
        channelUserListListeners.get(channelName).add(mockUser);

        return null;
      }
    });

    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        for (OnMessageListener onMessageListener : onMessageListeners) {
          ChatMessage chatMessage = new ChatMessage(Instant.now(), "Mock User", "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam");
          onMessageListener.onMessage(channelName, chatMessage);
        }
      }
    }, 0, CHAT_MESSAGE_INTERVAL);
  }
}
