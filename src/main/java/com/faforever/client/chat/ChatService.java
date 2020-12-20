package com.faforever.client.chat;

import com.faforever.client.net.ConnectionState;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.MapChangeListener;

import java.util.concurrent.CompletableFuture;

public interface ChatService {

  void connect();

  void disconnect();

  CompletableFuture<String> sendMessageInBackground(String target, String message);

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

  void leaveChannel(String channelName);

  CompletableFuture<String> sendActionInBackground(String target, String action);

  void joinChannel(String channelName);

  boolean isDefaultChannel(String channelName);

  void close();

  ReadOnlyObjectProperty<ConnectionState> connectionStateProperty();

  void reconnect();

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
