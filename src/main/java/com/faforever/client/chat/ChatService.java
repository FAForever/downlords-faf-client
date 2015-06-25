package com.faforever.client.chat;

import com.faforever.client.util.Callback;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;

public interface ChatService {

  void addOnMessageListener(OnChatMessageListener listener);

  void addOnChatConnectedListener(OnChatConnectedListener listener);

  void addOnUserListListener(OnChatUserListListener listener);

  void addOnChatDisconnectedListener(OnChatDisconnectedListener listener);

  void addOnPrivateChatMessageListener(OnPrivateChatMessageListener listener);

  void addOnChatUserJoinedChannelListener(OnChatUserJoinedChannelListener listener);

  void addOnChatUserLeftChannelListener(OnChatUserLeftChannelListener listener);

  void addOnModeratorSetListener(OnModeratorSetListener listener);

  void addOnChatUserQuitListener(OnChatUserQuitListener listener);

  void connect();

  void sendMessageInBackground(String target, String message, Callback<String> callback);

  /**
   * Gets the list of chat users for the given channel as soon as it is available.
   * <p>
   * <strong>IMPORTANT:</strong> All operations on the returned list must be synchronized, even iteration. Use the map as monitor.
   * </p>
   */
  ObservableMap<String, ChatUser> getChatUsersForChannel(String channelName);

  void addChannelUserListListener(String channelName, MapChangeListener<String, ChatUser> listener);

  void leaveChannel(String channelName);

  void sendActionInBackground(String target, String action, Callback<String> callback);

  void joinChannel(String channelName);
}
