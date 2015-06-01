package com.faforever.client.chat;

import com.faforever.client.util.Callback;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

public interface ChatService {

  void addOnMessageListener(OnMessageListener listener);

  void addOnConnectedListener(OnConnectedListener listener);

  void addOnUserListListener(OnChatUserListListener listener);

  void addOnDisconnectedListener(OnChatDisconnectedListener listener);

  void addOnPrivateMessageListener(OnPrivateMessageListener listener);

  void addOnUserJoinedChannelListener(OnUserJoinedChannelListener listener);

  void addOnChatUserLeftListener(OnChatUserLeftListener listener);

  void connect();

  void sendMessageInBackground(String target, String message, Callback<String> callback);

  /**
   * Gets the list of chat users for the given channel as soon as it is available.
   * <p>
   * <strong>IMPORTANT:</strong> All operations on the returned list must be synchronized, even iteration. Use the map as monitor.
   * </p>
   */
  ObservableSet<ChatUser> getChatUsersForChannel(String channelName);

  void addChannelUserListListener(String channelName, SetChangeListener<ChatUser> listener);

  void leaveChannel(String channelName);

  void sendAction(String target, String action, Callback<String> callback);

  void joinChannel(String channelName);
}
