package com.faforever.client.chat;

import com.faforever.client.fx.JavaFxUtil;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class ChatChannel {

  @Getter
  @EqualsAndHashCode.Include
  @ToString.Include
  private final String name;

  private final ObservableMap<String, ChatChannelUser> usernameToChatUser = FXCollections.synchronizedObservableMap(
      FXCollections.observableHashMap());
  private final ObservableList<ChatChannelUser> users = JavaFxUtil.attachListToMap(
      FXCollections.synchronizedObservableList(FXCollections.observableArrayList(
          item -> new Observable[]{item.categoryProperty(), item.colorProperty(), item.typingProperty()})),
      usernameToChatUser);
  private final ObservableList<ChatChannelUser> typingUsers = new FilteredList<>(users, ChatChannelUser::isTyping);
  private final ObjectProperty<ChannelTopic> topic = new SimpleObjectProperty<>(new ChannelTopic(null, ""));
  private final Set<Consumer<ChatMessage>> messageListeners = new HashSet<>();
  private final List<ChatMessage> messages = new ArrayList<>();
  private final BooleanProperty open = new SimpleBooleanProperty();

  private int maxNumMessages = Integer.MAX_VALUE;

  public void setMaxNumMessages(int maxNumMessages) {
    this.maxNumMessages = maxNumMessages;
    if (messages.size() > maxNumMessages) {
      messages.subList(0, messages.size() - maxNumMessages).clear();
    }
  }

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

  @VisibleForTesting
  void addUser(ChatChannelUser user) {
    usernameToChatUser.put(user.getUsername(), user);
  }

  public ChatChannelUser createUserIfNecessary(String username, Consumer<ChatChannelUser> userInitializer) {
    return usernameToChatUser.computeIfAbsent(username, name -> {
      ChatChannelUser chatChannelUser = new ChatChannelUser(name, this);
      userInitializer.accept(chatChannelUser);
      return chatChannelUser;
    });
  }

  public void clearUsers() {
    usernameToChatUser.clear();
  }

  public void addUsersListeners(ListChangeListener<ChatChannelUser> listener) {
    users.addListener(listener);
  }

  public void removeUserListener(ListChangeListener<ChatChannelUser> listener) {
    users.removeListener(listener);
  }

  public void addTypingUsersListener(ListChangeListener<ChatChannelUser> listener) {
    typingUsers.addListener(listener);
  }

  public void removeTypingUserListener(ListChangeListener<ChatChannelUser> listener) {
    typingUsers.removeListener(listener);
  }

  public ObservableList<ChatChannelUser> getTypingUsers() {
    return FXCollections.unmodifiableObservableList(typingUsers);
  }

  public ObservableList<ChatChannelUser> getUsers() {
    return FXCollections.unmodifiableObservableList(users);
  }

  public Optional<ChatChannelUser> getUser(String username) {
    return Optional.ofNullable(usernameToChatUser.get(username));
  }

  public void addMessage(ChatMessage message) {
    messages.add(message);
    messageListeners.forEach(chatMessageConsumer -> chatMessageConsumer.accept(message));
    if (messages.size() > maxNumMessages) {
      messages.remove(0);
    }
  }

  public void addMessageListener(Consumer<ChatMessage> messageListener) {
    messageListeners.add(messageListener);
    messages.forEach(messageListener);
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

  public boolean isOpen() {
    return open.get();
  }

  public BooleanProperty openProperty() {
    return open;
  }

  public void setOpen(boolean open) {
    this.open.set(open);
  }
}
