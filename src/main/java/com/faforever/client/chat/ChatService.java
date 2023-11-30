package com.faforever.client.chat;

import com.faforever.client.net.ConnectionState;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.MapChangeListener;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface ChatService {

  String PARTY_CHANNEL_SUFFIX = "'sParty";

  void connect();

  void disconnect();

  CompletableFuture<Void> sendMessageInBackground(ChatChannel chatChannel, String message);

  boolean userExistsInAnyChannel(String username);

  ChatChannel getOrCreateChannel(String channelName);

  ChatChannelUser getOrCreateChatUser(String username, String channel);

  void addChannelsListener(MapChangeListener<String, ChatChannel> listener);

  void removeChannelsListener(MapChangeListener<String, ChatChannel> listener);

  default void leaveChannel(String channelName) {
    leaveChannel(getOrCreateChannel(channelName));
  }

  void leaveChannel(ChatChannel channel);

  CompletableFuture<Void> sendActionInBackground(ChatChannel chatChannel, String action);

  void joinChannel(String channelName);

  boolean isDefaultChannel(ChatChannel chatChannel);

  void close();

  ReadOnlyObjectProperty<ConnectionState> connectionStateProperty();

  ConnectionState getConnectionState();

  void reconnect();

  void whois(String username);

  void setChannelTopic(ChatChannel chatChannel, String text);

  /**
   * Increase or decrease the number of unread messages.
   *
   * @param delta a positive or negative number
   */
  void incrementUnreadMessagesCount(int delta);

  void onInitiatePrivateChat(String username);

  Set<ChatChannel> getChannels();
}
