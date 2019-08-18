package com.faforever.client.chat.jan;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.faforever.client.chat.Channel;
import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.OldChatService;
import com.faforever.client.chat.PircBotXChatService;
import com.faforever.client.net.ConnectionState;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.MapChangeListener;

/**
 * @deprecated Use wrapped services directly.
 * @see {@link ChatRoomService}, {@link ChatServiceImpl}, {@link ChatConnector}
 */
@Deprecated
@Lazy
@Service
public class ChatServiceAdapter implements OldChatService, InitializingBean, DisposableBean {

  private final ChatServiceImpl chatService;
  private final ChatRoomService chatRoomService;
  
  // TODO replace class with ChatConnector interface
  private final PircBotXChatService chatConnector;
  
  
  public ChatServiceAdapter(ChatServiceImpl chatService, ChatRoomService chatRoomService, PircBotXChatService chatConnector) {
    this.chatService = chatService;
    this.chatRoomService = chatRoomService;
    this.chatConnector = chatConnector;
  }

  @Override
  public void destroy() throws Exception {
    close();
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    chatService.afterPropertiesSet();
  }

  @Override
  public void connect() {
    chatConnector.connect();
  }

  @Override
  public void disconnect() {
    chatConnector.disconnect();
  }

  @Override
  public void reconnect() {
    chatConnector.reconnect();
  }

  @Override
  public Channel getOrCreateChannel(String channelName) {
    return chatRoomService.getOrCreateChannel(channelName);
  }

  @Override
  public ChatChannelUser getOrCreateChatUser(String username, String channel, boolean isModerator) {
    return chatRoomService.getOrCreateChatUser(username, channel, isModerator);
  }

  @Override
  public void addUsersListener(String channelName, MapChangeListener<String, ChatChannelUser> listener) {
    chatRoomService.addUsersListener(channelName, listener);
  }

  @Override
  public void addChatUsersByNameListener(MapChangeListener<String, ChatChannelUser> listener) {
    chatRoomService.addChatUsersByNameListener(listener);
  }

  @Override
  public void addChannelsListener(MapChangeListener<String, Channel> listener) {
    chatRoomService.addChannelsListener(listener);
  }

  @Override
  public void removeUsersListener(String channelName, MapChangeListener<String, ChatChannelUser> listener) {
    chatRoomService.removeUsersListener(channelName, listener);
  }

  @Override
  public void joinChannel(String channelName) {
    chatRoomService.joinChannel(channelName);
  }

  @Override
  public void leaveChannel(String channelName) {
    chatRoomService.joinChannel(channelName);
  }

  @Override
  public CompletableFuture<String> sendMessageInBackground(String target, String message) {
    return chatService.sendMessageInBackground(target, message);
  }

  @Override
  public CompletableFuture<String> sendActionInBackground(String target, String action) {
    return chatService.sendActionInBackground(target, action);
  }

  @Override
  public boolean isDefaultChannel(String channelName) {
    return chatConnector.isDefaultChannel(channelName);
  }

  @Override
  public void close() {
    chatConnector.close();
  }

  @Override
  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return chatConnector.connectionStateProperty();
  }

  @Override
  public void whois(String username) {
    if (chatConnector instanceof PircBotXChatService) {
      ((PircBotXChatService)chatConnector).whois(username);
    } else {
      throw new UnsupportedOperationException("Only supported by IRC ChatConnector!");
    }
  }

  @Override
  public void incrementUnreadMessagesCount(int delta) {
    chatService.incrementUnreadMessagesCount(delta);
    
  }

  @Override
  public ReadOnlyIntegerProperty unreadMessagesCount() {
    // TODO what is this even, its not used by anything
    throw new UnsupportedOperationException("Not yet implemented!");
  }

  @Override
  public ChatChannelUser getChatUser(String username, String channelName) {
    return chatRoomService.getChatUser(username, channelName);
  }

  @Override
  public String getDefaultChannelName() {
    return chatConnector.getDefaultChannelName();
  }
}
