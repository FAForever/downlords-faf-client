package com.faforever.client.chat;

import com.faforever.client.util.Callback;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

import java.util.Collection;

public interface ChatService {

  void addOnMessageListener(OnMessageListener listener);

  void addOnConnectedListener(OnConnectedListener listener);

  void addOnUserListListener(OnChatUserListListener listener);

  void addOnDisconnectedListener(OnChatDisconnectedListener listener);

  void addOnPrivateMessageListener(OnPrivateMessageListener listener);

  void addOnChannelJoinedListener(OnChannelJoinedListener listener);

  void addOnChatUserLeftListener(OnChatUserLeftListener listener);

  void connect();

  void sendMessage(String target, String message);

  /**
   * Gets the list of chat users for the given channel as soon as it is available.
   * <p>
   * <strong>IMPORTANT:</strong> All operations on the returned list must be synchronized, even iteration. Use the map as monitor.
   * </p>
   */
  ObservableSet<ChatUser> getChatUsersForChannel(String channelName);

  void addChannelUserListListener(String channelName, SetChangeListener<ChatUser> listener);
}
