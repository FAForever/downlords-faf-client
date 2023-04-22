package com.faforever.client.chat;

import com.faforever.client.fx.JavaFxUtil;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class ChatChannel {

  @Getter
  private final String name;

  private final ObservableMap<String, ChatChannelUser> usernameToChatUser = FXCollections.synchronizedObservableMap(FXCollections.observableHashMap());
  private final ObservableList<ChatChannelUser> users = JavaFxUtil.attachListToMap(FXCollections.synchronizedObservableList(FXCollections.observableArrayList(item -> new Observable[]{item.categoriesProperty(), item.colorProperty(), item.moderatorProperty()})), usernameToChatUser);
  private final ObjectProperty<ChannelTopic> topic = new SimpleObjectProperty<>(new ChannelTopic("", ""));
  private final Set<Consumer<ChatMessage>> messageListeners = new HashSet<>();
  private final List<ChatMessage> unprocessedMessages = new ArrayList<>();

  public ChannelTopic getTopic() {
    return topic.get();
  }

  public void setTopic(ChannelTopic topic) {
    this.topic.set(topic);
  }

  public ObjectProperty<ChannelTopic> topicProperty() {
    return topic;
  }

  public ChatChannelUser removeUser(String username) {
    return usernameToChatUser.remove(username);
  }

  public void addUsers(List<ChatChannelUser> users) {
    users.forEach(this::addUser);
  }

  public void addUser(ChatChannelUser user) {
    usernameToChatUser.put(user.getUsername(), user);
  }

  public void clearUsers() {
    usernameToChatUser.clear();
  }

  public void addUsersListeners(ListChangeListener<ChatChannelUser> listener) {
    JavaFxUtil.addListener(users, listener);
  }

  public void removeUserListener(ListChangeListener<ChatChannelUser> listener) {
    JavaFxUtil.removeListener(users, listener);
  }

  public ObservableList<ChatChannelUser> getUsers() {
    return users;
  }

  public Optional<ChatChannelUser> getUser(String username) {
    return Optional.ofNullable(usernameToChatUser.get(username));
  }

  public void addMessage(ChatMessage message) {
    if (messageListeners.isEmpty()) {
      unprocessedMessages.add(message);
    }
    messageListeners.forEach(chatMessageConsumer -> chatMessageConsumer.accept(message));
  }

  public void addMessageListener(Consumer<ChatMessage> messageListener) {
    messageListeners.add(messageListener);
    drainUnprocessed();
  }

  private void drainUnprocessed() {
    Iterator<ChatMessage> unprocessedIterator = unprocessedMessages.iterator();
    while (unprocessedIterator.hasNext()) {
      ChatMessage message = unprocessedIterator.next();
      messageListeners.forEach(chatMessageConsumer -> chatMessageConsumer.accept(message));
      unprocessedIterator.remove();
    }
  }

  public void removeMessageListener(Consumer<ChatMessage> messageListener) {
    messageListeners.remove(messageListener);
  }

  public boolean isPrivateChannel() {
    return !name.startsWith("#");
  }

  public boolean isPartyChannel() {
    return name.endsWith(ChatService.PARTY_CHANNEL_SUFFIX);
  }
}
