package com.faforever.client.chat;

import com.faforever.client.fx.JavaFxUtil;
import javafx.beans.property.ReadOnlySetWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Value
public class ChatChannel {

  ObservableMap<String, ChatChannelUser> users;
  StringProperty topic;
  String name;
  ObservableSet<String> moderators;

  public ChatChannel(String name) {
    this.name = name;
    users = FXCollections.synchronizedObservableMap(FXCollections.observableHashMap());
    moderators = FXCollections.observableSet();
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
    if (moderators.contains(user.getUsername())) {
      user.setModerator(true);
    }
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

  public void addModerator(String username) {
    Optional.ofNullable(users.get(username)).ifPresent(user -> user.setModerator(true));
    synchronized (moderators) {
      moderators.add(username);
    }
  }

  public void removeModerator(String username) {
    Optional.ofNullable(users.get(username)).ifPresent(user -> user.setModerator(false));
    synchronized (moderators) {
      moderators.remove(username);
    }
  }

  public ReadOnlySetWrapper<String> getModerators() {
    return new ReadOnlySetWrapper<>(moderators);
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
