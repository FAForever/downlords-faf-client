package com.faforever.client.chat;

import com.faforever.client.fx.JavaFxUtil;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.List;
import java.util.Optional;

@Value
@RequiredArgsConstructor
public class ChatChannel {

  String name;

  @Getter(AccessLevel.NONE)
  ObservableMap<String, ChatChannelUser> usernameToChatUser = FXCollections.synchronizedObservableMap(FXCollections.observableHashMap());
  ObservableList<ChatChannelUser> users = JavaFxUtil.attachListToMap(FXCollections.synchronizedObservableList(FXCollections.observableArrayList(item -> new Observable[]{item.categoriesProperty(), item.colorProperty(), item.moderatorProperty()})), usernameToChatUser);
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

  public boolean containsUser(String username) {
    return usernameToChatUser.containsKey(username);
  }

  public void addUsersListeners(MapChangeListener<String, ChatChannelUser> listener) {
    JavaFxUtil.addListener(usernameToChatUser, listener);
  }

  public void removeUserListener(MapChangeListener<String, ChatChannelUser> listener) {
    usernameToChatUser.removeListener(listener);
  }

  /**
   * Returns an unmodifiable observable list of current users.
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
