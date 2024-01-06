package com.faforever.client.chat;

import com.faforever.client.chat.ChatMessage.Type;
import com.faforever.client.fx.JavaFxUtil;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class ChatChannel {

  private static final Comparator<ChatMessage> CHAT_MESSAGE_COMPARATOR = Comparator.comparing(ChatMessage::getType)
                                                                                   .thenComparing(ChatMessage::getTime,
                                                                                                  Comparator.nullsLast(
                                                                                                      Comparator.naturalOrder()));
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
  private final ObservableList<ChatChannelUser> unmodifiableUsers = FXCollections.unmodifiableObservableList(users);
  private final ObservableList<ChatChannelUser> typingUsers = new FilteredList<>(users, ChatChannelUser::isTyping);
  private final ObjectProperty<ChannelTopic> topic = new SimpleObjectProperty<>(new ChannelTopic(null, ""));
  private final ObservableMap<String, ChatMessage> messagesById = FXCollections.synchronizedObservableMap(
      FXCollections.observableHashMap());
  private final ObservableList<ChatMessage> rawMessages = JavaFxUtil.attachListToMap(
      FXCollections.synchronizedObservableList(FXCollections.observableArrayList()), messagesById);
  private final ObservableList<ChatMessage> messages = FXCollections.synchronizedObservableList(
      new SortedList<>(rawMessages, CHAT_MESSAGE_COMPARATOR));
  private final BooleanProperty open = new SimpleBooleanProperty();
  private final IntegerProperty maxNumMessages = new SimpleIntegerProperty(Integer.MAX_VALUE);
  private final IntegerProperty numUnreadMessages = new SimpleIntegerProperty();

  public ChatChannel(String name) {
    this.name = name;
    maxNumMessages.subscribe(this::pruneMessages);
    open.subscribe(open -> {
      if (open) {
        setNumUnreadMessages(0);
      }
    });
  }

  private void pruneMessages() {
    int maxNumMessages = getMaxNumMessages();
    int numMessages = messages.size();
    if (numMessages > maxNumMessages) {
      List.copyOf(messages.subList(0, numMessages - maxNumMessages))
          .forEach(message -> messagesById.remove(message.getId()));
    }
  }

  public int getNumUnreadMessages() {
    return numUnreadMessages.get();
  }

  public IntegerProperty numUnreadMessagesProperty() {
    return numUnreadMessages;
  }

  public void setNumUnreadMessages(int numUnreadMessages) {
    this.numUnreadMessages.set(numUnreadMessages);
  }

  public void setMaxNumMessages(int maxNumMessages) {
    this.maxNumMessages.set(Math.max(maxNumMessages, 0));
    pruneMessages();
  }

  public int getMaxNumMessages() {
    return maxNumMessages.get();
  }

  public IntegerProperty maxNumMessagesProperty() {
    return maxNumMessages;
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
    List.copyOf(usernameToChatUser.keySet()).forEach(this::removeUser);
  }

  public ObservableList<ChatChannelUser> getTypingUsers() {
    return typingUsers;
  }

  public ObservableList<ChatChannelUser> getUsers() {
    return unmodifiableUsers;
  }

  public Optional<ChatChannelUser> getUser(String username) {
    return Optional.ofNullable(usernameToChatUser.get(username));
  }

  public void removePendingMessage(String messageId) {
    messagesById.computeIfPresent(messageId,
                                  (ignored, chatMessage) -> chatMessage.getType() == Type.PENDING ? null : chatMessage);
  }

  public void addMessage(ChatMessage message) {
    messagesById.put(message.getId(), message);
    pruneMessages();
  }

  public ObservableList<ChatMessage> getMessages() {
    return messages;
  }

  public boolean isPrivateChannel() {
    return !name.startsWith("#");
  }

  public boolean isPartyChannel() {
    return !isPrivateChannel() && name.endsWith(ChatService.PARTY_CHANNEL_SUFFIX);
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
