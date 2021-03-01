package com.faforever.client.chat;

import com.faforever.client.fx.JavaFxUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Value
public class ChatChannel {

  ObservableMap<String, ChatChannelUser> users;
  StringProperty topic;
  String name;

  public ChatChannel(String name) {
    this.name = name;
    users = FXCollections.synchronizedObservableMap(FXCollections.observableHashMap());
    topic = new SimpleStringProperty();
  }

  public String getTopic() {
    return topic.get();
  }

  public void setTopic(String topic) {
    this.topic.set(topic);
  }

  public StringProperty topicProperty() {
    return topic;
  }

  public ChatChannelUser removeUser(String username) {
    return users.remove(username);
  }

  public void addUsers(List<ChatChannelUser> users) {
    users.forEach(this::addUser);
  }

  public void addUser(ChatChannelUser user) {
    users.put(user.getUsername(), user);
  }

  public void clearUsers() {
    users.clear();
  }

  public void addUsersListeners(MapChangeListener<String, ChatChannelUser> listener) {
    JavaFxUtil.addListener(users, listener);
  }

  public void removeUserListener(MapChangeListener<String, ChatChannelUser> listener) {
    users.removeListener(listener);
  }

  /**
   * Returns an unmodifiable copy of the current users.
   */
  public List<ChatChannelUser> getUsers() {
    return Collections.unmodifiableList(new ArrayList<>(users.values()));
  }

  public ChatChannelUser getUser(String username) {
    return users.get(username);
  }

  public String getName() {
    return name;
  }
}
