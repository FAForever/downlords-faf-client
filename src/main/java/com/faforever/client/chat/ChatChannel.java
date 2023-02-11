package com.faforever.client.chat;

import com.faforever.client.fx.JavaFxUtil;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.List;
import java.util.Optional;

@Value
@RequiredArgsConstructor
public class ChatChannel {

  String name;

  ObservableMap<String, ChatChannelUser> usernameToChatUser = FXCollections.synchronizedObservableMap(FXCollections.observableHashMap());
  ObservableList<ChatChannelUser> users = JavaFxUtil.attachListToMap(FXCollections.observableArrayList(), usernameToChatUser);
  ObjectProperty<ChannelTopic> topic = new SimpleObjectProperty<>(new ChannelTopic("", ""));

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

  public boolean containsUser(ChatChannelUser user) {
    return usernameToChatUser.containsValue(user);
  }

  public void addUsersListeners(MapChangeListener<String, ChatChannelUser> listener) {
    JavaFxUtil.addListener(usernameToChatUser, listener);
  }

  public void removeUserListener(MapChangeListener<String, ChatChannelUser> listener) {
    usernameToChatUser.removeListener(listener);
  }

  /**
   * Returns an unmodifiable copy of the current users.
   */
  public ObservableList<ChatChannelUser> getUsers() {
    return users;
  }

  public int getUserCount() {
    return users.size();
  }

  public Optional<ChatChannelUser> getUser(String username) {
    return Optional.ofNullable(usernameToChatUser.get(username));
  }

  public String getName() {
    return name;
  }
}
