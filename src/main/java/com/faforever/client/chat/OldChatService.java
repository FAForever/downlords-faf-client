package com.faforever.client.chat;

import com.faforever.client.chat.jan.ChatConnector;
import com.faforever.client.chat.jan.ChatRoomService;
import com.faforever.client.chat.jan.ChatServiceAdapter;
import com.faforever.client.chat.jan.ChatServiceImpl;
import com.faforever.client.net.ConnectionState;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.MapChangeListener;

import java.util.concurrent.CompletableFuture;

/**
 * 
 * @deprecated Use services wrapped by {@link ChatServiceAdapter} directly instead of this interface:
 * {@link ChatRoomService}, {@link ChatServiceImpl}, {@link ChatConnector}
 */
@Deprecated
public interface OldChatService {

  void connect();

  void disconnect();
  
  void reconnect();

  /**
   * Gets the list of chat users for the given channel as soon as it is available. <p> <strong>IMPORTANT:</strong> All
   * operations on the returned list must be synchronized, even iteration. Use the map as monitor. </p>
   */
  Channel getOrCreateChannel(String channelName);

  ChatChannelUser getOrCreateChatUser(String username, String channel, boolean isModerator);

  void addUsersListener(String channelName, MapChangeListener<String, ChatChannelUser> listener);

  void addChatUsersByNameListener(MapChangeListener<String, ChatChannelUser> listener);

  void addChannelsListener(MapChangeListener<String, Channel> listener);

  void removeUsersListener(String channelName, MapChangeListener<String, ChatChannelUser> listener);

  void joinChannel(String channelName);
  
  void leaveChannel(String channelName);

  CompletableFuture<String> sendMessageInBackground(String target, String message);
  
  CompletableFuture<String> sendActionInBackground(String target, String action);

  boolean isDefaultChannel(String channelName);

  void close();

  ReadOnlyObjectProperty<ConnectionState> connectionStateProperty();

  void whois(String username);

  /**
   * Increase or decrease the number of unread messages.
   *
   * @param delta a positive or negative number
   */
  void incrementUnreadMessagesCount(int delta);

  ReadOnlyIntegerProperty unreadMessagesCount();

  ChatChannelUser getChatUser(String username, String channelName);

  String getDefaultChannelName();
}
