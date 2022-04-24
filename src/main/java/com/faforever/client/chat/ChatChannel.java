package com.faforever.client.chat;

import com.faforever.client.fx.JavaFxUtil;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Value
@RequiredArgsConstructor
public class ChatChannel {

  ObservableMap<String, ChatChannelUser> users = FXCollections.synchronizedObservableMap(FXCollections.observableHashMap());
  ObjectProperty<ChannelTopic> topic = new SimpleObjectProperty<>(new ChannelTopic("", ""));
  String name;

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

  public boolean containsUser(ChatChannelUser user) {
    return users.containsValue(user);
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

  public int getUserCount() {
    return users.size();
  }

  public ChatChannelUser getUser(String username) {
    return users.get(username);
  }

  public String getName() {
    return name;
  }
}
